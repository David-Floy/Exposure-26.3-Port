## What changed

Describe the change and its user/developer impact.

## Root cause

Explain the first root failure and why this fix belongs in the Minecraft 26.1.2 Fabric port.

## Validation

- [ ] Targeted reproduction/test passes.
- [ ] `:common:compileJava` and `:fabric:compileJava` pass.
- [ ] At least 58 tests pass with no failures, errors, or skipped tests.
- [ ] Both Access Widener validations pass.
- [ ] Complete `build` passes.
- [ ] Client runtime evidence is attached when rendering/client behavior changed.
- [ ] Dedicated server was validated when server-side behavior changed.

List exact versions and commands/results here.

## Compatibility and data boundary

- [ ] This PR does not change packet schema, codecs, menu open-data, persistent data, Album/photo formats, or Capture architecture.
- [ ] If it does, prior discussion and a compatibility/migration plan are linked below.

Describe remaining unverified environments and confirm no logs, worlds, screenshots, test assets, caches, credentials, or JARs are included.
