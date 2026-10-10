# TG Photos source reuse and dependency licensing

Checked 2026-10-10 through GitHub text resources and official release documentation.
License snapshots in `docs/licensing/upstream` are evidence only. Application code/assets from these projects
were not transplanted. Repository-level licensing is not a complete file-specific/transitive legal audit.

| Project | Current source/license checked | Decision / privacy boundary |
|---|---|---|
| [Aves](https://github.com/deckerst/aves/blob/main/LICENSE) | BSD-3-Clause; README confirms Flutter Android gallery/metadata | navigation/metadata ideas only; no Flutter source or backend |
| [Ente](https://github.com/ente/ente/blob/main/LICENSE) | AGPL-3.0; README describes client/server platform | recovery/sync UX research only; never integrate its account/server endpoints |
| [Immich](https://github.com/immich-app/immich/blob/main/LICENSE) | AGPL-3.0; self-hosted media platform | timeline/backup ideas only; no Immich service/client/backend |
| [Fossify Gallery](https://github.com/FossifyOrg/Gallery/blob/main/LICENSE) | GPL-3.0; local gallery/editor feature reference | clean-room behavior research; no source/assets copied |
| [Material Files](https://github.com/zhanghai/MaterialFiles/blob/master/LICENSE) | GPL-3.0; README includes archive/NAS/root features | file interaction research only; do not import NAS/network/root clients |
| [Amaze](https://github.com/TeamAmaze/AmazeFileManager) | current default release/4.0 page identifies GPL-3.0; full license fetch blocked by slash-branch connector parsing | behavior research only; no plugins, backend or source copied |
| [Photok](https://github.com/leonlatsch/Photok/blob/master/LICENSE) | Apache-2.0; README describes on-device encrypted vault | candidate audited local vault patterns later; none copied yet |
| [TDLib](https://github.com/tdlib/td/blob/master/LICENSE_1_0.txt) | Boost-1.0; official JNI/async/encrypted database support | target transport; no binary/dependency yet; native transitive audit and Telegram terms required |
| [Media3](https://github.com/androidx/media/blob/release/LICENSE) | Apache-2.0 | declared player/Transformer libraries, source API boundary only |
| [Coil](https://github.com/coil-kt/coil/blob/main/LICENSE.txt) | Apache-2.0 | future custom encrypted fetcher candidate; not a current dependency |
| [Tink Java](https://github.com/tink-crypto/tink-java/blob/main/LICENSE) | Apache-2.0 | reviewed AEAD candidate; not integrated |
| [libsodium](https://github.com/jedisct1/libsodium/blob/master/LICENSE) | ISC | Argon2id candidate; ABI/JNI/provenance/parameter audit pending |
| [SQLCipher Android](https://github.com/sqlcipher/sqlcipher-android/blob/master/LICENSE) | BSD-3-Clause text; native/dependency terms still require review | candidate full encrypted SQLite backend; not a dependency |
| [VLC Android](https://github.com/videolan/vlc-android/blob/master/COPYING) | repository COPYING is GPL-2.0, not a blanket LGPL permission | no integration; specific LibVLC engine/binding obligations must be audited separately |

Only Gradle wrapper text launchers were copied, from `gradle/gradle` tag v8.13.0 with original copyright/Apache
headers preserved. The official wrapper binary is now present; bootstrap verifies its published SHA-256.
Other app code is original Compose/domain foundation. The uploaded concepts are privately retained references,
not distributable original app artwork. No upstream trademark/logo/photo was shipped.

Declared dependencies: Kotlin/compiler/coroutines/serialization (Apache-2.0), AndroidX Activity/Core/Splash/Compose/Material3/
Room/WorkManager/Media3 (Apache-2.0), KSP (Apache-2.0), JUnit 4 (EPL-1.0), Gradle/AGP (Apache-2.0).
`dependency-inventory.json` records the declared direct inventory only; it is not a resolved SBOM or binary audit.
Transitive licenses, hashes, advisories, manifests, init providers and runtime endpoints remain unverified because
dependencies could not be downloaded/resolved in this environment. Recheck these before reuse/distribution.

Official version references:
- [AGP 8.13 compatibility](https://developer.android.com/build/releases/agp-8-13-0-release-notes)
- [Material3 stable/Expressive split](https://developer.android.com/jetpack/androidx/releases/compose-material3)
- [Room releases](https://developer.android.com/jetpack/androidx/releases/room)
- [WorkManager releases](https://developer.android.com/jetpack/androidx/releases/work)
- [Gradle distribution/wrapper checksums](https://gradle.org/release-checksums/)

Project license for original code is not selected on the owner's behalf. Do not call reference-only copyleft
apps safe to copy into a proprietary product. Privacy review is independent of license compatibility.
# Build-only GitHub Actions

The source-check workflow uses official `actions/checkout` and `actions/setup-java` v4, pinned to
`11d5960a326750d5838078e36cf38b85af677262` and `cf277c60eb25467037889841efdb72551f06f6c3`.
These are MIT-licensed build tools, not application runtime dependencies. Their upstream notices apply
on the CI runner; no action source or JDK binary is bundled into the application.

## Argon2id backend draft

Bouncy Castle `org.bouncycastle:bcprov-jdk18on:1.82` is a proposed JVM/Android Argon2id backend,
verified against upstream tag `r1rv82`, source commit `de42702b6cda2631e8e3ff94f8458198860b328e`.
The MIT license is preserved in `docs/licensing/upstream/bouncycastle.txt` and NOTICE.
Only its low-level Argon2 generator is called; no global provider is installed, no native binary or
network/backend is added. This version is a reproducible source pin, not a claim that current
vulnerability scanning or Android compatibility review is complete. Resolved dependency scanning
and on-device calibration remain mandatory before app integration.

## Native build candidates

The draft Android native workflow pins TDLib 1.8.68 source `c15d3f5a5de6e3ba5839822c451152e5e18bb700`
and OpenSSL 3.5.9 source `45e844fa2a14ec92d146bd8f5778ac130b6625fb` (published 2026-09-29).
OpenSSL Apache-2.0 license is preserved in `docs/licensing/upstream/openssl.txt`; TDLib Boost-1.0
and OpenSSL licenses accompany any workflow binary artifacts. These candidates have not yet passed
native/device/independent vulnerability gates. No obsolete default OpenSSL 1.1.1 is enabled.
Official `actions/upload-artifact` v6 is build-only MIT, pinned at
`b7c566a772e6b6bfb58ed0dc250532a479d7789f`; it stores public-source native binaries/provenance
in the owner private repository artifacts, not private configuration or user files.
