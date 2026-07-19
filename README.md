# Exposure 1.9.18 for Minecraft 26.1.2

> **Unofficial Fabric Port — Beta 1**

This repository contains an unofficial Minecraft 26.1.2 Fabric port of [Exposure](https://github.com/mortuusars/Exposure), the camera mod created by [Mortuusars](https://github.com/mortuusars). It is based on Exposure 1.9.18 at fixed upstream commit [`9383e0c9237099947a1e6399fcace5772f496c7d`](https://github.com/mortuusars/Exposure/commit/9383e0c9237099947a1e6399fcace5772f496c7d).

Filmorae maintains this port but is not the original author. Problems specific to this repository must **not** be reported to the official Exposure issue tracker; use this fork's [issue tracker](https://github.com/Filmorae/Exposure-26.1.2-Port/issues).

## Status

- Beta/prerelease; Fabric only.
- Port version: `1.9.18-port.1+mc26.1.2`.
- The mod id remains `exposure` for dependent-mod recognition.
- Compatibility with old Exposure worlds, albums, photographs, and repository data is not guaranteed. Back up a world before testing.

The controlled migration verified the core camera and viewfinder, hand-held and Camera Stand Capture, direct and background Capture, photograph storage/rendering/export, film development and printing, Album/Signed Album/Lectern flows, photograph frames, recipes, commands, integrated-server behavior, and dedicated-server startup/shutdown. See [COMPATIBILITY.md](COMPATIBILITY.md) for the exact matrix and [KNOWN_ISSUES.md](KNOWN_ISSUES.md) for support limits.

## Requirements

- Minecraft 26.1.2
- Java 25
- Fabric Loader 0.19.3 or compatible newer 0.19.x version
- [Fabric API](https://modrinth.com/mod/fabric-api) 0.150.0+26.1.2 or newer compatible 26.1.2 build
- [Forge Config API Port](https://modrinth.com/mod/forge-config-api-port) 26.1.4

Optional integrations in the source tree are not required runtime dependencies and are not all enabled or verified for this prerelease.

## Installation

1. Install Minecraft 26.1.2 with Fabric Loader and Java 25.
2. Install the required Fabric API and Forge Config API Port versions above.
3. Place the port JAR in the instance's `mods` directory.
4. Start with a backed-up test world and confirm the loaded version identifies this unofficial port.

Do not download this port from the official Exposure CurseForge or Modrinth project pages; those pages belong to the upstream project.

## Shader and rendering boundary

Capture was controlled-tested without shaders and with Iris 1.11.2, Sodium 0.9.1, and BSL 8.4.01.2. That exact result does not establish compatibility with every Shader Pack, GPU, player-model layer, or skin-layer mod. The port uses Minecraft's normal frame lifecycle and does not hard-code an Iris or Shader-Pack-private framebuffer.

## Reporting a bug

Use the repository bug template and include the Minecraft/port versions, Fabric Loader/API versions, complete mod list, Shader/Iris/Sodium versions, exact reproduction steps, `latest.log`, `debug.log`, any crash report, and a screenshot or exported photograph. State whether the problem reproduces in a minimal environment.

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

Exposure is licensed under the [MIT License](LICENSE.md). Copyright and credit remain with Mortuusars and the upstream contributors. This repository preserves the original license and identifies Filmorae only as the maintainer of the unofficial port.
