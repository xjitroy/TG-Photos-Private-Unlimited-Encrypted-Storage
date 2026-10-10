# Downloaded source references from the original ZIP

On the owner's request, 42 files were downloaded into ignored `.toolchain/upstream/` from all **14**
repositories listed in the original master plan. Each repository has a pinned README, root license
and a relevant source file. All downloaded bytes were checked against GitHub's Git blob SHA and
recorded SHA-256. These are selected source snapshots, not complete repository clones.

`upstream-source-manifest.json` records exact commits, paths, sizes and checksums. Restore them on a
machine with permitted HTTPS using `python3 scripts/download_upstream_references.py`. Validate current
local copies offline with `python3 scripts/download_upstream_references.py --verify-only`.
The snapshots are reference-only and outside Gradle source sets; no upstream application is executed,
no backend client is installed, and no personal file, API registration or key is sent to these projects.

| Repository | Downloaded source purpose | License / reuse boundary |
|---|---|---|
| deckerst/aves | dynamic album modelling | BSD-3-Clause; assess components before adaptation |
| ente/ente | backup item state modelling | AGPL-3.0; research only |
| immich-app/immich | backup repository behaviour | AGPL-3.0; research only |
| FossifyOrg/Gallery | folder deletion confirmation | GPL-3.0; research only |
| zhanghai/MaterialFiles | document-provider copy/move behaviour | GPL-3.0; research only |
| TeamAmaze/AmazeFileManager | scoped file operations | GPL-3.0; research only |
| leonlatsch/Photok | vault service/session behaviour | Apache-2.0; audited adaptation only |
| tdlib/td | official JSON native C API header | Boost-1.0; intended supported Telegram dependency |
| androidx/media | custom DataSource contract | Apache-2.0; use pinned published Media3 APIs |
| coil-kt/coil | Fetcher contract | Apache-2.0; secure custom fetching assessment |
| tink-crypto/tink-java | HKDF implementation reference | Apache-2.0; compare protocol/provider behaviour |
| jedisct1/libsodium | Argon2id API/header and limits | ISC; native KDF candidate, not enabled |
| sqlcipher/sqlcipher-android | encrypted database API | BSD-style/MIT parts; inspect full native distribution obligations |
| videolan/vlc-android | playback service behaviour | GPL-2.0 Android app; research only, distinct from LGPL LibVLC |

No GPL/AGPL source was transplanted into the application. Root license labels alone are not a license
audit of every dependency/asset. Current branch HEAD snapshots are for research; runtime versions remain
those explicitly pinned in the dependency inventory. In particular, the downloaded latest Media3 source
does not prove compatibility with the declared 1.8.0 artifact. Cross-check the exact release API before use.

Full application clones do not provide a ready-made native encrypted Telegram product. The source snapshots
support specific API/UX review while keeping app code, credentials and reference material separate.
