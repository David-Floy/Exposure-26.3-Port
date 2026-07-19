# Prerelease checklist

## Identity and metadata

- [ ] Port version and display name clearly identify an unofficial prerelease.
- [ ] Minecraft, Java, Fabric Loader/API, and Forge Config API Port versions are correct.
- [ ] README, known issues, compatibility matrix, and release notes agree.
- [ ] Fork source and issue URLs are used; official CurseForge/Modrinth project IDs and automated publishing are disabled.
- [ ] Non-official status, original author, upstream project, and MIT license are prominent.

## Automated gates

- [ ] `:common:compileJava` succeeds.
- [ ] `:fabric:compileJava` succeeds.
- [ ] `:common:compileTestJava` succeeds.
- [ ] At least 58 tests pass with 0 failures, 0 errors, and 0 skipped.
- [ ] `:common:validateAccessWidener` succeeds.
- [ ] `:fabric:validateAccessWidener` succeeds.
- [ ] Complete `build` succeeds from a fresh checkout without reused project build outputs.

## Runtime gates

- [ ] Client smoke test covers startup, world entry, core camera/capture, GUI/Album, frames, export, save/reload, and clean exit.
- [ ] Dedicated server reaches `Done`, exercises relevant server paths, and stops normally.
- [ ] Formal test instance repeats the supported dependency matrix, including the documented Shader combination when required.

## Artifact and safety audit

- [ ] Scan JAR contents for local paths, logs, worlds, screenshots, test assets, Shader Packs, credentials, and server/RCON configuration.
- [ ] Record final JAR path, size, modification time, and SHA-256.
- [ ] Confirm no logs, worlds, caches, build directories, test material, screenshots, credentials, or generated JARs are tracked.
- [ ] Prepare release notes with installation requirements, verified features, compatibility limits, data warning, known issues, feedback requirements, upstream attribution, and final SHA-256.
- [ ] Review release notes before creating a separate GitHub Prerelease.
