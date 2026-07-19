# Phase 12：普通 GuiGraphics 二维绘制与 Screen/Widget 抽取接口收尾

日期：2026-07-14  
目标：Minecraft 26.1.2 Fabric（Gradle 9.4.0、Java 25、Loom 1.16.3）

## 结论

本阶段完成了能与照片像素、模型、`RenderType`/Vertex、Shader、`RenderTarget`、截图和取景器明确分离的普通二维 GUI 提交迁移。`common` Java 编译错误由 Phase 11 的 **565** 降至 **390**（减少 175）。普通 GUI 路径中已不再发现旧即时 `drawString`、`renderTooltip`、`renderItem`、`renderItemDecorations` 或未指定 `RenderPipelines.GUI_TEXTURED` 的普通 `blit` 调用。

`PhotographSlotWidget`、`PhotographScreen`、`FilmFrameInspectScreen`、`TestImageScreen` 和 `PhotographClientTooltip` 中仍有旧三维 pose 调用；逐一确认它们直接包围照片像素、动态照片 renderer、`MultiBufferSource` 或旧 Vertex 渲染，因而按阶段范围保留给后续渲染阶段。普通文字、背景、边框、按钮和 tooltip 已从这些块中拆出并迁移。

## 26.1.2 实际抽取式 GUI 架构

本阶段以本地 26.1.2 merged jar 的实际签名为准：

- `Screen.extractRenderStateWithTooltipAndSubtitles` 依次驱动 `extractBackground`、`extractRenderState`，然后统一抽取 deferred 元素。
- `AbstractContainerScreen.extractRenderState` 负责菜单的 contents、carried/snapback item 与 tooltip；其 `extractContents` 先提交 Screen widget，再以 `Matrix3x2fStack` 的 `leftPos/topPos` 作用域提交 labels、slot highlight 和 slots。
- `AbstractWidget.extractRenderState` 最终调用子类的 `extractWidgetRenderState`；按钮内容使用 `extractContents`。
- 普通二维纹理使用 `GuiGraphicsExtractor.blit(RenderPipelines.GUI_TEXTURED, ...)` 或 `blitSprite(RenderPipelines.GUI_TEXTURED, ...)`。
- 文字使用 `text`/`centeredText`，普通与物品提示使用 `setTooltipForNextFrame`，使 tooltip 保持在所有普通内容之后。
- 二维变换使用 `Matrix3x2fStack.pushMatrix/popMatrix/translate/scale`；需要在照片或已有内容之上的普通二维层使用 `nextStratum()`，不再以三维 Z translate 实现。

## 修改文件与原因

| 文件 | 本阶段调整 |
| --- | --- |
| `client/gui/component/FocalLengthButton.java` | 双色焦距文字改为 extractor `text`，坐标与颜色不变。 |
| `client/gui/component/FrameCounterButton.java` | 帧计数文字改为 `text`。 |
| `client/gui/component/ShutterSpeedButton.java` | 快门速度文字改为 `text`。 |
| `client/gui/screen/element/ToggleImageButton.java` | 保持三态 sprite，使用 GUI textured 管线提交。 |
| `client/gui/screen/test/Slider.java` | 普通背景/handle sprite 和 widget scrolling text 改为抽取式提交，以 `nextStratum` 保持 handle 层级；`VertexConsumer` 渐变仍保留。 |
| `client/gui/screen/ItemRenameScreen.java` | 容器尺寸移至当前父类构造器；普通背景纹理、resize 与 extract 边界适配 26.1.2。 |
| `client/gui/screen/camera/CameraAttachmentsScreen.java` | 176×185 容器背景、槽位占位、相机部件、滤镜 tint、按钮 tooltip 全部迁移到 GUI textured/deferred tooltip；Toast 入口更新为 `getToastManager`。 |
| `client/gui/screen/LightroomScreen.java` | 176×209 容器背景、film overlay tint、槽位外围、普通 tooltip 与推进/弹出箭头迁移；照片 frame renderer 块未动。 |
| `client/gui/screen/album/AlbumScreen.java` | 298×188 容器背景、书页、签名、库存面板、空照片槽、页码与普通 tooltip 迁移；当前 `ClickEvent` 和 container `hasClickedOutside` 签名适配。 |
| `client/gui/screen/album/AlbumViewScreen.java` | 背景状态设置移除，当前 `ClickEvent` 处理适配；保留已迁移的普通书页/text/tooltip。 |
| `client/gui/screen/album/AlbumSigningScreen.java` | 标签文字和签名提示使用抽取式文字/deferred tooltip。 |
| `client/gui/screen/album/PhotographSlotWidget.java` | 相纸、边框、空槽、overlay 和文本 tooltip 使用 GUI textured/deferred API；overlay 用 `nextStratum` 保持在照片本体上方。照片 renderer 本体保留。 |
| `client/gui/screen/album/LecternAlbumScreen.java` | 页面标签与普通 tooltip 改为 `text`/`setTooltipForNextFrame`。 |
| `client/gui/screen/PhotographScreen.java` | 外围 frame 信息文字、tooltip 与控件层改为 extractor/`nextStratum`；照片 renderer 块保留。 |
| `client/gui/screen/FilmFrameInspectScreen.java` | 照片上方控件层改为 `nextStratum`；底片图片与旧 GUI vertex 绘制块保留。 |
| `client/gui/screen/test/TestImageScreen.java` | Screen 入口改为 `extractRenderState`；“Capturing/No Image”普通文字改为 `text`。 |
| `client/gui/toast/ToastIcon.java` | sprite icon 使用 GUI textured 管线，原版 tutorial icon 改调用 `extractRenderState`。 |
| `client/gui/toast/BetterTutorialToast.java` | 按当前 `Toast` 的 `getWantedVisibility`、`update`、`extractRenderState` 拆分状态与绘制，保留超时/条件隐藏及一次性 `onHide` 语义。 |
| `client/gui/tooltip/CameraStandTooltip.java` | 普通文本/物品提示改为 deferred tooltip；标准 item 使用 `item`，背景用当前 `extractTooltipBackground`。 |
| `client/gui/tooltip/PhotographClientTooltip.java` | 当前 `ClientTooltipComponent.getHeight(Font)`/`extractImage` 签名适配；可分离的堆叠计数文字迁移为二维 matrix/text。照片 renderer 本体保留。 |
| `client/util/bugger/Bugger.java` | 调试覆盖层普通缩放作用域迁移为 `pushMatrix`/二维 `scale`/`popMatrix`。 |

另保留 Phase 11 已完成的 `ItemListScreen`、`CycleButton`、`TextBox`、`TextBlock`、`GuiUtil` 等迁移，不作回退。

## 视觉与容器顺序

- 标准 texture 调用保持原 `Identifier`、U/V、源/目标宽高、atlas 256/512 尺寸、tint/alpha 和代码提交顺序。滤镜与 film tint 改为同一 ARGB 值传给 GUI textured 调用，而非全局 `RenderSystem` 状态。
- 标题、页码、签名、作者、附件说明、按钮标题和状态文字保留原 Component、颜色、阴影、对齐与相对 `leftPos/topPos` 坐标。
- `AlbumScreen`、`CameraAttachmentsScreen`、`LightroomScreen` 与 `ItemRenameScreen` 均让 `AbstractContainerScreen` 继续提交 slots、carried item、slot highlight 和默认 item tooltip；自定义背景/普通外围纹理在 contents 边界提交，自定义普通 tooltip 在 `extractTooltip` 中注册，避免重复提交。
- 普通 widget hover/focus/active/disabled 的 sprite 或 UV 选择没有删减。`PhotographSlotWidget` 保留相纸、照片 overlay、边框和所有文字 tooltip 分支；只照片本体交给后续 renderer 阶段。
- 需要位于照片/图片上方的普通控件与 overlay 通过 `nextStratum()` 提交，替代旧三维 Z 偏移；二维 push/pop 严格配对。`ItemList`、`TextBox`、滚动文本既有 Scissor 作用域未回退。

## 分组验证

| 验证点 | common 错误数 | 结果 |
| --- | ---: | --- |
| Phase 12 基线：`phase12-common-baseline.log` | 565 | 通过执行，作为基线。 |
| 第一组工具/widget：`phase12-group1-widgets-camera.log` | 502 | 普通 widget、按钮与附件二维提交收敛。 |
| 第二组 Screen/容器：`phase12-group2-screens.log` | 457 | 相册、附件、Lightroom、重命名容器边界收敛。 |
| 第三组 tooltip/toast：`phase12-group3-tooltips-toasts.log` | 414 | 当前 Toast 和 deferred 标准物品 tooltip 收敛。 |
| 第四组 inspect 外围：`phase12-group4-inspect-perimeter.log` | 404 | 照片/底片查看器的普通上层控件与箭头收敛。 |
| 最终：`phase12-common-compile-final.log` | **390** | 失败，均待后续范围处理。 |

执行 `./gradlew :fabric:compileJava` 已保存至 `phase12-fabric-compile-final.log`：构建停在 `:common:compileJava` 的 390 条错误，**fabric javac 未开始**。

执行 `./gradlew clean build --continue` 已保存至 `phase12-clean-build-final.log`：同样有 390 条 common Java 错误；`common` 与 `fabric` 的 access-widener 验证仍报告既有 11 条旧目标。未编辑 access widener/Class Tweaker。

## 静态检查

- 参与编译的普通 GUI 路径：未发现旧 `drawString`、`drawCenteredString`、`renderTooltip`、`renderComponentTooltip`、`renderItem`、`renderItemDecorations`。
- 使用 PCRE 检查后，未发现未显式使用 `RenderPipelines.GUI_TEXTURED` 的普通 `guiGraphics.blit`。
- 未新增空 `extractRenderState`、空 `extractContents`、空 `extractWidgetRenderState`、no-op widget、固定不绘制分支、占位 texture 或删除的视觉分支。
- 仅剩的三维 pose 匹配位于 `PhotographClientTooltip`、`PhotographSlotWidget`、`PhotographScreen`、`FilmFrameInspectScreen`、`TestImageScreen` 的照片/动态图片块；它们均依赖 `MultiBufferSource`、照片 renderer、旧 vertex 绘制或动态图片，故不在本阶段修改。
- Phase 10 的 `KeyEvent`、`CharacterEvent`、`MouseButtonEvent` 处理没有改回旧签名。本次静态匹配到的 `mouseScrolled(double,...)`/`mouseDragged(double,...)` 是已有的当前 widget/Screen 方法，未改动其消费返回值。
- 本阶段未修改 networking、ExtraData、SavedData、照片格式、相机算法、模型/Shader/RenderTarget/截图/取景器源文件，也未把客户端 GUI 接入 dedicated-server 初始化路径。

## 剩余错误与明确跳过范围

最终 390 条错误仍以以下阶段为主。编译器完整列举前停止显示详细 diagnostics；可见的 275 个 source-location diagnostics 粗略分布为：照片/动态图片相关约 75、Render/model/pipeline 约 66、Shader/截图/取景器相关约 47、其余旧版非 GUI API 约 87；未列出的后续 diagnostics 同样需要下一阶段重新基线确认。

明确保留：

- `BakedModel`、`ModelResourceLocation`、`ItemProperties` 与 item model resolver；
- `RenderType`、`RenderStateShard`、Vertex/`VertexConsumer`、`MultiBufferSource`、实体 renderer 与客户端渲染 Mixin；
- `LightTexture`、Shader、`GameRenderer`、`RenderTarget`、截图、动态照片 texture、image/photograph renderer；
- Viewfinder、CameraCapture、相机画面、实体取景语义；
- access widener 中 11 条旧目标：RenderType/RenderStateShard 留给 RenderPipeline，GameRenderer 留给 Shader/截图/取景器，ItemProperties 留给模型阶段，EnderMan 留给相机捕获语义阶段。

风险评估：普通 GUI 的坐标、UV、atlas 尺寸、颜色和层次按原值迁移；`nextStratum` 替代的原 Z 层在实际游戏运行时仍应做视觉回归检查。没有发现 deferred tooltip 重复注册、Scissor 未关闭、输入消费回退或 dedicated-server 客户端类加载改动。照片/模型渲染尚未编译，因此相应界面的最终运行时视觉仍不可验证。

## 失败尝试与关键决策

- 未将照片 renderer、Vertex 渐变或旧三维 pose 强行套入 `GUI_TEXTURED`；这会改变照片/RenderTarget 的语义，已明确避免。
- `BetterTutorialToast` 不能继续实现旧 `render(GuiGraphicsExtractor, ToastComponent, long)`；改按实际 `Toast.update` 与 `Toast.extractRenderState` 分离，避免在绘制提取阶段反复触发隐藏回调。
- `CameraStandTooltip` 的标准物品采用公开 `item` 和 deferred item-tooltip API，背景采用当前 `TooltipRenderUtil.extractTooltipBackground`，没有删除相机图标或 tooltip。

## 下一步

普通 GUI 错误已基本清零，下一阶段不应在本线程扩张到模型或 RenderPipeline。建议新建对话并继续使用 **GPT-5.6 Terra**：下一阶段先单独处理“模型与 item model”，随后分别拆分“RenderPipeline 与客户端渲染 Mixin”、“Shader 与截图”、“取景器与相机捕获语义”。Terra 更适合需要根据 merged jar 验证大量渲染 API 与保持视觉/数据语义的迁移；每个子阶段都应重新建立编译基线。
