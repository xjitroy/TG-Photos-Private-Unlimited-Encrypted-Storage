# TG Photos continuation — 2026-10-10

## Completed and verified now

- Official Gradle 8.13 wrapper JAR retrieved from the upstream GitHub base64 file tool; SHA-256 verified:
  `81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f`.
- Discovered `jdk.compiler` in the installed Java 21 runtime. `javac` is absent from PATH, but
  `java com.sun.tools.javac.Main` works. `--release 17` is unavailable because `ct.sym` is absent;
  offline checks use Java 17 source/target on the Java 21 provider, not proof of Android/JDK17 compatibility.
- Real bounded JCA AES-256-GCM envelopes, canonical file/version/index/scope authenticated context,
  random root-key wrapping primitives and Kotlin chunk bridge written. **31 offline primitive/key-hierarchy checks pass**.
- Streamed encrypted backup coordinator, opaque transport identifiers, encrypted manifest-last commit,
  checkpoint-content checks and independent remote authentication/hash verification written.
  **13 offline protocol/fault checks pass** with in-memory test transport/checkpoints.
- Source-policy checks still pass. No production keys, plaintext file transport or deletion operation exists.

## Security source compiled in CI, not integrated into the application

- Bouncy Castle 1.82 Argon2id 1.3 backend, bounded KDF parameters and 6 JVM KDF/recovery tests; no weak hash/PBKDF2 fallback. This backend is not app-wired.
- TDLib runtime credential/session request factory, JSON authorization reducer and original JNI/CMake boundary.
  Official header/API/CMake definitions were checked, with intended native source pin
  `tdlib/td@c15d3f5a5de6e3ba5839822c451152e5e18bb700` (1.8.68).
- Request correlation/receiver/gateway drafts compile and six dispatcher tests pass in CI. Native build, Keystore/device validation and live UI wiring remain pending.
- No production CryptoEngine, app-wired KDF, live TDLib gateway, scheduler or authenticated media player is enabled.

## Limits and open gates

Gradle distribution download fails in the sandbox with `SocketException: Operation not permitted`.
Local Android SDK/full JDK17/dependencies/NDK are not provisioned. The private GitHub CI runner
resolved the Android toolchain: Kotlin/Android compilation, Room KSP, model/crypto JVM tests and
Debug/Release lint PASS in run 38065928993 (source commit b8047c80ed4f3be25a14da4d214b6d6422a64db6).
JNI/NDK linking, real device tests and runtime screenshots remain not run. No APK/AAB was created. Newer session source also passes compilation and lint in run 38067274325.

The offline tests verify primitives/protocol properties, not Argon2id, full password recovery,
actual Telegram behavior, persisted resume, 10GB/device performance or audited E2EE.
The coordinator's checkpoint store is an interface; a durable encrypted implementation is mandatory.
A crash after remote upload acknowledgement but before saving a checkpoint can leave orphan/duplicate objects;
prepare persisted ciphertext/operation identity before upload when implementing durable queue semantics.
Progress objects are not consent for deletion. No Android file is deleted by the coordinator.

API registration is privately stored and excluded from Git/archive; its screenshot is not uploaded.
The owner authorizes uploads to the selected private repository. Security drafts stay unmerged on the feature
branch for independent human review. The owner subsequently authorized eventual APK creation/delivery after requirements/design and
production gates pass. Google Play publication remains unapproved.

## Original source references and latest instruction

All 14 original upstream repositories have locally downloaded selected source/README/license snapshots:
43 files including the complete pinned TDLib API schema, Git blob SHA and SHA-256 verified. See UPSTREAM_REFERENCE_SOURCES.md and its manifest.
These are ignored reference material, not copied into runtime code or connected to external app servers.
The latest owner request authorizes producing the APK when the real application is production-ready;
it does not turn incomplete live integrations or an unaudited encryption draft into a completed feature.

## Native integration work started

Correlated TDLib JSON session/gateway drafts now implement a single owned native receiver, ordered
authorization updates, bounded pending requests, timeout cleanup, redacted server failures/FloodWait,
and confirmed native closure. Six JVM dispatcher tests pass in source run 38067274325; total JVM summary: 17 tests, zero failures/errors/skips.
JNI execute/receive calls now share synchronization to protect returned native pointer lifetime.
A native-only workflow builds pinned TDLib/OpenSSL for arm64-v8a and x86_64 with NDK 28.2,
API 26 and ELF 16KB alignment checks. It produces library/license/provenance artifacts, not an APK.
Native linking/device/login tests and app wiring remain pending until actual workflow results pass.

## Private session key and independent vault locking

An Android Keystore binding draft now wraps a random 256-bit TDLib database key with AES-GCM in
noBackupFilesDir using AtomicFile. Missing wrappers/corrupt envelopes fail closed rather than resetting
a session. It compiles in source run 38067943717, but is not app-wired; Android lock/invalidation/reboot/device tests are pending.
The key hierarchy now supports cloud operations while the independent vault root is locked. Vault lock
wipes its retained root and rejects subsequent vault derivations without changing cloud keys. Three
additional offline checks pass. Consumers must wipe already-derived keys and previews separately.

## Authenticated persistent cache and full auth schema drafts

A bounded AuthenticatedDiskCache persists only AES-GCM ciphertext under opaque UUID filenames in a
caller-supplied dedicated app-private directory. Writes fsync ciphertext temporary files, atomically rename,
and fsync the parent; failed/unfinished writes never promote plaintext. Reads require trusted manifest
version/length, reject symlinks, authenticate before returning in-memory bytes and derive separate cloud/vault
cache keys. Startup removes ciphertext-only temporary files. A quota rejects growth rather than silently
removing user content. Ten Linux/JCA fixture checks pass, including fresh service restore, corruption of
all envelope bytes, substitution/replay, vault relock, symlink rejection, quota failure and interrupted writes.
This is not yet connected to EncryptedCache, Android viewers or Room/WorkManager; filesystem/device,
eviction policy and lifecycle tests remain mandatory. It is not a durable backup outbox/checkpoint store.

Pinned TDLib schema is used for nested email OTP authentication and resend-code reason. Email address/code
requests wipe mutable caller input, and the reducer retains masked delivery metadata. Four additional JUnit
schema/rejection tests pass in source run 38068420902; its total JVM summary is 21 passing tests. Neither email authentication nor other real login UI is app-wired.

## Authenticated media and emulator QA source

AuthenticatedRangeReader authenticates a complete bounded chunk before copying any requested range to a
consumer; seek mapping uses trusted chunk-length offsets, one in-memory plaintext chunk is retained, and
replacement/close wipes the owned buffer/key. Nine offline JCA fixture checks pass for sequential/seek reads,
replay/reordering/corruption, immutable destination on auth failure, truncation, invalid ranges, cancellation,
close and empty files. All active readers must be closed on vault/account lock; automatic lifecycle wiring
is pending. CiphertextSource is a bounded/deadline/interruption transport contract, not a live downloader.

An original EncryptedMediaDataSource draft follows the exact Media3 1.8.0 API, tag source commit
b7bbc6e2bc3e45ff3ed99884c114c50f03bba5c9. It accepts only opaque tgphotos media URIs and forwards fully
authenticated ranges; actual ExoPlayer setup, buffering, tracks, playback resume/editing and device tests
are pending. Android/Java compilation is included in the next source CI check.

The owner-authorized Android preview QA workflow may build a temporary debug APK solely to install into a
fresh Android 35 emulator. It checks preview navigation, Settings, theme persistence and vault secure-window
flags using UI-tree-derived coordinates, and retains account-free screenshots/test evidence only. No APK
artifact is uploaded/delivered; no API registration, account, private media, live feature or release signing is
present. Actual emulator results are pending. This is not production/live integration acceptance.

## Native provenance and dependency review preparation

Native validation now additionally checks ELF64/target ABI, required exported TDLib C/JNI symbols,
16KB-aligned/congruent LOAD segments and allowed Android/system DSO dependencies. Native JSON copies
are bounded before allocating Java/C++ response buffers. These source changes await native compilation.
SQLCipher's embedded Zetetic BSD notice and host-only GPL TL-parser license are retained distinctly.

A non-packaging Gradle inventory task records resolved external JVM/Android coordinates, artifact SHA-256
and sizes for app debug/release plus draft security/backup/media/cache module configurations. It excludes
project-built artifacts and private configuration; the source-check workflow uploads only this review JSON
as a private artifact. Inventory generation awaits CI. It is not a complete native SBOM or an automatic
license, vulnerability or network privacy audit; all such reviews remain explicit production gates.

Preview QA first run 38068966719 successfully assembled the temporary development APK (4m 8s), then
failed before installation because platform-tools/adb was not on the CI PATH. No UI acceptance is claimed.
The workflow now explicitly provisions platform-tools, adds its directory to the next steps' PATH, bounds
wait-for-device and preserves account-free emulator startup logs. The debug APK was removed in cleanup;
no APK artifact was uploaded or delivered. The rerun will verify actual installation/navigation.

## Observed binary/manifest evidence and privacy fixes

Source run 38069536270 passed the dependency-inventory task: seven configurations and 142 external
packages were recorded; 138 have direct cached POM license declarations. Four Guava/failureaccess entries
require inherited-license/source review. This is inventory evidence, not clearance or a vulnerability scan.
Downloaded evidence ZIP hashes match GitHub artifact digests.

Native run 38067274352 completed x86_64 compilation/16KB checks. Downloaded artifacts were independently
inspected with GNU readelf locally: ELF64 x86_64 ABI, unversioned Android SONAMEs, allowed dependencies,
required public C/JNI symbols and 16KB-aligned/congruent LOAD segments pass. The original producer
provenance is preserved separately from local ELF review. Native binary names are already correct on
Android; no upstream SONAME patch is needed or applied. Arm64 result and new JNI bounds build are pending.

The actual QA APK manifest exposed Compose tooling's exported PreviewActivity. A debug manifest overlay
removes it. Main manifest removes automatic EmojiCompat font-provider initialization while retaining the
non-exported reviewed local lifecycle/profile startup. The audit keeps its strict export/initializer checks;
next QA run must confirm the merged manifest and then run the emulator. Earlier QA never installed the app.

Media reader close now invalidates/wipes owned state without waiting for a blocked remote fetch. A ninth
fault test confirms a late fetch cannot publish plaintext after closure; transport operation copies are wiped
when the in-flight call finishes. UI/player-held bytes and decoder state still need explicit lifecycle clearing.
Native client creation now requires positive logging-suppression acknowledgements; a new rejection test
awaits CI. Null/unknown acknowledgements can no longer start a client with unconfirmed logging policy.

## Both native ABIs compiled; reproducible JNI rebuild preparation

Baseline run 38067274352 completed successfully for arm64-v8a and x86_64. Both downloaded ZIP digests
match GitHub metadata; both libraries pass local GNU-readelf ABI, SONAME, dependency, C/JNI export and
16KB LOAD checks. No native library is yet packaged into the application or tested on-device.

A pinned native-baseline manifest binds source commits, original producer run/source, NDK/API and exact
TDLib binary hashes. The native workflow may reuse only those verified TDLib/OpenSSL binaries when
pins match, then rebuild the current original JNI boundary against the official pinned header. Missing/
expired artifacts or changed pins cause a full public-source rebuild; integrity mismatches fail closed.
The output retains original producer provenance, fresh JNI provenance and bundled SQLCipher/TDLib/OpenSSL
notices. This avoids recompiling unchanged public upstream code for every boundary change without claiming
that new code has already compiled. The new rebuild route and native JSON bounds await CI.

Preview QA run 38070420517 passed the actual merged APK manifest restrictions (only launcher plus
permission-protected profile receiver exported; no network/media permissions; Emoji initializer absent),
but emulator startup failed because the SDK avdmanager/emulator did not share an AVD search directory.
The workflow now uses an explicit workspace ANDROID_AVD_HOME and known AVD path/INI without repurposing HOME.
Source lint also reported the manifest's runtime-only Startup class missing from compile dependencies;
Startup 1.1.1 was already resolved in both app runtime variants and is now an explicit implementation
with the same version, rather than suppressing lint. Both fixes require the next actual CI results.

## Current verified source and native rebuild (17:35 UTC)

Source 13eccdd6a1e62764db18c02b32096604e82d4130 passes run 38071501320: compilation, Room KSP,
22 JVM tests with zero failures/errors/skips, Debug/Release lint and resolved dependency evidence.
The strict logging-ack test passes. Native run 38071501341 passes both ABI rebuilds: verified unchanged
TDLib/OpenSSL baseline plus freshly compiled bounded JNI; ELF checks pass. Native Android loading is
still untested. This evidence applies to the named source, not subsequent changes automatically.

Preview QA run 38071501334 successfully booted/installed on Android 35 and passed the actual merged
manifest check, but its first UI assertion failed: Explore was below the scroll viewport. XML evidence
confirms the scrollable login page. QA now scrolls using UI-tree bounds and checks disabled state through
Compose ancestors; it does not remove the disabled-auth requirement. Screenshots are saved before
assertions for failure diagnosis, except protected Vault images. Full UI acceptance remains pending.

LocalSessionKeyStore now serializes creation across instances in this single-process app. Four real
Android instrumentation checks use random isolated fixture directories/aliases: fresh-instance recovery,
concurrent creation agreement, corrupted envelope rejection and missing wrapper rejection without reset.
No owner account/key/storage is used. Their compile/runtime results are pending. Hardware backing,
locked-device/reboot/invalidation/API-matrix/cross-device tests and live session wiring remain pending.
The runtime workflow creates/deletes test APKs and uploads only account-free QA evidence.
