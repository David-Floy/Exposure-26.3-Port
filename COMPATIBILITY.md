# Verified compatibility matrix

This table records controlled verification only. An entry does not imply support for different versions, GPUs, mod combinations, or Shader Packs.

| Component or environment | Verified version/state | Scope |
|---|---|---|
| Minecraft | 26.1.2 | Required target |
| Java | 25 | Compile, tests, client, and server gates |
| Fabric Loader | 0.19.3 | Client and dedicated server |
| Fabric API | 0.150.0+26.1.2 | Build and controlled runtime baseline |
| Fabric API | 0.151.0+26.1.2 | Controlled runtime compatibility |
| Forge Config API Port | 26.1.4 | Required runtime dependency |
| No Shader Pack | Verified | Direct/background Capture and core rendering |
| Iris | 1.11.2 | Controlled Shader Capture matrix only |
| Sodium | 0.9.1 | Controlled Shader Capture matrix only |
| BSL | 8.4.01.2 | Controlled Iris/Sodium Capture matrix only |
| Integrated server | Verified | World entry, persistence, menus, commands, capture/export scenarios |
| Dedicated server | Verified | Startup to `Done`, server-safe paths, persistence/export scenarios, normal stop |

Everything not listed is unverified unless a later dated entry or community report documents the exact environment and reproduction. Community reports should be labeled as such and should not replace controlled verification.
