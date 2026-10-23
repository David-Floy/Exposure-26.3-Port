# Known issues and support boundary

- This is an unofficial port maintained independently from the original Exposure project.
- Compatibility with worlds, albums, photographs, exposure repositories, or other data from older Exposure versions is not guaranteed. Back up a world before testing the port.
- Third-party Shader Packs, player model layers, skin/outer-layer mods, and custom rendering pipelines may require targeted compatibility validation. A result verified with one named combination must not be generalized to all combinations.
- When a glass photograph frame is occupied, the disappearance of the front pane/highlight is part of the fixed upstream design: the photograph replaces the front pane. If this port later keeps a reflection or pane, document it as an intentional visual enhancement that diverges from upstream, not as a current port blocker.

## Minecraft 26.3 port status

- Capture and readback are verified on OpenGL and, by the maintainer's in-game test, on Vulkan (AMD proprietary driver). Improved Transparency (order-independent transparency) is untested; photographs and glass frames use vanilla `entityTranslucent`, which 26.3 routes through its OIT pipelines when the option is enabled.
- Iris and Sodium for 26.3 have not been tested with this port yet.
- The "Keep Post Effect" capture option controls only the vanilla spectator post effect (creeper/spider/enderman), as in 26.1.2. Post effects that 26.3 applies to the player through `/posteffect` (`LocalPlayer#getActivePostEffects`) are not disabled for photographs.
- During a background capture, the frame after arming renders the world off-screen into the capture target, so that single presented frame repeats the previous frame's image with the current GUI drawn over it. This follows from the architecture (no extra `renderLevel`) and was not observed as a visible artifact in testing.
- The Album note and signing text boxes were adapted to SDL text input (focus is reported to the text input manager); typing in them has not been re-tested in-game yet.
- Optional integrations (JEI, Jade, Create, Easy Anvils) remain excluded from the build, as in the 26.1.2 port.

When reporting a problem, include:

- complete mod list;
- Minecraft and port versions;
- Fabric Loader and Fabric API versions;
- graphics API (OpenGL or Vulkan) and whether Improved Transparency is enabled;
- Iris, Sodium, and Shader Pack versions when applicable;
- exact reproduction steps and whether a minimal environment reproduces it;
- complete `latest.log` and `debug.log`;
- crash report, if one exists;
- screenshots or the exported photograph that demonstrates the result.

Do not report issues specific to this port to the official Exposure issue tracker.
