# Continue from current source

Use `feature/phase-2-security-foundation`; preserve existing implementation and package ID.
Owner requests actual feature integration and a production APK, not repeated phase lists/planning.
Preview artifact 11686539050/run38099671415 is installed/debug-signed evidence only; not production.
It is private and expires 2026-11-10. Source/current APK are also in workspace; workspace is temporary.

Latest implementation: authenticated remote manifest parsing and an unlocked backup media session
now connect the encrypted upload journal/coordinator to seekable gallery/video/document reads.
Lock closes readers immediately, refuses new readers and prevents a blocked manifest fetch from
publishing after lock. The session has bounded reader admission and independent manifest/file keys.
13 protocol +8 filesystem journal checks and 9 range +9 integrated media checks pass locally.
No app login, real TDLib transfer, biometric lifecycle, background folder policy or player acceptance
is implied. Opening a manifest or partial media read never supplies full device-deletion evidence.

Continue actual integration: authoritative Telegram login, trusted first-setup/account binding,
calibrated password setup and recovery; connect durable transfer/reconciliation to folder controls;
connect media session to the Media3 player and actual gallery/files/Vault providers. Reuse existing
codec/native/cache/journal implementations. Keep both Backup and Delete from device actions;
device deletion requires complete independent restore verification and explicit confirmation.

Required blocked checks: current Android compilation/lint and root JVM/Android tests. Latest known
CI attempts fail before job steps, with no retrievable logs/annotation text. No local SDK/KVM; normal
network check cannot connect to the configured proxy. Do not weaken encryption or claim tests pass.
No production release signing identity or complete device/account/fault acceptance exists.
Document agent-led security review (not independent audit) and pass relevant checks before merging.
Keep private GitHub updated after completed changes; no Play publication. Do not upload credentials,
production signing/encryption keys, user data or screenshot registration material.
