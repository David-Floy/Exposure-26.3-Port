# Minecraft 26.1.2 Fabric Phase 19：common 尾项与首次 Fabric javac

## 结论

- `:common:compileJava` 已从 **62 个错误清零**，最终成功日志为 `phase19-common-compile-final.log`。
- `:fabric:compileJava` 首次真正进入 javac；初始为 40 个错误。完成与本轮 common/注册直接对应的低风险适配后，剩余 **35 个错误**，见 `phase19-fabric-after-direct-adaptations.log`。
- 未改动 networking packet schema、SavedData、ExtraData、Frame/ExposureData、照片路径/格式、像素编码协议、调色板算法、曝光算法、胶片逻辑、Phase 17 GPU 截图或 Phase 18 viewfinder/capture 语义。

## 输入与基线

读取了 Phase 18/17 报告及 Phase 18 的 common、Fabric、clean build、Access Widener 日志；工作区未发现 `AGENTS.md`。实际执行 `./gradlew :common:compileJava` 的结果与交接一致：62 个 Java 错误。

### 初始 62 条错误清单

| 组/文件 | 行号与旧入口 | 26.1.2 等价入口 | 数量 | 生命周期/语义风险 | 连锁 |
|---|---|---|---:|---|---|
| NativeImageFileSaver | 28 `setPixelRGBA` | `setPixelABGR(ARGBtoABGR)` | 1 | 新建图像 owner；PNG/alpha | 否 |
| FileCaptureTask | 88 `displayClientMessage(...,false)` | `sendSystemMessage` | 1 | 无 | 否 |
| UrlCaptureTask | 70 `displayClientMessage(...,false)` | `sendSystemMessage` | 1 | URL 流关闭 | 否 |
| ImageExporter | 109 `setPixelRGBA` | `setPixelABGR(ARGBtoABGR)` | 1 | 导出 NativeImage owner | 否 |
| ClientGUI | 67 `ClientLevel.getRecipeManager`、73 `Recipe.getIngredients`、88 `Ingredient.getItems` | connection recipe manager、`placementInfo().ingredients()`、`items()` | 3 | tooltip 内容 | 否 |
| PhotographSlotWidget | 115/122/131/135/189/190/191 未导入 `Minecrft` | 导入现有 client helper；保留 extractor tooltip | 7 | 无 | 否 |
| PhotographScreen | 271/283/295 `displayClientMessage` | `sendSystemMessage` | 3 | 无 | 否 |
| LightroomScreen | 423/425 五参数 `hasClickedOutside` | 四参数 override | 2 | 点击边界 | 否 |
| TestImageScreen | 280/289/292/299/330 message；421/423 旧拖动签名 | `sendSystemMessage`；`MouseButtonEvent` | 7 | 截图任务不变；输入 | 否 |
| ResourceImage | 60 `getPixelRGBA`、68 `getTextureImage`、77/80 `reset`、104 `location` | `getPixel`、Resource/InputStream、`loadContents`、`resourceId` | 5 | CPU 缓存与资源关闭 | 否 |
| WrappedNativeImage | 25 `getPixelRGBA` | `getPixel` | 1 | NativeImage owner | 否 |
| ExportExposuresTask | 49/153/157 `displayClientMessage` | system/overlay message | 3 | 工作线程回主线程提示 | 否 |
| Bugger | 69/70/73/79/80/83 `Screen` modifier 状态 | `Minecraft.hasControlDown/hasShiftDown` | 6 | dev-only client | 否 |
| GuiUtil | 48/52/58/85/91 旧 RenderSystem/BufferUploader | 无调用方，删除旧即时重载 | 5 | GUI 提交 | 否 |
| ColorPalettes | 20×2、25×2 `Registry.getHolder` | `Registry.get(ResourceKey/Identifier)` | 4 | 动态 registry fallback | 否 |
| Exposure | 130 `noCollission`；233 lambda Output 访问诊断×2 | `noCollision`；Fabric creative-tab output event | 3 | 注册/静态初始化 | 后两条同一根因 |
| ChromaticSheetItem | 82 旧 `inventoryTick`、85 `CustomData.contains` | equipment-slot tick、`copyTag().contains` | 2 | component/自动合成 | 否 |
| BuggerScreenOverlayMixin | 21/22/23/28/29/30/38 profiler/drawManaged/drawString | `Profiler.get`、直接 extractor、`text` | 7 | required client mixin | 否 |

总计：`1+1+1+1+3+7+3+2+7+5+1+3+6+5+4+3+2+7 = 62`。除 Exposure creative-tab 的两个重复 access 诊断外，均为直接诊断。

## 完成的迁移

### 普通 GUI 与 GuiUtil

- `PhotographSlotWidget` 继续使用 `GuiGraphicsExtractor`、`GUI_TEXTURED`、`nextStratum` 和 Phase 15 动态照片 GUI 后端；tooltip 仍为 deferred `setTooltipForNextFrame`，点击/滚轮区域未变。
- `TestImageScreen` 保留 Capture/Phase 17 截图任务；只更新 client message 与右键拖动事件对象，没有在每帧创建图像或纹理。
- `PhotographScreen`、`LightroomScreen` 保留照片、菜单、缩放和拖放逻辑；后者仅对齐当前 outside-click 签名。
- `ClientGUI` 从已同步的 client `RecipeManager` 读取 recipe，并以 `placementInfo().ingredients()` 保持 tooltip 的源材料顺序；`Ingredient.items()` 保持轮播展示。
- `GuiUtil` 的五个 PoseStack/RenderSystem/BufferUploader 即时重载经调用点搜索后为 0，已删除；保留 extractor `fill` 和 scissor 的辅助方法。没有空兼容重载。

### CPU 图像、资源和所有权

- `ResourceImage` 在首次 CPU 像素访问时用 `ResourceManager.getResourceOrThrow(...).open()` 和 try-with-resources 解码一份独立 `NativeImage`；GPU `TextureContents` 仍由 `SimpleTexture.loadContents` 所有。reload 与 close 都关闭且清空 CPU 副本，避免 GPU/CPU 共享同一 native 指针。
- `WrappedNativeImage` 是单一 owner 包装；`close()` 使用 closed guard 幂等。截图 future 的成功路径仍把 NativeImage 转移给该包装，失败路径仍关闭。
- `NativeImageFileSaver` 与 `ImageExporter` 均用 `setPixelABGR(Color.ARGBtoABGR(...))`，保持 RGBA/alpha 与 PNG 输出；临时 NativeImage 为 try-with-resources owner。`NativeImageFileSaver.save` 和 `ImageExporter.export` 现在返回写入结果；导出任务不会把写盘失败计入已导出。
- PNG 文件加载器在超过尺寸限制时关闭已解码的 NativeImage，修复拒绝路径泄漏。

### 文件、URL 与导出

- `FileCaptureTask` 保留相对路径限制、越界检查、扩展名/文件检查、异步 `completeAsync` 与原 timeout；仅迁移消息 API。
- `UrlCaptureTask` 保留 http/https、域名白名单、DNS 本地地址阻断、尺寸限制和 worker-thread 执行；改为显式 `URLConnection`、连接/读取 timeout、try-with-resources InputStream，解码失败仍返回原错误。
- `ExportExposuresTask` 保留 ID 顺序、请求重试、尺寸/look modifier、目录/文件名、覆盖策略和取消；chat 是 system message，状态条是 overlay message。

### Bugger

`Bugger` 仍受开发环境与原 page/按键条件控制，未删除或降级。Overlay mixin 目标更新为 `DebugScreenOverlay.extractRenderState`，使用 `Profiler.get()`、extractor `text` 与原 draw-lines invoker；mixin JSON 的 `required: true` 和 `defaultRequire: 1` 未改。没有新增 Access Widener，也没有引入 dedicated-server client 加载路径。

### Registry、注册与 Chromatic Sheet

- `ColorPalettes` 改用 26.1.2 `Registry.get`，仍按 requested → default → any 的原 fallback 顺序，未知 palette 不被固定为默认。
- Flash block 使用拼写更正后的 `noCollision`。
- Creative tab builder 因其旧 output 类型在 common 中不可访问，条目填充迁移到 Fabric `CreativeModeTabEvents.modifyOutputEvent`；ID、16 个条目和原顺序完全保留。
- BlockEntity builder 迁移到 `FabricBlockEntityTypeBuilder`；实体类型以同一 `ResourceKey` build+register，保持 ID。
- `ChromaticSheetItem` 保留三层/顺序/component/printed 标记/合成与 packet 行为，使用当前 equipment-slot inventory tick 以及 `CustomData.copyTag().contains`。

## 各组消除与验证

| 检查 | 结果 |
|---|---|
| group 1 GUI | 成功，`phase19-group1-gui.log` |
| group 2 resource image | 成功，`phase19-group2-resource-image.log` |
| group 3 file/url/export | 成功，`phase19-group3-file-url-export.log` |
| group 4 Bugger | 成功，`phase19-group4-bugger.log` |
| group 5 registry/data | 成功，`phase19-group5-registry-data.log` |
| common final | 成功，`phase19-common-compile-final.log` |
| Fabric first javac | 40 errors，`phase19-fabric-first-javac.log` |
| Fabric after direct fixes | 35 errors，`phase19-fabric-after-direct-adaptations.log` |
| clean build --continue | 失败：Fabric 35 + common test 10，`phase19-clean-build-final.log` |
| common/fabric AW | 都成功，`phase19-common-accesswidener.log`、`phase19-fabric-accesswidener.log` |

`./gradlew build` 未运行：其前置的 `:fabric:compileJava` 尚失败。没有运行可能写入资源的 data-generation 任务。

## 首次 Fabric javac 分类

初始 40，直接修复 creative tab API、BlockEntity builder、EntityType ResourceKey 后为 35：

| 分类 | 文件 | 余量 | Phase 20 工作 |
|---|---|---:|---|
| Fabric initializer/config | ExposureFabric、ExposureFabricClient | 6 | Forge Config API Port 26.1.4 的新 registry/config-screen API |
| client API rename/event | ExposureFabricClient | 8 | key mapping、color provider、tooltip、HUD 事件的新入口与环境隔离 |
| menu/network glue | PlatformHelperImpl、RegisterImpl | 11 | 已删除的 PacketByteBufs/ExtendedScreenHandler* 的新 menu payload 边界；不得改 packet schema |
| renderer/client mixin | LevelRendererMixin | 7 | Camera render state、outline color、LevelRenderer descriptor |
| resource reload | IdentifiableResourceReloadListenerWrapper | 3 | 新 SharedState reload 合约 |
| registry/data generation | RegisterImpl | 0 | 本阶段直接项已清零 |
| model/ExtraModelKey | 无 | 0 | 无当前 javac 证据 |

剩余错误跨配置、client event、network menu、renderer mixin、reload 五套架构，不适合本阶段继续批量机械替换。

## clean build 额外发现

`common:compileTestJava` 另有 10 个历史测试错误：`IntTag.getAsInt`×4、`ByteTag.getAsByte`×4、`ExposureFrameHistory.save/load`×2。这些不阻塞 common production javac，且触及测试/存储 API，留给后续单独分类，不在本阶段修改。

## 静态检查与风险结论

- 本阶段 GUI 路径没有旧 PoseStack/BufferUploader/RenderSystem immediate 调用；仅保留 extractor 形式的 `renderTooltip` 方法名。
- 没有发现本阶段新增空 render、空 widget、固定不绘制分支、空图成功路径、固定 palette/registry 返回或静默导出成功。
- Resource、InputStream、文件流与临时 NativeImage 的新增路径均有 try-with-resources；`WrappedNativeImage` 为幂等单 owner。Phase 17 截图 future 的失败关闭路径仍存在。
- File/URL 读取在 worker future 内；URL 现在有连接/读取 timeout。导出仍由调用方的 worker `runAsync` 或任务 worker 执行。
- mixin `required/defaultRequire` 未降级；Fabric initializer 本身没有 client imports。Fabric client initializer/renderer 尚未完成编译，仍需在 Phase 20 核验 dedicated server 隔离。
- palette ID、registry ID、texture ID、item-model property ID 未改；没有编辑 networking、ExtraData、SavedData、Frame、ExposureData，亦未编辑 Phase 17/18 的 Shader/RenderTarget/Screenshot/Viewfinder/CameraCapture 实现。

## 修改文件

`GuiUtil`、`PhotographSlotWidget`、`TestImageScreen`、`PhotographScreen`、`LightroomScreen`、`ClientGUI`、`ResourceImage`、`WrappedNativeImage`、`NativeImageFileSaver`、`FileCaptureTask`、`UrlCaptureTask`、`NativeImagePngFileLoader`、`ImageExporter`、`ExportExposuresTask`、`Bugger`、`BuggerScreenOverlayMixin`、`ColorPalettes`、`Exposure`、`ChromaticSheetItem`、`fabric/ExposureFabric`、`fabric/RegisterImpl`。

## 交接

- 已完成：common 62→0、首个 Fabric javac 基线、35 个 Fabric 余项分类、低风险直接 registry/creative-tab 适配。
- 关键决策：CPU ResourceImage 不与 GPU TextureContents 共享 NativeImage；creative tab 输出使用当前 Fabric event；不为 Fabric 大组错误修改协议或 mixin 宽容度。
- 失败尝试/限制：旧 `ItemGroupEvents`、screenhandler、client-rendering/keybinding API 在 Fabric API 0.150.0+26.1.2 已不存在；`clean build` 同时暴露 Fabric 与 10 个旧测试 API 错误。
- 下一步：新建对话进入 **Phase 20：Fabric integration**，优先采用 **GPT-5.6 Sol**（跨 API 架构与 runtime 风险）；完成后再做 client、dedicated server、Mixin runtime 验证。若后段只剩完全同构的 rename，可建议 GPT-5.6 Luna，但本阶段未切换模型。
