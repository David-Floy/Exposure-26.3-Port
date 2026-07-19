# Exposure 26.1.2 Fabric 迁移报告：第十一阶段（GUI 二维绘制基础）

日期：2026-07-14  
范围：仅处理 `GuiGraphicsExtractor`、Screen/Widget 的抽取式二维绘制入口，以及已能和照片/模型渲染分离的文字、矩形、裁剪、Sprite/纹理提交。

## 结果与基线

| 检查 | Phase 10 | 本阶段最终 |
| --- | ---: | ---: |
| `:common:compileJava` Java 错误 | 673 | **565** |
| 变化 | — | **-108** |
| `:fabric:compileJava` | 被 common 阻断 | 仍被 common 阻断；Fabric javac 未开始 |
| `clean build --continue` | 673 common 错误 + 11 条旧 AW 目标 | 565 common 错误 + 同一 11 条旧 AW 目标 |

基线在 `phase11-common-baseline.log`，中间验证在 `phase11-ui-pass1.log`、`phase11-ui-pass2.log`，最终日志为 `phase11-common-compile-final.log`、`phase11-fabric-compile-final.log` 和 `phase11-clean-build-final.log`。

## 26.1.2 实际抽取式架构

本阶段直接检查了本地 26.1.2 `minecraft-merged-deobf` jar，而非根据旧版 GUI API 推断。确认的实际边界如下：

- Screen 驱动方调用 `extractRenderStateWithTooltipAndSubtitles`：先执行 `extractBackground`，再分层执行 `extractRenderState`，最后由 extractor 的 `extractDeferredElements` 提交 deferred tooltip、hover 文本和 preedit overlay。
- `AbstractWidget#extractRenderState` 是 final：它先按当前 Scissor 和坐标计算 hover，再调用子类 `extractWidgetRenderState`，并刷新 Widget tooltip。因此普通 `AbstractWidget` 子类改写该方法；`AbstractButton`/`ImageButton` 子类则改写其 `extractContents`。
- `AbstractContainerScreen#extractRenderState` 依次调用 `extractContents`、carried/snapback item 和 `extractTooltip`。其 `extractContents` 内部调用 Screen 的 widget 收集，再以 `leftPos/topPos` 的二维 matrix 作用域提交 labels、slot highlights 和 slots。
- `GuiGraphicsExtractor` 的立即式 `drawString`、`drawCenteredString`、`renderItem`、`renderItemDecorations`、`renderTooltip` 已分别由 `text`、`centeredText`、`item`、`itemDecorations`、`setTooltipForNextFrame` 取代；普通 texture `blit` 和 `blitSprite` 要显式使用 vanilla GUI textured pipeline。二维 pose 为 `Matrix3x2fStack`，使用 `pushMatrix/popMatrix`、二维 `translate/scale`；Scissor 仍是 `enableScissor/disableScissor`。

## 已修改的文件

- `client/gui/component/CycleButton.java`：改为 `extractContents`，普通 sprite 改由标准 GUI textured pipeline 提交；键盘、鼠标与 callback 保持 Phase 10 行为。
- `client/gui/screen/element/ToggleImageButton.java`：改为公开 `extractContents`，保留 on/off、active、hover sprite 选择。
- `client/gui/screen/element/TextBlock.java`：改为 `extractWidgetRenderState`，用 extractor 的 widget active-text 收集器保留链接 hover/tooltip/cursor 跟踪；文本点击位置改为逐 code point 的 style 计算，避免依赖删除的 `StringSplitter#componentStyleAtWidth`。
- `client/gui/screen/element/textbox/TextBox.java`：改为抽取式 widget 入口，正文/光标文本使用 `text`，选择区和插入光标使用二维 `fill`；保留颜色、焦点、选择、换行、复制粘贴和 Phase 10 事件签名。
- `client/gui/screen/ItemListScreen.java`：拆分为 `extractBackground` 与 `extractRenderState`；动画矩阵改为二维 push/translate/scale，普通背景纹理、slot、item、item decoration、slot highlight 与 deferred item tooltip 均走当前 extractor API，坐标和 256×256 UV 保持不变。
- `client/gui/screen/album/AlbumSigningScreen.java`、`AlbumViewScreen.java`：迁移 Screen 抽取入口、透明背景、相册背景 texture 和页码/标签文本。
- `client/gui/screen/album/AlbumScreen.java`：开始拆分容器的 contents/background/labels/tooltip 边界，widget 直接提交改为 `extractRenderState`，并改用二维 matrix 作用域。
- `client/gui/screen/FilmFrameInspectScreen.java`、`LightroomFrameInspectScreen.java`、`PhotographScreen.java`、`ChildPhotographScreen.java`：迁移外围 Screen 抽取入口；照片图像实际渲染块保持原样，留给照片图像/RenderPipeline 阶段。
- `client/gui/screen/ItemRenameScreen.java`、`LightroomScreen.java`、`camera/CameraAttachmentsScreen.java`：开始转为 container contents/tooltip 的当前入口，保留菜单绑定与 Phase 10 输入语义。
- `client/gui/screen/camera/button/FocalLengthButton.java`、`FrameCounterButton.java`、`ShutterSpeedButton.java`、`test/Slider.java`、`album/PhotographSlotWidget.java`：入口按 AbstractButton 或 AbstractWidget 的实际继承关系迁移。
- `client/util/GuiUtil.java`：GuiGraphics 版本的纯色 rectangle 直接改为 extractor `fill`；滚动字符串继续使用同一 Scissor 范围，改为 `text/centeredText`。

没有删除 Screen、Widget、Tooltip、背景、裁剪或视觉状态分支，也没有触碰 Phase 10 的输入消费返回值。

## 视觉语义与二维 API

- 已迁移部分保持原有 `leftPos/topPos`、控件坐标、UV、texture 尺寸、文字颜色、阴影参数、hover/focus/active 判断与 tooltip 条件。ItemList 的缩放中心仍为屏幕中心，push/pop 作用域不会泄漏到 tooltip。
- Tooltip 采用 deferred `setTooltipForNextFrame`，因此内容仍在背景、widget、slot 之后提交并由原版定位器约束屏幕边界。
- TextBox 保留自定义 selectionColor/selectionUnfocusedColor，未降级为固定文本高亮色。
- 仍未完成的普通二维 texture 重载、tooltip 与文字调用都保留为显式编译诊断，未以空 render、占位图或删除分支规避。

## 明确跳过

以下调用仍保留到后续专门阶段：

- `PhotographSlotWidget`、`PhotographScreen`、`FilmFrameInspectScreen`、`LightroomScreen` 和 `TestImageScreen` 中的照片像素/动态图片、`MultiBufferSource`、照片 renderer、实体/物品模型和截图相关块。
- `GuiUtil` 仍被 viewfinder 等路径调用的旧 `PoseStack` + vertex 绘制 overload。
- `Slider`/`TestImageScreen` 中旧 `RenderType`/`VertexConsumer` 横向渐变实现；不能用竖直 `fillGradient` 或纯色替代以改变视觉。
- `RenderType`、`RenderStateShard`、`RenderPipeline`（非标准 GUI textured API）、model baking、`ItemProperties`、shader、RenderTarget、GameRenderer、viewfinder、截图、照片上传与 entity renderer。
- networking、ExtraData、SavedData、旧照片路径、Frame/ExposureData/图片格式、相机算法和 11 条 Class Tweaker 目标。

11 条 access-widener/Class Tweaker 失败项未修改：RenderType/RenderStateShard 归 RenderPipeline 阶段；GameRenderer 归 Shader/截图/取景器阶段；ItemProperties 归模型和 item-model 阶段；EnderMan 归相机捕获语义阶段。

## 验证、静态检查与风险

- `:common:compileJava` 最终为 565 条错误；下降来自 GUI/Screen/Widget 的首层签名和 ItemList/文字/矩形/滚动文本迁移。其余数字不能按文件直接相加，因为 javac 连锁错误会折叠。
- `:fabric:compileJava` 只执行前置 common 编译，故 Fabric javac 仍未开始。
- `clean build --continue` 重现 565 条 common 错误，并重现既有 common/fabric access-widener 验证失败。
- 静态搜索仍定位到未迁移的旧 `drawString`、`renderTooltip`、`blit`、`pushPose` 和 `RenderType` 调用；其中一部分属于本阶段未完成的二维迁移，照片/模型/截图路径明确留给后续阶段。未发现本阶段新增的空 `extractRenderState`、空 `extractWidgetRenderState`、no-op widget 或固定不绘制分支。
- 已确认没有回退 KeyEvent、CharacterEvent、MouseButtonEvent、TextBox 编辑、相册焦点/页码、菜单请求或网络协议签名；本轮没有让 GUI 类进入 dedicated-server 初始化路径的改动。

当前风险为中等：已迁移的 ItemList、CycleButton、ToggleImageButton、TextBox、AlbumSigning/AlbumView 的二维层级与裁剪具备明确的新 API 对应；Album/CameraAttachments/Lightroom 仍含尚未消除的旧 texture/tooltip 调用，无法在当前构建状态下做客户端视觉回归。照片和模型块被刻意保持原样，避免在 GUI 阶段误改渲染语义。

## 阶段交接

- 已完成：本地 jar API 核验、Screen/Widget 抽取式入口的首批迁移、普通文本/矩形/滚动 Scissor、ItemList 的 texture/item/tooltip 路径，以及部分相册与容器外层。
- 关键决策：以 `extractBackground` → `extractRenderState` → deferred tooltip 的真实顺序拆分；只使用 `RenderPipelines.GUI_TEXTURED` 作为新 GuiGraphics 标准纹理入口，不为旧模型/vertex 代码猜测替代物。
- 失败尝试/限制：旧横向渐变、照片图像与 viewfinder 代码直接依赖已移除的 vertex/RenderType/PoseStack 体系；按范围限制未将其替换为不同外观的矩形或临时禁用。
- 遗留：继续完成未迁移的普通二维 blit/text/tooltip/pose 调用，特别是 Album、CameraAttachments、Lightroom 和普通按钮；然后单独处理模型与 item model、RenderPipeline 与客户端渲染 Mixin、Shader 与截图、取景器与相机捕获语义。
