# TG Photos — start here for the next developer or agent

Owner repository: https://github.com/xjitroy/TG-Photos-Private-Unlimited-Encrypted-Storage
The owner explicitly authorizes source/documentation uploads and continued development.
Repository is private as observed on 2026-10-10; do not change visibility or publish the application.
The repository identifier is the owner's existing name; application branding remains **TG Photos**.

## Read first

1. `AGENTS.md`: permissions, privacy, branding and security review rules.
2. `docs/STATUS.md` and `docs/PROGRESS.md`: completed source, real checks and current blockers.
3. `docs/REQUIREMENTS.md`, `PLAN.md`, `docs/ARCHITECTURE.md` and the ADRs.
4. `docs/PRIVACY_CONTRACT.md`, `docs/THREAT_MODEL.md`, `docs/SECURITY_REVIEW_PACKET.md`, `docs/LICENSING.md` and test evidence.
5. `docs/MASTER_PLAN.md`: all imported original requirements, with naming/approval overrides.
6. `docs/reference/README.md`: original PDF/DOCX/Markdown archive and checksum-verified extraction of all 14 UI images.
7. `docs/MASTER_PLAN_RECONCILIATION.md`: section-by-section ZIP and latest owner-instruction comparison.
8. `docs/UPSTREAM_REFERENCE_SOURCES.md`: downloaded reference source from all 14 ZIP repositories and restore commands.

`main` holds the Phase 1 foundation. Continued, unmerged security work is on
`feature/phase-2-security-foundation`. Fetch/switch to this branch before continuing the latest work.
The owner now requests agent-led security review (2026-10-11); document its actual scope/findings and complete tests before merging security integrations. Do not claim independent third-party audit.
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

1. Source b7befe23 passes Android compilation, Room KSP, 24 JVM tests and Debug/Release lint in
   run 38074390463. Native rebuild 38071501341 passes both ABIs with fresh bounded JNI. Check newer
   runs before claiming changed-source compatibility; Android native load/UTF-8/bounds pass; immediate reownership fix passes all six Android tests in run 38074390506 attempt 2. New source 3bf7faa passes all 11 preview UI checks plus six Android tests in run 38087302897; source run 38087302893 also passes 24 JVM tests/lints. Read docs/evidence/android-preview-runtime.json for scope/hashes; broader live/device acceptance remains open.
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

On 2026-10-11 the owner explicitly requested the current APK. The separate android-preview-apk.yml
workflow builds and privately retains a labelled debug preview plus actual manifest, signature and
SHA-256 evidence for owner delivery. No owner credentials/native runtime fixture/user files are included.
The signing certificate is a runner-generated development identity, not the future stable release key.
App source remains the tested preview; live functionality and production security gates are unchanged.

Latest owner instructions on 2026-10-11: complete all actual features for production, retain editable
source/build documentation and APKs privately, target Android8/API26 onward including Android16, and
perform agent-led security review. APKs are updated by rebuilding source with a stable signing identity;
the preview debug identity is ephemeral, not the future production identity. No unverified future-OS
guarantee or automatic publication is authorized.

Current next-source root drafts: d9eaca5b61d2d411531bb54b13a59099e4cb6f65. Runs 38100308560
and 38100308516 fail before runner steps start, with one check annotation and no logs. Reason is not
retrievable through the available annotation endpoint; do not assume billing or report tests as passed.
A local public-download network check was canceled by the user; do not repeat that permission request.
No local Android SDK/KVM is available. Six new JVM/two new Android root tests remain unverified.
The earlier O_DIRECTORY compile failure was corrected with public fstat/S_ISDIR validation; corrected
root source still needs compilation/runtime proof. Existing source tests and APK evidence remain
bound to their earlier named commits; do not transfer acceptance to the new drafts.

Owner APK artifact: run 38099671415, artifact 11686539050 (private, expires 2026-11-10).
Direct download: https://github.com/xjitroy/TG-Photos-Private-Unlimited-Encrypted-Storage/actions/runs/38099671415/artifacts/11686539050
APK SHA256: 006b1fccca31426acf358538ebbd1850f0a968e75bb652ae4958b27c43b378aa
Local delivery: /workspace/deliverables/TGPhotos-preview/TG-Photos-0.1.0-preview.apk
No permanent GitHub Release asset has been created; raw workflow artifact retention is finite.
