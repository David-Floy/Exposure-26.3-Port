# Contributing

All pull requests for this port must be based on and target the `26.1.2` branch. Do not send port-specific crashes, compatibility requests, or data issues to the official Exposure issue tracker.

A fix should include:

- a minimal reproduction and exact environment;
- the first root failure rather than only downstream symptoms;
- an explanation of why the change belongs in the 26.1.2 port;
- targeted tests and the final build/Access Widener results;
- the remaining client, server, Shader, mod, or data compatibility boundary.

Discuss protocol or persistent-data changes before implementation. This includes packets, codecs, field order, menu open-data, `Frame`, `ExposureData`, `SavedData`, Album data, photograph files, and repository layout.

Contributors must retain the MIT license and attribution to Mortuusars and other upstream contributors. Filmorae maintains this unofficial port and must not be represented as the original author. Keep the mod id and Java namespace compatible with Exposure.

Follow `AGENTS.md` and `PORTING.md`. Never include build outputs, Gradle caches, logs, worlds, screenshots, test skins, Shader Packs, credentials, or JARs in a commit.
