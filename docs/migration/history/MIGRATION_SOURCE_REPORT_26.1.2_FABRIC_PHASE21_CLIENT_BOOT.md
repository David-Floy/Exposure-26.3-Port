# Minecraft 26.1.2 Fabric 迁移 Phase 21：首次客户端启动验证

日期：2026-07-16  
工作区：`D:\Codex\Exposure`  
结论：**通过。Fabric 开发客户端完成初始化并有画面证据到达主菜单；最终日志没有 Exposure 导致的 Mixin apply failure、资源 JSON/ItemTintSource 解码错误、Shader/PostChain 错误或客户端初始化崩溃；测试客户端已通过 “Quit Game” 干净结束；修改后的编译、39 项测试、双 Access Widener 和 build 全部通过。**

## 1. 输入、基线与范围

开始前完整读取并遵守：

- `MIGRATION_SOURCE_REPORT_26.1.2_FABRIC_PHASE20_FABRIC_INTEGRATION_BUILD.md`

只按本轮边界定向回看：

- `MIGRATION_SOURCE_REPORT_26.1.2_FABRIC_PHASE17_SHADER_RENDERTARGET_SCREENSHOT.md`：19 个 post effect、ShaderManager/PostChain owner、RenderTarget 与异步截图读取边界。
- `MIGRATION_SOURCE_REPORT_26.1.2_FABRIC_PHASE18_VIEWFINDER_CAPTURE_SEMANTICS.md`：Viewfinder、FOV、CameraCapture、EntitiesInFrame、EnderMan 语义边界。
- `MIGRATION_SOURCE_REPORT_26.1.2_FABRIC_PHASE19_COMMON_TAIL_FIRST_FABRIC_JAVAC.md`：common tail 与首次 Fabric javac 边界。

没有全量重扫历史阶段，没有重新处理已清零的编译迁移，没有执行 `clean`，没有进入新世界或旧世界，没有执行相机功能矩阵，没有启动 dedicated server。

Phase 20 输入基线保持为：

- Fabric production 编译错误 0；common test 编译错误 0。
- JUnit 39/39 通过。
- common/Fabric Access Widener 均通过。
- `clean build --continue` 和 `build` 成功。
- custom packet ID 30 个且唯一，活跃 `PacketDefinition` 29 个；`query_exposure_data` 未激活。

本轮没有修改 packet ID、方向、字段顺序、StreamCodec、菜单字段线上顺序、SavedData、ExtraData、Frame、ExposureData、照片路径/格式、像素编码、调色板、曝光、胶片或相机算法。

## 2. 运行环境与实际命令

### 2.1 环境

- Windows / PowerShell，时区 Asia/Shanghai。
- Java：OpenJDK 25.0.3，实际客户端进程为 `C:\Program Files\Java\jdk-25.0.3\bin\java.exe`。
- Gradle Wrapper：Gradle 9.4.0。
- Fabric Loom：1.16.3。
- Minecraft：26.1.2。
- Fabric Loader：0.19.3。
- Fabric API：0.150.0+26.1.2。
- Forge Config API Port：26.1.4。
- 每次 Gradle 命令只在当前 PowerShell 进程设置：
  `$env:GRADLE_USER_HOME='D:\Codex\Exposure\.gradle-home'`。
- 未修改用户全局 Gradle、Java 或系统环境。Loom 既有 launch classpath 仍可引用此前解析在用户 Gradle cache 中的依赖；本轮没有写入或重配用户全局环境。

### 2.2 基线与最终验证命令

非 clean 基线：

```powershell
.\gradlew.bat :common:compileJava :fabric:compileJava :common:compileTestJava :common:test :common:validateAccessWidener :fabric:validateAccessWidener --rerun-tasks --console=plain --stacktrace
```

客户端启动；每次确定性修复后使用同一任务：

```powershell
.\gradlew.bat :fabric:runClient --console=plain --stacktrace
```

各修复先运行最小相关验证：

```powershell
.\gradlew.bat :common:compileJava :fabric:compileJava --rerun-tasks --console=plain --stacktrace
.\gradlew.bat :common:processResources :fabric:processResources --rerun-tasks --console=plain --stacktrace
```

修改完成后的最终非 clean 回归：

```powershell
.\gradlew.bat :common:compileJava :fabric:compileJava :common:compileTestJava :common:test :common:validateAccessWidener :fabric:validateAccessWidener build --rerun-tasks --console=plain --stacktrace
```

另外使用 Java 25 JDK 的 `javap.exe -c -p -s` 定向核对 `LevelRenderer.extractVisibleEntities` 的 descriptor 与 `entitiesForRendering()` 调用次数；没有反编译或重扫全部历史目标。

## 3. 源码与构建基线复核

基线命令成功，日志为 `phase21-client-boot-baseline.log`：

- `:common:compileJava`、`:fabric:compileJava` 成功。
- `:common:compileTestJava` 成功。
- common test XML：9 个 suite、39 tests、0 failures、0 errors、0 skipped。
- `:common:validateAccessWidener`、`:fabric:validateAccessWidener` 成功。

启动前没有 Minecraft/KnotClient 游戏进程。工作区不是 Git repository，因此本轮以已知基线、文件时间、保存的原始日志和明确修改清单进行审计，没有把不存在的 Git diff 当作证据。

## 4. 首次启动结果与失败迭代

### 4.1 首次 runClient

首次命令为 `:fabric:runClient`，最早且足以解释终止的根错误是：

```text
java.lang.RuntimeException: Missing classTweaker file exposure.accesswidener from mod exposure
```

原始证据：

- `phase21-client-boot-first-runClient.log`
- `phase21-client-boot-first-latest.log`
- `phase21-client-boot-first-debug.log`

根边界是多模块开发 run 的 mod source-set/classpath grouping：Fabric resource root 被作为 `exposure` mod 根，但 common 的 `exposure.accesswidener` 当时只位于依赖 jar。production Fabric jar 已包含合并资源，问题只发生在开发启动布局。失败 JVM 没有自行退出，已显式停止，没有留在后台。

最小修复是在 `fabric/build.gradle` 的 Loom `mods.exposure` 中同时加入 common 与 Fabric main source set，并保持 common AW 为 `loom.accessWidenerPath`。没有复制或删除 AW，也没有降低校验。

### 4.2 确定性失败、根边界与最小修复

每轮只处理最早能解释终止的根错误；后续连锁错误没有被虚构为独立根因。

| 启动轮次 | 最早根错误/结果 | 实际 API 或资源边界 | 最小修复 | 原始证据 |
|---|---|---|---|---|
| 1 | 缺少 `exposure.accesswidener` | 多模块开发 mod source grouping | Loom mod source set 同时包含 common/Fabric | `phase21-client-boot-first-*` |
| 2 | `BoatMixin.onInteract` 在 `Boat` 找不到 `interact` | 26.1.2 方法上移到 `AbstractBoat`，且签名新增 hit location | Mixin target 改为 `AbstractBoat`、补 `Vec3`；用 `this instanceof Boat` 保持只作用普通 Boat，不扩到 ChestBoat | `phase21-client-boot-second-*` |
| 3 | `MinecraftMixin.disconnect` required injection 0/1 | `GameRenderer.resetData()` 位于 `disconnect(Screen,boolean,boolean)` | descriptor 改为三参数重载并补 handler boolean；注入点与 required 保持 | `phase21-client-boot-third-*` |
| 4 | `NullPointerException: Block id not set` | 26.1.2 Block/Item 构造 properties 需要 registry ResourceKey | 仅为原有两个 block 和 18 个 item 的 properties 写入原 ID 对应 key | `phase21-client-boot-fourth-*`、crash report |
| 5 | `BuggerKeyboardHandlerMixin` descriptor 无效 | `KeyboardHandler.keyPress` 改为 `(long,int,KeyEvent)` | handler 接收 `KeyEvent` 并读取 key/scancode/modifiers | `phase21-client-boot-fifth-*`、crash report |
| 6 | `GuiMixin` 找不到 `render` | GUI 已迁移到 render-state extraction | 目标改为 `extractRenderState` / `extractCrosshair`；原 Viewfinder 分支语义不变 | `phase21-client-boot-sixth-*`、crash report |
| 7 | `BuggerScreenRenderLinesInvoker` 找不到 `renderLines` | DebugScreenOverlay 方法名为 `extractLines` | 只更新 Invoker 目标名 | `phase21-client-boot-seventh-*`、crash report |
| 8 | `PlayerItemInHandLayerMixin` 的 `@Shadow getParentModel` 不在直接 target | 方法由父类 `RenderLayer` 继承 | 移除错误 shadow，通过目标实例调用继承 public 方法；手部 pose/照片提交语义不变 | `phase21-client-boot-eighth-*` |
| 9 | 已到主菜单，但资源 reload 期出现 `minecraft:pipeline/gui_text` source 缺失/编译错误 | dev-only Bugger overlay 在世界为空、shader source reload 未完成时强制提交 GUI text | Bugger overlay 仅在 dev env 且 `Minecraft.level != null` 时绘制；保留世界内调试用途 | 中间 `phase21-client-boot-final-*` |
| 10 | 已到主菜单，shader 错误清零；离线 Mixin 审计仍发现 41 条 class-version debug 诊断 | Exposure Mixin 配置仍声明 JAVA_17，而 26.1.2 类为 Java 25/major 69 | 仅 common/Fabric Mixin config 提升为 `JAVA_25`；required/defaultRequire 不变；未动 NeoForge 源集 | `phase21-client-boot-final-success-*` |
| 11 | 最终成功 | 主菜单启动边界稳定 | 无进一步修改 | `phase21-client-boot-final-success-java25-*` |

`Exposure.java` 的 properties key 初版误用不可见的 `Registries.BLOCK/ITEM` 常量，首次最小编译失败；保留于 `phase21-client-boot-fix4-block-item-ids-compile.log`。随后改用实际可用的 `BuiltInRegistries.BLOCK/ITEM.key()`，`phase21-client-boot-fix4-block-item-ids-compile-final.log` 成功。这是唯一编译失败尝试，没有通过绕开 registry ID 要求处理。

## 5. 最终到达主菜单与进程结束证据

最终第 11 次启动：

- GUI 自动化聚焦真实 `Minecraft* 26.1.2` Java 窗口后，画面明确显示完整主菜单：
  `Singleplayer`、`Multiplayer`、`Minecraft Realms`、`Options...`、`Quit Game`。
- 没有点击 Singleplayer/Multiplayer/Realms，没有创建或加载世界。
- 日志记录 `Reloading ResourceManager: vanilla, exposure, ...`。
- OpenAL 初始化成功、sound engine started。
- blocks/items/gui 等 texture atlas 创建完成。
- 主菜单确认后点击 `Quit Game`。
- 最终日志记录 `[Render thread/INFO] (Minecraft) Stopping!`。
- Gradle `:fabric:runClient` 返回 `BUILD SUCCESSFUL in 1m 43s`。
- 桌面窗口列表中已无 Minecraft Java 窗口；进程检查无 KnotClient/dev-launch-injector 游戏 JVM。

最终证据：

- `phase21-client-boot-final-success-java25-runClient.log`
- `phase21-client-boot-final-success-java25-latest.log`
- `phase21-client-boot-final-success-java25-debug.log`

## 6. 最终 ERROR、FATAL 与重要 WARN 审计

最终日志中没有 FATAL。没有 Exposure namespace/logger、Exposure Mixin、Exposure resource 或 Exposure initializer 的 ERROR。

| 级别/日志 | 归属与判断 |
|---|---|
| FCAP 建议安装 Mod Menu | 可选第三方体验建议；Mod Menu 不在本轮依赖要求中，不影响配置 spec 或客户端启动。没有为消警告引入 Mod Menu。 |
| Fabric Loader: `Found 1 non-fabric mod: exposure-common-...jar` | 多模块开发 run 将 common jar 作为 grouped source/dependency 的开发环境提示；最终 Fabric production jar 仍合并 common 内容。不是 production loader 回退，也不是 NeoForge mod。 |
| `Failed to fetch user properties`，HTTP 401 | Loom 开发会话的占位/未认证用户，非 Exposure。 |
| `Failed to fetch Realms feature flags`、`Couldn't connect to realms` | 同一未认证开发会话的 Realms 连锁症状；主菜单、资源 reload 与 Exposure initializer 均已完成。 |

明确为 Exposure 的启动问题且已清零：

- 第 1–8 轮的 AW/Mixin/registry 根错误。
- 第 9 轮由 dev-only Bugger overlay 过早提交 GUI text 引发的三个 `minecraft:pipeline/gui_text` ERROR。
- Mixin config 的 JAVA_17/major 69 debug 诊断；最终 common/Fabric 均为 JAVA_25。

最终日志不存在：

- Mixin apply failure、`InjectionError`、`InvalidInjectionException`。
- `ClassNotFoundException`、`NoClassDefFoundError`。
- Exposure 资源 JSON、ItemTintSource codec、model、menu、config 解码/注册错误。
- Exposure Shader/PostChain compile、link、uniform、sampler、target、resource location 或 lifecycle 错误。

## 7. Mixin apply 审计

### 7.1 配置安全性

- `exposure-common.mixins.json` 与 `exposure.mixins.json` 均保持 `"required": true`。
- 两者均保持 `"injectors": {"defaultRequire": 1}`。
- 没有设置 optional，没有降低 require，没有删除 Mixin，没有吞异常。
- 两者 compatibility level 现为 `JAVA_25`；最终日志全局也显示 `Compatibility level set to JAVA_25`。

### 7.2 required Fabric LevelRendererMixin

目标 descriptor：

```text
extractVisibleEntities(
  Lnet/minecraft/client/Camera;
  Lnet/minecraft/client/renderer/culling/Frustum;
  Lnet/minecraft/client/DeltaTracker;
  Lnet/minecraft/client/renderer/state/level/LevelRenderState;
)V
```

注入点：

```text
INVOKE
Lnet/minecraft/client/multiplayer/ClientLevel;
entitiesForRendering()Ljava/lang/Iterable;
```

结果：

- 最终 debug.log 明确记录 `Mixing LevelRendererMixin from exposure.mixins.json into net.minecraft.client.renderer.LevelRenderer`。
- handler descriptor 与目标方法四个参数完全一致。
- `javap` 对实际 26.1.2 `extractVisibleEntities` 只发现 **1 次** `ClientLevel.entitiesForRendering()` 调用。
- 因配置 required/defaultRequire=1、LevelRenderer 已加载、apply 没有异常且目标调用唯一，可确定实际注入次数为 **1**。

### 7.3 全部 Exposure client Mixin

最终主菜单边界共配置 28 个 client Mixin（common 27 + Fabric 1）：

- 实际 target 已加载并成功 apply：26。
- common 已 apply 的 25 个：
  `AnvilScreenMixin`、`AvatarRendererMixin`、`AvatarRenderStateMixin`、`BuggerKeyboardHandlerMixin`、`BuggerScreenOverlayMixin`、`BuggerScreenRenderLinesInvoker`、`CameraMixin`、`ClientPacketListenerMixin`、`EntityMixin`、`ExposureLightTextureMixin`、`GameRendererMixin`、`GameRendererLightmapAccessor`、`GuiMixin`、`HumanoidModelMixin`、`HumanoidMobRendererMixin`、`HumanoidRenderStateMixin`、`ItemFrameRendererMixin`、`ItemFrameRenderStateMixin`、`ItemInHandRendererMixin`、`KeyboardHandlerMixin`、`KeyMappingMixin`、`LocalPlayerMixin`、`MinecraftMixin`、`MouseHandlerMixin`、`PlayerItemInHandLayerMixin`。
- Fabric 已 apply：`LevelRendererMixin`。
- 在主菜单边界未触发 target class 加载：`KeyBindTooltipMixin`（需要进入按键设置列表）、`MultiPlayerGameModeMixin`（需要进入世界/创建 game mode）。它们仍为 required 配置项，没有被设为 optional；本轮没有越界打开设置矩阵或世界来强行触发，列入后续运行风险。

common initializer `ExposureFabric.onInitialize` 没有导入 `net.minecraft.client`、Screen、LevelRenderer 或 Shader；客户端类只在 `ExposureFabricClient.onInitializeClient` 路径注册。最终启动没有 common initializer 解析客户端专用类的 `ClassNotFoundException`/`NoClassDefFoundError`。

## 8. camera tint codec 与客户端注册

### 8.1 `exposure:camera_glass`

- `ExposureFabricClient` 在 client initializer 中、资源 reload 前把 `CameraGlassTintSource.MAP_CODEC` 注册为 `exposure:camera_glass`。
- `assets/exposure/items/camera.json` 中实际有 14 次 `exposure:camera_glass`，对应 Phase 20 的 14 个玻璃 tint model leaf。
- camera item JSON 总体还有未使用该 tint 的 selfie stick/flash/viewfinder 等组合模型；没有把它们误计为玻璃分支。
- 最终资源 reload 成功；没有 unknown tint source、ItemTintSource codec、item model 或 JSON decode 错误。
- 本轮未进入世界，因此不声称已目视验证 GUI/手持玻璃颜色。

### 8.2 其他 client initializer 注册

| 注册项 | 启动审计结果 |
|---|---|
| HUD | `exposure:camera_stand_tooltip` 通过 `HudElementRegistry.addLast` 注册；无 duplicate/lifecycle 错误。 |
| KeyMapping | `key.exposure.camera_controls`、category `exposure:main` 经 `KeyMappingHelper` 注册；无重复/缺失错误。 |
| Tooltip callback | `ClientTooltipComponentCallback` 的 Photograph 分支注册完成；无 callback 类型错误。 |
| Client resource reload listener | `exposure:clear_client_exposures_cache` 注册；最终 resource reload 无异常。 |
| Entity renderer | Photograph frame、clear photograph frame、camera stand 三个 renderer 注册完成；无 missing/duplicate 错误。 |
| Block entity renderer | 当前 Fabric client 源码没有专用 block-entity-renderer 注册；只有 common block entity type 注册，因此本轮没有可失败的 client BE renderer lifecycle 项，也没有日志报告缺失 renderer。 |
| MenuScreens | 6 个活跃 screen factory 注册：camera in hand、camera on stand、album、lectern album、lightroom、item rename；无重复/生命周期错误。 |
| dormant signed album menu | Phase 20 已明确记录：第 7 个 `signed_album` menu type 保留但无 server open path、没有 `MenuScreens` 注册，实际 SignedAlbumItem 直接打开 client-only `AlbumViewScreen`；本轮未把这一历史 dormant 边界误报为新启动缺失。 |
| 配置 Screen | `ConfigurationScreen::new` 只在 `ExposureFabricClient` 注册；没有进入 common initializer。 |

## 9. Phase 17 的 19 个 Shader/PostChain

实际发现两套同名资源：

- 当前 26.1.2 资源：`assets/exposure/post_effect/<name>.json`，共 19 个。
- 为旧稳定业务 Identifier 保留的 legacy 数据：`assets/exposure/shaders/post/<name>.json`，共 19 个；`Shader.postEffectId` 将 `exposure:shaders/post/<name>.json` 严格桥接到 `exposure:<name>`。未删除旧资源或更改 filter 数据。

19 个当前 post-effect JSON 均可解析，均为 2 passes、1 个 persistent `swap` target；自定义颜色效果引用 `minecraft:program/exposure_color_processor`，`invert` 引用 vanilla invert，`bsod` 的第二 pass 引用 vanilla bits。自定义 `exposure_color_processor.fsh`/定义资源实际存在。

| 效果 | 启动阶段实际结果 |
|---|---|
| `black_tint` | 当前 JSON 存在且语法通过；Exposure resource pack reload 无 JSON/位置/target/sampler/uniform/lifecycle 错误。 |
| `blue_filter` | 同上。 |
| `brown_tint` | 同上。 |
| `bsod` | JSON/两 pass/persistent swap 结构存在；无 bits pass、Resolution/MosaicSize 或资源错误。 |
| `crisp` | 当前 JSON 存在且语法通过；启动日志无相关错误。 |
| `cyan_tint` | 同上。 |
| `gray_tint` | 同上。 |
| `green_filter` | 同上。 |
| `invert` | JSON/vanilla invert + blit 两 pass 结构存在；启动日志无相关错误。 |
| `light_blue_tint` | 当前 JSON 存在且语法通过；启动日志无相关错误。 |
| `light_gray_tint` | 同上。 |
| `lime_tint` | 同上。 |
| `magenta_tint` | 同上。 |
| `orange_tint` | 同上。 |
| `pink_tint` | 同上。 |
| `purple_tint` | 同上。 |
| `red_filter` | 同上。 |
| `white_tint` | 同上。 |
| `yellow_tint` | 同上。 |

重要限定：ShaderManager 在资源 reload 阶段发现/准备资源，但未选择的 Exposure filter 不会因为停留主菜单就逐条调用 `getPostChain/process`。因此本轮可确认的是 **19 个资源存在、现代 JSON 结构通过、Exposure resource reload 与启动阶段没有编译/链接/uniform/sampler/target/JSON/位置/lifecycle 失败**；不伪造“19 个效果已逐一 GPU 应用或目视验证”。逐个 PostChain 实例化、驱动链接、窗口 resize 与视觉结果属于下一阶段世界/核心功能矩阵。

第 9 轮出现的 `minecraft:pipeline/gui_text` 三条 ERROR 与上述 19 个 PostChain 无关，但其触发方是 Exposure dev-only Bugger overlay 的过早 GUI text 提交。加入“仅世界存在时绘制”的最小生命周期门槛后，第 10、11 轮该错误均为 0；没有删除任何 Shader、没有替换空资源，也没有绕过 ShaderManager。

## 10. Forge Config API Port 三类配置

最终 debug.log 明确记录：

- `exposure-server.toml`：spec/file 被 ConfigTracker tracking；客户端生命周期不加载或创建 server config，磁盘不存在该文件是正常结果。
- `exposure-common.toml`：从 `fabric/run/config/exposure-common.toml` 成功加载并 watching。
- `exposure-client.toml`：从 `fabric/run/config/exposure-client.toml` 成功加载并 watching。

现有 common/client TOML 未被删除、重建或改写；没有损坏，因此没有备份/修复操作。没有配置 parse、reload、save、type 或 lifecycle 错误。

日志中的 `net.neoforged.fml.config.ConfigTracker` 和源码中的 `net.neoforged.fml.config.ModConfig` 是 Forge Config API Port 提供的兼容 API namespace，不代表加载 NeoForge loader。最终 Fabric mod 列表没有 NeoForge loader/FML 模组；common initializer 也没有引用 `ConfigurationScreen`，该 Screen 只存在于 client initializer。

## 11. 修改文件与原因

| 文件 | 最小修改 |
|---|---|
| `fabric/build.gradle` | Loom dev mod grouping 加入 common/Fabric source set，使开发启动能看到 common AW、Mixin 与资源。 |
| `common/.../mixin/BoatMixin.java` | 对齐 `AbstractBoat`/hit-location 签名，并用 Boat guard 保持原适用范围。 |
| `common/.../mixin/client/MinecraftMixin.java` | disconnect 目标改为实际三参数重载。 |
| `common/.../Exposure.java` | 为原 ID 的两个 block、18 个 item properties 设置 26.1.2 强制 ResourceKey。 |
| `common/.../mixin/client/BuggerKeyboardHandlerMixin.java` | 对齐 `KeyEvent` 参数。 |
| `common/.../mixin/client/GuiMixin.java` | 对齐 GUI extract 方法名。 |
| `common/.../mixin/client/BuggerScreenRenderLinesInvoker.java` | Invoker 对齐 `extractLines`。 |
| `common/.../mixin/client/PlayerItemInHandLayerMixin.java` | 从 target 实例调用继承的 `getParentModel`，不再错误 shadow。 |
| `common/.../mixin/client/BuggerScreenOverlayMixin.java` | dev overlay 仅在已有世界时提交 GUI text，避免资源 reload 前 pipeline 编译。 |
| `common/src/main/resources/exposure-common.mixins.json` | compatibility level JAVA_17 → JAVA_25；required/defaultRequire 不变。 |
| `fabric/src/main/resources/exposure.mixins.json` | compatibility level JAVA_17 → JAVA_25；required/defaultRequire 不变。 |

没有修改 Phase 17 的 Shader/PostChain/RenderTarget/async screenshot 业务实现或 19 个效果资源；没有修改 Phase 18 的 Viewfinder、FOV、CameraCapture、EntitiesInFrame、EnderMan 路径；没有引入 Architectury、Forge/NeoForge loader、Quilt、Create、JEI 或 Jade。

## 12. 最终验证

最终验证日志：`phase21-client-boot-final-validation.log`。

结果：

- `:common:compileJava`：通过。
- `:fabric:compileJava`：通过。
- `:common:compileTestJava`：通过。
- `:common:test`：39/39，0 failures，0 errors，0 skipped。
- `:common:validateAccessWidener`：通过。
- `:fabric:validateAccessWidener`：通过。
- `:common:build`、`:fabric:build`：通过。
- 总结果：`BUILD SUCCESSFUL`，15 actionable tasks，15 executed。

PowerShell 将 javac 的 deprecation/unchecked stderr note 包装显示为 `NativeCommandError` 文本，但 Gradle task 与进程 exit code 均成功；它不是 Java 编译错误。

最终 jar：

- `common/build/libs/exposure-common-26.1.2-1.9.18.jar`
- `fabric/build/libs/exposure-fabric-26.1.2-1.9.18.jar`

packet 源码未修改；Phase 20 的 30 个唯一 packet ID、29 个活跃 `PacketDefinition` 继续作为本轮基线。定向搜索确认 `query_exposure_data` 仍只在历史 `QueryExposureDataC2SP.ID` 中，没有加入 C2S definitions 或 Fabric receiver。

## 13. 保存的日志与 crash report

关键日志均位于 `D:\Codex\Exposure`，文件名均以 `phase21-client-boot-` 开头。

首次启动：

- `phase21-client-boot-first-runClient.log`
- `phase21-client-boot-first-latest.log`
- `phase21-client-boot-first-debug.log`

最终成功：

- `phase21-client-boot-final-success-java25-runClient.log`
- `phase21-client-boot-final-success-java25-latest.log`
- `phase21-client-boot-final-success-java25-debug.log`
- `phase21-client-boot-final-validation.log`

失败/中间启动：

- `phase21-client-boot-second-{runClient,latest,debug}.log`
- `phase21-client-boot-third-{runClient,latest,debug}.log`
- `phase21-client-boot-fourth-{runClient,latest,debug}.log`
- `phase21-client-boot-fifth-{runClient,latest,debug}.log`
- `phase21-client-boot-sixth-{runClient,latest,debug}.log`
- `phase21-client-boot-seventh-{runClient,latest,debug}.log`
- `phase21-client-boot-eighth-{runClient,latest,debug}.log`
- 中间首次到主菜单但仍有 gui_text lifecycle ERROR：`phase21-client-boot-final-{runClient,latest,debug}.log`
- Bugger guard 后成功：`phase21-client-boot-final-success-{runClient,latest,debug}.log`

crash reports：

- `phase21-client-boot-fourth-crash-report.txt`
- `phase21-client-boot-fifth-crash-report.txt`
- `phase21-client-boot-sixth-crash-report.txt`
- `phase21-client-boot-seventh-crash-report.txt`

第 1–3 轮没有生成 crash report；第 8 轮进入 broken resource-reload fallback 后由测试者关闭，未生成 crash report；原始 latest/debug/runClient 已保留。所有最小编译验证日志也以 `phase21-client-boot-fix1-` 至 `phase21-client-boot-fix10-` 保存，没有覆盖失败现场。

## 14. 仍未验证的风险

严格按本轮范围，下列内容没有验证：

1. 没有进入世界；`MultiPlayerGameModeMixin` 未发生实际 lazy apply。
2. 没有打开 Controls 列表；`KeyBindTooltipMixin` 未发生实际 lazy apply。
3. 19 个 Exposure PostChain 没有逐一业务激活、驱动编译/链接、resize 或目视比较；启动阶段没有发现错误，但不能替代功能矩阵。
4. camera.json 14 个 glass tint leaf 已成功解码，但没有进入世界目视验证 GUI/手持颜色。
5. Camera Stand HUD、KeyMapping 实际输入、Photograph tooltip、六个 MenuScreen、config Screen、entity renderer 没有做交互矩阵。
6. server config 在客户端只 tracking，未加载/创建；dedicated server 未启动。
7. dormant `signed_album` menu 与 lectern album 历史边界继续沿用 Phase 20 风险记录。
8. 未验证新/旧世界、SavedData round-trip、照片文件、像素/调色板/曝光/胶片/相机算法。
9. 开发会话无有效 Mojang/Realms 认证；相关 401 不影响本轮 Exposure 启动结论。

## 15. 下一阶段建议

下一步默认应是 **Phase 21 的世界进入与核心功能运行时矩阵**，而不是重新开始编译迁移。建议按风险优先顺序：

1. 进入一个受控测试世界，先触发 `MultiPlayerGameModeMixin`、LevelRenderer/Camera Stand 与 renderer 路径。
2. 逐一实例化 19 个 PostChain，记录 GPU compile/link、uniform/sampler/target、resize 与视觉结果。
3. 验证 camera 14 个 glass tint 分支、Viewfinder/FOV/CameraCapture/EntitiesInFrame/EnderMan 核心矩阵。
4. 打开 Controls、MenuScreens 与 config Screen，触发剩余 lazy Mixin 和 UI 生命周期。
5. dedicated server 与旧世界/旧数据继续保持独立后续阶段。

## 状态交接

- **已完成**：非 clean 基线复核；首次 Fabric client 启动；10 个确定性启动/审计边界的最小修复；最终主菜单画面确认；干净退出；完整 ERROR/WARN、Mixin、LevelRenderer、tint、注册、19 Shader、三类配置审计；最终编译/39 测试/双 AW/build。
- **关键决策**：不降低 Mixin require，不删除/空置 Shader，不绕过 ShaderManager；开发 source grouping 修正而非复制 AW；只修最早根错误；保留 Phase 17/18 语义与所有业务协议/数据格式。
- **验证结果**：最终 runClient 与 build 均 `BUILD SUCCESSFUL`；39/39；LevelRendererMixin 唯一注入点成功 apply 1 次；Exposure 启动 ERROR 0；客户端进程已结束。
- **失败尝试**：首次缺 AW；随后依次暴露 Boat、Minecraft disconnect、registry ID、Keyboard、Gui extraction、Debug invoker、Player hand inherited method；一次错误 registry constant 编译尝试；一次 gui_text 过早生命周期错误。原始日志/crash report 均保留。
- **遗留问题**：只剩本轮明确未触发/未交互的运行时风险，不存在已知主菜单启动阻塞。
- **下一步**：Phase 21 世界进入与核心功能运行时矩阵。
