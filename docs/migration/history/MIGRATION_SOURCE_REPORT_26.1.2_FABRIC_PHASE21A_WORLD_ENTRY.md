# Minecraft 26.1.2 Fabric 迁移报告：Phase 21A 世界进入、懒加载路径与基础运行时排雷

## 1. 结论

Phase 21A 已完成。当前源码对应的 Fabric 开发客户端能够创建并进入受控测试世界 `Phase21A World Entry`，integrated server 能启动，本地玩家能登录，HUD、世界画面和区块能稳定出现；世界能够保存退出、再次进入并再次保存退出。`KeyBindTooltipMixin` 与 `MultiPlayerGameModeMixin` 均在其真实懒加载路径中成功 apply，未通过降低 `require`、改 optional、删除 Mixin 或吞异常来绕过。

本轮确认并修复了三个确定性运行时问题：

1. integrated server 初次存档早于 Exposure server 状态初始化，导致 `ServerEvents.onServerSave` 访问未初始化仓库；
2. 26.1.2 的 `MultiPlayerGameMode` 交互方法已由旧目标 `interactAt` 变为 `interact(...)`，required Mixin 在实际进入世界时失败；
3. `CameraStandTooltip` 把 `minecraft:default` 当作自定义 tooltip style 传入 26.1.2 API，生成紫黑缺失纹理背景；默认样式应传 `null`。

最终非 clean 验证通过：common/Fabric `compileJava`、common `compileTestJava`、9 个测试套件共 39 个测试、双 Access Widener 和完整 `build` 全部成功。最终 Fabric JAR 已生成，但未安装到用户的正式 Minecraft Launcher 实例；本报告不把 dev `runClient` 结果表述为 production JAR 验收，也不声称已经完成完整视觉或拍摄验收。

## 2. 输入、边界与环境

### 2.1 已读取输入

- 完整读取 `MIGRATION_SOURCE_REPORT_26.1.2_FABRIC_PHASE21_CLIENT_BOOT.md`，以其中的源码状态、已修复项和遗留风险为最高优先级基线。
- 按需读取 `MIGRATION_SOURCE_REPORT_26.1.2_FABRIC_PHASE20_FABRIC_INTEGRATION_BUILD.md` 中七个 menu type、`signed_album` dormant 边界及可达 UI 路径。
- 按需参考 `MIGRATION_SOURCE_REPORT_26.1.2_FABRIC_PHASE14_RENDER_PIPELINE_RENDERERS.md` 中 Camera Stand renderer 状态。
- Phase 17/18 只沿用与本轮实际错误和禁止回退有关的 Shader/RenderTarget/Screenshot ownership、Viewfinder/FOV/Capture/EntitiesInFrame/EnderMan 边界，没有重新扫描或改写已完成迁移。

### 2.2 用户早期 JAR 证据及限制

用户报告：较早构建的初代 Fabric JAR 能到主菜单，但在加入世界、尚未完成世界进入前崩溃。这是有效的生产侧症状证据，但该 JAR 早于 Phase 21 的全部修复，且本轮没有其精确构建时间、源码快照和完整崩溃日志，因此不能把它的根因直接等同于本轮当前源码发现的任一根因。

本轮从当前 Phase 21 源码与当前构建基线稳定复现并诊断，不继续把旧 JAR 当唯一测试对象。新 JAR 仅构建、未覆盖或删除用户旧 JAR，也未擅自写入正式 Launcher 实例。

### 2.3 实际环境

- 工作区：`D:\Codex\Exposure`；不是 Git 仓库，因此没有声称存在 Git diff、commit 或回滚分支。
- Java：`C:\Program Files\Java\jdk-25.0.3`（Java 25）。
- Gradle：项目 wrapper，Gradle 9.4.0。
- Gradle user home：`D:\Codex\Exposure\.gradle-home`。
- Minecraft：26.1.2；Fabric Loader 0.19.3；Fabric Loom 1.16.3。
- 全程未执行 `clean`，未删除既有 `run` 目录、配置、日志、存档或证据。

## 3. 命令与静态基线

环境变量在每次 Gradle 运行前显式设为：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-25.0.3'
$env:GRADLE_USER_HOME='D:\Codex\Exposure\.gradle-home'
```

Phase 21A 非 clean 基线命令：

```powershell
.\gradlew.bat :common:compileJava :fabric:compileJava :common:compileTestJava :common:test :common:validateAccessWidener :fabric:validateAccessWidener --rerun-tasks --console=plain --stacktrace
```

结果：`BUILD SUCCESSFUL`；common/Fabric 编译通过；9 个测试套件、39 个测试，0 failures、0 errors、0 skipped；两个 Access Widener 验证通过。证据为 `phase21a-world-entry-baseline.log`。

每个最小源码修复后先运行：

```powershell
.\gradlew.bat :common:compileJava :fabric:compileJava --rerun-tasks --console=plain --stacktrace
```

三个最终修复均先通过最小相关编译，之后才重新启动客户端。

开发客户端命令：

```powershell
.\gradlew.bat :fabric:runClient --console=plain --stacktrace
```

## 4. 客户端启动与诊断序列

| 轮次 | 实际触发路径 | 结果与最早根错误 | 处理 |
|---|---|---|---|
| 1 | 主菜单 → Options/Controls/Key Binds → 新建默认世界 | `KeyBindTooltipMixin` 成功真实 apply；新世界加载期间 integrated server 在 `SERVER_STARTED` 前先保存，`ServerEvents.onServerSave` 访问未初始化 `ExposureServer`，NPE 终止进入 | 保存完整 runClient/latest/debug/crash report；只修复 server 生命周期边界 |
| 2 | 再次进入同一受控测试流程 | server save 根错误消失；客户端真正加载 `MultiPlayerGameMode` 时 required `MultiPlayerGameModeMixin` 无法找到旧目标 `interactAt`，抛出 `InvalidInjectionException` | 用 26.1.2 类签名核对 descriptor；只更新 Mixin 目标，未降低安全检查 |
| 3 | 进入世界并稳定观察 | integrated server 启动、玩家登录、HUD/世界/区块出现；Mixin 成功；资源重载与有限 UI/物品烟雾测试完成；相机支架目标附近出现紫黑缺失纹理块 | 保存成功现场；继续只定位明显阻碍后续人工测试的确定性问题 |
| 4 | 将 stand 上相机渲染上下文从 `NONE` 临时改为 `FIXED` | 缺失纹理仍存在，renderer context 假设被否定 | 完整回退实验改动；保留编译日志和“仍缺失”截图 |
| 5 | 临时规范化插入 stand 的相机 active 状态；并用 vanilla `item_display` 渲染同一 ItemStack 比较 | `item_display` 中同一相机正常，active 状态实验不改变紫黑块 | 回退业务状态实验；由对照排除相机 ItemStack/model 本体 |
| 6 | 临时以 null owner 更新 top item；移动 stand/改变准星命中进行分离观察 | owner 实验不改变结果；紫黑块随准星命中 stand 的 HUD 提示出现/消失，而 stand 上相机本体持续正常 | 回退 owner 实验；根因收敛到 `CameraStandTooltip` 背景 style |
| 7 | 应用 tooltip 最小修复后进入、保存退出、重进同一世界、再次保存退出、Quit Game | 两次 integrated server 启动与本地登录均成功；存档与 stand 上相机持久化；runClient `BUILD SUCCESSFUL`，退出码 0 | 保存 final-success runClient/latest/debug 和重进截图 |
| 8（补充证据） | 近距离真实命中 stand 碰撞箱 | 明确显示正常的默认深色 tooltip 背景及相机图标，无紫黑缺失纹理；正常保存并退出，游戏客户端进程为 0 | 以 `phase21a-world-entry-final-camera-stand-tooltip.png` 作为最终 tooltip 证据 |

第 4～6 轮是受控单变量诊断。`CameraStandEntityRenderer` 的 `ItemDisplayContext.NONE`、相机插入时的 active 业务状态和 `ItemModelResolver` owner 均已恢复原状，不在最终变更中。

## 5. 根错误、触发边界与最小修复

### 5.1 根错误一：初次保存早于 Exposure server 初始化

最早异常是：

```text
Cannot get a field in ExposureServer: server is not initialized yet.
```

实际路径为 integrated server 加载存档/出生区块期间发生一次保存，Fabric 的 save 回调进入 `ServerEvents.onServerSave()`；但 `Exposure.initServer(server)` 在 `serverStarted` 才初始化 `ExposureRepository` 和 `ExposureFrameHistory`。主菜单没有 server 和保存回调，因此 Phase 21 主菜单启动不会暴露此问题。

最小修复：

- `ExposureServer.isInitialized()` 只在 repository 与 frame history 均存在时返回 true；
- `onServerSave()` 只在已初始化后执行超时 expected exposure 清理；
- `ExposureServer.clear()` 在 server stop 时清除静态 server 状态，避免同一 dev 客户端连续进入世界时遗留上一实例状态。

语义风险：初始化前发生的那次保存不运行 Exposure repository 清理；此时 repository 本来尚不存在，因此这是生命周期保护，不是吞掉已建立仓库上的错误。真正初始化后的保存仍执行原清理逻辑。

### 5.2 根错误二：required `MultiPlayerGameModeMixin` 目标改名

26.1.2 的实际目标是：

```text
interact(Player, Entity, EntityHitResult, InteractionHand): InteractionResult
```

旧 `interactAt` 名称在 26.1.2 已不存在。该类只在世界交互路径真正加载，因此主菜单阶段不会失败。

最小修复仅把 `@Inject` 的完整方法 descriptor 更新为 `interact(...)`，继续在 `EntityHitResult.getLocation()` 调用点注入，handler 的 active camera 语义、取消行为和第二个 `useItemOn` 注入不变。最终 debug 日志记录：

- `Mixing client.MultiPlayerGameModeMixin ... into net.minecraft.client.multiplayer.MultiPlayerGameMode`；
- `onInteractAt(...)` 与 `onUseItemOn(...)` 两个 handler 均成功准备/apply；
- 没有 `InjectionError`、`InvalidInjectionException`、目标缺失或注入次数异常。

语义风险：这是 required Mixin 的精确 26.1.2 绑定，未来再次变更方法或调用点时仍会 fail-fast；本轮没有把它改成静默失败。

### 5.3 根错误三：tooltip 默认 style 传参错误

最初紫黑块容易被误判为 Camera Stand 上相机的 missing model。单变量实验与 vanilla `item_display` 对照证明：同一相机 ItemStack 本体和 renderer 正常；紫黑块只随准星对 stand 的 EntityHitResult 出现。

最终根因位于：

```java
TooltipRenderUtil.extractTooltipBackground(guiGraphics, x, y, 18, 18,
        Identifier.withDefaultNamespace("default"));
```

在 26.1.2 中，非 null Identifier 表示自定义 tooltip style，并据此解析自定义背景/边框 sprite；`minecraft:default` 不是“选择默认样式”的哨兵，因而请求不存在的 style sprite 并显示缺失纹理。API 的 vanilla 默认路径由 null 选择。

最小修复为传 `null` 并删除不再使用的 `Identifier` import。最终近距离命中截图显示默认深色边框和相机 item icon，紫黑缺失纹理消失。该修复不改变相机 model、Camera Stand renderer、ItemStack 数据或业务逻辑。

## 6. 最终源码变更清单

仅以下四个生产源码文件是本轮最终变更：

1. `common/src/main/java/io/github/mortuusars/exposure/ExposureServer.java`
   - 新增 `isInitialized()` 与 `clear()`。
2. `common/src/main/java/io/github/mortuusars/exposure/event/ServerEvents.java`
   - save 清理增加初始化保护；server stop 清除静态状态。
3. `common/src/main/java/io/github/mortuusars/exposure/mixin/client/MultiPlayerGameModeMixin.java`
   - required 注入目标更新为 26.1.2 的完整 `interact(...)` descriptor。
4. `common/src/main/java/io/github/mortuusars/exposure/client/gui/tooltip/CameraStandTooltip.java`
   - 默认 tooltip style 参数由 `minecraft:default` 改为 null。

未修改 packet ID、方向、字段顺序、StreamCodec、menu 字段线上顺序、SavedData、ExposureData、Frame、照片路径/格式、像素编码、调色板、曝光、胶片或相机算法；未重新引入 Architectury/Forge/NeoForge 或无关依赖；未修改任何配方、registry 或业务数据来方便测试。

## 7. 懒加载、世界生命周期与有限烟雾测试

### 7.1 required Mixin

- `KeyBindTooltipMixin`：第一轮真实打开 Options → Controls/Key Binds。`phase21a-world-entry-first-debug.log` 记录其 mix into `KeyBindsList$KeyEntry` 及 `changeBackpackKeyHover` handler，未发生注入错误。
- `MultiPlayerGameModeMixin`：最终成功世界运行的 debug 日志记录类 mixin 和两个 handler；旧 descriptor 的失败现场完整保留在第二轮日志。

### 7.2 世界与 integrated server

- 使用默认世界生成创建新的受控 Creative 测试世界 `Phase21A World Entry`，没有打开、复制、升级或写入用户正式旧世界，也没有用超平坦、空世界或禁用数据包规避加载。
- 日志确认 integrated Minecraft server 26.1.2 启动、出生区块加载、Player 本地登录并 joined game、view/simulation distance 建立、HUD 和世界画面出现。
- 成功运行中没有 Exposure 异常刷屏、断线、自动退回菜单、ClassNotFoundException、NoClassDefFoundError、AbstractMethodError、NoSuchMethodError、Mixin apply failure、packet receiver 未注册、render-state 类型错误或 client/server 环境混用错误。
- Save and Quit 后日志确认 saving chunks、saving players、stopping server、all dimensions saved；随后同一存档再次进入并再次完成本地登录，Camera Stand 与其中相机保持存在；再次保存退出。
- 最终 `Quit Game` 后 runClient 返回 0。只针对 `runClient`/KnotClient/devlaunchinjector 的进程检查结果为 0；Gradle daemon 被按要求保留，没有误杀无关 Java 进程。

### 7.3 有限 UI、物品与资源生命周期

在世界稳定后仅做了确定性烟雾测试：

- 通过正常 registry ID/Creative 条件取得 Camera、Camera Stand、Album、Photograph、Lightroom 和测试所需基础物品；
- Camera Stand 能放置，相机能装入 stand，客户端和 integrated server 不崩溃；
- Camera 菜单、Album、Item Rename、Lightroom 各完成一次可达路径的打开/关闭；只判断菜单构造与 open-data 路径没有明显异常，不判断布局美观、按钮语义或完整业务正确性；
- Phase 20 的七个 menu type 历史边界保持不变；没有伪造 `signed_album` server open path，也没有删除 dormant 类型；lectern album 的完整合法数据条件本轮未构造，因此未验证；
- 世界内执行一次客户端资源重载，重载完成，没有观察到 capture 清理、shader source、gui_text、resource listener future/barrier 或客户端 exposure cache 生命周期错误。

资源重载成功只证明这次生命周期没有明显错误，不证明 19 个 PostChain 已逐一正确渲染。

## 8. 已知非阻塞错误与明确未验收范围

### 8.1 数据包配方迁移债务

每次 server 数据加载稳定出现 17 个 Exposure recipe JSON parse error，主要是旧 ingredient/tag/component/pattern 格式不符合 26.1.2 codec。服务器仍加载 1515 个 recipes 并能进入世界；最终两次世界进入的同一日志中合计 34 条，即每次 17 条。

这些错误不再阻止 Phase 21A 世界进入，但意味着不能声称 data resources 全部干净，也会影响对应配方可用性。根据本轮“不改配方/业务数据来方便测试”的边界，本轮没有顺手迁移它们；应在独立的配方/数据包迁移阶段处理。

### 8.2 开发环境网络噪声

offline/dev 用户属性、Realms feature flags 和 Realms 连接出现 401/认证错误；它们不在 Exposure world-entry 根路径，未阻止本地单人世界，也未作为生产逻辑问题修改。

### 8.3 未完成的视觉与拍摄验收

- 没有主观确认 Camera 的 14 个 glass tint 颜色正确；本轮最多只保留“可存在/无明显 missing model 或 unknown tint source”的基础观察，不能替代人工色彩比较。
- 没有逐一验收 19 个滤镜/PostChain 画面。
- 未完成 direct/background capture、Viewfinder/FOV、EntitiesInFrame、EnderMan、照片像素内容、窗口 resize、Camera Stand 本地玩家补偿的完整矩阵。
- 没有完成 production JAR 世界进入和核心拍摄验收。

## 9. 最终验证与构建产物

最终非 clean 命令：

```powershell
.\gradlew.bat :common:compileJava :fabric:compileJava :common:compileTestJava :common:test :common:validateAccessWidener :fabric:validateAccessWidener build --rerun-tasks --console=plain --stacktrace
```

捕获日志记录 `BUILD SUCCESSFUL in 16s`，15 个 actionable tasks 全部 executed。为排除 PowerShell `Tee-Object` 把 javac 的 deprecation/unchecked 提示误分类为 `NativeCommandError` 的外层状态歧义，随后直接重复同一 Gradle 命令；该次 `BUILD SUCCESSFUL in 13s`，进程退出码 0。

最终结果：

- common `compileJava`：通过；
- Fabric `compileJava`：通过；
- common `compileTestJava`：通过；
- common tests：9 suites / 39 tests / 0 failures / 0 errors / 0 skipped；
- common Access Widener：通过；
- Fabric Access Widener：通过；
- common/Fabric `build`：通过；
- 未执行 `clean`。

最终 Fabric 产物：

- 路径：`fabric/build/libs/exposure-fabric-26.1.2-1.9.18.jar`
- 大小：2,055,316 bytes
- 修改时间：2026-07-16 20:16:56 +08:00
- SHA-256：`14BB3F7CF771E238D13618ED721154D7EB2C38B3AC391A45F5F2418B4EE20D47`
- `fabric.mod.json`：Exposure 1.9.18，Minecraft `~26.1.2`，Java `>=25`，Fabric Loader `>=0.19.3`。

同时生成 common JAR：

- 路径：`common/build/libs/exposure-common-26.1.2-1.9.18.jar`
- 大小：2,011,464 bytes
- 修改时间：2026-07-16 20:16:59 +08:00
- SHA-256：`D2E4CAC58ADF10FB365CE5B9E0287D1C2D412AB5D59863C3323D812251D45549`

## 10. 证据索引

### 10.1 基线、失败现场与诊断

- `phase21a-world-entry-baseline.log`
- `phase21a-world-entry-first-runClient.log`
- `phase21a-world-entry-first-latest.log`
- `phase21a-world-entry-first-debug.log`
- `phase21a-world-entry-first-crash-report.txt`
- `phase21a-world-entry-first-main-menu.png`
- `phase21a-world-entry-first-world-config.png`
- `phase21a-world-entry-second-runClient.log`
- `phase21a-world-entry-second-latest.log`
- `phase21a-world-entry-second-debug.log`
- `phase21a-world-entry-second-network-protocol-error.png`
- `phase21a-world-entry-multiplayergamemode-javap.log`
- `phase21a-world-entry-item-layer-javap.log`
- `phase21a-world-entry-fix1-server-save-lifecycle-compile.log`
- `phase21a-world-entry-fix2-multiplayergamemode-mixin-compile.log`
- `phase21a-world-entry-fix3-camera-stand-fixed-context-compile.log`
- `phase21a-world-entry-fix3-camera-stand-inactive-normalization-compile.log`
- `phase21a-world-entry-fix3-camera-stand-null-owner-compile.log`
- `phase21a-world-entry-fix3-camera-stand-tooltip-default-style-compile.log`
- `phase21a-world-entry-fourth-fixed-context-still-missing.png`

临时实验日志保留用于说明排除过程；对应实验源码均已回退。

### 10.2 成功世界、UI、重载、持久化与最终退出

- `phase21a-world-entry-third-runClient.log`
- `phase21a-world-entry-third-latest.log`
- `phase21a-world-entry-third-debug.log`
- `phase21a-world-entry-third-world-resource-reload.png`
- `phase21a-world-entry-third-camera-menu.png`
- `phase21a-world-entry-third-album-menu.png`
- `phase21a-world-entry-third-item-rename-menu.png`
- `phase21a-world-entry-third-lightroom-menu.png`
- `phase21a-world-entry-third-camera-stand-missing-model.png`（实际为修复前 tooltip 背景缺失纹理）
- `phase21a-world-entry-final-success-runClient.log`
- `phase21a-world-entry-final-success-latest.log`
- `phase21a-world-entry-final-success-debug.log`
- `phase21a-world-entry-final-reentry.png`
- `phase21a-world-entry-final-proof-latest.log`
- `phase21a-world-entry-final-proof-debug.log`
- `phase21a-world-entry-final-camera-stand-tooltip.png`
- `phase21a-world-entry-final-validation-build.log`

## 11. 状态交接

### 已完成

- 当前源码非 clean 基线通过；
- 新建默认受控世界并稳定进入；
- 两个此前未触发的 required Mixin 均在真实路径成功 apply；
- 修复 server 初始保存生命周期、26.1.2 交互 Mixin descriptor 和 Camera Stand tooltip 默认 style；
- integrated server、客户端登录、HUD、世界画面、区块、保存/停止/重进均通过；
- 有限 UI、Camera Stand、物品和资源重载烟雾测试通过其本轮判定范围；
- 最终客户端正常退出，残留游戏进程为 0；
- 39/39 测试、双 Access Widener 和完整 build 通过；
- 新 Fabric JAR 已生成并记录元数据。

### 关键决策

- 没有把用户早期 JAR 的症状未经证据地等同于当前源码根因；
- 每轮只修最早、可解释终止路径的确定性根错误；
- required Mixin 保持 fail-fast；
- Camera Stand 紫黑块通过对照实验定位为 HUD tooltip，而不是修改 renderer/model/ItemStack 业务逻辑；
- 没有为了 Phase 21A 成功而迁移或禁用配方错误，没有回退 Phase 17/18 语义。

### 失败尝试与遗留问题

- `FIXED` render context、相机 inactive 规范化、null owner 三个假设均被实验否定并回退；
- 17 个配方 parse error/每次数据加载仍存在，是独立迁移债务；
- 完整视觉、滤镜和拍摄语义仍未验收；
- 新 JAR 尚未在 production-like/正式 Launcher 实例验证。

### 下一步

默认下一步是 Phase 21B：由用户使用本报告记录的最新 Fabric JAR，在隔离或明确可回滚的生产环境配置中完成一次人工世界进入、14 tint 色彩、19 PostChain、核心 direct/background capture、Viewfinder/FOV、EntitiesInFrame、EnderMan、照片像素与窗口 resize 验收。应同时单独排期迁移 26.1.2 配方 JSON；不要把 Phase 21A 的 dev `runClient` 成功当成 production JAR 或完整拍摄验收。
