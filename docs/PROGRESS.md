# TG Photos continuation — 2026-10-10

## Completed and verified now

- Official Gradle 8.13 wrapper JAR retrieved from the upstream GitHub base64 file tool; SHA-256 verified:
  `81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f`.
- Discovered `jdk.compiler` in the installed Java 21 runtime. `javac` is absent from PATH, but
  `java com.sun.tools.javac.Main` works. `--release 17` is unavailable because `ct.sym` is absent;
  offline checks use Java 17 source/target on the Java 21 provider, not proof of Android/JDK17 compatibility.
- Real bounded JCA AES-256-GCM envelopes, canonical file/version/index/scope authenticated context,
  random root-key wrapping primitives and Kotlin chunk bridge written. **19 offline primitive checks pass**.
- Streamed encrypted backup coordinator, opaque transport identifiers, encrypted manifest-last commit,
  checkpoint-content checks and independent remote authentication/hash verification written.
  **13 offline protocol/fault checks pass** with in-memory test transport/checkpoints.
- Source-policy checks still pass. No production keys, plaintext file transport or deletion operation exists.

## Work written but not integrated/compiled

- Strict Argon2id KDF contract with unavailable backend; no weak hash/PBKDF2 fallback.
- TDLib runtime credential/session request factory, JSON authorization reducer and original JNI/CMake boundary.
  Official header/API/CMake definitions were checked, with intended native source pin
  `tdlib/td@c15d3f5a5de6e3ba5839822c451152e5e18bb700` (1.8.68).
- Request correlation/receiver/session/Keystore/UI wiring and native build are still pending.
- No production CryptoEngine, KDF, live TDLib gateway, scheduler or authenticated media player is enabled.

## Limits and open gates

Gradle distribution download fails in the sandbox with `SocketException: Operation not permitted`.
Android SDK/full JDK17/dependencies/NDK are not provisioned. Kotlin/Android/Room KSP compilation, lint,
5 prepared JUnit methods, JNI compilation, device tests and screenshots remain blocked/not run.

The offline tests verify primitives/protocol properties, not Argon2id, full password recovery,
actual Telegram behavior, persisted resume, 10GB/device performance or audited E2EE.
The coordinator's checkpoint store is an interface; a durable encrypted implementation is mandatory.
A crash after remote upload acknowledgement but before saving a checkpoint can leave orphan/duplicate objects;
prepare persisted ciphertext/operation identity before upload when implementing durable queue semantics.
Progress objects are not consent for deletion. No Android file is deleted by the coordinator.

API registration is privately stored and excluded from Git/archive; its screenshot is not uploaded.
The owner authorizes uploads to the selected private repository. Security drafts stay unmerged on the feature
branch for independent human review. No APK or Google Play action is authorized.
