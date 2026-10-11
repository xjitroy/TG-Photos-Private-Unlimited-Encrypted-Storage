# TG Photos complete requirements checklist

Source: owner request, latest branding/privacy clarification and all master-plan sections.
**Source** means code exists; compilation/runtime evidence must be checked per named commit. **Preview** means labeled sample UI only.
**Pending** means no live implementation; no production feature completion is claimed.

| ID | Requirement | Module / phase | Current status |
|---|---|---|---|
| F01 | Official app/launcher/login/header/Settings/About name TG Photos | app + features / P1 | Source |
| F02 | Splash identity and original icon; name never replaced | app / P1 | Source; system splash shows icon, login shows name |
| F03 | New package proposal; preserve established IDs | app / P0 | app.tgphotos.android proposed; no old project |
| F04 | Play title TG Photos: Private Backup, 30-character compliance | branding / P0 | documented, 25 characters |
| F05 | Preserve extended preferred name, no unverified unlimited claim | branding / P0 | documented |
| F06 | No official Telegram affiliation claim | auth/About/branding / P1 | Source |
| F07 | Native Kotlin/Compose, Material 3 Expressive/Material You | app/designsystem / P1 | Source; Android compilation/lint verified; runtime visual acceptance pending |
| F08 | Dark/Light/System + persistent preference + dynamic colors | designsystem/app/settings / P1 | Source |
| F09 | Four tabs: Photos (Cloud), Albums, Files, Private Vault | app / P1 | Source |
| F10 | Settings from top-right overflow; no fifth tab | app / P1 | Source |
| F11 | Media3, Room, WorkManager, Android Keystore | core / P1–P6 | first three declared/foundation; Keystore envelope draft compiled; Android provider tests pending |
| F12 | Original polished UI inspired by 14 reference images | features / P1 | Preview source; runtime review pending |
| F13 | Accessibility/48dp/large font/RTL/error-empty-loading states | features / P1–P7 | source baseline/empty-unavailable states; Android compile/lint passed; device QA pending |
| A01 | Telegram phone authentication through official TDLib | telegram/auth / P2 | Preview; adapter unavailable |
| A02 | Automatic country suggestion and editable selector | auth / P1–P2 | Preview starter list; full list/E.164 pending |
| A03 | Authoritative OTP delivery type/length/resend | telegram/auth / P2 | UI preview only; no fabricated OTP |
| A04 | Open Telegram shortcut, never secretly scrape OTP | auth / P1–P2 | Source shortcut; no data passed/read |
| A05 | Telegram 2FA distinctly separate from encryption password | auth / P1–P2 | Separate previews; authentication pending |
| A06 | Email/email-code authorization branches when requested | telegram / P2 | official schema/reducer/gateway drafts and JVM tests pass; live handlers/UI pending |
| A07 | Protected session, expiration/logout and secure lifecycle | telegram / P2 | Pending; no live session |
| A08 | Own API ID/hash, secure local/CI setup, no hardcoded secrets | telegram / P2 | owner registration stored in ignored private configuration; runtime deployment pending |
| E01 | Random 256-bit root; never a short-password direct media key | crypto / P2 | random 256-bit generator/root wrappers implemented; full setup/recovery flow pending |
| E02 | Secure calibrated Argon2id, unique salt, strength UX | crypto/auth / P2 | Bouncy Castle Argon2id 1.3 backend + 6 JVM tests pass in GitHub CI; calibration/app wiring pending |
| E03 | AES-256-GCM authenticated bounded file chunks | crypto/backup / P2–P3 | AES-GCM primitive + bridge; mutation/bounds tests pass; app engine unavailable |
| E04 | Per-file/version fresh keys and safe nonces | crypto / P2–P3 | HKDF file/version context and random nonces implemented; offline tests pass; app wiring pending |
| E05 | HKDF file/manifest/cache/vault domain-separated keys | crypto / P2 | independent cloud/vault root validation + 5 scoped HKDF purposes; offline tests pass |
| E06 | Encrypted authenticated manifests/schema/transport pointers | crypto/backup / P3 | encrypted manifest coordinator draft; 13 in-memory protocol checks pass; live transport pending |
| E07 | Encrypted names/path/EXIF/metadata/search index | crypto/database / P2–P4 | encrypted-envelope schema source only |
| E08 | Encrypted thumbnails/persistent cache, in-memory decode only | crypto/cache / P3–P5 | authenticated disk-cache primitive + 10 offline fault tests; thumbnail/viewer integration pending |
| E09 | Passphrase wrap + offline recovery + separate recovery wrap | crypto/auth / P2 | root/recovery wrap primitives pass; passphrase-backed tests prepared; export/UI pending |
| E10 | Keystore local convenience wrapper; second-device recovery | crypto / P2 | session database wrapper draft compiled; isolated Android tests prepared; media/root recovery remains pending |
| E11 | Password rotation/key versions/cross-device restore | crypto / P2–P6 | Pending |
| E12 | No plaintext private keys/passwords uploaded | crypto/transport / all | required boundary; no uploads yet |
| E13 | Corruption/wrong password/duplicate/missing/reordered chunks fail closed | crypto/backup/media / P2–P5 | wrong-key/corruption/reorder/truncation primitive checks pass; live/device integration pending |
| B01 | Per-folder opt-in switches, recursive filters | albums/backup / P3 | switch preview; no policies/scheduling |
| B02 | MediaStore observer and SAF periodic scan/new-file detection | albums/backup / P3 | Pending |
| B03 | Durable background queue/checkpoints/idempotency | backup/database / P3 | unscheduled failure worker only |
| B04 | Photo/video/audio/document/archive/arbitrary binary backup | backup / P3 | Pending |
| B05 | Multiple selection/upload/download | photos/files/backup / P3–P4 | file sample selection only |
| B06 | Resumable upload/download, retries/backoff/FloodWait | backup/telegram / P3 | resumable coordinator test fixtures pass; durable queue/TDLib/FloodWait pending |
| B07 | Progress/retry/error/permission/account-limited/paused states | backup/model / P3 | enum foundation; no live progress |
| B08 | Wi-Fi/metered/roaming/charging/battery constraints | backup/settings / P3 | documented, pending |
| B09 | Original quality; no silent transcode | backup/media / P3–P5 | policy, pending |
| B10 | Chunks uploaded/verified first; manifest committed last | backup/telegram / P3 | encrypted manifest-last coordinator draft; 13 in-memory checks pass |
| B11 | Complete independent restored hash/bytes/exact version before deletion | model/backup / P3 | domain gate source + independent remote protocol verification; Android consent flow pending |
| B12 | Explicit confirmation + Android scoped deletion consent | files/backup / P3–P4 | Pending; no delete operation |
| B13 | Restart/offline/low disk/SD removal/revoked grants consistency | backup/database / P3 | Pending |
| B14 | Backup notifications branded TG Photos and no plaintext private info | backup/app / P3 | Pending; no notifications generated |
| G01 | Vivo-inspired local gallery, cloud timeline and albums | photos/albums / P1–P4 | Preview source |
| G02 | Date clusters/photos/videos/favorites/availability badges | photos / P4 | sample filters/favorites; real dates/status pending |
| G03 | Smooth viewer transitions/stable IDs/swipes | photos/media / P4–P5 | illustration swipe source; live shared transitions pending |
| G04 | Favorites/cloud albums/info/encrypted search | photos/database / P4 | sample favorites; live actions pending |
| G05 | Download/share with explicit decrypt consent/offline pin/move vault | photos/vault/cache / P4–P6 | Pending |
| G06 | Photo viewer/basic crop/rotate/exposure/filter/export | photos/media / P4–P5 | illustration viewer; editing pending |
| G07 | Adjacent 2–3 ahead/1–2 behind prefetch, cancel stale work | media/cache / P5 | full-chunk authenticated range primitive; app player/budget lifecycle pending |
| V01 | 9:16 Reels/1:1 square/16:9 normal nondestructive modes | media / P5 | ratio enum foundation only |
| V02 | Horizontal/vertical video navigation and quality playback | media/photos / P5 | Pending |
| V03 | Multiple audio/subtitle tracks/external subtitles/speed | media / P5 | Pending |
| V04 | Resume per file/version; next-video prebuffer | media/database/cache / P5 | playback contract only |
| V05 | Adaptive seekable encrypted streaming and bounded caching | media/crypto/cache / P5 | authenticated range-reader + Media3 adapter drafts; 9 offline fault checks pass; player/device integration pending |
| V06 | Authenticate chunks before decoder; respect power/network budget | media/cache / P5 | full-chunk authenticated range primitive; app player/budget lifecycle pending |
| V07 | Basic video trim/crop/rotate/speed/export and audio cut/fade/volume | media / P5 | Media3 Transformer declared; pending |
| V08 | Codec/4K/HDR graceful fallback; optional VLC audited separately | media / P5 | Pending; no universal-format claim |
| M01 | Local scoped-storage/SAF files and encrypted cloud switching | files / P4 | Preview switch; no actual access |
| M02 | Folder browsing/breadcrumbs/create/manage and multi-selection | files / P4 | sample folders/selection; operations pending |
| M03 | Copy/move/rename/delete/backup/restore | files / P4 | Pending; no destructive operation |
| M04 | Search/sorting/type filters | files / P4 | Preview source only |
| M05 | PDF/text/document previews; explicit safe external fallback | files/media / P4 | Pending |
| M06 | ZIP/7z/RAR support where safe; path traversal/bomb limits | files / P4 | Pending |
| M07 | Scoped grants/revoke/export temporary URIs/cleanup | files / P4–P6 | Pending; no FileProvider |
| M08 | Nondestructive edits, protected workspace, new encrypted version | files/media/crypto / P5 | Pending |
| P01 | Separate vault authentication/biometric protection | vault/crypto / P6 | locked preview only |
| P02 | Independent encrypted vault key/index/ACL hierarchy | vault/crypto / P6 | independent root validation/domain-separated keys source; vault unlock/index/ACL pending |
| P03 | FLAG_SECURE + recents protection on sensitive surfaces | vault/app / P6 | FLAG_SECURE source; device/recents tests pending |
| P04 | Background/time-based auto-lock and key/preview clearing | vault/cache/crypto / P6 | Pending; preview always locked |
| P05 | Encrypted import/export/previews; no media exposure to other apps | vault/files / P6 | Pending; no import/access yet |
| S01 | Settings account/backup/storage/security/player/themes/recovery/about/notices | settings / P1–P6 | theme controls functional source; other sections informative previews |
| S02 | No hidden backend/analytics/ad/crash-upload/remote conversion | all / all | no source endpoints/permission/SDK; full resolved/runtime audit pending |
| S03 | No data delivered to applications used as code/design references | all / all | mandatory privacy contract; no app code/backend integrated |
| S04 | Explain Telegram identity/traffic and OS/root boundaries accurately | security/privacy / all | documented |
| S05 | App-private encrypted persistent media/session/cache, OS backup excluded | app/core / all | backup exclusions source + Keystore/cache drafts; app wiring and device tests pending |
| O01 | Verify 14 referenced repositories/licenses before reuse | docs / P0 | license/README screening done; exact reuse/native dependency audit pending |
| O02 | Prefer permissive audited components, respect GPL/AGPL/LGPL obligations | all / all | clean-room app; no copyleft source copied |
| O03 | Attributions/NOTICE/resolved SBOM/build provenance/pinned dependencies | gradle/docs / all | resolved JVM/Android inventory and native provenance exist; full SBOM/license/advisory review pending |
| O04 | Feature branch/local commits, preserve original documents/code | repo / all | local feature commits; original master ZIP and all 14 UI references preserved in private GitHub |
| O05 | No hardcoded keys/credentials, sensitive logs or plaintext shared staging | all / all | no such source; runtime tests pending |
| O06 | Independent security review and verified recovery before production claims | all / P2–P7 | mandatory pending gate |
| O07 | Available checks/tests/format/lint/device/performance/security with honest logs | scripts/docs / all | 12 source + 31 crypto + 13 backup checks pass; Android compile/JVM/lint pass in GitHub CI; device/audit pending |
| O08 | No APK creation until explicit owner approval; no Play publishing | build/repo / all | latest owner authorizes eventual APK delivery after production gates; temporary account-free QA debug APK built and removed; no delivered production APK or Play publication |
| O09 | Bengali progress communication, English identifiers/files/code | delivery / all | applied |
| O10 | Report changed files/completed source/results/remaining tasks | docs/status / all | documented |

This checklist retains all priority requests and operational master-plan requirements. Phase exit checks are
in PLAN.md and TEST_MATRIX.md. A Preview/Source status must not be converted to completed production functionality
until the corresponding live implementation and acceptance checks pass.

## Owner clarification: 2026-10-11

- Albums controls persisted per-folder auto-backup, including Camera when enabled.
- Leaving a screen/task should not cancel authorized uploads; use OS-compliant resumable background work and foreground transfer notification as required. Account for force-stop, permission revocation and OEM power restrictions.
- Each photo/file exposes two separate actions: Backup and Delete from device. Never delete before independently verified full remote restore plus explicit item confirmation. Preserve encrypted metadata/previews and Vault access boundaries.
- Retain all other original-plan requirements; these clarifications do not waive unfinished features or production gates. These controls are not yet app-integrated.
