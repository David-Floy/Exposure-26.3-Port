# Phase 15：动态照片提交、照片框与 Item Frame

日期：2026-07-14。目标：Minecraft 26.1.2 Fabric / Loom 1.16.3 / Java 25。

## 结果与修改文件

`common` 错误从 **282** 降至 **195**；`:fabric:compileJava` 仍被 common 阻断。修改了 `RenderedImageInstance`、`ImageRenderer`、新建 `MipmappedDynamicTexture`/`ImageRenderRequest`、`PhotographRenderer`/`TextureRenderer`、照片框两 renderer 与 `PhotographFrameRenderState`、Item Frame renderer/state mixin、`ClientEvents`、5 个照片 GUI/tooltip 调用点、access widener 与 mixin 清单。

没有修改 networking、ExtraData/SavedData、Frame/像素编码、资源 item model、CameraStand、Shader、RenderTarget、capture 或 viewfinder。

## 旧数据流与新边界

旧路径为 `Frame -> renderedExposures.getOrCreate -> PhotographStyle.process -> RenderableImage -> ImageRenderer cache -> RenderedImageInstance -> DynamicTexture + 自定义 RenderType + MultiBufferSource`。缓存键仍是 `RenderableImageIdentifier.toResourceLocation()`；资源 reload、disconnect、packet 数据失效仍经 `ImageRenderer.clearCache/clearCacheOf` 释放纹理。

新 `ImageRenderRequest` 只保存 image identifier、quad 坐标/UV、light、RGBA；像素仍只留在 `ImageRenderer` 缓存。世界提交使用 `SubmitNodeCollector.submitCustomGeometry` 与 `RenderTypes.entityTranslucent(texture)`；顶点继续是原四顶点、同一 UV 顺序、lightmap、overlay=NO_OVERLAY、法线与矩阵。照片纸张/overlay 同样走 collector。`PhotographRenderRequest` 保存 image request、纸张/overlay Identifier、纸张旋转和正反面标志，不保存 ItemStack。

## 26.1.2 实际 API

`DynamicTexture` 只有一层 RGBA8、nearest/repeat sampler；因此不能保留旧 mipmap 行为。`MipmappedDynamicTexture` 继承 `AbstractTexture`，以 `GpuDevice.createTexture(COPY_DST|TEXTURE_BINDING, RGBA8, mipLevels+1)` 创建纹理，使用 nearest+mipmap repeat sampler，并以 `SpriteContents.increaseMipLevel`/逐层 `uploadFirstFrame(GpuTexture, level)` 上传。`TextureManager.register/release` 维持 Identifier 生命周期；没有 GC/finalizer 清理。

GUI 没有公开的 collector/custom-geometry callback：`GuiGraphicsExtractor` 只记录 `GuiRenderState` 的 texture setup。因此 GUI 仅在纹理已就绪时写入 `GUI_TEXTURED` blit；第一次请求排到 extraction 后的客户端执行队列上传，下一帧再次请求，不写 missing/static fallback，也不在 extraction 直接上传。

## renderer 迁移

`PhotographFrameEntityRenderer` 改为 `EntityRenderer<T, PhotographFrameRenderState>`。状态只含方向、旋转、size、透明/发光、frame `BlockStateModelPart`、fallback `ItemStackRenderState` 与照片 request；不含实体、Level、ItemStack 或像素。提交顺序仍是照片/物品，再 frame；small/medium/large、solid/cutout、墙面偏移、旋转、invisible、glow full-bright、outline 和 name tag 都被保留。玻璃框复用状态并以 `cutoutMovingBlock` 提交，照片仍先于玻璃模型。

Item Frame 使用 required `ItemFrameRenderStateMixin` 保存轻量 `PhotographRenderRequest`。renderer mixin 在 `extractRenderState` 尾部仅识别 Exposure Photograph，清空原版 item render state（不影响 frame/普通物品），在 `submit` 尾部复现旧矩阵/rotation/scale 后提交照片。普通 Item Frame 保持原版路径。

GUI 已迁移 PhotographScreen、PhotographSlotWidget、PhotographClientTooltip、FilmFrameInspectScreen、LightroomScreen 预览和 TestImageScreen 的照片本体；普通 Phase 12 GUI 未回退。GUI 使用同一像素缓存、UV/颜色语义与 `GUI_TEXTURED`，世界不混用 GUI pipeline。

## RenderType/Class Tweaker

旧 `RenderType.create`、CompositeState、TextureStateShard、text shader、transparency、lightmap 的 7 个已无调用方，已删除。映射：旧 POSITION_COLOR_TEX_LIGHTMAP/QUADS、text shader、translucent、lightmap、mipmap => collector custom quad + `ENTITY_TRANSLUCENT`、RGBA dynamic texture sampler、collector sorting/lightmap；纸张/overlay 同理。depth/cull/blend/sorting 由 ENTITY_TRANSLUCENT，frame solid/cutout 由 moving-block RenderTypes。

`:common:validateAccessWidener` 与 `:fabric:validateAccessWidener` 现在只剩 3 个禁止本阶段处理的旧目标：GameRenderer `loadEffect`、GameRenderer `getFov`、EnderMan `isLookingAtMe`。

## 验证与遗留

| 命令 | 结果 |
|---|---|
| `:common:compileJava` baseline | 282 errors |
| 纹理/照片框组 | 250 errors |
| Item Frame 组 | 234 errors |
| GUI 组/最终 common | 195 errors |
| `:fabric:compileJava` | common 失败，javac 未开始 |
| `clean build --continue` | common 195 errors；AW 仅上述 3 个禁止目标 |

剩余主要为 Shader/RenderTarget/viewfinder/capture、Humanoid/hand immediate renderer、以及其他未迁移 API。`ItemInHandRendererMixin` 仍调用旧 PhotographRenderer immediate overload，是下一渲染阶段必须完成的直接相关边界；没有用空 renderer、固定照片或删除分支绕开。

运行时尚未可测，风险为 GUI 首帧的一帧重试、mipmap/透明排序与 Item Frame 矩阵需编译后视觉回归。下一阶段建议使用 **GPT-5.6 Sol**，新建对话，专注手持照片、Shader/RenderTarget/截图/取景器/capture；不要扩大到 networking 或存储。
