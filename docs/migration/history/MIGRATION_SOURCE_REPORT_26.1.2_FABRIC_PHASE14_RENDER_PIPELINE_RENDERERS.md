# Phase 14 — RenderPipeline, renderer, and render-Mixin migration

Date: 2026-07-14  
Target: Minecraft 26.1.2 Fabric (Gradle 9.4.0, Java 25, Loom 1.16.3)

## Result

This pass established the actual 26.1.2 renderer boundary from the merged jar and migrated the complete Camera Stand outer renderer path.  `common` compilation errors reduced from **303** to **282**.  The remaining Photograph Frame, dynamic-photo, Shader, RenderTarget, screenshot, viewfinder, and capture paths were deliberately not replaced with defaults, empty submissions, static photographs, or GUI pipelines.

## Modified files

| File | Reason |
| --- | --- |
| `common/.../client/render/CameraStandRenderState.java` | New minimal per-frame state: transformed rotations, flags, model parts, and `ItemStackRenderState`; it holds no entity or `ItemStack`. |
| `common/.../client/render/CameraStandEntityRenderer.java` | Replaced old `EntityRenderer#render`, dispatcher/model-renderer calls, and `ItemRenderer#renderStatic` with extract/submit, `SubmitNodeCollector`, `BlockStateModelPart`, and `ItemModelResolver`. |
| `common/.../mixin/client/ExposureLightTextureMixin.java` | Retargeted the brightness hook from removed `LightTexture` to `Lightmap#getBrightness`. |
| `common/.../client/gui/screen/test/Slider.java` | Replaced obsolete GUI `VertexConsumer` submission with extractor fills, retaining left-to-right ARGB interpolation. |
| `common/.../client/gui/screen/test/TestImageScreen.java` | Same horizontal-gradient migration as Slider. |

No Phase 13 item-model, `ExtraModelKey`, `BlockStateModel` lookup, networking, ExtraData, SavedData, photo format, or camera-algorithm files were changed.

## Verified 26.1.2 renderer architecture

The project’s `minecraft-merged.jar` exposes:

- `EntityRenderer<T, S extends EntityRenderState>` with `createRenderState`, `extractRenderState(T,S,float)`, and `submit(S,PoseStack,SubmitNodeCollector,CameraRenderState)`.
- `EntityRenderDispatcher` extracts the state before submission.  Base state owns `lightCoords` and `outlineColor`.
- `BlockStateModel` only supplies `collectParts(RandomSource,List<BlockStateModelPart>)`; it is not an old `BakedModel` and there is no old `BlockRenderDispatcher` immediate path.
- `SubmitNodeCollector.submitBlockModel(PoseStack, RenderType, List<BlockStateModelPart>, int[] tints, int light, int overlay, int outline)` is the block-part submission boundary.
- `ItemModelResolver.updateForNonLiving(ItemStackRenderState, ItemStack, ItemDisplayContext, Entity)` selects the Phase 13 item-model tree. `ItemStackRenderState.submit` submits that selection later.
- `RenderType` now lives in `net.minecraft.client.renderer.rendertype`; standard public setup factories are `RenderTypes`, while GPU pipelines are static `RenderPipelines`. GUI extraction has its own `GuiGraphicsExtractor` pipeline system and must not be used for world entities.

`RenderPipelines` statically registers vanilla pipelines (for example `SOLID_BLOCK`, `CUTOUT_BLOCK`, `ENTITY_*`, `GUI`, and `GUI_TEXTURED`).  No public Fabric registration point was found or used for a custom Exposure pipeline in this pass.  Custom Shader/mipmap work therefore remains deferred.

## Legacy RenderType mapping

| Legacy use | 26.1.2 mapping/status | Texture, format, mode, state |
| --- | --- | --- |
| Camera Stand extra block model (`RenderType.solid`) | `RenderTypes.solidMovingBlock()` through `submitBlockModel` | Block atlas from baked parts; block vertex format/quads; opaque/depth-writing/culling semantics supplied by the standard moving-block setup; light, overlay, outline supplied to collector. |
| Camera Stand mounted camera (`ItemRenderer.renderStatic`) | `ItemModelResolver` + `ItemStackRenderState.submit` | Existing item-model selection; fixed `NONE` display context; light/overlay/outline retained. |
| Frame solid/cutout block sheets | Not migrated: frame surrounds dynamic photograph submission and needs the same state split. | Must retain solid vs cutout distinction, atlas texture, depth/cull, light/overlay, and order. |
| Dynamic photograph `text` / custom mipmap `RenderType.create` | Deferred | Uses photograph texture and custom mipmapping, translucency/lightmap, and old composite state: Shader/photo phase. |
| Paper/overlay texture quads | Deferred | Used by `PhotographRenderer`; dynamic-photo path, not replaced. |
| Slider/Test horizontal GUI gradients | `GuiGraphicsExtractor.fill` columns | World RenderType is not used; each column preserves ARGB alpha and left-to-right interpolation. |

No distinct legacy visual semantics were collapsed into one default pipeline.

## Camera Stand migration

The new state snapshots interpolated yaw/pitch, vehicle yaw, hurt rotation, malfunction state, and the scale of the resolved camera item. It collects the Phase 13 `CAMERA_STAND` and `CAMERA_STAND_MOUNT` extra `BlockStateModel` parts using a stable entity-id seed. Submission preserves original order: stand, mount, then camera. It preserves original matrix scopes, translations, rotations, malfunction transforms, `OverlayTexture.NO_OVERLAY`, packed light, outline, and camera-item model selection. No cuboid replacement or old `BakedModel` fallback was introduced.

## Photograph Frame and glass frame

These renderers were inspected but not changed. Their outer frame is separable in principle, but their current method still interleaves the ordinary frame part with `PhotographRenderer` / `ImageRenderer` submissions. Migrating only the visible frame while silently dropping, freezing, or substituting the photo would violate the phase constraints. The next rendering pass should first provide a photo render request/state and collector-based dynamic texture submission, then move the frame and glass model part submission without changing model IDs, size selection, facing, invisibility, glow, or ordering.

## Client Mixin status

- `ExposureLightTextureMixin` now targets `Lightmap`; `Lightmap.getBrightness(DimensionType,int)` is the current static brightness method.
- `HumanoidModelMixin` is deferred: `HumanoidModel` now accepts `HumanoidRenderState`, while the old hook receives a `LivingEntity`; preserving camera pose semantics requires state extraction rather than retaining an entity in state.
- `ItemFrameRendererMixin` is deferred: Item Frames now submit `ItemFrameRenderState`; the old interception depends on the removed immediate `render(ItemFrame,...,MultiBufferSource)` boundary.
- `MinecraftMixin` shader/RenderTarget hook and Fabric `LevelRendererMixin` are intentionally untouched, as they are screenshot/viewfinder/render-target architecture.
- No required mixin was removed, made optional, or had `defaultRequire` lowered.

## Class Tweaker

The six RenderType/RenderStateShard entries remain because `RenderedImageInstance` still directly uses the legacy custom mipmap composite state. Removing them before the dynamic-photo/Shader migration would hide an active call site. Both validation tasks report the unchanged 10 stale targets: six RenderType/RenderStateShard, two GameRenderer, and one EnderMan method plus their associated entries. GameRenderer and EnderMan were not modified.

## Validation

| Command | Result |
| --- | --- |
| `:common:compileJava` baseline | failed, 303 errors (`phase14-common-baseline.log`) |
| Group 2 Camera Stand | failed, 292 errors; Camera Stand diagnostics removed (`phase14-group2-camera-stand.log`) |
| Group 4 Mixin/vertex | failed, 282 errors (`phase14-group4-mixins-vertex.log`) |
| final `:common:compileJava` | failed, 282 errors (`phase14-common-compile-final.log`) |
| `:fabric:compileJava` | blocked by failed `:common:compileJava` (`phase14-fabric-compile-final.log`) |
| `clean build --continue` | failed: common compilation and unchanged access-widener targets (`phase14-clean-build-final.log`) |
| `:common:validateAccessWidener` | failed, unchanged 10 targets (`phase14-common-accesswidener.log`) |
| `:fabric:validateAccessWidener` | failed, unchanged 10 targets (`phase14-fabric-accesswidener.log`) |

## Remaining error distribution and risks

The largest remaining groups are dynamic photo/image texture and legacy RenderType state; Photograph Frame/Glass outer renderer; viewfinder, screenshot, RenderTarget, and camera capture; then old GUI photo rendering and unrelated API migrations. `fabric` javac did not start because common still fails.

Risk remains around block moving-layer choice and runtime visual verification of Camera Stand. The selected `RenderTypes.solidMovingBlock()` preserves the former solid block semantics, but game runtime validation awaits a compiling client. The migrated GUI gradients keep direction and alpha but should also receive visual regression testing. Frame/glass ordering, blend/depth/sorting, photo pixels, screenshot, viewfinder, and Shader behavior remain unverified because they were intentionally not migrated.

## Next step

Use a new conversation with **GPT-5.6 Sol** for a narrowly scoped dynamic-photo submission pass: migrate `RenderedImageInstance`/`PhotographRenderer` from old composite RenderType and immediate buffers to collector/custom geometry, then split Photograph Frame and Item Frame renderer state safely. Keep Shader loading, RenderTarget, screenshot, viewfinder, and camera capture as a separate follow-up unless the collector implementation proves it must cross that boundary.
