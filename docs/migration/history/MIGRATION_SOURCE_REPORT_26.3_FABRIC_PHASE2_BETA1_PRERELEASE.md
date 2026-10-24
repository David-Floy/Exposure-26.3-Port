# Minecraft 26.3 Fabric port — Phase 2 report: Beta 1 prerelease preparation

## Conclusion

The 26.3 port at `21e67d79` was prepared as the Beta 1 prerelease `1.9.18-port.1+mc26.3`, following `RELEASE_CHECKLIST.md`. The metadata identify the beta, the JAR contains the MIT license text, and release notes were written. All automated gates pass, and the maintainer's formal test of the release JAR in a launcher instance passed. The release commit is tagged `26.3-Fabric`; the maintainer creates the GitHub Prerelease.

## Changes

- `gradle.properties`: `mod_release_type=beta`; the description ends with "Beta 1." (shown in the mod list), as for the first 26.1.2 port release.
- `fabric/build.gradle`: the JAR contains `LICENSE_exposure.md`. The MIT license requires its notice in every copy; earlier port JARs, including the 26.1.2 releases, shipped without it.
- Release notes: `docs/releases/1.9.18-port.1-mc26.3-beta.1.md`. `README.md` (status, download location), `PORTING.md` (current release artifact), and `CHANGELOG.md` were updated.

## Checklist status

| `RELEASE_CHECKLIST.md` item | Status |
|---|---|
| Identity and metadata | Done: name, version, beta description, fork URLs, no CurseForge/Modrinth publishing, attribution and MIT license |
| Automated gates | Done: compilation, 70 tests (23 suites, 0 failures, 0 errors, 0 skipped), both Access Widener validations, complete `--rerun-tasks build`. The release commit is built again from a fresh clone with LF line endings before tagging; `26.3-Fabric` is only set when that build reproduces the checksum below |
| Client smoke test | Development client on OpenGL and Vulkan (Phase 1 runtime evidence) |
| Dedicated server | Development server to `Done` and normal stop (Phase 1) |
| Formal test instance | Done: maintainer test of the release JAR with the required dependency versions (mod list, new world, capture, development and printing, Album notes and signing including text input, frames, export, save/reload, clean exit); also with Iris 1.11.7+26.3-fabric, Sodium mc26.3-0.9.2-fabric, and Lithium mc26.3-0.26.2-fabric, no problems found (Shader Pack and graphics API not recorded) |
| Artifact audit | Done: 1240 entries, no logs, worlds, screenshots, local paths, or credentials |
| Release notes | Written; the maintainer reviews them when creating the GitHub Prerelease |

## Artifact

`exposure-fabric-26.3-1.9.18-port.1+mc26.3.jar` from a fresh clone of the release commit with LF line endings (`core.autocrlf=false`, the content the repository stores and CI checks out): 2,046,776 bytes, 2026-10-24 12:45:38 +02:00, SHA-256 `bff1732d6c4caa730084aaac0a3829a14a34f5f3100a2147352881c2d516d147`. The JAR uses fixed entry timestamps and contains no documentation besides the license, so documentation commits do not change it.

Line endings are the only source of checksum differences. A fresh Windows clone with `core.autocrlf=true` checks text files out as CRLF and produced SHA-256 `20056557c8df905c1b4cfa043efd90137b16653ee89a34f372db53a36e4b654e`; the local working tree, with CRLF originals and LF edits, produced `c4bd72d8e746b17ea6787293bf1a0fcd5af2c87eb3c493d2a577fa810bc264f9`, the checksum first recorded in the release commit. All 211 differing entries are identical after removing carriage returns. Build release JARs from an LF checkout.

A first fresh-clone attempt under the session scratch directory failed on Windows' 260-character path limit; because the commands were chained, the build then ran in the main working tree and only compared that JAR with itself. The check was repeated with a short path and a guard that stops unless the build runs inside the clone.

## Remaining steps

1. Create the GitHub Prerelease from tag `26.3-Fabric` with these release notes and the JAR.
2. Verify Improved Transparency (OIT) for capture, held photographs, frames, and glass frames.
3. Record a named Shader Pack and the graphics API for the Iris matrix.
