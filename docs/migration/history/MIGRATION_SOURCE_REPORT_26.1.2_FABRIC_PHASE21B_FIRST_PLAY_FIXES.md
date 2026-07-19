# Minecraft 26.1.2 Fabric 迁移 Phase 21B：首次人工测试定向修复报告

## 1. 状态摘要

Phase 21B 只处理了首次人工测试确认的三个问题：胶卷冲洗失败、简体中文暗房名称出现裸 translation key、相机取景器左侧遮挡存在透明细缝。三项均已定位到确定根因并以最小修改修复。

- 合法的实拍黑白胶卷已用正常水瓶配方冲洗为 `exposure:developed_black_and_white_film`，原始 1 张 `Frame`、曝光标识及相关组件保留；冲洗后的胶卷能够放入暗房并显示缩略图和“照片数: 1”。
- 简体中文的暗房 BlockItem 与菜单不再显示 `item.exposure.lightroom` 等裸 key；简体中文显示“暗房”，英文显示 “Lightroom”。
- 左侧深灰遮挡与取景器边界增加 1 个 GUI 像素的语义化重叠；GUI scale 2、3、4，以及 854×480、2560×1369、1280×800 客户区均未再看到透明缝，也未明显侵入有效画面。
- 非 clean 最终验证通过：41/41 测试、common/Fabric compileJava、common compileTestJava、双 Access Widener、完整 build。
- 新 JAR 已生成并另存，未覆盖用户此前测试的 JAR。

有一项必须如实保留的范围外遗留：全局数据包启动/重载仍会打印 15 个 Phase 21B 开始前已存在的其他 Exposure 配方格式错误（如相机、暗房方块本身的合成配方、照片复制配方等）。本轮两个 `*_film_developing` 配方不再在错误列表中，并且每次均使加载总数从修复前的 1515 恢复为 1517。由于用户明确要求本轮只处理三个已知问题，未扩展修复其余配方；因此本报告不会声称全局 Exposure 重载日志已经零错误。

## 2. 输入、约束与基线

开始前完整读取了 `MIGRATION_SOURCE_REPORT_26.1.2_FABRIC_PHASE21_CLIENT_BOOT.md`，沿用 Phase 21 已通过的客户端启动、Mixin、资源、测试和构建基线。仅按需要定向查阅 Phase 18 的取景器/GUI 迁移记录、Phase 21A 受控世界记录，以及原版 Exposure 1.21.1 对应实现；没有重新扫描或重做 Phase 17、18、20、21。

环境：

- 工作区：`D:\Codex\Exposure`
- Java：`C:\Program Files\Java\jdk-25.0.3`（Java 25）
- Gradle：项目 `gradlew.bat`
- 独立 Gradle home：`D:\Codex\Exposure.gradle-home`
- 未修改用户全局 Java、Gradle 或 Minecraft 实例；未执行 `clean`。
- 工作区不是 Git 仓库。本轮未使用、也不声称使用 Git diff、commit 或 branch。
- 只使用受控世界 `fabric\run\saves\Phase21A World Entry`，没有打开或改写用户正式旧世界。

修改前已保存：

- 源码/资源清单：`phase21b-first-play-prechange-source-resource-manifest.csv`
- 源码/资源快照：`phase21b-first-play-prechange-source-resource-snapshot.zip`
- 原测试选项：`phase21b-first-play-prechange-options.txt`
- 既有 latest/debug 日志副本：`phase21b-first-play-prechange-existing-latest.log`、`phase21b-first-play-prechange-existing-debug.log`
- 用户此前测试 JAR：`phase21b-first-play-previous-user-tested-exposure-fabric-26.1.2-1.9.18.jar`

非 clean 基线 `phase21b-first-play-baseline.log` 通过：common/Fabric compileJava、common compileTestJava、39/39 测试及双 Access Widener。

用户首次人工测试中已经确认可用且本轮未改动的功能：相机安装胶卷、拍照、Camera Stand、闪光灯。

## 3. 问题一：拍摄后胶卷不能冲洗

### 3.1 现象与最小复现

正常玩法链路：

1. 取得可安装的未曝光黑白胶卷并装入手持相机。
2. 完成一次真实拍摄，取出带 1 张曝光帧的胶卷。
3. 在工作台放入该胶卷与正常 Water Bottle。
4. 修复前输出槽为空，看起来像“配方无效”。

首次失败画面：`phase21b-first-play-before-film-developing-empty-output.png`。

拍摄后实际胶卷并非空壳：运行时记录到曝光标识 `Player241_136338`、黑白类型、photographer、`extra_data` 和帧数据；因此失败不是相机取出胶卷时丢失 Frame、CameraId 或曝光状态。

### 3.2 实际入口和完整路径

冲洗不是 Lightroom/暗房菜单配方，也没有按钮 packet。它是标准 crafting 类型上的自定义 recipe serializer：

- Recipe type：继承 `CustomRecipe` 的 crafting recipe，实际运行于 `RecipeType.CRAFTING`；没有单独的 Lightroom recipe type。
- Serializer 注册：`Exposure.RecipeSerializers.FILM_DEVELOPING`，注册 ID `exposure:film_developing`。
- Serializer 实现：`ComponentTransferringRecipeSerializer`。
- RecipeInput：`CraftingInput`。
- 配方 ID：
  - `exposure:black_and_white_film_developing`
  - `exposure:color_film_developing`
- JSON：
  - `fabric/src/main/resources/data/exposure/recipe/black_and_white_film_developing.json`
  - `fabric/src/main/resources/data/exposure/recipe/color_film_developing.json`
- 胶卷 tag：
  - `#exposure:black_and_white_film_rolls`
  - `#exposure:color_film_rolls`
- 黑白药液：带 `minecraft:potion_contents.potion=minecraft:water` 的 `minecraft:potion`。
- 彩色药液：分别为 awkward、thick、mundane potion。
- 输出：`exposure:developed_black_and_white_film` 或 `exposure:developed_color_film`。
- 匹配：`ComponentTransferringRecipe.matches(CraftingInput, Level)` 要求 source ingredient、全部药液、非空格数量同时精确匹配；不删除玩法条件。
- 构造：serializer 先用 `ItemStackTemplate` 延迟创建结果；`assemble` 找到 source 胶卷后调用 `applyComponents`，把胶卷的完整组件 patch 应用到 developed film。
- 后续：`DevelopedFilmItem` 继续通过 `Exposure.DataComponents.FILM_FRAMES` 读取 `List<Frame>`；暗房只消费已经冲洗的胶卷用于选帧/打印，不承担冲洗。

### 3.3 真实根因

日志给出了两层确定的 decode 根因：

1. 26.1.2 的 `Ingredient.CODEC` 不再接受旧式 `{ "item": ... }` / `{ "tag": ... }` 作为普通 ingredient。胶卷 tag 必须写成 `"#exposure:..."`，普通 base item 必须写成 `"minecraft:potion"`。
2. 自定义 serializer 在 registry item components 尚未绑定的资源加载阶段用 `ItemStack.CODEC` 急切构造 result，报错为 `Item ... does not have components yet`。26.1.2 的 recipe result 应使用 `ItemStackTemplate.CODEC` / `ItemStackTemplate.STREAM_CODEC` 延迟构造。

因此用户会观察到“像配方无效”：两个 recipe 在资源加载阶段根本没有进入 RecipeManager，工作台自然没有任何可匹配结果。问题不是暗房菜单、不是什么按钮不可用，也不是胶卷拍摄状态被拒绝。

证据：

- 首次复现日志：`phase21b-first-play-first-reproduction-latest.log`
- 首次完整 debug：`phase21b-first-play-first-reproduction-debug.log`
- 解码与胶卷状态诊断：`phase21b-first-play-first-reproduction-recipe-and-film-diagnostic.txt`
- 修复前加载数：1515；修复后及每次 `/reload`：1517。

### 3.4 最小修复

- 将 `ComponentTransferringRecipe` 的 result 存储从 `ItemStack` 改为 `ItemStackTemplate`，只在实际取结果时 `create()`。
- serializer 的磁盘 codec 与网络 StreamCodec 同步改为 `ItemStackTemplate.CODEC` / `ItemStackTemplate.STREAM_CODEC`。
- 三个子类构造参数同步改为 template；没有更改任何业务 packet 或保存格式。
- 两个 developing JSON 的胶卷 tag 与药水 base 改为 26.1.2 ingredient 格式；保留 Fabric components ingredient 对药水组件的严格判断。

`PhotographCopyingRecipe` 和 `PhotographAgingRecipe` 只做同一公共构造签名的编译级同步，没有改变其玩法；其各自旧 JSON 的范围外格式错误仍保留，未借本轮扩大修复。

### 3.5 自动化回归

新增 `FilmDevelopingRecipeTests` 两项：

- 合法 source 胶卷带 `List<Frame>` 组件、配方药液齐全时能够匹配，输出为目标 item，Frame 列表经生产 `assemble/transferComponents` 路径保留。
- 缺少药液或 source item 错误时仍被拒绝。

测试使用生产 `FilmDevelopingRecipe` matcher、assembler 与 `ItemStackTemplate` 结果路径；测试组件采用与生产 `FILM_FRAMES` 同型的 `Frame.CODEC` 列表，以避免为测试增加生产兼容入口。真实 registry/tag/Fabric components ingredient 则由运行时正常配方验证覆盖。

定向测试：`phase21b-first-play-film-developing-tests-targeted-pass.log`。最初的 Minecraft bootstrap/registry components 测试失败尝试被保留在同前缀的三个 earlier test 日志中，没有隐藏。

### 3.6 运行时修复验证

- 启动后两个配方均成功注册；RecipeManager 总数为 1517。
- 一个用 `/give minecraft:potion` 得到、没有 `potion_contents` 的裸 potion 被正确拒绝，证明没有降低组件约束。
- 正常 Water Bottle 匹配成功。
- 实拍黑白胶卷输出 `exposure:developed_black_and_white_film`，曝光标识、photographer、extra data 和 1 张 Frame 仍存在。
- 输出放入暗房后显示黑白缩略图和“照片数: 1”，证明不是空结果、未丢物、未重置帧，且照片查看/打印的下一步入口可达。
- 保存退出、二次进入后，同一 developed film 仍显示缩略图和 1 张照片。

关键证据：

- `phase21b-first-play-after-film-developing-frame-data.png`
- `phase21b-first-play-after-developed-film-lightroom-thumbnail.png`
- `phase21b-first-play-after-reentry-zh-cn-lightroom-and-frame.png`
- `phase21b-first-play-film-developing-confirmed-diagnostic.txt`
- `phase21b-first-play-film-matcher-and-success-latest.log`
- `phase21b-first-play-film-fix-runtime-runClient.log`

## 4. 问题二：简体中文暗房名称显示裸 key

### 4.1 现象与准确位置

修复前在简体中文中，创造栏/手持暗房 BlockItem 显示的准确原始文本为 `item.exposure.lightroom`。这不是放置后 block description、tooltip 或字幕；菜单标题原路径为 `container.exposure.lightroom`。

修复前截图：`phase21b-first-play-before-zh-cn-lightroom-item-key.png`。

### 4.2 Component 路径与根因

- block key：`block.exposure.lightroom`，现有语言文件已有。
- 26.1.2 BlockItem 创建路径使用 `itemProperties("lightroom")`，因此实际 description ID 是 `item.exposure.lightroom`。
- menu provider/screen title 使用 `Component.translatable("container.exposure.lightroom")`，现有语言文件已有。
- 原版 1.21.1 的 BlockItem 属性路径会继承 block 名称，因此旧资源不需要独立 item key；26.1.2 显式 item description ID 后，en_us/zh_cn 没有同步增加该条目。

根因是语言资源缺项，不是 `Component.literal`、错误 namespace 或菜单 open-data。

诊断：`phase21b-first-play-localization-component-diagnostic.txt`。

### 4.3 修复与验证

只补资源，没有在 Java 中硬编码中文：

- `en_us.json`：`"item.exposure.lightroom": "Lightroom"`
- `zh_cn.json`：`"item.exposure.lightroom": "暗房"`

“暗房”沿用项目现有 `block.exposure.lightroom` / `container.exposure.lightroom` 术语。

验证：

- zh_cn：BlockItem 和菜单均显示“暗房”，没有裸 key；二次进入仍正确。
- en_us：语言切换触发资源重载后，BlockItem 和菜单均显示 “Lightroom”。
- 再次切换/重载未产生这两个语言资源的 decode 错误。

证据：

- `phase21b-first-play-after-zh-cn-lightroom-menu.png`
- `phase21b-first-play-after-reentry-zh-cn-lightroom-and-frame.png`
- `phase21b-first-play-after-en-us-lightroom-item.png`
- `phase21b-first-play-after-en-us-lightroom-menu.png`

## 5. 问题三：取景器左侧遮挡透明细缝

### 5.1 根因

Phase 18/21 后 GUI 提交从立即绘制进入 render-state extraction。`GuiUtil.drawRect` 在提交 `GuiGraphicsExtractor.fill` 前会对两个端点分别向下取整，而 fill 的远边界为排他边界。取景器 opening 仍从浮点坐标开始：当 `opening.x` 带小数时，左遮挡的取整后右端可能比取景器纹理浮点起点早一个子 GUI 像素；经 GUI scale 放大后就成为可见的一个物理像素透明缝。

根因不是纹理损坏，也不在照片 capture/crop/FOV 计算。

诊断：`phase21b-first-play-viewfinder-boundary-diagnostic.txt`。

### 5.2 最小边界修复

在 `ViewfinderOverlay` 定义 `MASK_OVERLAP = 1f`，仅让左遮挡的靠取景器端向右覆盖 1 个 GUI 像素，并同步增加负 width 的绝对值以保持远端不变。

该策略直接对应排他边界与 floor 语义；没有改变 opening 大小、取景器纹理、FOV、相机捕获区域或真实照片裁剪。重叠只有必要的 1 GUI 像素，没有使用针对单一截图的大偏移。

### 5.3 多缩放/尺寸验证

| GUI scale | 客户区 | 比例/场景 | 结果 |
|---|---:|---|---|
| 2 | 854×480 | 约 16:9，小窗口 | 细缝消失，无明显过度覆盖 |
| 3 | 2560×1369 | 最大化窗口 | 细缝消失，resize 后边界稳定 |
| 4 | 2560×1369 | 最大化窗口 | 细缝消失，无明显过度覆盖 |
| 4 | 1280×800 | 精确 16:10 客户区 | 细缝消失，无明显过度覆盖 |

证据：

- 修复前：`phase21b-first-play-before-viewfinder-left-seam-scale-auto-856x512.png`
- 修复后：`phase21b-first-play-after-viewfinder-scale2-854x480.png`
- 修复后：`phase21b-first-play-after-viewfinder-scale3-2560x1369.png`
- 修复后：`phase21b-first-play-after-viewfinder-scale4-2560x1369.png`
- 修复后：`phase21b-first-play-after-viewfinder-scale4-1280x800.png`

## 6. 修改文件清单

精确清单另存为 `phase21b-first-play-modified-source-resource-files.txt`；修改后清单为 `phase21b-first-play-postchange-source-resource-manifest.csv`。

生产源码/资源修改：

1. `common/src/main/java/io/github/mortuusars/exposure/world/item/crafting/recipe/ComponentTransferringRecipe.java` — result 改为延迟 `ItemStackTemplate` 并在 assemble 时创建。
2. `common/src/main/java/io/github/mortuusars/exposure/world/item/crafting/recipe/FilmDevelopingRecipe.java` — 同步 template 构造签名。
3. `common/src/main/java/io/github/mortuusars/exposure/world/item/crafting/recipe/PhotographCopyingRecipe.java` — 同步公共构造签名。
4. `common/src/main/java/io/github/mortuusars/exposure/world/item/crafting/recipe/PhotographAgingRecipe.java` — 同步公共构造签名。
5. `common/src/main/java/io/github/mortuusars/exposure/world/item/crafting/recipe/serializer/ComponentTransferringRecipeSerializer.java` — 生产 MapCodec/StreamCodec 使用 `ItemStackTemplate`。
6. `fabric/src/main/resources/data/exposure/recipe/black_and_white_film_developing.json` — 26.1.2 ingredient/tag/base 格式。
7. `fabric/src/main/resources/data/exposure/recipe/color_film_developing.json` — 26.1.2 ingredient/tag/base 格式。
8. `common/src/main/resources/assets/exposure/lang/en_us.json` — 补 `item.exposure.lightroom`。
9. `common/src/main/resources/assets/exposure/lang/zh_cn.json` — 补 `item.exposure.lightroom`，译名“暗房”。
10. `common/src/main/java/io/github/mortuusars/exposure/client/camera/viewfinder/ViewfinderOverlay.java` — 左遮挡增加 1 GUI 像素重叠。

新增测试：

11. `common/src/test/java/io/github/mortuusars/exposure/world/item/crafting/recipe/FilmDevelopingRecipeTests.java`。

没有修改 Gradle 配置。

## 7. 资源重载、二次进入和干净退出

- F3+T 客户端资源重载成功，语言/GUI 资源正常重建。
- `/reload` 多次完成；每次 RecipeManager 都记录 `Loaded 1517 recipes`，两个 film developing 配方没有 decode 错误。
- 保存退出受控世界后再次进入：暗房标题仍为“暗房”，developed film 的缩略图和 1 张 Frame 仍存在。
- 切换 en_us 后资源重载，item/menu 均为 “Lightroom”。
- 第二次独立 runClient 使用精确 1280×800 客户区完成 16:10 取景器复测。
- 两次均通过 Quit Game 退出；最终 `GAME_JAVA_COUNT=0`。

完整首轮运行日志：

- `phase21b-first-play-final-runtime-latest.log`
- `phase21b-first-play-final-runtime-debug.log`
- `phase21b-first-play-final-runtime-runClient.log`

最终 16:10 独立运行日志：

- `phase21b-first-play-near-16x10-latest.log`
- `phase21b-first-play-near-16x10-debug.log`
- `phase21b-first-play-near-16x10-runClient.log`

进程证据：`phase21b-first-play-final-game-java-count.txt`。

全局重载的 15 个其他旧配方错误在首轮完整日志中可见。它们不包含 `black_and_white_film_developing` 或 `color_film_developing`，也不阻止本轮实拍胶卷成功冲洗；但这意味着只能确认本轮 recipe、语言和 GUI 资源重载正常，不能确认整个 Exposure 数据包零错误。

## 8. 编译、测试和构建矩阵

| 验证 | 结果 |
|---|---|
| `:common:compileJava` | 通过 |
| `:fabric:compileJava` | 通过 |
| `:common:compileTestJava` | 通过 |
| 原有测试 | 39/39 通过 |
| 新增测试 | 2/2 通过 |
| 总测试 | 41/41，0 failure，0 error，0 skipped |
| `:common:validateAccessWidener` | 通过 |
| `:fabric:validateAccessWidener` | 通过 |
| `build` | 通过 |

最终强制执行日志：`phase21b-first-play-final-build-verification.log`，包含 `BUILD SUCCESSFUL in 18s`。PowerShell 将 javac 写到 stderr 的普通 deprecation/unchecked 警告包装成了工具层 NativeCommandError，导致外层显示 exit 1；Gradle 自身任务全部成功。随后用同一 Java/Gradle home 非 clean 确认，外层 exit 0 且再次 `BUILD SUCCESSFUL`：`phase21b-first-play-final-build-confirmation.log`。

JUnit XML 汇总：10 suites，41 tests，0 failures，0 errors，0 skipped。

## 9. 新 JAR

新 JAR（未安装到用户正式实例）：

- 路径：`D:\Codex\Exposure\phase21b-first-play-exposure-fabric-26.1.2-1.9.18.jar`
- 大小：2,055,454 bytes
- 修改时间：2026-07-17 15:49:35 +08:00
- SHA-256：`642E8FB42B7EFFC842AED8CB1EBCC6E0BF29280E71F8E58C45D92497753FC6A4`

上一用户测试 JAR：

- 路径：`D:\Codex\Exposure\phase21b-first-play-previous-user-tested-exposure-fabric-26.1.2-1.9.18.jar`
- 大小：2,055,316 bytes
- SHA-256：`14BB3F7CF771E238D13618ED721154D7EB2C38B3AC391A45F5F2418B4EE20D47`

新文件增加 138 bytes，hash 已变化；旧 JAR 未覆盖、未删除。完整信息：`phase21b-first-play-final-jar-info.txt`。

## 10. 失败尝试与排除项

- 最初自动化拍摄时准星对着 Camera Stand，触发的是 stand 交互而非手持相机；随后转离 stand，用手持相机完成真实拍摄链路。
- 使用 `/give minecraft:potion` 得到的裸 potion 没有 `potion_contents`，被配方正确拒绝；换为正常 Water Bottle 后成功。没有为表面成功放宽约束。
- 为分离 tag 与 component ingredient 问题，曾临时把黑白药液简化为普通 potion；诊断结束后恢复严格 water component ingredient，并重新 processResources、运行时验证。
- 新测试首次因 Minecraft bootstrap 和 item components 尚未绑定而失败；补齐测试 bootstrap 后定向测试通过。失败日志全部保留。
- 第一次最小 Gradle 组合验证遇到依赖下载/超时；未执行 clean，清理遗留构建客户端后同任务重试成功。
- 最大化窗口直接拖拽恢复尺寸没有得到可靠客户区，且会把鼠标输入传给游戏改变视角；最终在游戏关闭时用测试 options 的 overrideWidth/overrideHeight 启动独立 1280×800 运行，结束后把 options 恢复到本轮开始前。
- 最终强制 build 的外层 stderr 误判已通过第二次 exit 0 确认排除，未跳过任何测试。

## 11. 明确未修改的协议与数据边界

本轮没有：

- 改变任何 packet ID、方向、字段顺序或 StreamCodec 业务 schema；
- 激活 `query_exposure_data`；
- 改变 Phase 20 菜单字段线上顺序或 `byte[]` open-data envelope；
- 改变 SavedData、ExtraData、Frame、ExposureData、CameraId 或照片文件格式；
- 改变像素编码、调色板、曝光算法、胶片算法、CameraCapture 核心语义；
- 回退 Phase 17 ShaderManager、PostChain、RenderTarget、异步 ScreenshotReadback ownership；
- 回退 Phase 18 Viewfinder、FOV、CameraCapture、EntitiesInFrame、EnderMan 设计；
- 删除匹配条件、伪造默认照片、清空胶卷数据、给予预制成品、禁用界面或静默吞错。

`ItemStackTemplate.STREAM_CODEC` 只是修正 recipe serializer 自身的 26.1.2 表达，与业务 packet schema 无关。取景器改动仅是显示遮挡提交坐标，不进入 capture/crop。

## 12. 遗留人工验收与下一阶段

下一步默认由用户把新 JAR 放入测试实例进行第二轮人工测试，不应由本轮擅自安装到正式实例。重点：

1. 冲洗后的实际照片查看、照片内容与持久化。
2. 19 个滤镜视觉矩阵。
3. Viewfinder/FOV。
4. 两种 capture 路径。
5. Camera Stand 本地玩家补绘。
6. 其余菜单交互。

建议另开后续定向阶段处理全局重载中剩余的 15 个旧配方 JSON 格式错误；它们已被明确记录，但不应混入本轮三个已知缺陷的因果验证。

## 13. 简洁状态交接

- **已完成**：胶卷冲洗 codec/ingredient 修复与 2 项回归；zh_cn/en_us 暗房名称修复；取景器左遮挡边界修复；资源重载、保存退出、二次进入、两种语言、三种 GUI scale、16:9/16:10 运行时验证；41/41、双 AW、build、新 JAR。
- **关键决策**：冲洗保持 crafting 入口和药水组件约束；结果用 26.1.2 `ItemStackTemplate` 延迟构造；暗房只补资源 key；遮挡只增加 1 GUI 像素语义重叠。
- **验证结果**：合法实拍胶卷成功冲洗，1 张 Frame 保留且可在暗房显示；非法输入仍拒绝；中英文无裸 key；细缝在所有目标组合消失；游戏 Java 残留 0。
- **失败尝试**：stand 误交互、裸 potion、测试 bootstrap、首次最小验证超时、窗口拖拽、PowerShell stderr 外层误判，均已记录并有确定结论。
- **遗留问题**：全局数据包仍有 15 个范围外既有配方错误；照片色彩/滤镜/完整视觉内容尚未在本轮验收。
- **下一步**：用户使用 `phase21b-first-play-exposure-fabric-26.1.2-1.9.18.jar` 进行第二轮人工测试，重点覆盖冲洗后照片查看、实际内容、19 个滤镜、Viewfinder/FOV、两种 capture、Camera Stand 本地玩家补绘和其余菜单。
