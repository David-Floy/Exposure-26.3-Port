# Exposure → Minecraft 26.1.2 Fabric 迁移报告：Phase 17

## 1. 阶段结论

本阶段已按限定范围完成 Shader、RenderTarget、GPU readback、后台/直接截图底座，以及 `GameRenderer.loadEffect` 对应访问边界的源码迁移。

- `:common:compileJava`：186 条错误降至 131 条。
- Phase 17 涉及的 `Shader`、`CaptureShader`、`ViewfinderShader`、`SetPostEffectAction`、`ClientPacketsHandler`、`BackgroundScreenshotCaptureTask`、`DirectScreenshotCaptureTask`、`ScreenshotReadback` 均不再出现在 javac 错误中。
- `:fabric:compileJava` 仍由 common 的 131 条错误阻断，fabric javac 未开始。
- `GameRenderer.setPostEffect(Identifier)` 新 Access Widener 目标通过验证；验证器只剩本阶段明确禁止修改的 `GameRenderer.getFov` 和 `EnderMan.isLookingAtMe`。
- 没有用禁用 Shader、空 pass、主屏幕替代离屏 target、固定尺寸 target、空/全黑图像或跳过截图来规避错误。
- networking、packet schema、`CaptureParameters`、`ExposureData`、照片路径/格式/像素编码、SavedData、ExtraData、Frame 和拍摄算法均未修改。

由于 common 尚未编译通过，本阶段只能确认源码签名、资源格式、静态生命周期和分组编译边界；Shader 实际编译、Mixin 注入及游戏内视觉结果仍需在后续 common 清零后运行验证。

## 2. 修改文件与原因

### Java

| 文件 | 修改原因 |
|---|---|
| `common/src/main/java/io/github/mortuusars/exposure/client/util/Shader.java` | 用 26.1.2 `ShaderManager`/`PostChain` 重建通用 Shader 入口；桥接旧 Identifier；集中处理可用性、应用、清除和离屏处理。 |
| `common/src/main/java/io/github/mortuusars/exposure/client/capture/CaptureShader.java` | 不再持有旧 `PostChain` GPU 对象，只持有 capture-scoped Identifier，并通过公共 Shader 底座处理指定 target。 |
| `common/src/main/java/io/github/mortuusars/exposure/client/camera/viewfinder/ViewfinderShader.java` | 只迁移 Shader 底层依赖；保留 viewfinder 状态语义，不处理输入、zoom、overlay 或 FOV。 |
| `common/src/main/java/io/github/mortuusars/exposure/client/capture/ScreenshotReadback.java` | 新增统一异步 GPU readback 入口，将 callback API 转换为 `CompletableFuture<NativeImage>` 并处理取消/重复完成时的释放。 |
| `common/src/main/java/io/github/mortuusars/exposure/client/capture/task/BackgroundScreenshotCaptureTask.java` | 迁移独立 depth target、显式清除、嵌套世界渲染、Shader pass、异步 readback、异常恢复和 target 销毁。 |
| `common/src/main/java/io/github/mortuusars/exposure/client/capture/task/DirectScreenshotCaptureTask.java` | 迁移当前主 target 的异步 readback，并保证延迟到期后只提交一次读取。 |
| `common/src/main/java/io/github/mortuusars/exposure/client/capture/action/SetPostEffectAction.java` | 用当前 Identifier API 保存、替换和恢复 GameRenderer post effect。 |
| `common/src/main/java/io/github/mortuusars/exposure/network/handler/ClientPacketsHandler.java` | 仅将既有 `shader_apply` handler 接到 Shader 服务；packet ID、Optional 字段和 codec 未改。 |
| `common/src/main/java/io/github/mortuusars/exposure/mixin/client/GameRendererMixin.java` | Shader/capture cycle 注入点移到世界和原版 post effect 之后、GUI 之前；删除已失效的手工 resize 调用。原有 FOV 代码未改。 |

### Shader 资源

- 新增 `common/src/main/resources/assets/exposure/post_effect/` 下 19 个 26.1.2 post-effect JSON：`black_tint`、`blue_filter`、`brown_tint`、`bsod`、`crisp`、`cyan_tint`、`gray_tint`、`green_filter`、`invert`、`light_blue_tint`、`light_gray_tint`、`lime_tint`、`magenta_tint`、`orange_tint`、`pink_tint`、`purple_tint`、`red_filter`、`white_tint`、`yellow_tint`。
- 更新 `common/src/main/resources/assets/minecraft/shaders/program/exposure_color_processor.fsh` 为 26.1.2 GLSL 330、sampler 与 UBO 形式；计算次序和输出 alpha 保持不变。
- 旧 `assets/exposure/shaders/post/*.json` 和 data filter 中的 `exposure:shaders/post/*.json` 值均保留，避免改变持久化/数据包语义。

### Access Widener

- `common/src/main/resources/exposure.accesswidener`：只把旧 `GameRenderer.loadEffect(ResourceLocation)` 对应目标迁移为真实 official 名称与 descriptor：
  `GameRenderer.setPostEffect(Identifier)V`。
- `GameRenderer.getFov` 和 `EnderMan.isLookingAtMe` 条目原样保留。

### 审计与验证产物

- `phase17-common-baseline.log`
- `phase17-group2-shader-packet.log`
- `phase17-group3-rendertarget.log`
- `phase17-group4-screenshot-readback.log`
- `phase17-common-compile-final.log`
- `phase17-fabric-compile-final.log`
- `phase17-clean-build-final.log`
- `phase17-common-accesswidener.log`
- `phase17-fabric-accesswidener.log`
- `phase17-merged-jar-gpu-architecture.log`
- `phase17-shader-resource-audit.log`

## 3. 旧 GPU 数据流

### Shader

旧 `Shader` 直接使用资源管理器、主 `RenderTarget` 和旧构造器创建 `PostChain`，自行缓存对象，并依赖旧 `resize`、`process(partialTick)`、bind/unbind 和 shutdown 路径。`CaptureShader` 与 `ViewfinderShader` 也直接拥有链对象。该模型的问题是：26.1.2 已不公开旧构造器和这些状态式方法，且 mod 自己持有的链会在资源 reload 后成为陈旧 GPU 对象。

旧业务 Identifier 来自 filter 数据，形式为 `exposure:shaders/post/<name>.json`。每条链使用 `minecraft:program/exposure_color_processor`、原版 blit/bits pass、`DiffuseSampler`，以及 `Mul`、`Add`、`Saturation`、`Contrast` 等 uniform；`bsod` 还使用 `Resolution` 和 `MosaicSize`。这些业务值与 pass 顺序是本阶段的兼容基线。

### 后台截图

旧流程创建独立 texture target，绑定为写 target，通过 `MinecraftMixin` 在 capture 作用域内把 `getMainRenderTarget()` 重定向到它，再次渲染世界，依次应用当前 GameRenderer effect 和 capture filter，然后同步取得 `NativeImage`，最后解绑、恢复状态并销毁 target。target 需要 depth，尺寸来自当时主 framebuffer。

### 直接截图

旧流程等待配置的帧延迟后，直接对主 target 调用返回 `NativeImage` 的同步截图 API。后续 capture pipeline 再进行 CPU 图像效果、调色板处理、编码或上传。

## 4. Minecraft 26.1.2 实际 Shader/GPU 架构

以下结论来自当前 merged jar 与 Fabric API 的真实 class/descriptor，完整输出保存在 `phase17-merged-jar-gpu-architecture.log`。

1. `ShaderManager` 是 `SimplePreparableReloadListener` 和 `AutoCloseable`；它读取 Shader 源、`post_effect/*.json` 配置，维护 compilation cache，并拥有缓存 `PostChain` 的 reload/close 生命周期。
2. post effect 资源键来自 `FileToIdConverter.json("post_effect")`，因此 `assets/<namespace>/post_effect/<path>.json` 对应 `<namespace>:<path>`。
3. `ShaderManager.getPostChain(Identifier, Set<Identifier>)` 是当前公开获取入口；链缺失或编译/链接失败会返回不可用结果并由原版恢复/日志路径处理。
4. `PostPass` 按 JSON 的 vertex/fragment 创建 `RenderPipeline`。输入的 `sampler_name` 会形成对应的 GLSL `...Sampler`，配置 uniform 以 std140 UBO 提供。
5. `PostChain.process(RenderTarget, GraphicsResourceAllocator)` 是当前处理入口；没有旧公开构造器、`resize`、`getName` 或 `process(float)`。
6. `RenderTarget` 仍存在，拥有 color/depth `GpuTexture`；当前常用生命周期为 `resize`、`copyDepthFrom`、`blitToScreen` 和 `destroyBuffers`。旧 `bindWrite`、`bindRead`、`unbind`、`setClearColor` 已不存在。
7. `TextureTarget(String, width, height, useDepth)` 是本阶段独立离屏 target 的实际构造器。清除通过 `GpuDevice.createCommandEncoder()` 和 `CommandEncoder.clearColorAndDepthTextures(...)` 完成。
8. 更低层 API 包括 `GpuDevice`、`GpuTexture`、`GpuBuffer`、`CommandEncoder`、`RenderPass` 和 texture/buffer copy。后处理本身由 `PostChain` 封装，不需要 Exposure 重写全屏 quad。
9. 26.1.2 `Screenshot.takeScreenshot(RenderTarget, Consumer<NativeImage>)` 创建 readback buffer、提交 texture-to-buffer copy，并通过 fenced render task 映射结果；callback 前完成垂直翻转，并把输出 alpha 置为不透明。buffer view 与 GPU buffer 由原版路径释放。
10. readback 对调用方是异步 callback；等待不需要在渲染线程自旋或阻塞。
11. 主 `RenderTarget` 仍由 `Minecraft` 持有；GameRenderer 渲染世界并执行当前 post effect，然后结束 Fog frame，随后进入 GUI。Exposure 的注入点必须位于世界/原版 effect 之后、GUI 之前。
12. `RenderSystem.assertOnRenderThread` 和 fenced render-call 队列构成 GPU 线程边界。
13. Fabric API 0.150.0 没有比当前原版 `ShaderManager`/`PostChain` 更合适的公开 post-effect 或 RenderTarget owner 扩展入口；现有公开原版入口足够。

## 5. 新 Shader 生命周期与 reload 设计

- GPU Shader、compiled program、pipeline、post chain 和其 persistent internal targets 的 owner 是 Minecraft `ShaderManager`。
- Exposure 只长期保存业务 `Identifier`，不保存 `PostChain`、`CompiledShaderProgram`、`GpuTexture` 或 framebuffer handle。
- 每次实际处理时重新向当前 `ShaderManager` 查询缓存链，因此资源 reload 后自然得到新实例；旧链由 manager 在替换 cache 前 close，不会有 mod 持有的陈旧对象。
- 旧业务 Identifier 通过严格的前缀/后缀桥接：`<ns>:shaders/post/<name>.json` → `<ns>:<name>`。它只用于资源管理器 key，不拼接或读取任意文件路径。
- 若资源不存在或编译失败，应用入口记录原始和解析后的 Identifier，并清除 GameRenderer effect，避免后续渲染停留在半应用状态；不会永久安装空 pass。
- Exposure 的 19 条链都把中间 `swap` target 声明为 persistent，避免每帧 viewfinder/capture Shader 处理时重建中间纹理。其大小由 post-chain target descriptor 根据输入主 target 调整，reload/manager close 时释放。
- `ViewfinderShader.close()` 和 `CaptureShader.remove()` 清除 Identifier；它们不负责关闭 manager 所有的 GPU 对象。

## 6. uniform、sampler 与资源兼容

- `exposure_color_processor.fsh` 保持原计算顺序：采样 → `Mul`/`Add` → saturation → contrast → alpha `1.0`。
- sampler 业务语义保持为 Diffuse；当前 JSON 使用 `sampler_name: "Diffuse"`，GLSL 使用 `DiffuseSampler`。
- `Mul`、`Add`、`Saturation`、`Contrast` 进入 `ColorConfig` std140 UBO；原 JSON 省略的默认值显式补为 Mul=1、Add=0、Saturation=1、Contrast=1。
- blit 使用 `minecraft:post/blit`、`InSampler` 和 `BlitConfig.ColorModulate=(1,1,1,1)`。
- `bsod` 第二 pass 仍是 bits，`Resolution=16`、`MosaicSize=4`；pass 顺序未变。
- `phase17-shader-resource-audit.log` 对 19 个旧/新资源核对了 Identifier 桥接、pass 数量与顺序、输入/输出 target、所有旧 uniform 数值及 persistent swap，结果 PASS。
- 本机没有 `glslangValidator`，且 common 尚未可运行，因此 GLSL 最终驱动编译只能留到后续游戏内资源 reload 验证。这是运行时风险，不是以 fallback 隐藏的失败。

## 7. ClientPacketsHandler Shader 入口

`ClientPacketsHandler.applyShader` 仍接收原 `ShaderApplyS2CP`，保留 packet ID、方向、codec、字段顺序和 `Optional<Identifier>` 语义：

- Optional 有值：在现有客户端 handler 执行边界调用 `Shader.applyToGameRenderer`。
- Optional 为空：调用 `Shader.clearGameRendererEffect`。
- 无任意文件读取；Identifier 只作为资源 key。
- 加载失败记录日志并清除 effect，不破坏后续渲染状态。

没有改 networking 注册或服务器逻辑。

## 8. RenderTarget 创建、清除、恢复与释放

### 主画面 Shader

Exposure 不再手工 bind 主 target。`PostChain.process(mainTarget, allocator)` 处理当前 target，pass 自己声明输入、输出和中间 target。注入点位于 `FogRenderer.endFrame()` 调用前：世界、outline 和原版 post effect 已完成，GUI 尚未绘制，因此 direct capture 仍能看到 Exposure filter，而 GUI 不被错误纳入照片。

### 后台截图 target

1. 在渲染线程读取当前主 target 的 `width`、`height`。
2. 创建同尺寸、`useDepth=true` 的 `TextureTarget`。
3. 通过 command encoder 把颜色清为透明黑、depth 清为 1.0。
4. 在 `try/finally` capture 作用域内设置 `renderTarget`/`capturing`，现有 `MinecraftMixin` 只在这个作用域把主 target 请求重定向到离屏 target。
5. 关闭方块轮廓，调用 `LevelRenderer.allChanged()`，在离屏 target 中渲染世界，再按原顺序处理当前 GameRenderer effect 和 CaptureShader。
6. 提交 readback 后立即在 `finally` 恢复 capture 重定向、轮廓状态和 level renderer；离屏 target 保留到 GPU callback。
7. callback 无论成功或失败都显式 `destroyBuffers()`；同步异常且未提交 readback 时也在 `finally` 销毁。

不存在每帧创建 framebuffer：后台 target 只在一次离散 capture 中创建；viewfinder 的 post-chain swap target 为 manager-owned persistent 资源。窗口尺寸不是常量，而是 capture 开始时的实际 framebuffer 尺寸。

26.1.2 已不存在旧的世界 `setPanoramicMode` 对应 API，只有 GUI panorama 系统。因此旧 `USE_PANORAMIC_MODE` 水体兼容切换无法按原 API 恢复，本阶段没有伪造替代 target；该配置目前对后台世界重渲染不再产生旧效果，列为视觉兼容风险。

## 9. 截图与 GPU readback

### 统一 readback

`ScreenshotReadback.capture` 必须在渲染线程调用，把 26.1.2 callback 封装为 future：

- GPU copy、fence、map、垂直翻转由原版 `Screenshot` 完成。
- callback 获得的 `NativeImage` 已完全 CPU-owned，不跨线程传递仍由 GPU 持有的映射资源。
- future 已取消/已完成时立即关闭新返回的 `NativeImage`。
- 同步提交异常转为 exceptional completion。

### 后台截图

离屏 target 一直存活到 fence callback；先把 `NativeImage` 包装为现有 `WrappedNativeImage`，再销毁 target 并完成 capture future。失败时释放 target，不返回空白、黑图、旧帧或主菜单图像作为成功结果。

### 直接截图

保留至少一帧延迟和配置尺寸来源，读取的仍是当前主 target。新增 `readbackStarted`，避免异步 callback 尚未返回时每 tick 重复提交 GPU 下载。成功交付现有 `WrappedNativeImage`；future 已结束时关闭图片。

### 像素语义

- 宽高：保持来源 target 的实际宽高。
- 方向：沿用原版 screenshot callback 的 GPU readback 后垂直翻转。
- 通道/alpha：沿用 26.1.2 Screenshot 的 NativeImage 通道布局和不透明 alpha 处理；Exposure 未增加通道交换或颜色空间转换。
- Shader 仍在 GPU target 上、readback 之前执行；没有改照片后续像素算法。

## 10. 线程边界与文件写入

- Shader 查询、apply、process、离屏 target 创建/clear/render/destroy、readback 提交和 fenced callback 都在渲染线程，并在关键入口使用 `RenderSystem.assertOnRenderThread()`。
- readback 等待是异步的，没有在渲染线程调用 `join`、`get`、自旋或长期阻塞。
- callback 之后只有 CPU-owned `NativeImage/Image` 进入既有 capture pipeline；`CameraCaptureTemplate` 的图像效果、palettize 和上传仍使用既有 `thenAsync`/`acceptAsync`。
- 现有照片导出路径中，`PhotographScreen` 使用 `CompletableFuture.runAsync`，批量 `ExportExposuresTask` 使用 `CompletableFuture.supplyAsync` 执行 PNG 编码/写盘。本阶段没有改变文件名、路径、编码或导出协议。
- `NativeImageFileSaver` 等相邻类仍有 26.1.2 编译错误，且不属于 GPU readback 主路径；本阶段未借机改其像素/导出语义。后续修复时仍须保持写盘在工作线程。

## 11. GameRenderer.loadEffect 访问边界

旧目标用途是按 Identifier 选择 GameRenderer post effect。26.1.2 中对应状态为：

- private `setPostEffect(Identifier)`：设置 `postEffectId` 并激活 effect；
- public `clearPostEffect()`；
- public `currentPostEffect()`；
- public `effectActive` 状态仍被现有 capture action 保存/恢复。

因此没有继续寻找已删除的 `loadEffect`，也没有只注释 AW。所有调用方迁移后，AW 使用真实 official 名称和 descriptor：

`accessible method net/minecraft/client/renderer/GameRenderer setPostEffect (Lnet/minecraft/resources/Identifier;)V`

common/fabric AW 验证均不再报告该目标。

## 12. viewfinder 与 CameraCapture 对底座的依赖

### Viewfinder

仅确认并迁移以下底层依赖：

- filter data 提供旧稳定 Shader Identifier；
- `ViewfinderShader` 保存 Identifier、检查资源可用性，并在主 target 的世界渲染末端处理 post chain；
- `setActive`、`remove`、`close` 管理选择状态，不拥有 GPU 对象；
- 窗口 resize/reload 由当前 ShaderManager/PostChain target descriptor 处理。

未处理 `ViewfinderOverlay`、`ViewfinderCameraControlsScreen`、`ViewfinderZoom`、FOV 修改、鼠标键盘输入、controls 或完整 UI。

### CameraCapture

`CameraCaptureTemplate` 仍按原 action 顺序选择 direct/background screenshot、camera entity、FOV、GUI、post effect、filter、gamma 和 flash。底座只提供：

- `CaptureShader` 的 capture-scoped filter Identifier；
- `SetPostEffectAction` 的 GameRenderer effect 保存/应用/恢复；
- 独立 depth target 和主 target 两种截图源；
- `ScreenshotReadback` 的 CPU-owned `NativeImage` future。

没有改变 capture 调度、实体选择、曝光、裁剪、胶片、palettize、upload 或 `ExposureData` 构造。

## 13. 明确跳过的 Phase 18 内容

- `GameRenderer.getFov` 实际签名与 AW。
- `EnderMan.isLookingAtMe` 实际签名与 AW。
- viewfinder overlay、controls、zoom、输入、FOV 和交互。
- CameraCapture 拍摄调度、曝光流程、capture 选择算法。
- `EntitiesInFrame` culling box 与实体选择。
- CameraItem 的 EnderMan 观察逻辑。
- networking、ExtraData、SavedData、旧 NBT/path、Frame、照片文件和像素算法。
- JEI、Jade、Create、RealCamera、Easy Anvils，以及任何 Forge/NeoForge/Quilt 恢复。

## 14. 分组编译与错误变化

| 验证点 | 日志 | common javac 错误 | 结果 |
|---|---|---:|---|
| 阶段基线/组 1 架构审计 | `phase17-common-baseline.log` | 186 | 失败；设计期间未改源码，因此该同一源码快照也作为组 1 验证。 |
| 组 2：Shader + packet | `phase17-group2-shader-packet.log` | 146 | 失败；Shader/packet 本阶段错误清零。 |
| 组 3：RenderTarget | `phase17-group3-rendertarget.log` | 133 | 失败；旧 target/bind/clear 生命周期错误清零。 |
| 组 4：截图 + readback | `phase17-group4-screenshot-readback.log` | 131 | 失败；截图/readback 本阶段错误清零。 |
| 最终 common | `phase17-common-compile-final.log` | 131 | 失败，结果稳定。 |
| 最终 fabric | `phase17-fabric-compile-final.log` | 131 个 common 错误 | fabric javac 未开始。 |
| `clean build --continue` | `phase17-clean-build-final.log` | 131 | common 编译与两项保留 AW 导致失败。 |
| common AW | `phase17-common-accesswidener.log` | — | 只剩 `getFov`、`isLookingAtMe`。 |
| fabric AW | `phase17-fabric-accesswidener.log` | — | 只剩 `getFov`、`isLookingAtMe`。 |

## 15. 剩余 131 条错误分布

按最终 javac 诊断文件归类：

- viewfinder/FOV 邻接：约 62 条。`ViewfinderOverlay` 33、`ViewfinderCameraControlsScreen` 22、`ViewfinderZoom` 3、`Viewfinder` 2、`CameraClient` 1、`FocalLengthButton` 1。
- 普通 GUI/Bugger：约 40 条。包括 `BuggerScreenOverlayMixin`、`TestImageScreen`、`PhotographSlotWidget`、`Bugger`、`GuiUtil`、`ClientGUI`、`PhotographScreen`、`LightroomScreen`。
- ResourceImage/export/CPU 图像相邻 API：约 13 条。
- 其他数据/实体/API：约 16 条，包括 `ColorPalettes`、注册入口、Flash、ChromaticSheet、Gamma、`CaptureTemplate`、`EntitiesInFrame` 和 CameraItem/EnderMan。

Shader、RenderTarget、BackgroundScreenshot、DirectScreenshot 和 GPU readback 已不在剩余分布中。剩余错误以 viewfinder 为最大单组，但不完全等同于 Phase 18 禁止范围，仍有普通 GUI、ResourceImage/export 和其他 API 尾项。

## 16. Access Widener 最终状态

本阶段开始时三个旧目标中：

1. `GameRenderer.loadEffect`：已迁移为 `setPostEffect(Identifier)`，验证通过。
2. `GameRenderer.getFov(Camera,float,boolean)`：保留，验证失败，Phase 18。
3. `EnderMan.isLookingAtMe(Player)`：保留，验证失败，Phase 18。

所以当前剩余失效目标为 2 个，而不是通过删除规避。

## 17. 静态审计与风险

### 已确认

- 参与本阶段编译路径中没有旧 `PostChain` 构造器、`resize/getName/process(float)`、`bindWrite/bindRead/unbind/setClearColor`、同步单参数 Screenshot、`loadEffect/shutdownEffect/currentEffect` 调用。
- 未新增空 Shader、no-op pass、全黑成功截图、固定 RenderTarget、空 NativeImage 或主屏幕替代独立 depth target。
- 自定义离屏 target 的所有成功、callback 失败和同步异常路径都有显式 `destroyBuffers()`。
- ShaderManager-owned GPU 对象不被 Exposure 保存，reload 不留下旧 handle。
- readback 与 GPU target 的生命周期以 fence callback 为界；CPU future 不持有 GPU mapping。
- Shader Identifier、sampler、uniform、pass 顺序、尺寸来源、方向、alpha 和 packet schema 均保持迁移设计。
- 本阶段没有修改 FOV、EnderMan、EntitiesInFrame、viewfinder 交互和 capture 算法源码。

### 仍需运行验证的风险

1. common 未清零，无法启动客户端验证 19 个 post effect 的实际驱动编译、UBO layout 与视觉结果。
2. `GameRendererMixin` 的新 `FogRenderer.endFrame()` 注入点已按 merged bytecode 核对，但需运行 Mixin audit 确认实际注入并检查与其他客户端 mod 的冲突。
3. 旧世界 panoramic mode API 已删除；相关水体兼容配置当前无等价实现，可能产生后台重渲染视觉差异。
4. capture 开始与 GPU callback 之间若发生窗口 resize、资源 reload、世界退出或客户端关闭，target 被局部强引用保护到 fence，但尚无独立 capture cancellation/客户端关闭协调器；当前不会提前销毁 GPU 资源，代价是极端退出路径需要运行检查是否能收到 fenced callback。
5. 轮廓状态沿用旧流程的 `false`→`true` 恢复方式，因为 26.1.2 没有公开 getter，且本阶段不允许增加新的私有访问边界；若其他代码在 capture 前主动关闭轮廓，存在状态恢复偏差风险。
6. 颜色空间未增加转换，保持原始 target→NativeImage 流程；需要实机对比确认平台 framebuffer 格式与旧版本输出一致。

未发现静态可确认的 GPU 资源泄漏、非渲染线程 GPU 调用、同步等待 readback、target 异常路径悬挂或永久 Shader 状态污染。

## 18. 失败尝试与关键决策

- 最初 PowerShell `Tee-Object` 会包装/改变 javac 输出显示，随后改用 `cmd /c` 原样重定向，保存了完整 186 条基线。
- 系统 Oracle javap shim 不可用，改用实际 Java 25 JDK 的 `javap.exe` 检查 merged jar。
- 初版资源审计把 JSON 的 `1` 与 `1.0` 当作文本差异；改为数值比较后，19 条资源语义核对通过。
- 本机没有 `glslangValidator`，没有把缺少离线编译器解释为 Shader 成功；运行时编译验证明确保留。
- 没有为消除 panoramic 编译错误伪造等价 API；记录其删除与风险。
- 选择让 Minecraft `ShaderManager` 成为唯一 Shader GPU owner，而不是为旧抽象建立第二套编译/缓存系统。
- 选择 fenced async readback，而不是在渲染线程同步等待或直接复制上一帧。

## 19. 下一阶段建议

建议 Phase 18 新建对话，并使用 **GPT-5.6 Terra**。原因是下一阶段同时涉及 viewfinder 的高密度 GUI/input 迁移、GameRenderer FOV/相机状态、CameraCapture 业务时序、EntitiesInFrame culling 与 EnderMan 观察语义，跨越客户端渲染、交互和服务端实体 API；Terra 更适合这种需要持续源码/merged-jar 对照和严格范围控制的综合迁移。

Phase 18 应优先：

1. 先核对 26.1.2 `getFov`、Camera、KeyMapping/InputConstants 与 EnderMan 实际签名。
2. 分离 viewfinder 普通 GUI 绘制错误和真正 FOV/capture 语义错误。
3. 迁移并验证剩余两个 AW，而不是删除。
4. 在 common 能启动后，补做本阶段 19 个 Shader 的资源 reload、窗口 resize、direct/background 像素对比和退出时 fence/释放测试。
5. 普通 GUI、ResourceImage/export 与其他尾项继续按独立小组处理，不把它们混入 capture 算法重写。
