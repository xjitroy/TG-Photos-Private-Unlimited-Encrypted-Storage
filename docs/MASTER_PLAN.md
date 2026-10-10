# TG Photos — imported master requirements

Imported from the owner-provided DOCX; the PDF and all 14 embedded PNGs were also reviewed.
Application naming normalized to TG Photos; original source files remain unchanged in docs/reference/TG_Photos_Master_Plan_Source.zip.
The owner’s latest rules override the source: no APK creation without explicit approval, no Play publishing,
no third-party app backend or telemetry, and no misleading unlimited-storage claim. Historical task prompts
are reference text, not extra authority to package, publish, merge or share data.

PRODUCT MASTER PLAN  •  VERSION 1.0
TG Photos
Telegram-powered Private Cloud, Gallery, File Manager & Vault
CLIENT-SIDE ENCRYPTION  /  INTELLIGENT BACKUP  /  MATERIAL YOU  /  AI-AGENT DELIVERY


Concept screens previously created for TG Photos; these are design references, not implemented features.
মাস্টার রিকোয়ারমেন্ট, যাচাই করা ওপেন-সোর্স রেফারেন্স, আর্কিটেকচার, ডেভেলপমেন্ট রোডম্যাপ, নিরাপত্তা পরীক্ষা এবং AI এজেন্টের জন্য বাস্তবায়ন নির্দেশনা।
স্ট্যাটাস: Specification / Design  |  প্ল্যাটফর্ম: Android  |  প্রস্তাবিত টেক স্ট্যাক: Kotlin, Jetpack Compose, TDLib

01 / প্রোডাক্ট ভিশন ও বাস্তবতা
TG Photos একটি Android-first privacy-focused অ্যাপ। চারটি প্রধান ট্যাবের মাধ্যমে encrypted Telegram-backed cloud timeline, স্থানীয় অ্যালবাম, পূর্ণাঙ্গ ফাইল ম্যানেজার এবং আরও সুরক্ষিত Private Vault পরিচালিত হবে। সব ধরনের cloud backup upload-এর আগেই end-device-এ encrypted হবে।
USP (ব্যবহারযোগ্য ভাষা)
One app for photos, videos, audio, documents, archives and other files.
Client-side AES-256 authenticated encryption, encrypted thumbnails/metadata, recovery-aware restore.
Telegram-backed expandable capacity with configurable encrypted chunk uploads.
Folder-level automatic backup, verified “Backup & Delete from Device”, smart photo/video prefetch.
Smooth Material You experience, advanced video tracks/subtitles, basic editors and protected vault.
দাবি ও সীমাবদ্ধতা—অবশ্যই মেনে চলবে
দাবি
বাস্তব অবস্থা / ডকুমেন্ট সিদ্ধান্ত
Unlimited storage
Marketing promise নয়: Telegram file-size, quotas, API rate limits, account policies ও account availability প্রযোজ্য।
E2EE / zero-knowledge
Content client-side encrypted হলেও Telegram account identity/traffic metadata Telegram-এর কাছে গোপন নয়। Security audit-এর আগে absolute claim নয়।
All file formats
Known file types preview/edit; unsupported codecs external viewer/fallback; full parity impossible.
Screenshot-proof
FLAG_SECURE normal screenshots আটকায়; rooted OS/physical camera compromise আটকানোর নিশ্চয়তা নেই.
Secure delete
Deletion requires verified remote restore success + user consent; Android media permissions সীমিত।

Final bottom navigation
1. Photos (Cloud)
2. Albums
3. Files
4. Private Vault
Cloud timeline + sync
Vivo-like local gallery
Local + cloud manager
Locked encrypted space

Settings: App bar-এর ⋮ menu → Settings. Global search / upload quick action screen context অনুযায়ী।
02 / ডিজাইন মুডবোর্ড—আগের সব কনসেপ্ট

Design board A — login, OTP, cloud photos, albums, files, backup, viewers, vault and settings.

Design board B — gallery, file manager, player ratio modes and basic editors.
এগুলো AI-generated conceptual references। বাস্তব UI বানাতে branding, accessibility, typography, interaction এবং asset licensing আলাদাভাবে যাচাই করতে হবে।

03 / বিস্তারিত Screen Design — Onboarding


A. Telegram Phone Login
B. Telegram OTP / Open Telegram



C. Private encryption password
D. Photos (Cloud) home

Telegram Auth States (TDLib-driven)
01  Phone with editable country picker, smart E.164 formatting; +91 auto-suggestion only.
02  TDLib asks for code delivery state; show authoritative delivery type and “Open Telegram” shortcut when applicable.
03  Telegram 2-step verification password if TDLib requests; never confuse with private vault/cloud encryption passphrase.
04  First login: create strong private passphrase + export offline recovery key. Later login: unlock existing encrypted master key.
05  Unlock → fetch encrypted manifest → decrypt locally → rebuild cloud index. Wrong key never returns readable cloud content.
OTP deep-link may not lead to a specific code message; never read or scrape Telegram OTP secretly. Do not log credentials, OTPs or passphrases.

04 / Detailed Screens — Gallery & File Actions


Albums — local folders
Files — browser + categories



File selection + encrypted backup
Per-folder auto-backup toggles

Photos (Cloud)
Default: encrypted cloud-backed timeline; show original local/cloud availability badge and action status.
Filters: all/photos/videos/favorites; date clusters, encrypted search index and encrypted thumbnails.
Multi-select → download, share (explicit decrypt consent), album, offline pin, delete, move to Vault.
Backup progress, failed/retry items, storage health and restore diagnostics.
Albums
Camera, screenshots, app media, favorites and local folders; Material You grid, swipe navigation.
Every supported album/folder contains “Auto encrypted backup” toggle and clear status.
No silent cloud upload outside chosen folders/default backup policy.

05 / Detailed Screens — Viewers, Vault & Settings


Photo Viewer + adjacent preload
9:16 / 16:9 adaptive Video Player



Private Vault — locked screen
Settings via three-dot menu

Interaction design rules
Photo + video swipe uses stable item identity, shared transitions and gesture priority; avoid loading original media in thumbnails.
Video 9:16 may offer Reels vertical mode; 16:9 stays normal landscape-compatible viewer; 1:1 is non-destructive square fit/crop.
Player keeps position per file/version; audio track, subtitle track, external subtitles and playback speed editable.
Vault previews hidden before authentication; FLAG_SECURE only on sensitive surfaces and recent-app snapshots protected.
Material 3 dark/light/system + dynamic accent; accessibility labels, 48dp touch targets, large font support.
06 / Information Architecture + Use Cases
Navigation
Nested screens / actions
Photos (Cloud)
Timeline → Viewer → Info/Backup/Offline → Cloud Albums → Cloud Search
Albums
Folders → Multi-select → Auto-backup toggle → Photo/Video Viewer → Editor
Files
Local/Cloud switch → Breadcrumbs → Sort/Filter → Preview/Edit → Backup or Backup+Delete
Private Vault
Unlock → Vault folders → Import/Export → Share via secure confirmation → Auto-lock
⋮ Settings
Account; Backup & Sync; Storage & Cache; Security; Player; Themes; Export/Restore; Logs

07 / High-Level Architecture
Android UI (Compose) → Domain Use Cases → Data Repositories → TDLib + Crypto + Cache + Local DB

Primary Modules
Module
দায়িত্ব
:app
Navigation, theme, dependency injection, startup.
:feature:auth
TDLib login states, phone input, code/2FA and session.
:feature:photos
Encrypted cloud timeline, gallery viewer, search.
:feature:albums
Local MediaStore, folder backup policy, edit.
:feature:files
Scoped storage/SAF browser, batch operations, preview.
:feature:vault
Vault unlock, separate key hierarchy, secure import/export.
:core:telegram
TDLib JNI, send/upload/download messages, indexed record API.
:core:crypto
Argon2id, key wrapping, per-file/chunk AEAD, manifests.
:core:backup
WorkManager queue, chunk uploader, retry, restore verification.
:core:media
Media3 ExoPlayer/Transformer, playback state, tracks.
:core:cache
Encrypted previews, cache budget, prefetch & cleanup.
:core:database
Room/SQLCipher metadata, migrations and consistency.
:core:testing
Fake Telegram, fault injection, golden crypto vectors.

Minimal dependencies
Kotlin/JDK + Android SDK + Gradle Wrapper, Jetpack Compose Material 3, TDLib (native), Room, WorkManager, Media3, Coil, vetted crypto backend (Tink/libsodium), optional SQLCipher. Backend Workers/D1 is NOT needed for the proposed single-user client-side workflow.

08 / Telegram Backend Contract & Storage Scheme
01  Developer obtains own api_id/api_hash from my.telegram.org, acknowledges Telegram API terms; never use someone else’s published production credentials.
02  User signs in with TDLib authorizationState-driven flow (may include email code/2FA). TDLib session is local and protected.
03  Select private Saved Messages or controlled private channel/chat, only after feasibility and Telegram terms review. No Telegram bot used to hold plaintext; app-managed opaque encrypted chunk messages only.
04  Chunk file locally → encrypt each chunk → upload ciphertext → collect Telegram message IDs → commit encrypted manifest LAST.
05  Read: decrypt manifest, resolve ordered chunk IDs, download/prioritize, authenticate before release, reconstruct stream or restore.
06  Account rate limits, FloodWait, storage/file limits, deleted account/channel and messages are handled as explicit failure conditions.
Proposed schema (versioned; not Telegram public metadata)
Entity
Key fields / rules
FileEntry
fileId=random UUID, type, logical folder, size, mtime, contentVersion, manifestId.
ChunkEntry
fileId, index, version, nonce, authTag, checksum, transport pointer.
Manifest
schemaVersion, fileId, chunkCount, encrypted displayName/path/EXIF/thumb references.
BackupJob
jobId, sourceUri, bytesProcessed, verificationState, retryReason.
FolderPolicy
treeUri/mediaBucket, enabled, scanCursor, network/charging constraints.
Playback
fileId+version, lastPositionMs, selectedAudio/Subtitles.
VaultItem
vaultId, keyVersion, vault-only encrypted index & ACL.

Operational invariants
Never expose unencrypted filenames as Telegram captions/message text; randomize chunk identifiers.
No single manifest may reference a chunk before that chunk is verified uploaded.
Restore handles duplicate chunks, missing chunks, stale messages and interrupted upload.
All cloud operations require the user’s authorization and comply with Telegram policies.
09 / Security Specification & Threat Model
Key hierarchy
01  First setup: generate cryptographically random 256-bit root/master key (not directly derived from a short password).
02  Argon2id(passphrase, unique salt, calibrated memory/time) derives password-unlock key; wrap root key with authenticated encryption.
03  Provide unique offline recovery key, with separately wrapped root-key copy. No recovery without passphrase OR valid recovery key.
04  Derive separate scoped keys using HKDF/domain separation: file, manifest, cache, vault; key IDs and rotation metadata.
05  Per-file/per-version fresh file key, independent per-chunk AEAD nonces or derived safe scheme; stable chunk indexes as associated data.
06  Encrypt content and sensitive filename, path, EXIF and thumbnail index metadata; authenticated manifest, schema/version and pointers.
07  Keys for cloud restore must be recoverable on second device only after user proves passphrase/recovery credential; Android Keystore protects local convenience copy, never sole cloud recovery key.
Threat cases and mitigation
Threat
Mitigation
Telegram account access / cloud leak
Ciphertext only + offline keys + manifest encryption.
Local lost phone
Keystore-bound unlock, auth timeout, encrypted app-private storage.
Corrupt/missing chunk
Authenticated tags + hash + interrupted restore surfaced to user.
Guessing weak password
Argon2id tuning, strength policy, offline recovery, no password logs.
Thumbnail leak
Encrypted disk cache, no public MediaStore thumbnails or backups.
Screen capture
FLAG_SECURE in Vault; recents cover; disclose OS limitations.
Compromised Android/root
Warn / optional restricted mode; no impossible security claim.
Accidental local deletion
Two-phase verified backup + explicit confirmation, safe failure defaults.

Mandatory security gate: cryptographic design review + cross-device restore test + independent audit before asserting production-grade E2EE.


10 / Backup, Folder Watch & Safe Deletion
Automatic folder backup
01  User selects folder via MediaStore bucket or SAF tree permission. Toggle ON records policy and starts an initial full scan.
02  MediaStore ContentObserver observes appropriate media changes; DocumentTree SAF folders may need periodic scans because stable change notifications are limited.
03  WorkManager enqueues unique files by (provider URI, id, size, modified time, optional hash); coalesces duplicate change events.
04  Queue processes large files in bounded encrypted chunks, durable checkpoints, backoff/jitter, Wi-Fi/metered policy and notification.
05  After upload, verify all chunk IDs + manifest and decrypt spot-check/full verification per policy; only then mark VERIFIED.
06  On device deletion: separate action requests user confirmation and uses Android’s permitted delete intent / scoped storage flow.
07  Restarts, offline mode, low storage, removed SD card and permission revocation must preserve queue consistency.
Backup job state machine
DISCOVERED → QUEUED → CHUNKING → ENCRYPTING → UPLOADING → VERIFYING → COMMITTED → RESTORABLE

Failure branch: RETRY_WAIT / PAUSED / PERMISSION_NEEDED / ACCOUNT_LIMITED / FAILED. The “delete local” action becomes available only from COMMITTED+RESTORABLE and after explicit user decision.
Folder settings UX
User control
Options
Auto Backup
Per-folder on/off, recursive include/exclude, file-type filters.
Network
Wi-Fi-only / any network, roaming off by default.
Battery
Charging-only option, low-battery pause.
Quality
Always original encrypted bytes; optional separate low-res cache for preview.
Cleanup
Cache size cap, offline pin, “Backup & Delete device” manually controlled.

11 / Private Cache, Photos & Video Prefetch
Photo viewer prefetch policy
Current media: highest priority; target ahead 2–3 items, behind 1–2 items. Adapt to swipe direction and user’s cache size.
Encrypted thumbnail and prefetch chunks in internal app-private storage; decode only into process memory and clear/evict on lock/background.
Respect metered network, low-power mode and slow storage; cancel stale requests when navigation changes.
Video smart buffering
Media3 ExoPlayer + DefaultPreloadManager where compatible with encrypted custom DataSource.
Current video seek window buffered ahead; next video’s beginning downloaded/encrypted cached and prepared opportunistically.
Secure streaming must authenticate each AEAD chunk BEFORE providing decrypted bytes to decoder, allowing seek by chunk map.
Resume per ID and content version; subtitle/audio tracks, 9:16 Reels mode, 16:9 normal swipe and 1:1 crop preview.
Optional LibVLC only after license audit, ABI packaging and performance study; no claim every codec works on every device.
Cache and privacy checks
No decrypted thumbnail/preview stored on shared external storage; disable automatic Android backup for session secrets/cache.
No implicit FileProvider grant; explicit export/share uses ephemeral permission and cleanup.
Test with another non-root app, file explorer, crash report and device gallery to ensure no sensitive plaintext visible.
App-private sandbox is not a defense against root/OS compromise; be precise in security marketing.
12 / Files, Viewer & Basic Editors
Media / file family
Viewer & editor action
Images
JPEG/PNG/HEIC/AVIF/GIF/RAW where decoder supports; crop, rotate, exposure, filters and export.
Video
MP4/MKV/WebM/MOV where codec supported; 4K/HDR conditional; trim, crop, rotate, speed, remix/export.
Audio
MP3/AAC/FLAC/WAV/OGG where decoder supports; cut, fade, volume and output.
Subtitles
SRT/VTT/ASS conditional via player/fallback; track select, timing offset, supported subtitle editing.
Documents
PDF local render, TXT/CSV/JSON; DOCX/XLSX/PPTX via preview/conversion component or supported external handler.
Archives
ZIP/7z/RAR preview/extract where supported; encryption/password constraints and archive security checks.
Unsupported type
Safe metadata view, intentional “Open with…” and explicit access grant.

Never silently transcode on view. Edits are nondestructive until explicit Export/Replace. For cloud files, edit locally in protected workspace and re-encrypt as a NEW file version.
13 / Open-Source Code & Design References
অক্টোবর ২০২৬-এ repository README/license থেকে যাচাই করা রেফারেন্স। ট্রান্সপ্লান্টের আগে repository-specific licenses, dependencies, attribution, trademark এবং current commit পুনরায় যাচাই করতে হবে।
Project
Purpose
License
Use decision
Aves Gallery
Gallery ideas / metadata / navigation
BSD-3-Clause
Design patterns; Flutter code directly Kotlin-এ কপি নয়
Ente
E2EE photos UX & sync ideas
AGPL-3.0
Reference-only by default; code reuse needs AGPL compliance
Immich
Timeline & backup UX
AGPL-3.0
Reference-only; not a Telegram client
Fossify Gallery
Local album features
GPL-3.0
UI research; GPL compliance for reused code
Material Files
File browser interaction
GPL-3.0
Design reference; avoid direct source copy
Amaze File Manager
Advanced file actions
GPL-3.0
Behavior reference; GPL compliance if integrating
Photok
Vault privacy patterns
Apache-2.0
Review code/attribution; adapt only audited components
TDLib
Telegram authentication + transport
Boost-1.0
Core dependency; respect Telegram API terms
AndroidX Media3
Playback + transformer
Apache-2.0
Core playback/editing dependency
Coil
Image decode/load
Apache-2.0
Core image loading; custom secure fetcher
Tink Java
AEAD crypto
Apache-2.0
Candidate vetted encryption library
libsodium
Argon2id KDF
ISC
Candidate native KDF; architecture review
SQLCipher Android
Encrypted local SQLite
BSD-style (core)
Candidate encrypted local DB
libVLC
Codec fallback
LGPL (LibVLC engine)
Optional; verify binding/packaging obligations

Verified repository links (clickable)
Aves Gallery  •  BSD-3-Clause  →  https://github.com/deckerst/aves
Ente  •  AGPL-3.0  →  https://github.com/ente/ente
Immich  •  AGPL-3.0  →  https://github.com/immich-app/immich
Fossify Gallery  •  GPL-3.0  →  https://github.com/FossifyOrg/Gallery
Material Files  •  GPL-3.0  →  https://github.com/zhanghai/MaterialFiles
Amaze File Manager  •  GPL-3.0  →  https://github.com/TeamAmaze/AmazeFileManager
Photok  •  Apache-2.0  →  https://github.com/leonlatsch/Photok
TDLib  •  Boost-1.0  →  https://github.com/tdlib/td
AndroidX Media3  •  Apache-2.0  →  https://github.com/androidx/media
Coil  •  Apache-2.0  →  https://github.com/coil-kt/coil
Tink Java  •  Apache-2.0  →  https://github.com/tink-crypto/tink-java
libsodium  •  ISC  →  https://github.com/jedisct1/libsodium
SQLCipher Android  •  BSD-style (core)  →  https://github.com/sqlcipher/sqlcipher-android
libVLC  •  LGPL (LibVLC engine)  →  https://github.com/videolan/vlc-android
14 / License & Compliance Strategy
Prefer clean-room original Compose implementations; copying a screen layout concept differs from copying logos/assets/source code.
Photok Apache-2.0, Aves BSD, TDLib Boost and Media3 Apache still require applicable notices where reused.
GPL/AGPL modules are NOT declared “safe to copy into proprietary closed-source app”; seek legal review and comply with obligations.
LibVLC engine license differs from full VLC Android application license; review each component and native binary distribution.
Keep LICENSES/NOTICE, dependency SBOM, third-party attributions, build provenance and reproducible version pins.
Telegram API Terms require appropriate consent, ownership of API ID, no spamming/abuse, and truthful service description.

15 / AI Agent Delivery System
Codex/AI agents’ কাজ একটি controlled Git repository-তে ছোট, testable tasks হিসেবে ভাগ করা হবে। Security-sensitive tasks merge করতে independent human review বাধ্যতামূলক। কোনো agent private credentials/OTP পাবে না।
Agent role
Input
Output / check
Lead/Architect
এই spec + repo
modules, ADRs, conventions; scope guard
UI/UX Agent
attached moodboards
Compose components, dark/light, screenshots, accessibility
Telegram Agent
TDLib docs, API constraints
login state machine, chunk transport adapter, fake implementation
Crypto Agent
threat model
versioned envelopes, cross-device vectors, fail-closed decrypt API
Backup Agent
folder requirements
watcher, WorkManager jobs, idempotency, safe deletion
Media Agent
viewers/player spec
Media3 player, track selection, prefetch and editor
QA Agent
acceptance matrix
unit/instrumented/fault-injection tests, bug reports
Security Reviewer
PR diff + SBOM
threat review, unsafe log/cache/export findings; block on critical

Agent rules (mandatory)
01  Read specification and existing files before editing. Inventory current architecture; preserve working functionality.
02  Propose a narrow implementation task and acceptance criteria; implement on a feature branch.
03  Do not invent SDK/API methods, fake test results, fabricated encryption claims, or hardcode OTP/Telegram credentials.
04  Never add production secrets to Git. Use ignored local.properties/CI secrets; key material never in app logs.
05  Use pinned Gradle versions after compatibility review; commit Gradle wrapper, not the huge Android SDK directory.
06  Run formatting, static analysis, unit tests, instrumentation where possible, release lint, dependency scan and screenshot diff.
07  Submit PR summary + screenshots + tests + security/license notes; review before merge.
08  No signed release APK or production publish until explicit approval, independent security gate and verified restore test.
16 / Milestone Plan, Gates & Acceptance
Phase
Deliverables
Exit criteria
P0 Discovery
TDLib/auth viability, Telegram-policy checklist, license inventory, threat model
No blocker ignored; ADR approved
P1 Project Bootstrap
Kotlin Compose project, theme, navigation, fake-data UI
Build/test green, 4 tabs + settings
P2 Auth & Recovery
TDLib login, passphrase, key wrapping, recovery export
Cross-device restore of mock manifest
P3 Core Backup
Encrypted chunks, manifests, folder switches, retries
Interrupt/restart, restore hash match
P4 Gallery & Files
Cloud timeline, local albums, SAF manager, preview
Multi-select, permissions and no plaintext leak
P5 Media & Prefetch
Media3 streaming, adjacent preload, video resume, edit
Seek + subtitle/track QA, cache bounds
P6 Vault & Hardening
Biometric, FLAG_SECURE, separate keys, secure cache
Leak tests, threat checklist, full wipe
P7 Beta & Release
E2E tests, SBOM, accessibility, manual device matrix
Security review + product approval

Test acceptance: minimum required scenarios
Restore photos, arbitrary binary, 10GB+ files (subject to Telegram feasibility), filenames with Unicode, nested folders, empty files.
Kill app/power off halfway through chunk upload; resume without leaking/unrecoverable phantom state.
Corrupt one ciphertext byte; restore must FAIL authentication, not silently render partial content.
Change password and recover on second device; losing BOTH password & recovery key must not create a secret backdoor.
Revoke folder permission / remove SD card / Telegram FloodWait; show actionable status, no file loss.
On gallery swipe, bounded prefetch; on 4K video and subtitles, graceful fallback; low-memory behavior tested.
App-private encrypted thumbnails inaccessible via generic media scanner; Vault no screenshots on supported Android devices.
Backup+Delete only after independently verified restore and confirmation; no auto-delete from an incomplete job.
17 / Execution prompts for Codex
Master instruction (copy & paste)
You are the lead engineer for TG Photos, an Android-first Telegram-backed private cloud gallery. Read the provided TG Photos Master Plan and the two UI moodboards before writing code. Implement Kotlin + Jetpack Compose Material 3 dark/light/system dynamic color, bottom tabs Photos (Cloud), Albums, Files, Private Vault, and settings in the top overflow menu. Use TDLib for approved Telegram authorization and ciphertext transport. No cloud plaintext; design versioned authenticated per-chunk encryption, encrypted manifest/thumbnail/cache, separate Vault keys, Argon2id password unlock and offline recovery. Add folder-level automatic backup via MediaStore/SAF + WorkManager, verified restore before offering backup-and-delete, smart adjacent photo prefetch, and Media3 secure video streaming with audio/subtitle tracks and resume. Build clean-room Compose UI inspired by but not copying GPL/AGPL sources. Do NOT assume unlimited Telegram capacity or claim audited zero knowledge. Start by auditing the existing repository and writing PLAN.md, ADRs, task list, module graph, risk list and tests. Implement only Phase P1 first, with mocked TDLib/crypto interfaces, and pause before irreversible operations or signing release builds. Never hardcode credentials or leak sensitive data. Provide a PR-ready diff, test logs and screenshots.

Iteration prompt template
Implement milestone P{N} from the Master Plan only. Review existing code. List changed files, dependencies, acceptance tests, security implications, UX screenshots, known limitations and commands needed. Do not expand scope or claim completion before tests pass.

Security review prompt
Review the diff as a defensive Android security engineer. Focus on key/nonce reuse, unauthenticated streaming, plaintext cache, file URI leaks, unsafe delete workflows, crash logs, backup exclusions, path traversal, Telegram policy, dependency licenses and restore correctness. Produce findings with severity, exploit preconditions, evidence and smallest safe fix.

18 / Developer Setup & Useful Commands
Cloud Codex environment needs a compatible JDK, Gradle Wrapper, Android command-line SDK/platform/build-tools and optionally Android NDK/CMake for TDLib JNI. Exact version numbers should be pinned after compatibility testing in gradle/libs.versions.toml.
git clone <YOUR_PRIVATE_REPO_URL>cd TGPhotos./gradlew tasks./gradlew :app:compileDebugKotlin./gradlew testDebugUnitTest./gradlew lintDebug

On Windows use gradlew.bat. APK path appears in app/build/outputs/apk/debug/ only after a successful build. No build has been performed as part of this planning document.
Repository structure to create
TGPhotos/  app/  feature/{auth,photos,albums,files,vault}/  core/{telegram,crypto,backup,media,cache,database,common}/  docs/{MASTER_PLAN,THREAT_MODEL,ADRs,LICENSING,TEST_MATRIX}/  ui-reference/ (the two concept boards)  .github/workflows/  gradle/  AGENTS.md  README.md

19 / API Docs & Developer Reference Links
Telegram API ID / registration  —  https://core.telegram.org/api/obtaining_api_id
Telegram API Terms  —  https://core.telegram.org/api/terms
Official TDLib getting started  —  https://core.telegram.org/tdlib/getting-started
Material 3 for Compose  —  https://developer.android.com/jetpack/androidx/releases/compose-material3
Media3 preload manager  —  https://developer.android.com/media/media3/exoplayer/preloading-media/preloadmanager/create
Media3 Transformer  —  https://developer.android.com/media/media3/transformer/transformations
WorkManager  —  https://developer.android.com/develop/background-work/background-tasks/persistent
Android FLAG_SECURE  —  https://developer.android.com/security/fraud-prevention/activities
Android app-private file storage  —  https://developer.android.com/training/data-storage
Tink Java repository  —  https://github.com/tink-crypto/tink-java
libsodium repository  —  https://github.com/jedisct1/libsodium
END OF MASTER PLAN  |  This is a blueprint, not a finished app or audited cryptographic implementation.
