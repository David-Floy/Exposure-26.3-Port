# Upstream synchronization ledger

Do not merge a newer `upstream/1.21.1` wholesale into `26.1.2`. Review upstream commits individually and port their business intent onto the 26.1.2 APIs. Preserve local rendering, packet, data, and lifecycle invariants unless the reviewed change explicitly requires a compatible adaptation.

| Upstream commit | Upstream change summary | Adopted? | Local corresponding commit | 26.1.2 adaptation | Reason not adopted | Validation |
|---|---|---|---|---|---|---|
| `9383e0c9237099947a1e6399fcace5772f496c7d` | Fixed baseline for Exposure 1.9.18 on the upstream 1.21.1 branch | Yes | `0dd497ae8000f6c5a9891e9a2a72538e59068703` | Reconstructed as the honest Minecraft 26.1.2 Fabric production baseline; regression tests are in `8e0ed70d18aff6a8326739f55c9fe6ed03d006bd` | — | Common/Fabric compile and 58/58 migration tests |

For every future candidate, fill all columns. “Adopted” means the intent is present in a local commit, not that upstream source was copied mechanically. A rejected or deferred change must have a reason and a validation note showing why current behavior remains safe.
