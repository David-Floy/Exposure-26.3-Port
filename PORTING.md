# Minecraft 26.3 Fabric port

## Scope and provenance

- Upstream repository: <https://github.com/mortuusars/Exposure>
- Fixed upstream baseline: `9383e0c9237099947a1e6399fcace5772f496c7d`
- Upstream release lineage: Exposure 1.9.18
- Port lineage: the unofficial Minecraft 26.1.2 Fabric port (`1.9.18-port.2+mc26.1.2`, Filmorae), ported to 26.3 in this branch
- Target: Minecraft 26.3, Fabric, Java 25
- Maintained loader: Fabric only

The 1.21.1 NeoForge source is intentionally not enabled or carried as a supported module. Restoring another loader is a separate port, not a build toggle. This port does not promise compatibility with worlds, photograph repositories, albums, or other data created by older Exposure versions.

## Capture architecture

Capture must use Minecraft's normal extraction and frame graph rather than trying to replay old immediate-rendering calls.

For background Capture, the task arms a dedicated `RenderTarget` and redirects the **next complete normal world frame** to that exclusive target. The normal frame supplies clear, sky, opaque terrain, block entities, entities, particles/weather, translucent content, and post-processing in the game's expected order. The target stays owned by the capture task until the asynchronous image callback has completed or the task has been safely cancelled.

Immediately before GUI submission, Capture queues `ScreenshotReadback` and ends the main-target redirect. GUI, HUD and tooltips therefore do not enter the photograph; the hand is hidden by the capture actions (`HideGuiAction`, viewfinder). Readback remains asynchronous and fence-backed. Never replace this with synchronous `glReadPixels`, a nested/extra `renderLevel`, or desktop/window screenshot cropping.

Direct Capture also waits for a complete new normal frame. It submits asynchronous readback from the normal main target at the established frame boundary and does not own an extra target. Direct and background tasks must preserve their separate target ownership, cancellation, resize/world/resource-reset, late-callback, and close paths.

Compatibility work may adapt public frame lifecycle boundaries, but must not read an Iris-private or Shader-Pack-private framebuffer. The target seen through Minecraft's supported main-target path is the compatibility boundary.

### 26.3 frame boundaries and redirect points

Since 26.2 the main render target belongs to `GameRenderer`, which reads its own field directly while rendering a frame. The redirect therefore has three parts, all routed through `BackgroundScreenshotCaptureTask#redirectMainRenderTarget`:

- `GameRendererMixin` redirects the `mainRenderTarget` field reads in the frame methods (`render`, `renderItemInHand`, `render3dHud`, `integrate3DHudDepth`, `applyPostEffects`, `takeAutoScreenshot`, `processBlurEffect`) and the return value of `GameRenderer#mainRenderTarget()`. `resize` and `close` must never be redirected.
- `SkyRendererMixin` redirects the target that 26.3 `SkyRenderer` caches at construction. Without it the sky is drawn into the real main target and the photograph has no sky.
- `LevelRendererMixin` keeps a `SkyRenderer` created during a capture frame from caching the short-lived capture target.

The redirect is active only while the capture frame renders: from `GameRenderer#render` HEAD of the frame after arming (`gameRenderState.shouldRenderLevel`) until the `GuiRenderer#render()` call. A capture is armed in the middle of the previous frame (Exposure's task cycle ticks at `FogRenderer#endFrame`), and 26.3 presents the swapchain from `GameRenderer#mainRenderTarget()`; redirecting from the moment of arming would present the undefined, never-cleared capture target for one frame.

The capture target mirrors the main target's attachment formats (`RGBA8_UNORM` colour, `D32_FLOAT` depth in 26.3) by reading them from the main target's textures.

## Pixel and shader invariants

- `NativeImage` returns ARGB data. Do not swap red and blue again. `NativeImage.read(InputStream)` decodes to RGBA, exactly like the removed `read(Format.RGBA, stream)`.
- Exposure PostChain uniforms use std140 layout. **26.3 changed `Std140Builder#putVec3` to advance 12 bytes** (standard std140); 26.1.2 advanced 16 bytes, which is why the 26.1.2 shader carried explicit padding floats after each `vec3`. The colour processor block is now `vec3 Mul; vec3 Add; float Saturation; float Contrast;` (offsets 0/16/28/32). `ShaderResourceTests` writes every filter's JSON uniforms with the engine's `Std140Builder` and reads them back at the shader's std140 offsets; keep that test when touching uniforms.
- 26.3 compiles shaders to SPIR-V with Shaderc on both OpenGL and Vulkan: shaders need `#extension GL_ARB_separate_shader_objects : require`, explicit `layout(location = N)` on every `in`/`out`, and `#include` instead of `#moj_import`.
- Preserve upstream filter values and photographic intent. Do not compensate for a format/layout bug with a subjective color matrix.

## Rendering migration rules

Minecraft separates render-state extraction from submission. Extract stable data while the source entity, item, menu, or model is valid, store it in the corresponding render state/request, and submit later without reaching back into stale live state. Do not reintroduce immediate-render assumptions.

- 26.2 moved the in-game HUD from `Gui` into `Hud`, the screen to `Gui#screen()`/`Gui#setScreen`, and the toast manager to `Gui#toastManager()`. `Options#hideGui` is now `Hud#isHidden()`, which can only be toggled.
- `Hud#extractRenderState` publishes `isHidden` to `GuiRenderState#isHudHidden`, which is not reset per frame and is read by hand and screen-effect rendering. `HudMixin` must inject after that assignment, not at HEAD.
- The debug overlay's `extractRenderState` runs on every frame (26.1.2 and 26.3) and decides itself what to draw. Hooks that replace or extend the F3 screen, such as the development-only Bugger, must check `DebugScreenEntryList#isOverlayVisible()`; `showDebugScreen()` alone is also true while a debug entry is set to "always on".
- `minecraft:composite` item models apply each sub-model's own display transforms. Upstream merged the camera attachment models into the body model, so `items/camera.json` uses `exposure:shared_transform_composite` (`SharedTransformCompositeModel`), which applies the first (body) layer's display transform to every layer. Keep attachment models in the body's model space without their own `display` block; a resource pack that overrides `items/camera.json` with `minecraft:composite` misplaces the attachments.
- 26.3 replaced `ItemInHandRenderer` with the state-driven `FirstPersonHandsAndItemsRenderer`; held items and player visibility come from `FirstPersonHandsAndItemsRenderState` and `PlayerRenderState`.
- Level extraction moved from `LevelRenderer` into `LevelExtractor` (26.2). Entity outlines are derived from submitted features; there is no `haveGlowingEntities` flag anymore.

Held photograph front and back faces use a local unit quad (`x/y = 0..1`).  The back face must rotate around `(0.5, 0.5, 0)` and sit only a small distance behind the front face; do not carry forward the legacy `rotateY(180)` plus `translate(-0.5, 0, -0.5)` sequence.  That sequence places a submitted back quad half a unit away and appears as a grey duplicate in third person.  Keep the centre/bounds and reversed-normal regression test when adapting this renderer.

GUI and Album blits must explicitly distinguish:

- destination rectangle and destination scaling;
- source sprite rectangle;
- complete atlas dimensions;
- render-state stratum/layer ordering.

Do not treat an old removed `z` argument as a new `u` or `v` coordinate. Album pages, photographs, panels, overlays, and the signing background must retain explicit source regions and ordering.

## Input (SDL3, 26.3)

26.3 replaced GLFW with SDL3. All input codes changed:

- Keys are SDL scancodes (`KeyEvent#key`, `InputConstants.KEY_*`); `KeyEvent#scancode` was renamed to `keycode` and is now the logical key. Use `InputConstants` or `org.lwjgl.sdl.SDLScancode` constants, never literal GLFW numbers (for example `333` for keypad minus or `256` for escape).
- Mouse buttons are SDL buttons: left `1`, middle `2`, right `3` (`InputConstants.MOUSE_BUTTON_*`). Never compare `MouseButtonEvent#button()` with literal `0`/`1`.
- Container click packets still use protocol buttons (`0` primary, `1` secondary); `AbstractContainerScreen` maps SDL buttons back to them. Menu code must compare with the protocol value, not with `InputConstants`, whose values are compile-time constants and would be inlined.
- Modifiers are `SDL_KMOD_*` masks with separate left/right bits and lock-key state (Num Lock, Caps Lock). Exposure key bindings convert them with `Modifier#fromInput`, so a binding without modifiers keeps working with Num Lock enabled.
- Text input only arrives while a widget holds text input focus. Custom text widgets must call `Minecraft#onTextInputFocusChange` from `setFocused` (see `TextBox`).

## Stable protocol and data boundaries

The following are compatibility-sensitive and must not be changed as incidental port maintenance:

- packet identifiers, direction, field order, registration, and `StreamCodec` definitions;
- `Frame`, `ExposureData`, `ExtraData`, `CameraId`, and photograph pixel/file formats;
- `SavedData` location and schema, exposure repository expectations, and export path safety;
- menu open-data envelopes and field order;
- Album and Signed Album component/data layout.

Any deliberate change requires prior discussion, migration behavior, regression tests, and explicit release notes.

## Datapack formats (26.3)

26.3 data codecs **ignore unknown keys**. Datapack JSON in an older format still loads without an error but silently loses the ignored parts. `DataResourceFormatTests` rejects the legacy keys that Exposure data used:

- Loot tables: a pool or entry has one `condition` (several are combined with `minecraft:all_of`) and one `modifier` (an object or a list) instead of `conditions`/`functions`; conditions and functions are dispatched by `type` instead of `condition`/`function`. `rolls` is an int provider.
- Advancement predicates: `ContextAwarePredicate` became `Holder<LootItemCondition>`. Inline entity predicates must be written as `minecraft:entity_properties` conditions, and entity predicates are maps keyed by sub-predicate type (`minecraft:entity_type`, `minecraft:vehicle`, `exposure:tamed`, …) instead of `type`/`vehicle`/`type_specific` fields. `minecraft:recipe_unlocked` takes `recipes`.
- Item predicates use `"items": "#namespace:tag"`; a `tag` key has been ignored since 1.20.5.
- `FrameExposedTrigger`'s trigger-level `entities_in_frame` is a list of loot conditions; `frame.entities_in_frame` is Exposure's own `EntityInFramePredicate` list and keeps its format.

## Recipes and item prototypes

`Ingredient.CODEC` uses `namespace:item` for items and `#namespace:tag` for tags. Do not restore legacy `{ "item": ... }` or `{ "tag": ... }` objects. A Fabric components ingredient's `base` uses the same string representation.

Component-transferring recipes must apply only the source stack's runtime `getComponentsPatch()`. Do not copy the complete resolved prototype map: doing so can overwrite the result's item model, name, and identity. Preserve each result `ItemStackTemplate`, recipe type, count, remainder behavior, and component conditions unless a separately reviewed data-format change is intended.

## Entity and model lifecycle invariants

- `HangingEntity` owns synchronized direction data. `PhotographFrameEntity#defineSynchedData` must call `super.defineSynchedData(builder)` before defining Exposure fields and must preserve the parent update lifecycle.
- 26.3 entities take their network id from `Level#getNextEntityId()`; unit tests that construct entities need a level instance (see `PhotographFrameEntitySynchedDataTests`).
- The humanoid `hat` is a child of `head` (`head.getChild("hat")`). A child already inherits its parent transform. Do not copy the parent's pose into the child's local pose a second time.
- Selfie/held-camera poses flow through extracted humanoid render state and must preserve the selected hand, camera entity, yaw, pitch, and unaffected limb behavior.

## Local validation

Use Java 25 and the repository Gradle Wrapper. A reusable `GRADLE_USER_HOME` may point to an external cache, but do not copy build outputs or cache directories into the repository:

```powershell
.\gradlew.bat :common:compileJava :fabric:compileJava :common:compileTestJava
.\gradlew.bat :common:test --rerun-tasks
.\gradlew.bat :common:validateAccessWidener :fabric:validateAccessWidener
.\gradlew.bat --rerun-tasks build
```

The final test total must be at least 58 with no failures, errors, or skipped tests.

## Runtime validation boundary

- Client smoke testing covers startup, world entry, core camera/viewfinder/capture flows, Album and GUI flows, photo rendering/export, frames, and clean shutdown.
- Integrated-server testing covers persistence, menus, commands, capture upload, and save/reload behavior.
- Dedicated-server testing covers startup to `Done`, server-safe registration and command paths, persistence/export where relevant, and normal stop.
- Mixins use `defaultRequire: 1`, but an `@Inject` whose target method still exists with a changed signature, or an `@Invoker` with a changed descriptor, only fails when its target class loads. A clean client start is the minimum check after a Minecraft update.
- Shader claims apply only to the exact versions recorded in `COMPATIBILITY.md`. Other GPUs, render mods, player model layers, skin layers, and Shader Packs remain unverified until tested.

When adding a compatibility fix, record the affected versions, minimal reproduction, first root failure, implementation boundary, targeted validation, full gate, and remaining unverified matrix. Update `UPSTREAM_SYNC.md` if the business intent came from upstream.

## Current release artifact

- Version: `1.9.18-port.1+mc26.3` (development build, not released)
- Target: Minecraft 26.3, Fabric, Java 25
- File: `exposure-fabric-26.3-1.9.18-port.1+mc26.3.jar`
- The previous 26.1.2 release (`1.9.18-port.2+mc26.1.2`) is documented in the 26.1.2 branch. This workflow does not publish to Modrinth or CurseForge.
