# Minecraft 26.1.2 Fabric 迁移 Phase 24 报告

## 1. 结论

Phase 24 已完成三个限定范围内的体验修复：

1. Album 添加照片时的库存面板不再从 512×512 GUI atlas 的错误区域取样；已经填入的照片也不会越过 render-state 层级覆盖面板。普通 Album、Signed Album 与 Lectern Album 的照片页、翻页和重载路径均完成运行时验证。
2. `/exposure export` 已恢复。真正的首要故障是 26.1.2 将 overworld `SavedDataStorage` 移到维度目录后，手工扫描仍指向旧的 `world/data/exposures`；此外修复了导出任务完成状态、异步客户端缓存访问、输出路径安全和 dedicated server 命令建议中的客户端类引用。integrated server 的 31/32 张批量导出、零照片专服、单照片专服及 `id x2 negative` 均实际生成了可读取 PNG。
3. 已占用的 `glass_photograph_frame` 不再把专用玻璃模型标记为 invisible；renderer 会提交照片，再提交 `cutoutMovingBlock` 玻璃框模型。无 Shader 与 Iris + Sodium + BSL 环境均可见玻璃边缘/透明结构，普通木框未改动。

最终门禁：57/57 测试通过，0 failure、0 error、0 skipped；common/Fabric compileJava、common compileTestJava、双 Access Widener 和完整 build 通过；integrated client 与 dedicated server 均正常保存退出；残留游戏 JVM 为 0。

新 JAR：

- `D:\Codex\Exposure\phase24-exposure-fabric-26.1.2-1.9.18.jar`
- 大小：2,058,844 bytes
- 修改时间：2026-07-19 02:47:09 +08:00
- SHA-256：`166268BE34F8B9ECF97C3280F565B05DD0E0BD7503D2B06BF45C3194C391D96B`

Phase 23 JAR 未覆盖，哈希仍为 `3751FB1813F5AAA603423EFD993DAE7EDE9BC8D96044836531CA5F4AC395ACD2`。

## 2. 输入、工具链与修改前基线

- 工作区：`D:\Codex\Exposure`
- 工作区不是 Git 仓库。本阶段没有 Git diff、commit、branch 或回滚提交。
- Java：`C:\Program Files\Java\jdk-25.0.3`，Java 25.0.3 LTS。
- Gradle：项目 Wrapper，Gradle 9.4.0。
- ASCII `GRADLE_USER_HOME`：`D:\Codex\Exposure.gradle-home`。
- 未执行 `clean`，未修改用户全局 Java、Gradle、启动器或系统环境。
- 完整读取 Phase 23 和 Phase 22 报告，并定向查阅 Phase 18 GUI/render-state 迁移记录。
- 对照固定上游 Exposure 1.21.1 commit `9383e0c9237099947a1e6399fcace5772f496c7d` 的 Album、ExposureCommand/ExportCommand、ExposureRepository、ImageExporter 与 Photograph Frame 实现。
- 修改前源码/资源清单共 1004 项，证据为 `phase24-prechange-source-resource-manifest.csv`；快照为 `phase24-prechange-source-resource-snapshot.zip`，1,584,157 bytes。
- 非 clean 基线执行 common/Fabric compileJava、common compileTestJava、51 项测试与双 Access Widener，全部通过。日志：`phase24-baseline.log`，摘要：`phase24-baseline-summary.txt`。
- 基线及环境证据：`phase24-environment-and-prechange.txt`。

## 3. bugs 截图只读清单

`D:\Codex\Exposure\bugs` 只读检查完成；没有修改、移动、覆盖截图，也没有将其加入 JAR。

| 文件 | 尺寸 | 大小 | SHA-256 | 归类与观察 |
|---|---:|---:|---|---|
| `玻璃相框无边框错误.png` | 2560×1441 | 8,663,068 | `7FE7FBE439ECDDB946B024687F07651EF8B153F761FE3743A03E1F1DE9C0F3CE` | 已占用的 glass frame 只见完整方形照片，没有预期玻璃边缘/框体。 |
| `木相框正常.png` | 2560×1441 | 7,543,638 | `7842EA8687B0E82A65BFCDA157019CBC802E59282FA9994BF72A630E0BA8AF89` | 普通 photograph frame 对照正常，说明照片数据和共享动态图像本身可用。 |
| `相册填充界面错误.png` | 2560×1441 | 4,887,181 | `F06C665999DCF6FAD77AFB533EF6CABFA5F87743448DFACF7D5796BBA78AFD8C` | `AlbumScreen` 的添加照片/库存面板路径；面板显示 atlas 邻区放大片段，符合错误 UV 与 source/destination 混用。 |

完整清单：`phase24-bugs-screenshot-inventory.txt`。

## 4. Group 1：Album 页面和添加照片面板

### 4.1 准确界面与根因

错误截图对应可编辑普通 Album 的 `AlbumScreen`，处于向空页面添加 Photograph 的模式；不是只读 `AlbumViewScreen`，也不是 Signed/Lectern 自身的页面背景。

固定上游旧 `GuiGraphics` 调用为：

- 添加库存背景：destination `(firstSlot.x - 8, firstSlot.y - 18)`，旧 z=`10`，source `(u=0,v=188)`，source/destination `176×100`，atlas `512×512`。
- 禁用槽覆盖：旧 z=`350`，source `(u=176,v=188)`，source/destination `18×18`，atlas `512×512`。

26.1.2 迁移后的调用把旧 z 参数当成了 u：

- 错误背景相当于 `(u=10,v=0)`；
- 错误覆盖相当于 `(u=350,v=176)`。

因此截图中的异常并不是动态照片纹理 atlas、照片 320×320/256×256 尺寸、左右页 UV 或 Phase 22 的短 `blit` 问题，而是旧重载参数顺序在 `GuiGraphicsExtractor` 中发生语义漂移。该结论由用户截图、512×512 `album.png` 资源区域和固定上游调用共同确认。

### 4.2 source/destination/atlas 修复

`AlbumGUI.TextureRegion` 现在明确保存 source `(u,v,width,height)`，生产调用使用完整重载：

```text
destination width/height = region width/height
source width/height      = region width/height
atlas width/height       = 512/512
```

区域为：

- `ADDING_INVENTORY_BACKGROUND = (0,188,176,100)`
- `DISABLED_INVENTORY_SLOT = (176,188,18,18)`

没有把 atlas 512×512 当照片尺寸，没有扩大 source sample，也没有改照片 renderable、Frame、ExposureData 或 Album component。

### 4.3 render-state 层级修复

第一版 UV 修复后，真实已填照片测试暴露第二个独立问题：旧即时渲染的 z=`0` page/photo、z=`10` panel、z=`350` overlay 语义在 extraction 后丢失，页面照片会盖过添加面板。失败截图保留为 `phase24-g1-filled-left-adding-right-layering-failure.png`，没有隐藏或删除。

最终提交顺序：

1. `super.extractContents` 提交页面 widgets 和照片。
2. `nextStratum()` 提交添加库存背景。
3. 再一层重新提交 player inventory label 和 active slot items，使物品仍在面板之上。
4. 再一层提交非 Photograph 的 disabled-slot overlay。

同时将添加面板从 `renderBg` 移出，避免背景阶段早于照片 widget；pose 使用 `leftPos/topPos` 平移恢复 container slot 坐标。没有新增 scissor，没有额外 renderLevel，没有修改照片异步准备流程。

### 4.4 页面、比例和重载矩阵

真实照片 ID `phase22_command_capture` 来自既有有效 Exposure repository，通过正常 Photograph/Album component 和 Album/Lectern UI 路径显示；没有使用空白或伪造图片数据。

| 场景 | 结果 | 证据 |
|---|---|---|
| 自动 GUI、856×512；左页已有照片、右页进入添加模式 | panel 完整，照片在 panel 下，物品/label 在 panel 上 | `phase24-g1-scale-auto-856x512-adding-panel-fixed.png` |
| GUI scale 2、1280×720（16:9） | 连续完整，无 atlas 邻区、放大碎片或裁剪错位 | `phase24-g1-scale2-1280x720-filled-plus-panel.png` |
| GUI scale 3、1280×800（16:10） | 同上 | `phase24-g1-scale3-1280x800-filled-plus-panel.png` |
| GUI scale 4、1280×800（16:10） | 同上 | `phase24-g1-scale4-1280x800-filled-plus-panel.png` |
| Album 页 1–2 左右均有真实照片 | 正常 | `phase24-g1-multipage-pages-1-2.png` |
| Album 页 3–4 左右均有真实照片 | 正常；D 前翻和 A 后翻均工作 | `phase24-g1-multipage-pages-3-4.png` |
| Album 末跨页 5–6 | 末页路径正常，无 atlas/镜像/裁剪错误 | `phase24-g1-multipage-pages-5-6.png` |
| Signed Album 页 1–2 | 共享 `AlbumViewScreen` renderer 正常 | `phase24-g1-signed-album-pages-1-2.png` |
| Lectern Album 页 1–2 | `LecternAlbumScreen` 正常 | `phase24-g1-lectern-album-pages-1-2.png` |
| 保存退出并重进世界后 Lectern Album 页 1–2 | 数据和画面保持 | `phase24-g1-lectern-album-reloaded-pages-1-2.png` |
| Lectern/Signed 末跨页 5–6，6/6 均填充 | 正常 | `phase24-g1-lectern-album-pages-5-6-six-of-six.png` |

多页场景是在既有合法 Album 的真实 page 0 基础上，在受控测试世界内复制其合法 page component 到 page 1–5；随后通过生产签名 UI 得到 Signed Album，并正常放入 Lectern。没有改 Album 保存格式、照片文件或 menu open-data。

所有场景都保持页面编号、spread 顺序和前后翻页语义；普通 Album 与 Lectern Album 都经过关闭/重开，Lectern 还经过世界保存退出/重载。未改变 dormant signed_album server menu 边界。

## 5. Group 2：`/exposure` 命令树和 export

### 5.1 当前生产注册树

根节点 `/exposure` 要求 `COMMANDS_GAMEMASTER`；命令在服务器注册。`export` 另要求 `COMMANDS_ADMIN`。没有注册第二套客户端同名命令。

| 命令 | 参数/子节点 | 环境与作用 |
|---|---|---|
| `/exposure load` | `<capture_properties:compound_tag>` | 玩家服务器 source；解码 CaptureParameters，必须含 projection，向客户端发 LOAD_COMMAND capture。 |
| `/exposure expose` | `<capture_properties:compound_tag>` | 玩家服务器 source；触发真实 EXPOSE_COMMAND capture/upload。 |
| `/exposure export id` | `<id:string> [size:x1..x6] [look:aged|negative|negative_film]` | ADMIN；服务器查 ID 并发 ExportS2CP，客户端执行 PNG 写出；省略 look 为 regular。 |
| `/exposure export all` | `[size:x1..x6] [look:aged|negative|negative_film]` | ADMIN；扫描 repository 全部 ID；大于等于 50 保留上游二次确认语义。 |
| `/exposure export stop` | 无 | 向当前玩家发 ExportStopS2CP。 |
| `/exposure show latest` | `[player] [negative]` 或直接 `negative` | 服务端 FrameHistory + 客户端显示。 |
| `/exposure show id` | `<id:string> [negative]` | repository load 后向客户端发 ExposureDataResponse/Show。 |
| `/exposure show texture` | `<path:identifier> [negative]` | 显示资源纹理；服务器 argument 不再引用 client-only Minecraft。 |
| `/exposure palette export` | `<palette> json|png <file_path:string>` | 保留上游调色板文件导出。 |
| `/exposure palette convert` | `json_to_png|png_to_json <file_path:string>` | 保留上游格式转换。 |
| `/exposure debug clear_rendering_cache` | 无 | 清客户端 rendering cache。 |
| `/exposure debug highlight_entities_in_frame` | 无 | 切换服务器 debug 标志。 |
| `/exposure debug expose_rgb` | 无 | 需要手持相机；触发三通道 debug capture。 |
| `/exposure debug chromatic_from_last_three_exposures` | 无 | 使用最近三张非 chromatic Frame。 |
| `/exposure debug develop_film_in_hand` | `[keep_original]` | 需要手持 film roll。 |

固定上游对照没有 `query`、`delete`、`clear` 或 `reload` 一级命令；因此没有凭名称新增它们，也没有激活历史 `query_exposure_data` packet。

### 5.2 export 的准确失败阶段

修复前实际现象：

- Brigadier 根命令和 `export all` 已注册；权限和参数解析正常。
- `/exposure show id phase22_command_capture` 可以从 `SavedDataStorage` 加载，证明数据存在。
- `/exposure export all` 返回“Exporting 0 exposures”，没有文件。

首个失败边界是 `ExposureRepository.getAllIds()` 的手工文件扫描路径：

```text
错误：<world>/data/exposures
实际 26.1.2 overworld SavedData：<world>/dimensions/minecraft/overworld/data/exposures
```

`load(id)` 走 `server.overworld().getDataStorage()` 所以仍可成功，`getAllIds()` 却扫描旧目录，形成“show 能用、all 为 0”的表面矛盾。

修复后 `ExposureRepository` 以 `DimensionType.getStorageFolder(Level.OVERWORLD, server.getWorldPath(ROOT))` 得到和 overworld `SavedDataStorage` 相同的目录；没有改变 SavedDataType、文件格式或 ID 编码。

### 5.3 其他确定的迁移缺陷

- `ExportExposuresTask.tick()` 原先不在 future 完成后 `setDone()`，Cycles 可把已完成任务长期留在运行集合中；现只在 future done 时完成。
- 后台 export 线程直接访问 `ExposureClient.exposureStore()` 有客户端线程边界风险；现通过 `Minecrft.execute` 回到客户端线程并以 5 秒 timeout 等待。图像转换/PNG 编码仍在后台任务中。
- `TextureLocationArgument` 的服务器命令 suggestions 直接引用 `Minecraft`，dedicated server 在建议/命令树路径可能 class-load 客户端类；现返回空 Suggestions，参数解析仍使用服务器安全的 IdentifierArgument。
- 输出路径现在逐组件清理控制字符、Windows 非法字符、尾部点/空格、`.`/`..`、CON/PRN/AUX/NUL/COM*/LPT*，并在 normalize 后强制 `startsWith(outputFolder)`；安全 ID 子目录语义仍保留。
- 没有硬编码工作区、桌面或绝对本机路径。

### 5.4 输出目录与实际文件证据

沿用固定上游客户端导出语义：相对 Minecraft game directory 的 `exposures`，按世界组织时再加 LevelName 子目录。

- integrated server：`D:\Codex\Exposure\fabric\run\exposures\Phase21A World Entry\`
- dedicated server 连接客户端：`D:\Codex\Exposure\fabric\run\exposures\Minecraft Server\`
- 服务端只发送 ID/size/look；NativeImage 和文件写出位于客户端，不要求 dedicated server 加载客户端图像类。

integrated server 实证：

- 修复后第一次 `export all` 扫描并导出 31 张，截图 `phase24-g2-export-all-31-success.png`。
- `/exposure expose {id:"phase24_command_audit",fov:70.0d}` 生成真实第 32 张，随后 all 可导出 32 张。
- `/exposure export id phase22_command_capture x2 negative` 生成可读取 640×640 RGBA PNG。
- 彩色样本 `phase23_g1_high_sensitivity_color.png`：320×320，75,401 bytes，SHA-256 `346371C51FADADCA4DDEA7A628F31857A06A01162AAF744C1396B92D3C2BBE9D`。
- 黑白样本 `phase23_g1_black_and_white.png`：320×320，60,062 bytes，SHA-256 `454648EB20F71C68E66C85B83032642BAB15473091778C4F9B3E2C919AE50187`。
- 证据：`phase24-g2-export-image-validation.txt`。

dedicated server 连接客户端实证：

- 空 repository：`export all` 显示 Exporting 0 / No exposures were exported，截图 `phase24-g2-dedicated-export-zero-success.png`。
- `/exposure expose {id:"phase24_dedicated_export_test",fov:70.0d}` 在专服 repository 生成真实照片。
- 随后 `export all` 导出 1 张，截图 `phase24-g2-dedicated-export-one-success.png`。
- regular PNG：320×320，134,450 bytes，SHA-256 `8D65FC67AC73738E29309B9031475F8A7A8771E4BE056557AD51C054262197F1`。
- `id ... x2 negative` PNG：640×640，137,222 bytes，SHA-256 `596DF294BA095F43444917D8211CDA598D985A8D4E844F943A8053EFC9317980`。
- 证据：`phase24-g2-dedicated-export-image-validation.txt`、`phase24-g2-dedicated-export-x2-negative-validation.txt`。

有效照片全部成功；缺失 ID 路径会输出逐 ID not_received/error 并继续下一 ID，不会静默终止批次。没有用固定图片或只返回成功文本。

### 5.5 其他命令烟雾审计

| 命令路径 | 结果 |
|---|---|
| `load` 缺 projection | 正确拒绝：Cannot load: missing 'projection' property；无崩溃。 |
| `expose` | integrated 与 dedicated 均实际生成 repository Exposure。 |
| `show latest` | 无 FrameHistory 时给出合理“has not taken any photos”反馈。 |
| `show id` | 已有 ID 正常加载并显示。 |
| `show texture minecraft:stone negative` | 正常打开 texture 显示路径。 |
| `palette export ... json/png` | 生成 `phase24-palette-map-colors.json/png`。 |
| `palette convert json_to_png/png_to_json` | 双向转换成功。 |
| `export stop` | 无当前任务时不崩溃并保持上游语义。 |
| `debug clear_rendering_cache` | 正常。 |
| `debug highlight_entities_in_frame` | 正常切换。 |
| `debug expose_rgb` | 未手持相机时给出正确错误。 |
| `debug chromatic_from_last_three_exposures` | 不足三帧时给出正确错误。 |
| `debug develop_film_in_hand` | 手中非胶卷时给出正确错误。 |

生产命令树注册和 export 参数由 `ExposureCommandTreeTests` 直接调用 `ExposureCommand.register` 锁定，不是只检查字符串常量。

## 6. Group 3：玻璃相框结构

### 6.1 根因和关键决策

资源检查确认 clear frame 的 small/medium/large 专用模型、纹理和 model ID 均存在；不是 missing model 或资源 atlas 失败。实体 type、size、direction、itemRotation 也会进入 `PhotographFrameRenderState`。

真正丢失发生在 extraction 前：`GlassPhotographFrameEntity.isFrameInvisible()` 在有 Item 时返回 true。collector-era renderer 因而：

1. 不收集 `CLEAR_PHOTOGRAPH_FRAME_SMALL/MEDIUM/LARGE` model parts；
2. `frameInvisible=true` 令 photo border 为 0、center 为 0.497；
3. submit 只提交铺满整个 frame size 的照片 quad；
4. 不再执行 glass `cutoutMovingBlock` model pass。

这与用户截图“完整方形照片、没有任何玻璃结构”完全一致。

本阶段按明确完成标准采取最小修复：玻璃框的 invisible 只服从父类真正的 invisible 状态，不再以“有照片”作为 invisible 条件。没有改成木框模型，也没有用任意缩小照片伪造边框。

### 6.2 修复后的提交链

`GlassPhotographFrameEntityRenderer` 继续选择 clear-frame 专用 model：

- size 0：clear small
- size 1：clear medium
- size 2：clear large
- RenderType：`cutoutMovingBlock`

共享 renderer 的生产链为：

1. extraction 保存 direction/size/itemRotation/xRot/yRot。
2. `frameInvisible=false`，收集 clear-frame model parts。
3. 照片像素目标使用 `16*(size+1)-4`；不再使用 invisible 的满尺寸值。
4. submit 时先提交照片；border=`0.125`，z center=`0.48 - configured offset`。
5. 再提交 glass block model，保持透明边缘、厚度和模型朝向。

普通 wood renderer 源码未改；仍选择普通 model 和 `solidMovingBlock`。

### 6.3 运行时矩阵

- 空玻璃框：玻璃/pane 边缘与反射可见。
- 彩色照片：玻璃结构恢复，截图 `phase24-g3-glass-frame-color-structure-fixed.png`。
- 黑白照片 + `ItemRotation:1b`：照片旋转，玻璃仍可见。
- 四个水平朝向：west/east/north/south 受控 summon 烟雾通过。
- UP 地面和 DOWN 顶面：有支撑方块场景不崩溃；朝向 transform 正常。
- 取下/重新装入：空框与 occupied 状态切换正常。
- 区块/世界保存退出与重载：玻璃结构保持。
- 普通木框对照：模型、照片位置和边框未回归。
- 无 Shader：通过。
- Iris Fabric 1.11.2 + Sodium 0.9.1 + BSL 8.4.01.2：shaderpack 日志确认加载，玻璃边缘与照片区域均可见，无整框透明、z-fighting 或照片穿墙。截图 `phase24-g3-iris-bsl-glass-frame-photo.png`。

Iris/Sodium/BSL 仅复制到 `fabric/run` 做受控验证；结束后按精确路径删除临时 run copies，恢复原 `iris.properties` 与 options，没有安装到用户正式实例。证据：`phase24-g3-iris-runtime-copy-hashes.txt`、`phase24-g3-iris-runtime-copy-cleanup.txt`、`phase24-g3-iris-shader-runclient.out.log`。

## 7. 自动化测试

基线 51 项；Phase 24 新增/扩展后共 57 项。

- `ExposureRepositoryPathTests`：锁定生产 helper 必须解析到 `dimensions/minecraft/overworld/data/exposures`。
- `ImageExporterPathTests`：锁定安全子目录保留、目录穿越/非法 Windows 名称清理和 base folder 边界。
- `ExposureCommandTreeTests`：直接注册生产命令树，锁定六个一级节点、export id/all/stop、嵌套 size/look、show/palette/debug 子节点，并解析全部 size/look 值和拒绝无效值。
- `PhotographFrameEntitySynchedDataTests`：新增 occupied glass frame 仍为 visible 的生产实体状态断言。

最终结果从 JUnit XML 汇总：57 tests、0 failures、0 errors、0 skipped、18 suites。

最终日志：`phase24-final-full-gates-build.log`。

## 8. 最终编译、Access Widener、build 与运行时门禁

使用 Java 25、Wrapper、ASCII cache、无 clean 执行：

```text
:common:compileJava
:fabric:compileJava
:common:compileTestJava
:common:test
:common:validateAccessWidener
:fabric:validateAccessWidener
build
```

结果：BUILD SUCCESSFUL。

运行时：

- 多次 controlled runClient 覆盖 Album、命令、glass、GUI scale 与 Shader；均通过界面 Save and Quit/Quit Game 正常退出。
- integrated server 随客户端正常保存退出。
- dedicated server 使用连接客户端 Player560 验证零/一照片 export；最终以本地临时 RCON 发 `stop`，日志包含 Stopping server、Saving players、Saving worlds、三个维度 All chunks are saved、RCON Listener stopped，Gradle BUILD SUCCESSFUL。
- 临时 `online-mode=false`、ops 和 RCON 只用于本机 dev 客户端连接；最终已恢复 `online-mode=true`、`enable-rcon=false`、空 password、`ops.json=[]`。
- `fabric/run/options.txt` 最终恢复 `guiScale:0`、`overrideWidth:0`、`overrideHeight:0`。
- 最终残留 KnotClient/KnotServer/dev-launch-injector 游戏 JVM：0。

专服日志：`phase24-dedicated-connected-op-runserver.out.log`；干净停止应答：`phase24-dedicated-rcon-clean-stop.txt`。

## 9. 精确修改文件

### 生产源码（7）

1. `common/src/main/java/io/github/mortuusars/exposure/client/export/ImageExporter.java`
2. `common/src/main/java/io/github/mortuusars/exposure/client/gui/screen/album/AlbumGUI.java`
3. `common/src/main/java/io/github/mortuusars/exposure/client/gui/screen/album/AlbumScreen.java`
4. `common/src/main/java/io/github/mortuusars/exposure/client/task/ExportExposuresTask.java`
5. `common/src/main/java/io/github/mortuusars/exposure/commands/argument/TextureLocationArgument.java`
6. `common/src/main/java/io/github/mortuusars/exposure/world/entity/GlassPhotographFrameEntity.java`
7. `common/src/main/java/io/github/mortuusars/exposure/world/level/storage/ExposureRepository.java`

### 测试源码（4）

1. 新增 `common/src/test/java/io/github/mortuusars/exposure/client/export/ImageExporterPathTests.java`
2. 新增 `common/src/test/java/io/github/mortuusars/exposure/commands/ExposureCommandTreeTests.java`
3. 修改 `common/src/test/java/io/github/mortuusars/exposure/world/entity/PhotographFrameEntitySynchedDataTests.java`
4. 新增 `common/src/test/java/io/github/mortuusars/exposure/world/level/storage/ExposureRepositoryPathTests.java`

逐文件修改前/后 SHA-256：`phase24-source-resource-changes.csv`。

没有修改生产资源文件、packet、codec、保存格式或 Phase 22/23 Capture/色彩实现。

## 10. 失败尝试与保留证据

所有失败都保留 phase24 前缀证据，没有用隐藏功能或吞异常绕过。

1. 首次 runClient 使用了错误的 `D:\Codex\Exposure\.gradle-home`，未进入有效验证；进程已停止。日志 `phase24-g1-g2-runclient.out.log`。之后全部使用规定的 `D:\Codex\Exposure.gradle-home`。
2. Album 第一版只修 UV；真实左页照片 + 右页添加场景揭示照片覆盖 panel。失败截图 `phase24-g1-filled-left-adding-right-layering-failure.png`，随后实施 stratum 修复。
3. `ImageExporterPathTests` 首轮发现前导 `..` 清理仍可能留下不符合预期的 component；修正后通过。日志 `phase24-g2-compile-test.log` 与 retry。
4. glass test 首轮因裸测试环境的 `Items.PAPER` registry component 未 bind 失败；补齐 Minecraft 测试 bootstrap/bind 后生产断言通过。日志 `phase24-g3-compile-test.log` 与 retry。
5. 命令树测试首轮缺 Minecraft bootstrap/version 初始化；补齐测试启动环境后通过。两次失败和最终通过分别保存在 `phase24-g2-command-tree-test*.log`。
6. 专服第一轮因隐藏 Gradle server 没有可写 console，客户端断开后进程只能停止再配置本地 RCON；最终轮使用 RCON 正常保存停止。第一轮日志单独保留。
7. 首次手写 RCON BinaryWriter packet 因 PowerShell overload 写出错误而被服务器拒绝；第二次改为显式 little-endian byte buffer，认证、stop 和保存全部成功。
8. 多页受控数据最初尝试直接输入 26.1 ItemStack component SNBT，Brigadier 返回 Invalid string contents；改用既有合法 Album page component 的游戏内 `data modify` 复制，不改变格式或照片文件。
9. 多朝向 frame summon 中不受支撑的中间构造曾产生 invalid block-attached entity 日志；补支撑后的有效场景完成验证，没有客户端或服务器崩溃。

## 11. 明确未修改边界

- 未修改任何业务 packet ID、方向、字段顺序或 StreamCodec。
- 未激活 `query_exposure_data`。
- 未改变 Phase 20 menu open-data envelope。
- 未改变 SavedData、ExtraData、Frame、ExposureData、CameraId、Album/Signed Album 保存格式、照片文件格式、像素编码、调色板、曝光算法或胶片艺术参数。
- 未改变 Capture、冲洗、打印、配方、相框同步数据、复杂皮肤或玩家模型。
- 未回退 Phase 22 RenderTarget、GUI 前异步 readback、direct/background 时序；未加入同步 glReadPixels、额外 renderLevel 或屏幕裁剪。
- 未回退 Phase 23 彩色 Capture、PostChain UBO 或 headwear 修复。
- 未把玻璃框替换为木框，未禁用 renderer，未硬编码 Shader Pack。
- 未覆盖 Phase 23 JAR，未安装到用户正式实例。

## 12. 状态交接与人工复测清单

### 已完成

- Album atlas source/destination 参数与 render-state 层级修复。
- 普通/多页/Signed/Lectern/GUI scale/窗口比例/保存重载验证。
- `/exposure` 完整生产命令树审计；export repository、任务生命周期、客户端线程和路径安全修复。
- integrated/dedicated 的零、一、多照片和 id/size/look 实际文件导出。
- glass frame 专用模型恢复，无 Shader 与 Iris+BSL 验证，普通 wood 对照无回归。
- 57/57、双 AW、完整 build、客户端和专服干净退出。
- Phase 24 JAR 与完整证据已保存。

### 关键决策

- Album 不复制 Phase 22 暗房修复；按截图和实际 `album.png` atlas 确认是旧 z 参数被误当 UV，再独立处理 extraction 层级。
- export 保持服务器命令 + 客户端 PNG 编码边界；没有把 NativeImage 移进 dedicated server。
- glass 使用 clear-frame 专用 model 的已有 renderer，只修 entity visible 状态，不重画或替换模型。

### 遗留问题

本阶段三个完成标准均已满足，没有已知阻塞问题。第三方整合包可能仍通过其他 renderer/shader 修改透明排序；当前最接近环境 Iris 1.11.2 + Sodium 0.9.1 + BSL 8.4.01.2 已通过，因此若用户实例仍异常，需要以实际 mod/shader 列表和日志继续定位，不能硬编码单个 pack。

### 下一步

用户将 Phase 24 JAR 放入测试实例，聚焦人工复测：

1. 打开已有填充 Album，检查添加照片面板、首/中/末页、前后翻页、关闭重开与重进世界。
2. 测试 `/exposure export all`、`export id` 的 size/look 分支，并抽查 `show`、`expose`、`load`、`palette`、`debug` 的补全和反馈。
3. 在用户实际 Iris/Shader/整合包中检查空 glass frame、彩色/黑白照片、旋转、各朝向、取下重装和区块重载；同时确认 wood frame 不回归。

