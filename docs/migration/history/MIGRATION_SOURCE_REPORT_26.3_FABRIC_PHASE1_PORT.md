# Minecraft 26.3 Fabric port — Phase 1 report

## Conclusion

The unofficial Fabric port of Exposure 1.9.18 was ported from Minecraft 26.1.2 to Minecraft 26.3 on branch `26.3` (based on `26.1.2` at `454b54ab`). Both modules compile, 70 tests pass (23 suites, 0 failures, 0 errors, 0 skipped), both Access Widener validations and the complete `--rerun-tasks build` succeed. A development client on OpenGL started, entered a new world, captured and exported a photograph through the background capture path, and exited cleanly; a dedicated server reached `Done` and stopped normally.

The maintainer then tested the port in-game on OpenGL and Vulkan (see Runtime evidence). Not verified in this phase: Improved Transparency (OIT), Iris/Sodium/Shader Packs, and the individual flows listed under Remaining work. Nothing was committed.

## Inputs and baseline

- Read `AGENTS.md`, `PORTING.md`, Phase 25 (newest 26.1.2 report), and — for the capture subsystem — Phase 22.
- The maintainer's analysis `porting-26.3-analyse.md` (German, untracked) listed the expected API changes and open questions.
- 26.1.2 baseline in this checkout: 20 suites, 60 tests, 0 failures (Gradle 9.4.0, Loom 1.16.3).
- Method: Loom `genSources` for 26.3, plus both merged Minecraft jars (26.1.2 and 26.3) decompiled identically with Vineflower 1.12.0 so every mixin target method and vanilla asset/data file could be diffed directly. The NeoForge porting primers for 26.2 and 26.3 were used as a change index.

## Toolchain

| Component | 26.1.2 | 26.3 |
|---|---|---|
| Minecraft | 26.1.2 | 26.3 |
| Java | 25 | 25 (Temurin 25.0.4.1) |
| Gradle | 9.4.0 | 9.7.1 |
| Loom | 1.16.3 | 1.18.4 |
| Fabric Loader | 0.19.3 | 0.19.5 |
| Fabric API | 0.150.0+26.1.2 | 0.164.0+26.3 |
| Forge Config API Port | 26.1.4 | 26.3.1 |

The combination follows `fabric-example-mod` (branch `26.3`). Newer releases existed (Loom 1.19.x, Gradle 9.8.x, Fabric API 0.165.0+26.3) but were not needed.

## Changes by subsystem

### Access Widener

`validateAccessWidener` reported three dead entries; all others were still valid. They were renamed one-to-one: `GameRenderer#setPostEffect`/`effectActive` became `setSpectatedEntityPostEffect`/`spectatedEntityEffectActive` (26.3 keeps a separate list of player post effects next to this spectator slot), and `EnderMan` became `Enderman` (`isBeingStaredBy` unchanged).

Three entries were added for `SharedTransformCompositeModel` (`ItemStackRenderState#activeLayerCount`/`layers`, `LayerRenderState#itemTransform`); its type is registered through `ItemModels.ID_MAPPER`, which Fabric API's transitive Access Widener already opens in the Fabric module.

### GUI and HUD (26.2 reorganization)

`Minecraft#setScreen`/`screen`/`getToastManager` moved to `Gui`; the HUD moved to `Hud`; `Options#hideGui` became `Hud#isHidden()` (toggle only). `GuiMixin` was renamed to `HudMixin` (same bodies, targets `Hud#extractRenderState`/`extractCrosshair`), and the `setScreen` hook moved from `MinecraftMixin` into a new `GuiMixin`. `HudMixin` injects after `GuiRenderState#isHudHidden` is written: that field is not reset per frame and is read by hand/screen-effect rendering, so cancelling at HEAD would leave it stale. `HideGuiAction` toggles `Hud` and restores the previous state.

The development-only Bugger overlay (`BuggerScreenOverlayMixin`) hooks `DebugScreenOverlay#extractRenderState`. Upstream hooked `render`, which only ran while F3 was open; `extractRenderState` runs on every frame (already in 26.1.2), so the 26.1.2 port drew the Bugger page hint permanently in development clients while the arrow keys only worked with F3. The overlay and `BuggerKeyboardHandlerMixin` now share `Bugger#isDebugScreenOpen()`: the F3 overlay is open (`DebugScreenEntryList#isOverlayVisible()`) and `showDebugScreen()` allows drawing. `showDebugScreen()` alone is also true while a debug entry is "always on", which the vanilla Performance profile sets for FPS.

### Capture

- The main render target moved from `Minecraft` to `GameRenderer`, which reads its field directly. The former `@ModifyReturnValue` on `Minecraft#getMainRenderTarget` became: field redirects in the `GameRenderer` frame methods, a `@ModifyReturnValue` on `GameRenderer#mainRenderTarget()`, and an accessor for the real field.
- 26.3 presents the swapchain from `GameRenderer#mainRenderTarget()` and a new `TextureTarget` is not cleared. Because a capture is armed in the middle of the previous frame, the redirect is now limited to the capture frame itself (render HEAD after arming until the `GuiRenderer#render()` call). Without this, one presented frame would show the undefined capture target.
- 26.3 `SkyRenderer` caches the main target at construction. New `SkyRendererMixin` redirects it during the capture frame; `LevelRendererMixin` keeps a `SkyRenderer` created during a capture frame from caching the short-lived capture target.
- The capture target mirrors the main target's formats (`RGBA8_UNORM`/`D32_FLOAT`) read from its textures.
- `render(DeltaTracker, boolean)` became `render()`; the frame-started hook now reads `gameRenderState.shouldRenderLevel`. `FogRenderer#endFrame` still exists; `GuiRenderer#render` lost its `GpuBufferSlice` parameter.
- Readback remains `Screenshot.takeScreenshot` (asynchronous). No synchronous read, extra `renderLevel`, or screen cropping was added.

### Rendering

- `ItemInHandRenderer` became the state-driven `FirstPersonHandsAndItemsRenderer`; the mixin was renamed accordingly and now reads held items and visibility from `FirstPersonHandsAndItemsRenderState`/`PlayerRenderState`. The `ItemStack#isEmpty` ordinal-0 anchor and pose maths are unchanged.
- `extractVisibleEntities` moved to `LevelExtractor` (Fabric `LevelExtractorMixin`); the removed glowing flag needs no replacement because outlines are derived from submitted features.
- `PoseStack#mulPose(Quaternion)` → `rotate` (35 call sites, same semantics); `ArmedEntityRenderState` swing fields → `currentSwing`/`swingAnimation`; `LivingEntity#attackAnim` → `getSwingAnimation`.
- Textures moved to Renderpearl (`com.mojang.renderpearl.api.*`, `TextureFormat.RGBA8` → `GpuFormat.RGBA8_UNORM`). `NativeImage.read(Format.RGBA, stream)` → `read(stream)` (identical RGBA decoding).
- Camera attachments (flash, lens, viewfinder, selfie stick) were misplaced wherever the camera has a display transform (dropped, held, head, item frame); the maintainer reported it for a dropped camera. Cause, inherited from the 26.1.2 port: upstream's `CompositeModel` merged the attachment quads into the body model and used the body's transforms, while vanilla `minecraft:composite` applies each sub-model's own display transforms and the attachment models define none. `items/camera.json` now uses the new `exposure:shared_transform_composite` type (`SharedTransformCompositeModel`), which gives all layers the first (body) layer's display transform; local `transformation` values still apply per layer. The camera stand renders with `ItemDisplayContext.NONE` and was not affected.

### Shaders

- 26.3 compiles shaders to SPIR-V: added `#extension GL_ARB_separate_shader_objects : require` and `layout(location = 0)` on `texCoord`/`fragColor`. Post-effect JSON is unchanged.
- **`Std140Builder#putVec3` advances 12 bytes in 26.3 (16 in 26.1.2).** The 26.1.2 shader's explicit padding would have shifted `Saturation`/`Contrast` and broken every colour filter. The block is now standard std140. `ShaderResourceTests` was rewritten to write each filter's JSON uniforms through the engine (`PostChainConfig.CODEC`, `UniformValue#writeTo`, `Std140Builder`) and read them back at the shader's std140 offsets.

### Input (SDL3)

- `KeyEvent#scancode` → `keycode`; GLFW keypad minus (`GLFW_KEY_KP_SUBTRACT`, literal `333`) → `SDLScancode.SDL_SCANCODE_KP_MINUS`; literal escape `256` → `InputConstants.KEY_ESCAPE`; `InputConstants.isKeyDown` no longer takes a window.
- Mouse buttons are SDL (left 1, right 3). Seven comparisons in five widgets/screens (`CycleButton`, `AlbumScreen`, `TextBlock`, `TextBox`, `LightroomScreen`) used GLFW literals `0`/`1` (CycleButton would have cycled backwards on left click, text widgets ignored left clicks); they now use `InputConstants.MOUSE_BUTTON_*`.
- `AlbumMenu#clicked` compared the container click protocol button with `InputConstants.MOUSE_BUTTON_LEFT`, a compile-time constant that is now `1`; it now compares with the protocol value `0` (vanilla maps SDL buttons back in `AbstractContainerScreen`).
- Modifiers are `SDL_KMOD_*` masks with left/right bits and lock-key state. `Modifier#fromInput` converts them, so Ctrl shortcuts work and bindings without modifiers keep working with Num Lock on.
- `MouseHandlerMixin#onButton`: anchor `Minecraft#getOverlay` → `Gui#overlay`; the `buttonInfo` local no longer exists, `rawButtonInfo` is the effective button.
- `TextBox#setFocused` reports text input focus (`Minecraft#onTextInputFocusChange`), as SDL only delivers characters to a focused text input owner.

### Advancements, predicates and world APIs

- Predicate classes moved to `net.minecraft.advancements.predicates[.entity]`, triggers to `...advancements.triggers`; `ContextAwarePredicate` → `Holder<LootItemCondition>`; the entity sub-predicate registry stores `Codec` instead of `MapCodec` (`TamedPredicate` is now written as `"exposure:tamed": {"is_tamed": …}`).
- `LivingEntity#drop` and `Inventory#placeItemBackInInventory` take a `Prediction` (`PREDICTED` reproduces 26.1.2, as in vanilla `LecternMenu`); the debug command's random-scatter drop mirrors `Inventory#dropAll`.
- `EntityType` constants moved to `EntityTypes`; spawn configs are `PostSpawnProcessor`; `AxeItem` was removed, so `canStrip` checks the `minecraft:axe` block transformer component.
- Loot injection uses `Holder<LootTable>` from the reload provider (`NestedLootTable.lootTableReference`).
- Smaller replacements: `isSingleplayer` helper (same semantics as the removed method), `Blaze3D.openUri/openPath`, `ConfirmLinkScreen(URI)`, `MissingTextureAtlasSprite.getLocation()` for the removed missing-texture sentinel, unused feature helper registers feature types (features are data-driven).

### Datapack JSON

26.3 data codecs ignore unknown keys: data in the old format loads without an error and silently loses conditions, functions, and predicates. 23 files were converted with a scripted converter that fails on unexpected structures:

- Loot tables: `conditions`/`functions` lists → single `condition`/`modifier`; `condition`/`function` dispatch keys → `type`; `rolls` as int.
- Advancements: trigger-level entity predicates → `minecraft:entity_properties` conditions with sub-predicate maps (`minecraft:entity_type`, `minecraft:vehicle`, `exposure:tamed`); `recipe_unlocked` `recipe` → `recipes`. Exposure's own `frame.entities_in_frame` was left unchanged.
- The lightroom recipe advancement used an item predicate `tag` key that has been ignored since 1.20.5 (any inventory change unlocked it); it is now `"items": "#exposure:developed_film_rolls"`.

Recipes, tags, and Exposure's own registries (filters, lenses, palettes) needed no change. All referenced vanilla ids exist in 26.3. Assets needed no change (no `shade` flags, no removed tint sources).

## Validation

| Gate | Result |
|---|---|
| `:common:compileJava :fabric:compileJava :common:compileTestJava` | success |
| `:common:test --rerun-tasks` | 23 suites, 70 tests, 0 failures, 0 errors, 0 skipped |
| `:common:validateAccessWidener :fabric:validateAccessWidener` | success |
| `--rerun-tasks build` | success |
| JAR audit | 1239 entries, no logs/worlds/screenshots/local paths |

Test changes: `PhotographFrameEntitySynchedDataTests` needs a level because 26.3 entities take their id from `Level#getNextEntityId()` (an unconstructed `ClientLevel`; assertions unchanged) and uses `EntityTypes`; the std140 padding assertion was replaced by the engine-based layout test. New: `colorProcessorDeclaresVaryingLocationsRequiredBySpirvCompilation`, `DataResourceFormatTests` (3), `ModifierTests` (3), `SharedTransformCompositeModelTests` (3).

All gates above were re-run after the Bugger overlay fix and again after the camera attachment fix. Artifact of the last build: `fabric/build/libs/exposure-fabric-26.3-1.9.18-port.1+mc26.3.jar`, 2,046,785 bytes, 2026-10-23 22:38:18 +02:00, SHA-256 `299c48e56b336b044c80f96757f5932751c8a9b4dfc38fcb7f8be8b612975bdb`.

### Runtime evidence

Development client (`:fabric:runClient`): Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.164.0+26.3, Forge Config API Port 26.3.1, Java 25.0.4.1, Windows 11, AMD Radeon RX 6750 XT, OpenGL backend, Improved Transparency off, no Shader Pack.

- First run, first relevant error: `InvalidAccessorException: No candidates were found matching extractLines(GuiGraphicsExtractor, List, boolean)` for `BuggerScreenRenderLinesInvoker` — the method gained an `int scaledScreenWidth` parameter. Fixed by passing `guiWidth()`, the value the 26.1.2 method used internally.
- Second run, scenario: start, create a new world, take a photograph, export it, leave the world, quit.
- Expected: no mixin/resource/datapack errors; a photograph of the normal world frame without GUI or hand.
- Observed: no errors or warnings from Exposure; all resources loaded; the integrated server loaded 1895 advancements (1866 vanilla + 29 Exposure); the `exposure:adventure/exposure` advancement fired on the capture; the exported photograph (`fabric/run/exposures/New World/Player876_6054.png`, git-ignored) shows a complete world frame with sky and clouds, terrain, trees, horses, and distance fog, no HUD or hand, correct colours and orientation; clean exit.
- Maintainer test in the second-run client: the Bugger hint `[<-] and [->] to switch pages` stayed at the top of the screen during normal play, and the arrow keys had no effect. Cause: the Bugger overlay hook had no visibility check (inherited from the 26.1.2 port); fixed as described under GUI and HUD.
- Third run (after the Bugger fix): the client started on OpenGL and reached `Sound engine started` without mixin or other errors; `DebugScreenOverlay` and `KeyboardHandler` are transformed during game initialization, so both changed mixins applied. The maintainer then played in the existing world for about five minutes: no errors, clean exit (`Stopping!`). Observed by the maintainer: the hint is hidden during normal play and appears only with F3 open, and the arrow keys switch the Bugger pages.
- Fourth run (maintainer, Vulkan selected in the video settings; driver `1.4.315 AMD proprietary driver 26.7.1`; Improved Transparency off): in-game test in the existing world with 20 photographs captured. Observed by the maintainer: everything tested works on Vulkan as on OpenGL. The log shows no Exposure errors; the only error lines are the development environment's missing account authentication (`Failed to fetch user properties`, Realms) and the vanilla `minecraft:end_of_frame` warning (26.3 requests this optional, resource-pack-provided post effect on every frame). Finding: the flash and lens of a dropped camera were misplaced (cosmetic); fixed as described under Rendering.
- Fifth run (camera attachment fix, Vulkan): all resources loaded without errors, so the item definition with `exposure:shared_transform_composite` parses; clean exit. Observed by the maintainer: flash and lens sit correctly on a dropped camera, in the hand, and in an item frame.

Dedicated server (`:fabric:runServer`, EULA accepted by the maintainer for local tests): first start, loaded 1895 advancements, `Done (5.666s)!`, `stop` → `Stopping server`, `Saving worlds`, exit code 0. The only error line was the normal first-start `server.properties` not-found message.

## Unchanged boundaries

No packet ids, directions, field order, `StreamCodec`, menu open-data, `Frame`, `ExposureData`, `ExtraData`, `CameraId`, `SavedData`, Album/Signed Album layout, or photograph format changed. No mixin `require` was lowered, no feature or renderer was disabled, no exception is swallowed. The mod id and Java namespace are unchanged.

## Process notes

- `genSources` runs the Access Widener validation first; the three dead entries were commented out temporarily to generate sources and then replaced by the renamed entries.
- A converter bug truncated `loot_table/blocks/lightroom.json` once; it was restored from git before the corrected converter ran, and the converter now serializes before opening a file for writing.
- A full build while a development client was running failed in `:common:jar` (`FileSystemException`: on Windows the client holds `exposure-common-*.jar` open). Close the client before running the full build gate.

## Remaining work and unverified matrix

1. In-game checks of Album note/signing text input, viewfinder controls (zoom keys including keypad minus, Ctrl shortcuts in the photograph screen, middle-click), camera stand capture, direct capture, filters (colour processor output), photograph frames (incl. glass), lectern album, and loot chest photographs.
2. Improved Transparency (OIT) for capture, held photographs, frames, and glass frames (the maintainer's Vulkan test ran with OIT off).
3. Iris and Sodium for 26.3 with a Shader Pack.
4. Decide whether "Keep Post Effect" should also cover 26.3 player post effects (`/posteffect`).
5. Optional: port upstream Exposure 1.9.19 changes via `UPSTREAM_SYNC.md`.
