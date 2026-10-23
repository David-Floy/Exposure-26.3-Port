# Exposure 1.9.18 for Minecraft 26.3

> **Unofficial Fabric Port — development build**

This repository contains an unofficial Minecraft 26.3 Fabric port of [Exposure](https://github.com/mortuusars/Exposure), the camera mod created by [Mortuusars](https://github.com/mortuusars). It is based on Exposure 1.9.18 at fixed upstream commit [`9383e0c9237099947a1e6399fcace5772f496c7d`](https://github.com/mortuusars/Exposure/commit/9383e0c9237099947a1e6399fcace5772f496c7d) and continues the unofficial [Minecraft 26.1.2 Fabric port by Filmorae](https://github.com/Filmorae/Exposure-26.1.2-Port).

David-Floy maintains this 26.3 port; neither David-Floy nor Filmorae is the original author. Problems specific to this repository must **not** be reported to the official Exposure issue tracker; use this fork's [issue tracker](https://github.com/David-Floy/Exposure-26.3-Port/issues).

The port from Minecraft 26.1.2 to 26.3 was carried out with Claude Opus 5.5 (Anthropic) as an AI coding assistant.

## Status

- Fabric only.
- Port version: `1.9.18-port.1+mc26.3` (not released yet).
- The mod id remains `exposure` for dependent-mod recognition.
- Compatibility with old Exposure worlds, albums, photographs, and repository data is not guaranteed. Back up a world before testing.

The 26.3 build passes the automated gates (compilation, 70 migration tests, both Access Widener validations, full build). At runtime it has so far been verified with a development client on OpenGL (startup, new world, background Capture with an exported photograph, clean exit), in the maintainer's in-game test on Vulkan, and with a dedicated server (startup to `Done`, datapack loading, normal stop). Improved Transparency and Shader Packs are not verified yet. See [COMPATIBILITY.md](COMPATIBILITY.md) for the exact matrix and [KNOWN_ISSUES.md](KNOWN_ISSUES.md) for support limits.

## Requirements

- Minecraft 26.3
- Java 25
- Fabric Loader 0.19.5 or compatible newer version
- [Fabric API](https://modrinth.com/mod/fabric-api) 0.164.0+26.3 or newer compatible 26.3 build
- [Forge Config API Port](https://modrinth.com/mod/forge-config-api-port) 26.3.1

Optional integrations in the source tree are not required runtime dependencies and are not enabled in this build.

## Installation

1. Install Minecraft 26.3 with Fabric Loader and Java 25.
2. Install the required Fabric API and Forge Config API Port versions above.
3. Place the port JAR in the instance's `mods` directory.
4. Start with a backed-up test world and confirm the loaded version identifies this unofficial port.

Do not download this port from the official Exposure CurseForge or Modrinth project pages; those pages belong to the upstream project.

## Shader and rendering boundary

The port uses Minecraft's normal frame lifecycle and does not hard-code an Iris or Shader-Pack-private framebuffer. Capture has been verified without shaders on OpenGL and Vulkan; results with Improved Transparency, Iris/Sodium, other GPUs, player-model layers, or skin-layer mods are not established yet.

## Reporting a bug

Use the repository bug template and include the Minecraft/port versions, Fabric Loader/API versions, graphics API (OpenGL or Vulkan), complete mod list, Shader/Iris/Sodium versions, exact reproduction steps, `latest.log`, `debug.log`, any crash report, and a screenshot or exported photograph. State whether the problem reproduces in a minimal environment.

## Building

Use Java 25 and the checked-in Gradle Wrapper:

```text
./gradlew :common:compileJava :fabric:compileJava :common:compileTestJava
./gradlew :common:test --rerun-tasks
./gradlew :common:validateAccessWidener :fabric:validateAccessWidener
./gradlew --rerun-tasks build
```

Read [AGENTS.md](AGENTS.md), [PORTING.md](PORTING.md), and [CONTRIBUTING.md](CONTRIBUTING.md) before changing the port. Historical migration evidence is indexed under [docs/migration](docs/migration/README.md).

## License and attribution

Exposure is licensed under the [MIT License](LICENSE.md). Copyright and credit remain with Mortuusars and the upstream contributors. This repository preserves the original license; Filmorae is credited as the maintainer of the 26.1.2 port and David-Floy as the maintainer of the 26.3 port, not as original authors.
