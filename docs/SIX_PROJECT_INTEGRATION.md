# TG Photos: six-project integration and verified boundaries

Owner request and repository/root-license recheck: 2026-10-11. Exact default-branch commits,
license Git blob identities and SHA-256/file sizes are in `upstream-six-projects-current.json`.
All 18 selected README/license/component files were fetched and their Git blob and SHA-256
identities verified. This is selected research, not a claim of six full repository clones.
The older 43-file original-plan reference set remains intact and verified.
`ente-io/ente` redirects to canonical `ente/ente`; do not fetch or connect an Ente account/server.
Research updates do not silently update runtime dependency/native build pins.

| Project | Verified root license | TG Photos approach | Actual integration status |
| --- | --- | --- | --- |
| Aves | BSD-3-Clause | Original native Compose timeline/albums/viewer; metadata behavior research | Sample gallery/viewer only; real media index/metadata/favorites pending |
| Ente | AGPL-3.0 | Original Kotlin/Java backup/key protocol; no Ente client/backend imported | AES-GCM/Argon2id/chunk verification and encrypted journal drafts; app backup absent |
| Material Files | GPL-3.0 | Original file navigation/actions; no GPL app source imported | Sample search/sort/multiselection; real file operations pending |
| Photok | Apache-2.0 | Original separate-key Vault, local privacy/lifecycle patterns | Locked screen/FLAG_SECURE present; actual auth/import/biometric Vault pending |
| TDLib | BSL-1.0 | Official pinned library + original native/request adapter | Two 64-bit native builds and instrumented adapter proof; app login/transport not wired |
| Media3 | Apache-2.0 | Official libraries + original authenticated DataSource | Boundary/reader draft; real player/tracks/editing/prefetch pending |

Root licenses do not substitute for component/dependency/native notice review. GPL/AGPL copying
can impose source and distribution obligations; private GitHub does not automatically waive them.
No copyleft app source/assets were incorporated. Permissive reuse still requires applicable notices,
patent/trademark conditions and component review. Original TG Photos licensing is not chosen for the owner.
No external referenced-app endpoint, analytics or account dependency is added by this work.

## New concrete backup implementation

`EncryptedUploadJournal` implements the coordinator's write-ahead prepared-chunk/checkpoint contract:

1. Encrypt and atomically persist the content fingerprint, exact encrypted chunk and random operation UUID.
2. Fsync file, rename and fsync directory before the coordinator contacts remote transport.
3. After an authoritative acknowledgement, retain the encrypted confirmed pointer in the same journal.
4. After restart/uncertain acknowledgement, read/authenticate the record and retry the same operation/ciphertext.
5. Reject changed source/key, corruption, identity substitutions, pointer replacement and quota/path faults.

Prepared state is immutable; acknowledgement cannot be created without it. All persisted bodies are
AES-256-GCM encrypted under a caller-supplied **separate journal key**. Associated data binds a distinct
journal domain plus file UUID/content version/chunk index. Directory/file modes are 0700/0600, only
opaque identifiers/lengths are externally visible. Closing the journal wipes its owned key. One owner
instance is required; caller must pass trusted app-private/no-backup storage and derive its independent
key from the cloud hierarchy. Do not use file/manifest/password keys as journal keys.

This addresses retention of chunk ciphertext/operation identity, not automatic remote exactly-once
storage. The controlled fault-test transport reconciles operation identity; actual Telegram upload
reconciliation is still missing, and an opaque filename does not deduplicate Telegram messages.
Manifest upload itself is not yet journalled, confirmed records have no approved pruning/retention
policy, and disk-full during acknowledgement can leave a remotely stored chunk without a local ack.
The prepared record still permits reconciliation. Platform/power-loss, multiprocess locking, trusted
rollback anchors, Room/WorkManager binding, key lifecycle and real TDLib integration remain open.
Maximum envelope-size chunks need a smaller journal chunk size because the encrypted journal also
contains framing/ciphertext overhead. Default 1 MiB chunks fit; quota failures occur before upload.

## Owner's background/folder instruction

Albums is the control surface for per-folder automatic backup, including Camera when explicitly enabled.
Leaving the screen/task should not cancel an authorized running transfer. Implement unique resumable
WorkManager jobs, OS-required foreground notification for long work, network/battery constraints,
permission-aware folder change scans and reconciliation. Camera is not silently opted in. Disabling
backup prevents new work and follows a clear pause/cancel policy; preferences must survive restart.
Force-stop, revoked access, OEM power controls, network loss and Android scheduling restrictions must
be surfaced honestly. Periodic WorkManager is not instant continuous folder watching. There is no
background guarantee after force-stop; future OS support requires maintenance/testing.

The owner explicitly confirmed two separate per-photo/file actions: **Backup** and **Delete from device**.
Display the action state on gallery/file items; cloud/Vault items must respect their access/key boundaries.
Backup uses the selected item and current encrypted queue policy; expose queued/uploading/verified/failure states.
Delete from device is a distinct destructive action; this requirement is not blanket deletion consent.
Device deletion always requires independent remote restore verification and explicit user confirmation.
**These background/folder UI controls are requirements, not implemented functionality.** Current Worker
remains unscheduled and fails unavailable; existing Albums switches remain explicitly labelled previews.

## Checks and continuation

Local checks: 13 backup protocol cases plus 8 real Linux filesystem/controlled-transport journal cases
pass; source/privacy policy 12, CI-classification 4 and reference checks (43 original +18 current) pass.
These are not Android/Telegram power-loss or complete security acceptance. Six previous new root JVM
tests and two previous root Android tests still need verification. The latest CI source run 38101007749
fails before runner steps; do not attribute it to an unconfirmed billing/code issue. Android compile/lint
of this new change is blocked by no local SDK and unavailable CI runner execution. Earlier 24 JVM/
6 Android/11 preview UI passes apply only to their recorded older source snapshots.

Next: restore CI execution, compile/test current root+journal source, implement authoritative Telegram
login + calibrated secure provisioning/recovery, then real durable queue/transfer reconciliation and
Albums folder policies. Review and test each integration before enabling it or claiming production readiness.
