# TG Photos production delivery gates

Checked 2026-10-10 against the owner request and original ZIP. The owner authorizes eventual APK
creation/delivery, but does not authorize Play publication. No production APK is delivered yet.
The existing account-free QA APKs are temporary development/test packages and are removed after testing.

## Verified foundation, with limited scope

- Modular Android/Compose source, Room KSP, Debug/Release lint and 22 JVM tests pass in named CI runs.
- Offline suites: 31 crypto/key hierarchy, 13 backup protocol, 10 encrypted disk cache and 9 authenticated
  media range checks. These verify individual primitives/test fixtures, not the complete application.
- Official pinned TDLib/OpenSSL and original bounded JNI compile for arm64-v8a and x86_64. Hashes,
  ELF exports/dependencies and 16KB alignment are checked; Android native load/Unicode/bounds check passes; receiver reownership race is awaiting corrected-source runtime acceptance.
- Four isolated Android 35 Keystore provider tests pass: fresh-instance recovery, concurrent creation,
  corrupt-envelope rejection and missing-wrapper rejection without resetting the session.
- Actual preview APK manifest has no Internet/media/storage permissions and removes the exported
  Compose PreviewActivity/automatic EmojiCompat initializer. Live transport changes need a new audit.
- Original ZIP, three documents and 14 UI images are preserved/verified. All 14 upstream repositories
  have selected, pinned source/license reference downloads; none of their backends is integrated.

## Six remaining major phases

| Phase | Required concrete implementation and acceptance |
|---|---|
| P2 Login and encryption setup | Secure owner API provisioning; native lifecycle/UI binding; real authoritative phone/code/email/2FA flow; private passphrase strength/calibration; encrypted root/recovery persistence; second-device restore and logout/key lifecycle |
| P3 Backup and restore | Durable encrypted prepared outbox/checkpoints; crash-safe upload identity; official TDLib ciphertext transport/update handling; folder consent/MediaStore/SAF detection; WorkManager constraints/retries/progress; full independent restore/hash and scoped deletion consent |
| P4 Gallery and file manager | Real permitted local/cloud timeline/albums/search/favorites/selection; copy/move/rename/delete/restore; safe document/archive handling; permission/SD revocation and no-loss checks |
| P5 Playback and editing | App-wired authenticated Media3 source/player; seek/quality/ratios/swipes; audio/subtitle tracks; resume/prebuffer budgets; photo/video/audio nondestructive edits and protected export |
| P6 Private Vault | Separate authentication/biometrics/root/index; encrypted import/previews; background/time-based locking; close readers/wipe consumer keys/clear decoder and UI on lock; recents/screenshot/export leak tests |
| P7 Production validation and APK | Original-design runtime/accessibility acceptance; API/device/codec/network/fault/performance matrix; complete licenses/SBOM/advisories; independent human security review and verified recovery; private reproducible release signing; signed APK verification and owner delivery |

Several drafts help these phases but are not connected live features. The app still uses unavailable
transport/crypto adapters and labelled samples. Never turn these statuses into completed authentication,
backup, Vault, player or audited encryption claims just because compilation/tests pass.

## Time estimate and handoff

The owner was told there are six major phases left. A planning estimate for the full requested scope is
approximately 3–5 months of consistent experienced-developer work with reviewer/device access. This is
not a guaranteed calendar delivery date or a promise of unattended background work. Re-estimate after
each accepted phase using measured implementation/testing progress and actual integration blockers.

Read HANDOFF.md and PROGRESS.md, and inspect the latest feature-branch CI results before continuing.
The feature branch remains unmerged pending the independent security review required by AGENTS.md.
No credentials, OTPs, passphrases, keys, registration screenshots or user files belong in Git/artifacts.
Telegram account/traffic metadata remains observable; no zero-risk or unlimited-capacity guarantee is made.
