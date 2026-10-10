# TG Photos continuation — 2026-10-10

## Completed and verified now

- Official Gradle 8.13 wrapper JAR retrieved from the upstream GitHub base64 file tool; SHA-256 verified:
  `81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f`.
- Discovered `jdk.compiler` in the installed Java 21 runtime. `javac` is absent from PATH, but
  `java com.sun.tools.javac.Main` works. `--release 17` is unavailable because `ct.sym` is absent;
  offline checks use Java 17 source/target on the Java 21 provider, not proof of Android/JDK17 compatibility.
- Real bounded JCA AES-256-GCM envelopes, canonical file/version/index/scope authenticated context,
  random root-key wrapping primitives and Kotlin chunk bridge written. **28 offline primitive/key-hierarchy checks pass**.
- Streamed encrypted backup coordinator, opaque transport identifiers, encrypted manifest-last commit,
  checkpoint-content checks and independent remote authentication/hash verification written.
  **13 offline protocol/fault checks pass** with in-memory test transport/checkpoints.
- Source-policy checks still pass. No production keys, plaintext file transport or deletion operation exists.

## Work written but not integrated/compiled

- Bouncy Castle 1.82 Argon2id 1.3 backend, bounded KDF parameters and 6 JVM KDF/recovery tests; no weak hash/PBKDF2 fallback. This backend is not app-wired.
- TDLib runtime credential/session request factory, JSON authorization reducer and original JNI/CMake boundary.
  Official header/API/CMake definitions were checked, with intended native source pin
  `tdlib/td@c15d3f5a5de6e3ba5839822c451152e5e18bb700` (1.8.68).
- Request correlation/receiver/session/Keystore/UI wiring and native build are still pending.
- No production CryptoEngine, app-wired KDF, live TDLib gateway, scheduler or authenticated media player is enabled.

## Limits and open gates

Gradle distribution download fails in the sandbox with `SocketException: Operation not permitted`.
Local Android SDK/full JDK17/dependencies/NDK are not provisioned. The private GitHub CI runner
resolved the Android toolchain: Kotlin/Android compilation, Room KSP, model/crypto JVM tests and
Debug/Release lint PASS in run 38065928993 (source commit b8047c80ed4f3be25a14da4d214b6d6422a64db6).
JNI/NDK linking, real device tests and runtime screenshots remain not run. No APK/AAB was created.

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
42 files, Git blob SHA and SHA-256 verified. See UPSTREAM_REFERENCE_SOURCES.md and its manifest.
These are ignored reference material, not copied into runtime code or connected to external app servers.
The latest owner request authorizes producing the APK when the real application is production-ready;
it does not turn incomplete live integrations or an unaudited encryption draft into a completed feature.

## Native integration work started

Correlated TDLib JSON session/gateway drafts now implement a single owned native receiver, ordered
authorization updates, bounded pending requests, timeout cleanup, redacted server failures/FloodWait,
and confirmed native closure. Six JVM dispatcher tests are added and awaiting the new CI result.
JNI execute/receive calls now share synchronization to protect returned native pointer lifetime.
A native-only workflow builds pinned TDLib/OpenSSL for arm64-v8a and x86_64 with NDK 28.2,
API 26 and ELF 16KB alignment checks. It produces library/license/provenance artifacts, not an APK.
Native linking/device/login tests and app wiring remain pending until actual workflow results pass.
