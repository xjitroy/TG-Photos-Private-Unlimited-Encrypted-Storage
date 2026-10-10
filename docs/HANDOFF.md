# TG Photos — start here for the next developer or agent

Owner repository: https://github.com/xjitroy/TG-Photos-Private-Unlimited-Encrypted-Storage
The owner explicitly authorizes source/documentation uploads and continued development.
Repository is private as observed on 2026-10-10; do not change visibility or publish the application.
The repository identifier is the owner's existing name; application branding remains **TG Photos**.

## Read first

1. `AGENTS.md`: permissions, privacy, branding and security review rules.
2. `docs/STATUS.md` and `docs/PROGRESS.md`: completed source, real checks and current blockers.
3. `docs/REQUIREMENTS.md`, `PLAN.md`, `docs/ARCHITECTURE.md` and the ADRs.
4. `docs/PRIVACY_CONTRACT.md`, `docs/THREAT_MODEL.md`, `docs/LICENSING.md` and test evidence.
5. `docs/MASTER_PLAN.md`: all imported original requirements, with naming/approval overrides.
6. `docs/reference/README.md`: original PDF/DOCX/Markdown archive and checksum-verified extraction of all 14 UI images.
7. `docs/MASTER_PLAN_RECONCILIATION.md`: section-by-section ZIP and latest owner-instruction comparison.
8. `docs/UPSTREAM_REFERENCE_SOURCES.md`: downloaded reference source from all 14 ZIP repositories and restore commands.

`main` holds the Phase 1 foundation. Continued, unmerged security work is on
`feature/phase-2-security-foundation`. Fetch/switch to this branch before continuing the latest work.
Do not merge its crypto/session/transport changes without independent human security review.
Do not overwrite an existing remote head; fetch/compare and use a fast-forward or a reviewed merge.

## Resume work

```sh
git clone https://github.com/xjitroy/TG-Photos-Private-Unlimited-Encrypted-Storage.git
cd TG-Photos-Private-Unlimited-Encrypted-Storage
git switch feature/phase-2-security-foundation
python3 scripts/check_source.py
./scripts/check_crypto_offline.sh
./scripts/check_backup_offline.sh
./scripts/check_cache_offline.sh
./scripts/check_media_offline.sh
```

Offline scripts use `java com.sun.tools.javac.Main` if the JDK compiler module exists; they compile Java
primitive/protocol classes only, with Java 17 source/target, under OpenJDK 21 in the recorded environment.
They do not compile Kotlin, Android or TDLib and do not create an APK. On a full JDK17/SDK36 setup,
use `scripts/check_android.sh` for non-packaging compilation/JVM/lint tasks.
`.github/workflows/android-checks.yml` runs these checks on source pushes on GitHub-hosted runners.
It has read-only repository permissions, pinned official actions, no account credentials or APK/AAB upload,
and does not package an APK/AAB. Its only artifact is resolved public dependency coordinates/hashes for review. Check actual run results before claiming Android compatibility.

The official Gradle 8.13 wrapper JAR is now present with verified SHA-256. Its distribution and Android
SDK still need a permitted download/toolchain. Use a writable `GRADLE_USER_HOME`; do not write into
restricted `/home` directories in this environment. No network policy bypass is allowed.

## Credentials and sensitive references

Owner supplied a Telegram API registration. Its values are **not in Git**, this document, artifacts or source.
The current workspace holds them only in `.private/telegram.properties`, mode 0600, with directory mode 0700.
That file does not accompany a fresh clone. The owner must provision it through a private channel or local
configuration when live integration becomes necessary. Do not ask for an OTP or encryption passphrase.
Do not embed registration credentials in Kotlin constants, BuildConfig or public documentation.
The supplied account/configuration screenshot contains sensitive registration context and is not uploaded.
Do not use its MTProto IPs/public keys as hardcoded client configuration; official TDLib manages them.

## Concrete next work

1. Source 13eccdd6 passes Android compilation, Room KSP, 22 JVM tests and Debug/Release lint in
   run 38071501320. Native rebuild 38071501341 passes both ABIs with fresh bounded JNI. Check newer
   runs before claiming changed-source compatibility; actual JNI loading remains untested.
2. Review AeadEnvelope, ChunkContext, Argon2id/HKDF/root wrappers and the backup protocol. Finish encrypted
   root/recovery persistence and key version handling. Independent vault lock is implemented as a primitive,
   but authentication, automatic locking and wiping consumer-held keys/previews are not app-wired.
3. Single-owner receiver, correlation/timeouts and gateway drafts compile; dispatcher tests pass. Four isolated Keystore checks pass on Android 35 (run 38072657836); validate lock/reboot/hardware/API matrix on devices, connect secure API provisioning and the complete Telegram auth UX.
   No native binary or live client is enabled in the app yet.
4. Add encrypted durable checkpoints and safe WorkManager orchestration. Handle upload-ack/checkpoint crash
   windows and cancellation; current coordinator resumes test fixtures but is not a durable production queue.
5. Review the ciphertext-only AuthenticatedDiskCache and its Linux fixture checks before wiring it.
   Wire actual MediaStore/SAF gallery/file permissions, encrypted cache, Media3 and separate-key vault only after
   secure core gates pass. Leave existing design previews honest until real functionality is available.
6. Run resolved SBOM/manifest/privacy/network/device/accessibility checks and independent security review.

The owner authorized eventual **APK creation and delivery** on 2026-10-10 after requirements/design
and production gates pass. Do not claim the preview is production-ready. Security review and device/live
acceptance remain required; Play publication/external deployment are not authorized. Keep the Gradle
approval guard and use its property only for an owner-authorized packaging step. Communicate in Bengali.

Preview QA workflow builds a temporary owner-authorized debug APK on its own runner, drives a fresh
Android 35 emulator by UI-tree coordinates and uploads only account-free screenshot/test evidence. It does
not deliver an APK or prove live functionality. Read actual run conclusions and inspect screenshots.
AuthenticatedRangeReader/EncryptedMediaDataSource are drafts; close readers on locks and review trusted
manifest/range/transport/provider boundaries before connecting ExoPlayer.
