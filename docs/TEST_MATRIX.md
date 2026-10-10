# TG Photos verification matrix

| Area | Cases | Status |
|---|---|---|
| Source policy | branding, four tabs/overflow Settings, no media/network permissions/endpoints/telemetry/logging; reviewed non-exported startup policy, XML/catalog/module graph, disabled auth/vault, failed backup, no APK, packaging guard | 12 checks passed; source-only |
| JVM deletion safety | every backup state, exact file/version, incomplete restore, corrupt hash, unauthenticated manifest/chunk, local-only evidence, empty file | 3 JUnit methods pass in Android CI |
| JVM unavailable crypto | encryption never returns fake ciphertext; decryption never returns unauthenticated bytes | 2 JUnit methods pass in Android CI |
| Compile | app and all Android/JVM modules, Room KSP/schema export | Android/Room KSP compilation passes; newer media/native source pending |
| Lint | debug/release lint without packaging | Debug/Release lint passes in CI |
| Auth | phone/country/E.164, authoritative delivery/email/2FA, logout/expired session, no logs | P2 integration pending |
| Recovery/crypto | golden vectors, wrong passphrase, corruption, reordered/missing chunks, fresh-device restore/password rotation, key loss | pending |
| Backup | file types, Unicode/nested/empty/10GB+ where feasible; interruption/restart/duplicate/permission/FloodWait | pending |
| Delete | independently download+decrypt+hash; request confirmation only after exact version proof; cancel Android consent | domain source only; runtime pending |
| Gallery/files | scoped grants, deny/revoke/SD removal; real selection/operations; document/zip bombs/path traversal | pending |
| Media | 9:16/1:1/16:9; swipe/seek/track/subtitle/resume; corrupt chunk fails before decode; bounded next prebuffer | pending |
| Vault/local leak | separate auth/keys, screenshots/recents, background auto-lock, no MediaStore/plaintext external cache/export | FLAG_SECURE source only; pending |
| UX/device | API26/31/36, font200%, RTL, TalkBack, dark/light/dynamic, landscape/small-screen, low memory/4K/HDR | pending |
| Network/privacy | merged manifests/init providers/resolved dependencies; traffic capture on idle/auth/backup/playback/errors/vault | no internet permission in source; runtime pending |
| Supply chain | full SBOM/version hashes/advisories/license/native ABI provenance | direct inventory/source-level licensing only |

No emulator screenshots or test results are fabricated. Instrumentation/device test installation can create
APK artifacts. The owner has authorized eventual APK creation; the preview emulator workflow uses a
temporary debug package only for QA and uploads no APK. Live/device production acceptance remains pending.
