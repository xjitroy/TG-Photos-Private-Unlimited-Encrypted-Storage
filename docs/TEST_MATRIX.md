# TG Photos verification matrix

| Area | Cases | Status |
|---|---|---|
| Source policy | branding, four tabs/overflow Settings, no media/network permissions/endpoints/telemetry/logging; reviewed non-exported startup policy, XML/catalog/module graph, disabled auth/vault, failed backup, no APK, packaging guard | 12 checks passed; source-only |
| JVM deletion safety | every backup state, exact file/version, incomplete restore, corrupt hash, unauthenticated manifest/chunk, local-only evidence, empty file | 3 JUnit methods pass in Android CI |
| JVM unavailable crypto | encryption never returns fake ciphertext; decryption never returns unauthenticated bytes | 2 JUnit methods pass in Android CI |
| Compile | app and all Android/JVM modules, Room KSP/schema export | Android/Room KSP/media compilation passes for named commits; isolated Keystore test source compiles |
| Lint | debug/release lint without packaging | Debug/Release lint passes in CI |
| Dispatcher/schema | bounded correlation, shutdown, positive logging ack, official email OTP shapes | 9 dispatcher + 4 schema JVM tests pass; native load/Unicode/bounds and two-owner closure pass on Android 35; live integration pending |
| Android Keystore | isolated fresh/concurrent instances, corrupt envelope, missing alias, no reset | four Android 35 emulator provider checks pass; hardware/lock/reboot/API matrix pending |
| Native | both ABI build/link/16KB/exports; standard UTF-8/bounds/receiver ownership/closure on Android | both ABI builds and Android load/UTF-8/bounds check pass; immediate reownership race found, source fix awaiting runtime acceptance |
| Auth | phone/country/E.164, authoritative delivery/email/2FA, logout/expired session, no logs | P2 integration pending |
| Recovery/crypto | golden vectors, wrong passphrase, corruption, reordered/missing chunks, fresh-device restore/password rotation, key loss | 31 offline primitives + 6 Argon2id/recovery JVM tests pass; app/Android/fresh-device/rotation acceptance pending |
| Backup | file types, Unicode/nested/empty/10GB+ where feasible; interruption/restart/duplicate/permission/FloodWait | pending |
| Delete | independently download+decrypt+hash; request confirmation only after exact version proof; cancel Android consent | domain source only; runtime pending |
| Gallery/files | scoped grants, deny/revoke/SD removal; real selection/operations; document/zip bombs/path traversal | pending |
| Media | 9:16/1:1/16:9; swipe/seek/track/subtitle/resume; corrupt chunk fails before decode; bounded next prebuffer | 9 range-reader and 10 disk-cache fixture checks pass; full Android player acceptance pending |
| Vault/local leak | separate auth/keys, screenshots/recents, background auto-lock, no MediaStore/plaintext external cache/export | FLAG_SECURE source only; pending |
| UX/device | API26/31/36, font200%, RTL, TalkBack, dark/light/dynamic, landscape/small-screen, low memory/4K/HDR | pending |
| Network/privacy | merged manifests/init providers/resolved dependencies; traffic capture on idle/auth/backup/playback/errors/vault | actual preview merged APK has no Internet/media permission; live transport traffic audit pending |
| Supply chain | full SBOM/version hashes/advisories/license/native ABI provenance | 8-configuration resolved inventory generated; both native ABI provenance exists; complete SBOM/advisory/license review pending |

No emulator screenshots or test results are fabricated. Instrumentation/device test installation can create
APK artifacts. The owner has authorized eventual APK creation; the preview emulator workflow uses a
temporary debug package only for QA and uploads no APK. Live/device production acceptance remains pending.
