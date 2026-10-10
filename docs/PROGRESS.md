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
schema/rejection tests await CI. Neither email authentication nor other real login UI is app-wired.

## Authenticated media and emulator QA source

AuthenticatedRangeReader authenticates a complete bounded chunk before copying any requested range to a
consumer; seek mapping uses trusted chunk-length offsets, one in-memory plaintext chunk is retained, and
replacement/close wipes the owned buffer/key. Eight offline JCA fixture checks pass for sequential/seek reads,
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
