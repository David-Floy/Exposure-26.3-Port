# Repository instructions

This branch is the maintained, unofficial Minecraft 26.1.2 Fabric port of Exposure. These rules apply to human contributors, Codex, and other automation working in this repository.

## Start here

Before changing code:

1. Read this file, `PORTING.md`, and the newest report in `docs/migration/history`.
2. Read older migration reports only when a named subsystem or regression requires that history. Intermediate conclusions may have been superseded.
3. Confirm the task is within the Fabric 26.1.2 support boundary. Do not expand scope without evidence.

## Non-negotiable compatibility boundaries

- Keep the mod id `exposure` and the existing Java package namespace.
- Do not lower a Mixin `require` value to hide an injection failure.
- Do not delete a feature, disable a renderer, or swallow an exception to make a build or smoke test pass.
- Do not change packet identifiers, direction, field order, `StreamCodec`, menu open-data, `Frame`, `ExposureData`, `SavedData`, or photograph formats without an explicit design discussion and compatibility plan.
- Do not regress the Capture architecture documented in `PORTING.md`. In particular, do not add synchronous `glReadPixels`, an extra `renderLevel` call, desktop screenshot cropping, or a private Iris/Shader Pack framebuffer dependency.
- Do not hard-code Iris or any one Shader Pack's private framebuffer or render pass.
- Do not commit local paths, credentials, logs, worlds, screenshots, test skins, Shader Packs, Gradle caches, build outputs, JARs, or release artifacts.

## Validation

- Run the narrowest relevant test first, then the affected module compilation.
- Before handoff, run all migration tests, both Access Widener validations, and the complete build. The suite must contain at least 58 tests with no failures, errors, or skipped tests.
- Client rendering changes require accurate runtime evidence from the real game path. Record versions, scenario, expected result, observed result, and the first relevant error.
- Server-side changes require a dedicated-server smoke test that reaches `Done` and stops normally; an EULA-only launch is not a server validation.
- Never reduce, skip, or disable tests to pass a gate.

## Evidence and handoff

- Keep generated logs, JARs, screenshots, worlds, and diagnostic inventories outside the repository or under an ignored evidence directory.
- Update the current maintenance documents and handoff status at the end of each work round.
- Record upstream-derived fixes in `UPSTREAM_SYNC.md` with the upstream commit, local commit, adaptation, and validation.
- Document a compatibility fix in `PORTING.md`, `KNOWN_ISSUES.md`, or `COMPATIBILITY.md` as appropriate; do not rewrite historical reports to make old conclusions look current.
- Do not broaden a task based on speculation. Preserve the first failure and collect evidence before changing another subsystem.
