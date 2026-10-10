# TG Photos security draft review packet

Prepared 2026-10-10 for independent human review. This document is not an audit or approval.
Review the actual feature-branch head and diffs; source b7befe232f57dbeb31671fbf3432ee8d0ade565b
is the current named compilation/lifecycle-fix baseline. Further changes need their own assessment.
The owner authorized source/documentation upload and eventual APK delivery, not Play publication.

## Reviewable current scope

| Boundary | Source | Evidence and limits |
|---|---|---|
| Authenticated envelopes | core/crypto/.../AeadEnvelope.java, ChunkContext.java | Bounded full AES-256-GCM authentication before output, fresh CSPRNG nonces, authenticated version/context; offline mutation/replay/range checks; not a complete recovery protocol |
| Password/root hierarchy | BouncyCastlePasswordKeyDerivation.kt, RootKeyEnvelope.java, HkdfSha256.java, ScopedKeyHierarchy.java | Bounded Argon2id 1.3 parameters, independent cloud/Vault roots, HKDF domain separation and root wrapping; JVM/offline vectors; app provisioning/calibration/recovery persistence pending |
| Local Telegram database key | app/.../security/LocalSessionKeyStore.kt | Random 32-byte key, Android Keystore AES-GCM wrapper, AtomicFile under noBackupFilesDir, cross-instance process lock; four isolated Android 35 checks pass; physical hardware/locked-device/reboot/invalidation/API matrix pending |
| Official native boundary | core/telegram/src/main/cpp, NativeTdJson.java | Pinned TDLib/OpenSSL, original standard UTF-8 bridge, bounded JSON, serialized returned-pointer copying; both ABI ELF/build evidence; all six Android 35 provider/native checks pass in run 38074390506 attempt 2; physical/API matrix and live integration remain pending |
| Session/gateway drafts | TdLibRequests.kt, TdLibRequestSession.kt, TdLibTelegramGateway.kt | Positive log-suppression acknowledgements, authoritative states, correlation/deadlines/bounds, no unconfirmed receiver reset; 9 dispatcher + 4 schema JVM tests pass; live provisioning/login/session UI not wired |
| Backup protocol | EncryptedBackupCoordinator.java | Encrypted chunks/metadata/manifest-last commit, independently downloaded authenticated restore/hash; 13 fixtures; only in-memory checkpoints in tests, durable prepared outbox/ack crash handling not implemented |
| Persistent cache | AuthenticatedDiskCache.java | Ciphertext-only temp/write/fsync/atomic promotion, trusted read bounds/context, no-follow checks, quotas and separate root hierarchy; 10 Linux fixtures; dedicated Android private binding/eviction/lock lifecycle pending |
| Media ranges | AuthenticatedRangeReader.java, EncryptedMediaDataSource.kt | Full chunk authentication before decoder-facing bytes, owned-key/buffer wiping and late-fetch closure rejection; 9 fixtures and Android compilation; actual player/decoder/UI lock lifecycle not wired |
| Android attack surface | app manifests, check_apk_manifest.py | Actual preview APK has no network/media/storage permissions; exported tooling activity removed; reviewed non-exported local startup; live transport/permission/release manifest must be reviewed separately |

The app currently keeps unavailable crypto/transport adapters and honest labelled previews.
Do not interpret primitive tests or account-free native loading as live encrypted backup acceptance.

## Evidence to retrieve

- docs/evidence/android-ci.txt, native-baseline.txt, PROGRESS.md and TEST_MATRIX.md contain named sources/runs.
- Source run 38074390463: 24 JVM tests, zero failures/errors/skips; Android/Room/test-source compilation,
  Debug/Release lint and source/privacy/ZIP/CI classification checks pass.
- Native producer/rebuild run 38071501341: both ABI artifacts with exact hashes, licenses and provenance.
- Android provider run 38072657836: four Keystore tests pass; subsequent UI launch failed after test teardown.
- Android run 38073154300: native load/UTF-8/bounds and Keystore pass; immediate reownership fails.
  The source fix uses confirmed cleanup completion and delays Closed publication until ownership release.
- Corrected runtime run 38074390506 attempt 2: all six Android tests pass, including immediate native
  reownership. UI overall FAILED at Photos hierarchy acquisition after passing login; harness diagnostics
  are being improved. First attempt failed during Google SDK image ZIP provisioning.
- Diagnostic source 3bf7faab1705bd6c3b57b113159537f259ae402e: source run 38087302893 passes 24 JVM tests/lints; runtime run 38087302897 passes six Android tests and 11 preview UI checks. Empty crash buffer; real Photos hierarchy obtained after transient null-root retry. See android-preview-runtime.json for hashes/scope.
- Private dependency evidence: public resolved artifact/POM hashes, eight configurations, 149 packages.
  Missing/inherited Guava/failureaccess/Hamcrest notices, full native SBOM and advisory review remain open.

No account, API registration, phone, OTP, passphrase, production media/key or release signing is used by QA.
Only account-free screenshots/XML/logs/provider/native test evidence is uploaded; QA APKs are removed.

## Questions and findings that must be resolved before production

1. Review KDF memory/time calibration on supported devices and denial-of-service bounds. Validate root
   setup, password change, recovery export/import/version handling and an independently restored second device.
2. Review Keystore behavior across API26/31/35/36, secure-lockscreen configurations, reboot/lock/invalidation
   and physical hardware backing. Keep provider failures explicit; never silently recreate a missing session key.
3. Verify native close, cancellation, duplicate owners, unexpected closure and fatal parse/bounds failure.
   Unconfirmed native failure retains process ownership; recovery currently requires process restart.
4. Implement encrypted durable prepared outbox/operation identity before upload and resolve acknowledgement/
   checkpoint crash windows, orphan cleanup, retries, source changes and idempotent manifest publication.
5. Wire trusted manifests/keys into media/cache and clear active readers, decoder/UI-held buffers and previews
   on Vault/account lock. Java/provider erasure is best effort; do not promise perfect memory wiping.
6. Review scoped-storage grants/import/export/deletion consent and archive/editor workspaces. No user device
   deletion is eligible without a complete independent restore/hash for the exact version plus confirmation.
7. Audit all resolved dependencies/native components, license obligations/advisories and initialization behavior.
   Capture traffic for real idle/auth/backup/restore/player/Vault paths after networking is introduced.
8. Recheck signed release manifest, permissions, debuggability, backup exclusions, ABI/package alignment,
   reproducible signing/provenance and actual on-device feature acceptance before delivering production APK.

Referenced app projects supply design/source research only; none of their servers is an app backend.
Authorized Telegram storage still observes account, sizes/timing and traffic metadata. OS/root/IME/device
compromise boundaries must remain accurately disclosed; zero-risk/unlimited-capacity claims are unsupported.

## Independent review record

Reviewer identity, reviewed commit/tree, scope, findings, mitigations, residual risks and signed-off date:
**not provided / not approved**. Keep security integrations unmerged as required by AGENTS.md until a
qualified independent human review covers the actual changes. This packet supplies review material only.
