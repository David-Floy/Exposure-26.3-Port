# Minecraft 26.1.2 Fabric port

## Scope and provenance

- Upstream repository: <https://github.com/mortuusars/Exposure>
- Fixed upstream baseline: `9383e0c9237099947a1e6399fcace5772f496c7d`
- Upstream release lineage: Exposure 1.9.18
- Target: Minecraft 26.1.2, Fabric, Java 25
- Maintained loader: Fabric only

The 1.21.1 NeoForge source is intentionally not enabled or carried as a supported 26.1.2 module. Restoring another loader is a separate port, not a build toggle. This port does not promise compatibility with worlds, photograph repositories, albums, or other data created by older Exposure versions.

## Capture architecture

Capture must use Minecraft 26.1.2's normal extraction and frame graph rather than trying to replay old immediate-rendering calls.

For background Capture, the task arms a dedicated `RenderTarget` and redirects the **next complete normal world frame** to that exclusive target. The normal frame supplies clear, sky, opaque terrain, block entities, entities, particles/weather, translucent content, and post-processing in the game's expected order. The target stays owned by the capture task until the asynchronous image callback has completed or the task has been safely cancelled.

Immediately before GUI submission, Capture queues `ScreenshotReadback` and ends the main-target redirect. GUI, HUD, tooltips, and hand rendering therefore do not enter the photograph. Readback remains asynchronous and fence-backed. Never replace this with synchronous `glReadPixels`, a nested/extra `renderLevel`, or desktop/window screenshot cropping.

Direct Capture also waits for a complete new normal frame. It submits asynchronous readback from the normal main target at the established frame boundary and does not own an extra target. Direct and background tasks must preserve their separate target ownership, cancellation, resize/world/resource-reset, late-callback, and close paths.

Compatibility work may adapt public frame lifecycle boundaries, but must not read an Iris-private or Shader-Pack-private framebuffer. The target seen through Minecraft's supported main-target path is the compatibility boundary.

## Pixel and shader invariants

- `NativeImage` returns ARGB data in Minecraft 26.1.2. Do not swap red and blue again.
- Exposure PostChain uniforms use Std140 layout. A `vec3` occupies a 16-byte aligned slot; explicit padding and field order must remain consistent with the shader UBO.
- Preserve upstream filter values and photographic intent. Do not compensate for a format/layout bug with a subjective color matrix.

## Rendering migration rules

Minecraft 26.1.2 separates render-state extraction from submission. Extract stable data while the source entity, item, menu, or model is valid, store it in the corresponding render state/request, and submit later without reaching back into stale live state. Do not reintroduce immediate-render assumptions.

GUI and Album blits must explicitly distinguish:

- destination rectangle and destination scaling;
- source sprite rectangle;
- complete atlas dimensions;
- render-state stratum/layer ordering.

Do not treat an old removed `z` argument as a new `u` or `v` coordinate. Album pages, photographs, panels, overlays, and the signing background must retain explicit source regions and ordering.

## Stable protocol and data boundaries

The following are compatibility-sensitive and must not be changed as incidental port maintenance:

- packet identifiers, direction, field order, registration, and `StreamCodec` definitions;
- `Frame`, `ExposureData`, `ExtraData`, `CameraId`, and photograph pixel/file formats;
- `SavedData` location and schema, exposure repository expectations, and export path safety;
- menu open-data envelopes and field order;
- Album and Signed Album component/data layout.

Any deliberate change requires prior discussion, migration behavior, regression tests, and explicit release notes.

## Recipes and item prototypes

Minecraft 26.1.2 `Ingredient.CODEC` uses `namespace:item` for items and `#namespace:tag` for tags. Do not restore legacy `{ "item": ... }` or `{ "tag": ... }` objects. A Fabric components ingredient's `base` uses the same string representation.

Component-transferring recipes must apply only the source stack's runtime `getComponentsPatch()`. Do not copy the complete resolved prototype map: doing so can overwrite the result's item model, name, and identity. Preserve each result `ItemStackTemplate`, recipe type, count, remainder behavior, and component conditions unless a separately reviewed data-format change is intended.

## Entity and model lifecycle invariants

- `HangingEntity` owns synchronized direction data in 26.1.2. `PhotographFrameEntity#defineSynchedData` must call `super.defineSynchedData(builder)` before defining Exposure fields and must preserve the parent update lifecycle.
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
- Shader claims apply only to the exact versions recorded in `COMPATIBILITY.md`. Other GPUs, render mods, player model layers, skin layers, and Shader Packs remain unverified until tested.

When adding a compatibility fix, record the affected versions, minimal reproduction, first root failure, implementation boundary, targeted validation, full gate, and remaining unverified matrix. Update `UPSTREAM_SYNC.md` if the business intent came from upstream.
