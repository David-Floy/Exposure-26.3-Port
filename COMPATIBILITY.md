# Verified compatibility matrix

This table records controlled verification only. An entry does not imply support for different versions, GPUs, mod combinations, or Shader Packs.

## Minecraft 26.3 (this branch)

| Component or environment | Verified version/state | Scope |
|---|---|---|
| Minecraft | 26.3 | Required target |
| Java | 25 (Temurin 25.0.4.1) | Compile, tests, client, and server gates |
| Fabric Loader | 0.19.5 | Development client and dedicated server |
| Fabric API | 0.164.0+26.3 | Build and development runtime baseline |
| Forge Config API Port | 26.3.1 | Required runtime dependency |
| Graphics API | OpenGL, Vulkan | Development client on an AMD Radeon RX 6750 XT (AMD proprietary driver 26.7.1) |
| Vulkan | Partially verified | Maintainer in-game test with Vulkan 1.4.315: 20 photographs captured, no Exposure errors; the only finding, the camera attachment placement, was fixed and re-tested |
| Improved Transparency (OIT) | Not verified | Tested with the option off only |
| No Shader Pack | Partially verified | Startup, new world, background Capture with an exported photograph (full world frame incl. sky, no HUD/hand), clean exit |
| Iris / Sodium / Lithium | Partially verified | Maintainer test with the Beta 1 JAR in a launcher instance: Iris 1.11.7+26.3-fabric, Sodium mc26.3-0.9.2-fabric, Lithium mc26.3-0.26.2-fabric, no problems found; Shader Pack and graphics API were not recorded |
| Integrated server | Partially verified | World creation, datapack load (all 29 Exposure advancements), Capture advancement trigger, save and stop |
| Dedicated server | Verified | Startup to `Done`, datapack load (1866 vanilla + 29 Exposure advancements), normal `stop` |
| Release JAR in a launcher instance | Verified | Maintainer test of Beta 1 with the required dependency versions: mod list, new world, capture, development and printing, Album notes and signing (text input), frames, export, save/reload, clean exit |

Everything not listed is unverified unless a later dated entry or community report documents the exact environment and reproduction. Community reports should be labeled as such and should not replace controlled verification.

## Minecraft 26.1.2 (previous port, `26.1.2` branch)

The 26.1.2 matrix (Fabric Loader 0.19.3, Fabric API 0.150.0/0.151.0+26.1.2, Forge Config API Port 26.1.4, Iris 1.11.2 + Sodium 0.9.1 + BSL 8.4.01.2) applies to the 26.1.2 builds only and is kept in that branch.
