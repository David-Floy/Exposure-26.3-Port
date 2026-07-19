# Minecraft 26.1.2 Fabric 迁移 Phase 22：Capture、冲洗胶卷、暗房预览与配方

日期：2026-07-17（Asia/Shanghai）  
工作区：`D:\Codex\Exposure`  
状态：四个 Group 的实现、最小门禁、最终构建、受控客户端和 dedicated server 验证均已完成。下一步是用户用 Phase 22 JAR 在正式测试实例中进行第三轮人工复测。

## 1. 输入、范围与环境

开始前完整读取：

- `MIGRATION_SOURCE_REPORT_26.1.2_FABRIC_PHASE21B_FIRST_PLAY_FIXES.md`
- `MIGRATION_SOURCE_REPORT_26.1.2_FABRIC_PHASE21_CLIENT_BOOT.md`

按实际问题定向读取：

- `MIGRATION_SOURCE_REPORT_26.1.2_FABRIC_PHASE17_SHADER_RENDERTARGET_SCREENSHOT.md`
- `MIGRATION_SOURCE_REPORT_26.1.2_FABRIC_PHASE18_VIEWFINDER_CAPTURE_SEMANTICS.md`

没有全量重扫其他历史报告。业务意图对照了 Exposure 1.21.1 固定版本 `9383e0c9237099947a1e6399fcace5772f496c7d` 的 Camera Capture、FilmFrameInspectScreen 和 15 个 recipe 资源；26.1.2 的实际边界使用项目缓存中的 deobf Minecraft 类、`javap -c/-p` 和运行期阶段 PNG/日志确认，没有把已删除的旧渲染 API 机械恢复。

环境：

| 项目 | 实际值 |
|---|---|
| OS | Windows 11 10.0 amd64 |
| Java | Oracle JDK 25.0.3，`C:\Program Files\Java\jdk-25.0.3` |
| Gradle Wrapper | `D:\Codex\Exposure\gradlew.bat`，Gradle 9.4.0 |
| `GRADLE_USER_HOME` | 已存在且已验证的 ASCII 路径 `D:\Codex\Exposure.gradle-home` |
| Minecraft | 26.1.2 |
| Fabric Loader | 0.19.3 |
| 项目声明 Fabric API | 0.150.0+26.1.2 |
| 用户测试 Fabric API | 0.151.0+26.1.2 |
| Forge Config API Port | 26.1.4 |

没有修改用户全局 Java、Gradle、Minecraft Launcher 或系统环境，也没有执行 `clean`。工作区不是 Git 仓库；本报告只使用修改前哈希清单、快照、精确文件清单、日志和构建结果描述变化。

## 2. 用户第二轮人工反馈

Phase 21B JAR 的客户端与 dedicated server 都能稳定启动、进入世界并完成相机、胶卷、Camera Stand、闪光灯和基础冲洗流程，没有 Exposure 致命崩溃，但存在四组确定功能缺失：

1. 无 Shader 的照片空白或只有右下角手部；有 Shader 时主要只有天空，世界方块和实体缺失；快门有明显卡顿。
2. 冲洗结果仍显示未冲洗暗盒外观。
3. 暗房点击当前帧进入放大预览后布局错误。
4. dedicated server 有 15 个 Exposure recipe parse error。

严格按 Capture → developed film → 暗房 UI → recipes 顺序推进。Group 1 恢复完整世界画面后才进入其他组。

## 3. 修改前证据与基线

- 环境：`phase22-environment.log`
- 非 clean 基线：`phase22-baseline.log`
- 基线摘要：`phase22-baseline-summary.txt`
- 修改前源码/资源清单：`phase22-prechange-source-resource-manifest.csv`，998 项
- 修改前快照：`phase22-prechange-source-resource-snapshot.zip`
- Phase 21B 保留包：`phase21b-first-play-exposure-fabric-26.1.2-1.9.18.jar`
  - 2,055,454 字节
  - SHA-256 `642E8FB42B7EFFC842AED8CB1EBCC6E0BF29280E71F8E58C45D92497753FC6A4`

基线结果：common/Fabric `compileJava`、common `compileTestJava`、41/41 测试、common/Fabric 双 Access Widener 全部成功。

## 4. Group 1：照片 Capture RenderTarget 正确性

### 4.1 受控场景和路径矩阵

开发客户端没有第三方 Shader 时使用固定世界 `Phase22CaptureWorld`：正午、晴天、草地区域、蓝色混凝土结构、红色染色玻璃、箱子 block entity、命名奶牛和可见天空。典型运行窗口为 854×480、GUI scale 2、FOV 70、无滤镜；player 路径和 Camera Stand 路径分别运行。场景快照保存在 `phase22-g1-controlled-world-pretest-snapshot.zip`。

测试了源码中的真实两条路径：

| 路径 | 任务 | player | Camera Stand | 结果 |
|---|---|---:|---:|---|
| background | `BackgroundScreenshotCaptureTask` | 通过 | 通过 | 天空、地形、不透明/透明方块、普通实体、箱子 BE 完整；无手部/HUD |
| direct | `DirectScreenshotCaptureTask` | 通过 | 通过 | 同上；读取正常帧的主目标，无旧帧 |

Camera Stand 照片中的本地玩家只出现一次：没有缺失，也没有重复绘制第二个玩家。代表证据：

- `phase22-g1-fixed-scene-background-crisp-diagnostics/phase22-g1-auto-background-player.png`
- `phase22-g1-fixed-scene-direct-player-diagnostics/phase22-g1-auto-direct-player.png`
- `phase22-g1-fixed-scene-background-stand-final-diagnostics/phase22-g1-auto-background-stand.png`
- `phase22-g1-fixed-scene-direct-stand-diagnostics/phase22-g1-auto-direct-stand.png`

### 4.2 26.1.2 的完整调用链

1. **服务端创建业务请求。** `CameraItem.release/takePhoto` 在 server thread 计算 camera holder、CameraId、FOV、crop、滤镜、投影、胶片属性、快门和光照；`addNewFrame` 按 Phase 18 语义计算 `EntitiesInFrame`/positions，先把引用目标 exposure ID 的 Frame 写入胶卷，并让 `ExposureRepository` 期待该 ID，最后发送既有 `CaptureStartS2CP`。没有修改 packet ID、方向、字段或 `StreamCodec`。
2. **客户端主线程接收。** `ClientPacketsHandler.startCapture` 选择既有 `CaptureTemplates` 中的 `CameraCaptureTemplate`，创建 task 并放入 `ExposureClient.cycles()`。Capture request 和渲染状态修改都在客户端 render thread 执行。
3. **CaptureAction 生效。** `setCameraEntity`、regular/selfie camera、FOV override、`hideGui`、可选 post-effect 禁用、CaptureShader/filter、gamma 和 flash 按原顺序执行。`hideGui` 同时阻止手部和 HUD 被提取进待拍帧；Camera Stand 使用 stand 作为 camera entity，因此本地玩家作为普通实体进入世界帧。
4. **旧 background 错误边界。** 修改前 task 在当前帧 `FogRenderer.endFrame` 附近被 tick 后，创建并清空 `TextureTarget`，立即调用第二次 `gameRenderer.renderLevel(deltaTracker)`。26.1.2 已将 update/state extraction/render 分离；此时同一帧的 LevelRenderer submit/render-state 已被消费、清理或不再与新的目标绑定。第二次调用没有新的 camera/options/entity/feature extraction，导致目标只收到空白/晚期手部；Iris/Shader 环境仍可能向错误目标写入 sky pass，于是表现为“只有 Shader 天空”。用户现象是诊断假设，阶段导出和 26.1.2 descriptor/调用顺序才是结论依据。
5. **修复后的 background 置备。** task 在当前帧只创建独占 `TextureTarget` 并置为 armed，不再嵌套调用 `renderLevel`。下一次正常帧开始前，camera、FOV、GUI 和 LevelRenderState 已按 CaptureAction 重新提取。
6. **目标接管。** `GameRenderer.render` HEAD 调用 `onRenderFrameStarted`。捕获存活期间，既有 `MinecraftMixin.getMainRenderTarget` 返回独占 `TextureTarget`；因此 26.1.2 正常 render/frame graph 使用同一个目标完成 clear、sky、opaque terrain、block entities、entities、particles/weather、translucent 和后期链。模组不再自行猜测 bind/clear 时序。
7. **PostChain。** vanilla PostChain 及 Exposure `CaptureShader` 都从当前 `Minecraft.getMainRenderTarget()` 读取并写回该捕获目标。阶段哈希显示 Exposure PostChain 前为 `217D50...`，后/最终为 `D43056...`，说明滤镜处理的是完整世界目标而非主屏幕拷贝或 sky-only 目标。
8. **在 GUI 前结束重定向。** 注入点位于 `GuiRenderer.render(GpuBufferSlice)` 调用前。此处先提交 `ScreenshotReadback`，再关闭动态目标重定向；之后 `getMainRenderTarget` 自动恢复原 Minecraft 主目标，GUI/HUD/tooltip 不进入照片。
9. **GPU readback 与所有权。** `ScreenshotReadback.capture` 保持 Phase 17 的 `Screenshot.takeScreenshot` 异步 readback/fence 路径，没有同步 `glReadPixels`。`TextureTarget` 一直存活到 NativeImage callback；成功、异常、取消和 resize/world/resource reset 均有释放/关闭路径。若 future 已完成，迟到的 NativeImage 会关闭；不会复用旧 target 或泄漏 buffer/fence/future。
10. **像素和保存。** common pool 完成 crop/effects、palette conversion 和 `ExposureData` 构造，然后使用原 `ExposureDataC2SP` 上传；服务端按先前 expect 的 ID 编码并保存照片，既有 Frame 继续引用该 ID。没有修改像素编码、调色板、ExposureData、Frame 或文件格式。

direct 路径仍等待至少一个新正常帧，在 render thread 的 `FogRenderer.endFrame` 时点对完整主目标提交相同的异步 Screenshot readback。CaptureAction 让手部/HUD在这帧不可见；direct 没有额外 RenderTarget 所有权。

### 4.3 阶段 PNG 证据

实际业务捕获是诊断编号 `capture-002`；`capture-001` 是登录预热 dummy，不能当作业务失败或成功证据。固定场景 `capture-002` 的 SHA-256：

| 阶段 | 哈希前缀 | 结论 |
|---|---|---|
| frame start | `CA663916...` | 新帧和独占目标已开始 |
| opaque terrain 后 | `59263788...` | 天空、地形和不透明结构已进入 |
| entities + block entities 后 | `5C286F57...` | 实体/箱子阶段改变了目标 |
| translucent 后 | `390814C9...` | 红色染色玻璃等透明内容进入 |
| hand 前/后 | 均为 `217D50E6...` | 手部没有写入照片 |
| Capture PostChain 后 | `D4305687...` | 完整世界经过模组滤镜 |
| final readback | `D4305687...` | 最终读取目标正确 |

完整哈希和 PNG 位于 `phase22-g1-fixed-scene-background-crisp-hashes.txt` 与对应 diagnostics 目录。画面检查确认没有上下颠倒、左右镜像、偏移裁剪或只显示一角。

### 4.4 Shader 兼容

基础路径通过后，建立 production-like 隔离验证：Iris 1.11.2+mc26.1.2、Sodium 0.9.1+mc26.1.2、BSL v8.4.01.2。Iris/Sodium 只用于测试，没有加入 Exposure 生产依赖；临时 run copies 已移到 `phase22-g1-iris-production-like/removed-run-copies`。

background/direct 都得到完整 terrain、entities、block entity 和 BSL 光照/天空，不再 sky-only：

- `phase22-g1-iris-bsl-background-player-diagnostics/phase22-g1-auto-background-player.png`
- `phase22-g1-iris-bsl-direct-player-diagnostics/phase22-g1-auto-direct-player.png`
- `phase22-g1-iris-bsl-load-evidence.txt`

实现没有读取 Iris 私有类、私有 framebuffer 或硬编码 Shader Pack；兼容性来自正常 Minecraft 最终世界目标的正确生命周期。

### 4.5 连拍、重载、持久化与性能

三次 background 连拍，中间执行一次资源重载，结果哈希各不相同：

- `ECE42EA8171CB32EFC5BB8613718898C0DDDF96DC2613AEAF52B1BF7EE28A2E2`
- `3A1591C2D68881CCA77A6FF1349ECEA1F21A81348BA26D37C6B493F883B5F3D0`
- `E4F4DA1F9466CC515D7F1C4FF7700F8DB7F23EC0F7365D6ECFFF3E9A5AA32B05`

没有复用旧帧，资源重载后仍可拍摄。`/exposure expose` 注册后的 `phase22_command_capture.dat` 为 27,037 字节，SHA-256 `037E85FB728B98FDC5B9E9A5F9ECE3E5BAF986039442258F1540293D5AD3787B`；退出并重新进入后仍存在且可被暗房/界面读取。

稳定预热后的计时：

| 阶段 | 观测值 | 线程 |
|---|---:|---|
| background world + postchains | 1.83–2.61 ms | Render thread |
| background GPU readback + NativeImage | 4.27–7.74 ms | Render thread callback |
| background capture total | 12.58–17.37 ms | Render thread |
| direct wait + readback total | 20.93 ms | Render thread |
| pixel → NativeImage | 0.61–14.14 ms | common pool |
| PNG compression/write | 39.61–80.13 ms | common pool |
| effects/crop | 0.034 ms | common pool |
| palette conversion | 86.88 ms | common pool |
| ExposureData build | 0.020 ms | common pool |
| upload submit | 1.636 ms | common pool |

首轮 GPU/资源预热可达到约 92–128 ms，不作为稳定快门数据。旧实现的额外嵌套世界渲染已经移除，这是测量支持的正确性/性能共同修复；没有降低分辨率、质量、滤镜或实体数量。PNG、palette 等较重工作已在后台线程，无证据支持继续改变业务算法，因此未做无依据优化。最终交付前所有阶段导出和计时代码已删除。

Group 1 最小门禁：`phase22-g1-minimum-verification.log`，通过。

已知证据限制：computer-use bootstrap 两次因本机 kernel assets 路径错误失败，无法在修改旧源码前自动点击并保存同场景 pre-fix PNG。没有把预热 dummy 的空白阶段冒充 pre-fix 业务照片。修改前依据为用户原始照片现象、Phase 22 前源码快照、26.1.2 调用边界和修复后逐阶段输出；同场景的修复后、无 Shader/Shader、direct/background 图片均已保存。

## 5. Group 2：冲洗胶卷 ID、组件与模型

### 5.1 实际运行数据

真实 `FilmDevelopingRecipe` audit：

| 类型 | source ID | result ID | result model | translation key | Frame/size | 容器 |
|---|---|---|---|---|---|---:|
| 黑白 | `exposure:black_and_white_film` | `exposure:developed_black_and_white_film` | `exposure:developed_black_and_white_film` | `item.exposure.developed_black_and_white_film` | 1 / 320 | 1 bottle |
| 彩色 | `exposure:color_film` | `exposure:developed_color_film` | `exposure:developed_color_film` | `item.exposure.developed_color_film` | 1 / 320 | 3 bottles |

两条 recipe 的 result ID 从一开始就是 developed item；items JSON、leaf model 和 developed texture 也正确。undeveloped/developed 纹理哈希不同，详见 `phase22-g2-film-texture-hashes.txt`。

### 5.2 根因与修复

根因在 `ComponentTransferringRecipe.transferComponents`：旧迁移复制 `transferIngredientStack.getComponents()` 的完整 resolved prototype map。26.1.2 的 item prototype 包含 `ITEM_MODEL`、`ITEM_NAME` 等身份/外观默认值，因而 source 的未冲洗胶卷模型覆盖到 result stack。物品 registry ID 已是 developed，但视觉仍是暗盒。

改为只应用 `transferIngredientStack.getComponentsPatch()`。这只转移运行期写入的 Frame、frame size、摄影者/extra data 等 authored components；result item 自身 prototype 的 ID、model 和 name 保持不变。没有硬编码 result ID。

黑白和彩色运行审计的完整 patch 都只含 `film_frames` 和 `film_frame_size`，放入暗房仍可读取照片。保存/重进持久化由 Group 1 的 exposure 文件和 Group 3 的真实 Lightroom menu 共同验证。

测试加强了 result identity、模型/name 不被 source prototype 替换、Frame 保留、source stack 不变、result count、玻璃瓶 remainder、错误 source/缺少药水/多余输入拒绝。Group 2 证据：

- `phase22-g2-film-runtime-audit-summary.txt`
- `phase22-g2-film-runtime-audit-latest.log`
- `phase22-g2-film-developing-tests-retry.log`

## 6. Group 3：暗房帧放大预览

### 6.1 真实复现

使用简体中文、真实 Lightroom block/menu/screen 和包含两张照片的 `developed_color_film`：第一张为持久化的 `phase22_command_capture` 横向照片，第二张为不同内容的 diamond-block 资源照片。程序只自动执行与用户相同的输入：打开暗房、点击当前帧、换帧、继续放大、右键缩回、重开暗房；截图在 GUI 实际提交后读取。

修复前 scale 2 / 1280×720 的 `phase22-g3-ui-scale2-1280x720-postgui/phase22-g3-02-enlarged-default.png` 显示胶片边框被拆成左上、左下、右上等不连续大块，照片本身仍居中，说明不是 texture future、Frame 索引或状态转换失败。

### 6.2 根因

26.1.2 `GuiGraphicsExtractor.blit` 的八个整数参数重载把目标宽高和 source sample 宽高视为同一数值。旧迁移传入 `scaledSize` 后，不仅放大了 78×78 胶片边框，还从 256×256 atlas 采样 `scaledSize × scaledSize`；透明区和相邻图块一起进入，导致边框分裂。

`javap` 证据：

- `phase22-g3-javap-GuiGraphicsExtractor-blit.log`
- `phase22-g3-javap-GuiGraphicsExtractor-bytecode.log`

这不是 scissor 泄漏、pose 未恢复、framebuffer/GUI 尺寸混用、纹理 future 未完成或重复层提交。

### 6.3 修复与矩阵

两次边框 blit 改用完整重载，明确区分：

- destination：`scaledSize × scaledSize`
- source sprite：`BG_SIZE × BG_SIZE`，即 78×78
- atlas：256×256
- 第二层保留既有 `filmTint`

只修改 `FilmFrameInspectScreen` 的预览边框采样；未改照片像素、Capture crop、打印结果、Frame 格式、Lightroom menu/open-data 顺序。

验证矩阵：

| 窗口 | 比例 | GUI scale | 点击/放大 | 换帧 | 再缩放 | 缩回/重开 |
|---|---|---:|---:|---:|---:|---:|
| 1280×720 | 16:9 | 2 | 通过 | 通过 | 通过 | 通过 |
| 1280×800 | 16:10 | 3 | 通过 | 通过 | 通过 | 通过 |
| 1280×720 | 16:9 | 4 | 通过 | 通过 | 通过 | 通过 |

每组五张截图位于 `phase22-g3-ui-scale*-fixed`，15 张 SHA-256 位于 `phase22-g3-fixed-screenshot-sha256.txt`。Group 3 门禁：`phase22-g3-minimum-verification.log`，通过。

## 7. Group 4：15 个 recipe

### 7.1 根因规则

26.1.2 实际 `Ingredient.CODEC` 接受普通 item 字符串 `namespace:item`、tag 字符串 `#namespace:tag`，不再接受旧 `{ "item": ... }` / `{ "tag": ... }` 对象。Fabric components ingredient 的 `base` 也必须使用同一 codec 的字符串形式。

`album` 的 `List is too short: 0, expected range [1-9]` 是两项旧对象无法变成有效 Ingredient 后列表为空。当前资源和固定 1.21.1 原资源都明确是 shapeless；因此保持原 shapeless 业务语义，将两项转换为字符串，没有伪造 shaped pattern、空行或改造配方类型。

### 7.2 逐 ID 审计

| Recipe ID | 旧错误字段 | 26.1.2 修复 | 最终类型/输出 | 运行状态 |
|---|---|---|---|---|
| `exposure:album` | 两个 object ingredients → 空列表 | `minecraft:writable_book`、`minecraft:phantom_membrane` 字符串 | shapeless → album×1 | loaded，matches=true |
| `exposure:black_and_white_film` | shaped key item/tag objects | item/tag 全改字符串 | shaped → BW film×1 | loaded，matches=true |
| `exposure:broken_interplanar_projector_fixing` | source/ender eye objects | source 与 ingredient 字符串 | component-transferring → projector×1 | loaded，matches=true，状态 patch 保留 |
| `exposure:camera` | buttons/glass/iron tag 与 lever object | `#tag`/item 字符串 | shaped → camera×1 | loaded，matches=true |
| `exposure:camera_stand` | stick/iron/slab objects | 字符串 | shaped → stand×1 | loaded，matches=true |
| `exposure:color_film` | item/tag objects | 字符串 | shaped → color film×1 | loaded，matches=true |
| `exposure:glass_photograph_frame` | pane/frame objects | 字符串 | shaped → glass frame×1 | loaded |
| `exposure:high_sensitivity_black_and_white_film` | item/tag objects | 字符串 | shaped → high-sensitivity BW×1 | loaded |
| `exposure:high_sensitivity_color_film` | item/tag objects | 字符串 | shaped → high-sensitivity color×1 | loaded |
| `exposure:interplanar_projector` | glass/redstone/eye objects | 字符串 | shaped → projector×1 | loaded |
| `exposure:lightroom` | trapdoor/planks/torch objects | 字符串 | shaped → lightroom×1 | loaded，matches=true |
| `exposure:photograph_aging` | source、ager tag、brush objects | source/item/tag 字符串 | photograph-aging → aged photograph×1 | loaded，matches=true，Frame/type 保留，brush remainder 受损 |
| `exposure:photograph_copying_black_and_white` | tag objects；Fabric components `base` object | tag 字符串；`base: "minecraft:potion"` | photograph-copying → photograph×1 | loaded，matches=true，BW type/generation=1 保留 |
| `exposure:photograph_copying_color` | source/tag objects | source/tag 字符串 | photograph-copying → photograph×1 | loaded，matches=true，原照片 remainder、Frame/type、generation=1 正确 |
| `exposure:photograph_frame` | stick/planks/item-frame objects | 字符串 | shaped → photograph frame×1 | loaded，matches=true |

没有放宽黑白复制配方的 `exposure:photograph_type=black_and_white` component 条件，也没有改动 result `ItemStackTemplate`。黑白复制沿 1.21.1 原语义使用带 component 的 potion base；运行结果精确记录为 photograph，保留 potion contents、BW type、generation=1，其 remaining grid 为空。彩色复制保留原 PhotographItem 一份作为 remainder。冲洗胶卷 recipe 的 1/3 个 glass bottle remainder 已单独验证。

修复前 integrated client 日志为 1517 recipes；修复后 integrated client 和 dedicated server 都是 1532 recipes，正好增加 15。`phase22-g4-recipe-runtime-audit-summary.txt` 逐 ID 记录 `loaded=true`、实际 class 和 result ID。最终 dedicated server 的全局及目标 parse error 都为 0。

新增两项资源回归测试锁住 album 的原 shapeless 语义和三个 common 自定义 component-transferring recipe 的精确 source/ingredient/result；行为回归由 `FilmDevelopingRecipeTests` 和真实 RecipeManager smoke audit 共同覆盖。Group 4 门禁：`phase22-g4-minimum-verification.log`，通过。

### 7.3 Fabric API 版本差异

项目声明版本 0.150.0+26.1.2 的 integrated client 和 dedicated server 均通过。另以命令行 `-Pfabric_api_version=0.151.0+26.1.2` 覆盖依赖、不修改 `gradle.properties`，再次运行 integrated client：日志确认实际加载 `fabric-api 0.151.0+26.1.2`、1532 recipes、15 ID 全部 loaded、主要 smoke 全部 matches=true。证据：`phase22-g4-fapi0151-runClient.log`。没有把故障归因于 Fabric API，也没有擅自升级项目依赖。

Trinkets 的 `Updated CreativeModeInventoryScreen Mixin` 警告属于其他模组客户端 Mixin；未修改 Exposure 掩盖该警告。最终 Exposure dedicated server 没有客户端类误加载或致命错误。

## 8. 修改文件

最终源码/资源修改共 22 项，精确 old/new SHA-256 和长度见 `phase22-final-modified-source-resource-files.csv`。

生产 Java（5）：

- `common/src/main/java/io/github/mortuusars/exposure/client/capture/task/BackgroundScreenshotCaptureTask.java`
- `common/src/main/java/io/github/mortuusars/exposure/client/capture/task/DirectScreenshotCaptureTask.java`（仅同步更新已失效的旧注释；异步 readback 行为保持）
- `common/src/main/java/io/github/mortuusars/exposure/client/gui/screen/FilmFrameInspectScreen.java`
- `common/src/main/java/io/github/mortuusars/exposure/mixin/client/GameRendererMixin.java`
- `common/src/main/java/io/github/mortuusars/exposure/world/item/crafting/recipe/ComponentTransferringRecipe.java`

recipe 资源（15）：common 14 项加 Fabric `photograph_copying_black_and_white.json` 1 项。

测试（2）：

- `FilmDevelopingRecipeTests.java`
- 新增 `Phase22RecipeResourceTests.java`

所有临时 `Phase22CaptureDebugHarness`、`CaptureDiagnostics`、LevelRenderer stage mixin、GUI screenshot hook、timing wrapper 和 final auto-stop hook均已删除/恢复。最终 JAR 内容扫描未找到 `Phase22`、`CaptureDiagnostics` 或 `LevelRendererCaptureDebug` 类。

## 9. 最终验证

### 9.1 构建

`phase22-final-full-build.log`：

- common `compileJava`：成功
- Fabric `compileJava`：成功
- common `compileTestJava`：成功
- 43/43 测试：成功，0 failures，0 errors，0 skipped
- common Access Widener：成功
- Fabric Access Widener：成功
- 完整 `build`：`BUILD SUCCESSFUL`
- 未执行 `clean`

### 9.2 客户端

`phase22-final-runClient.log` / `phase22-final-client-latest.log` / `phase22-final-client-debug.log`：

- Fabric API 0.150.0+26.1.2
- Loaded 1532 recipes
- 进入受控单人世界
- 无目标 recipe parse error
- Minecraft `Stopping!`，integrated server 保存全部维度并退出
- 最终运行没有生成新 crash report
- `phase22-final-crash-reports-inventory.csv` 记录并哈希清点了运行目录中保留的全部 7 份历史/本阶段 crash report；本阶段两份新失败副本另有 `phase22-g2-...`、`phase22-g3-...` 前缀

### 9.3 Dedicated server

`phase22-final-dedicated-server.log` / `phase22-final-server-latest.log` / `phase22-final-server-debug.log`：

- Loaded 1532 recipes / 1646 advancements
- `Done (0.319s)`
- 所有 recipe parse error：0
- Exposure 客户端类误加载：0
- 控制台 `stop` 后保存 overworld/nether/end，`BUILD SUCCESSFUL`
- 最终残留 KnotClient/KnotServer/devlaunchinjector 游戏 JVM：0；仅存在用户工作期间已存在的 Gradle daemon，不是游戏进程

首次 dedicated 尝试只生成 EULA 并退出，记录于 `phase22-g4-dedicated-server-first.log`；接受隔离开发运行目录 EULA 后完成真正加载验证。该运行在 `fabric/run` 生成了临时 server world/properties；没有安装到用户正式实例。

## 10. 最终 JAR

`D:\Codex\Exposure\phase22-exposure-fabric-26.1.2-1.9.18.jar`

- 大小：2,055,496 字节
- 修改时间：2026-07-17 20:46:28 +08:00
- SHA-256：`6607A393B95AD097CFAFC2B7517FAB5514053907CA6F6689A30059F61C7B366F`

Phase 21B JAR 未覆盖。新 JAR 没有安装到用户正式实例。

## 11. 失败尝试与处理

1. computer-use 插件 bootstrap 两次失败：`failed to write kernel assets: 系统找不到指定的路径 (os error 3)`。改用显式开发开关的真实游戏内 harness；没有使用 PowerShell 鼠标/键盘伪自动化。
2. 最初 Capture harness 编译/登录时序迭代失败，保留 compile/retry 日志；修正为串行 `Cycles.enqueueTask`，不在 `removeIf` 中添加 parallel task。
3. 第一次 Camera Stand 诊断选中了旧 stand；保存 `phase22-g1-background-stand-wrong-existing-stand-*`，之后清理并按 tag/新实体重跑。
4. 第一次业务 pipeline 直接上传未由服务端 `expect` 注册的 ID，服务器按安全设计丢弃；随后使用 `/exposure expose` 完整业务路径注册并验证持久化。没有绕过 repository。
5. 第一次 film runtime audit 把客户端 `RecipeAccess` 错当 `RecipeManager`，崩溃副本为 `phase22-g2-failed-audit-client-crash-2026-07-17_19.51.14.txt`；改为 integrated server `getRecipeManager()`。
6. 第一次 developed film 单元测试的 source/result prototype 都未设置 identity components，null/null 不能证明模型隔离；修正 synthetic prototype 后通过。
7. Lightroom harness 第一次在 `Cycles.removeIf` 中加入并行任务导致 `ConcurrentModificationException`，崩溃副本为 `phase22-g3-failed-harness-crash-2026-07-17_20.02.52.txt`；改为串行队列。
8. 最初 GUI screenshot 在 GUI submit 前读取，只得到世界画面；移动诊断读取点到 `GuiRenderer.render` 之后再复现真实 bug。该 after-GUI hook 已删除。
9. 新增 glass-bottle remainder 断言第一次因单元测试夹具未绑定 `Items.GLASS_BOTTLE` prototype 失败；`phase22-g4-custom-recipe-tests.log` 保留，补齐夹具后 43/43 通过。
10. 第一次 dedicated run 只生成 EULA，未把它误报为服务器验证；第二次和最终第三次均真正到达 `Done` 并正常 stop。

## 12. 明确未修改的边界

- 所有业务 packet ID、方向、字段顺序、`StreamCodec`
- `query_exposure_data` 仍未激活
- Phase 20 menu 字段线上顺序及 `byte[]` open-data envelope
- SavedData、ExtraData、Frame、ExposureData、CameraId
- 照片文件格式、像素编码、调色板、曝光算法、胶片算法
- Phase 18 Viewfinder/FOV/CameraCapture/EntitiesInFrame/EnderMan 语义
- Phase 17 RenderTarget 与异步 ScreenshotReadback 所有权
- Lightroom menu/open-data 与照片打印结果
- 用户全局环境及正式 Minecraft 实例

## 13. 遗留风险与人工复测清单

自动与受控验证已覆盖完成标准，但 GPU/Shader Pack/正式整合包差异仍需要用户复测：

1. 用 Phase 22 JAR 在无 Shader 环境分别拍一张手持/background、手持/direct、Stand/background、Stand/direct。
2. 每张确认天空、地形、不透明方块、透明方块、普通实体、block entity 完整；无手/HUD；Stand 中本地玩家恰好一个。
3. 连拍三张并资源重载，再保存退出/重进，确认内容不复用且 Frame 可读。
4. 使用用户实际 Iris/Shader Pack 重复两种 Capture，确认不再 sky-only；如仍有问题，保留具体 Iris、Sodium、pack 版本及照片。
5. 观察首次拍摄和稳定拍摄卡顿；本阶段数据表明首次资源/GPU 预热与后台 palette 是主要成本，没有同步 `glReadPixels`。
6. 正常冲洗黑白和彩色胶卷，检查实际 result ID、components、developed 外观和暗房可读性。
7. 简体中文下以 GUI scale 2/3/4 打开放大、换帧、再缩放、右键缩回和重开。
8. 在 integrated server 和用户 dedicated server 检查日志：15 个目标 ID 不再 parse error。
9. 实际工作台烟雾测试 camera、camera stand、lightroom、两种基础胶卷、album、photograph frame、彩色/黑白 copying、aging，并检查数量、组件和 remainder。

## 14. 状态交接

已完成：Capture 正常世界帧和 Shader 路径、direct/background、手持/Stand、异步目标所有权、计时；developed film 身份/模型；暗房放大预览；15 个 recipe；43 项测试；双 AW；完整 build；最终客户端/dedicated server；Phase 22 JAR。

关键决策：不再从同一帧末尾第二次调用 `renderLevel`，而是把下一次完整正常帧重定向到独占 target，并在 GUI 前异步 readback；只转移 source components patch；GUI blit 明确拆分 destination/source/atlas 尺寸；recipe 按逐 ID DataResult 迁移。

未解决的高优先级问题：无。受控环境满足 Phase 22 完成标准。遗留范围仅为用户正式整合包/GPU/Shader Pack 的人工兼容复测；若用户仍能复现 Capture 错误，下一轮只应收集该环境的最终世界目标/阶段图和版本，不应回退同步读取、屏幕裁剪或扩大到新功能。

下一步默认动作：用户安装（自行安装）`phase22-exposure-fabric-26.1.2-1.9.18.jar` 到测试实例，按上述九项清单复测，并回传照片、客户端/服务器日志和具体 Shader 版本。
