# TG Photos privacy contract

Owner clarification, 2026-10-10: source/design reuse must never cause personal data, application information,
credentials, files or metadata to be delivered to the source application's server or any unapproved service.

## Mandatory implementation policy

1. No analytics, advertising, tracking, crash-upload, developer diagnostics upload or hidden third-party backend.
2. Do not transplant another app's network client, telemetry initialization, account backend or remote configuration.
3. Review exact source versions, native binaries, transitive dependencies, manifests and initialization providers.
   A compatible license is not evidence of privacy. Source availability alone is not evidence of safety.
4. Only Telegram's official, owner-authorized account transport may carry backup traffic after integration.
   No bots or other app-owned accounts receive data. Telegram infrastructure is external storage, not a personal server.
5. Encrypt content, filenames, paths, EXIF, thumbnails and manifests before transport. Passwords/private keys
   are never uploaded as plaintext. Authenticated encrypted recovery envelopes may be synchronized by design.
6. Persist private previews/cache only as ciphertext. Disable OS/cloud backup and device-transfer extraction of secrets.
7. Sharing/export, folder grants, independent restore and device deletion require appropriate user consent.
8. No remote media URLs, embedded web views or online subtitle/document conversion services by default.
9. Test the merged manifest and release dependency tree; perform network capture under login, background backup,
   playback, errors, locked vault and idle states. Exercise offline behavior and identify every destination/SDK.
10. Independent security review and cross-device restore verification precede production encryption claims.

## Present evidence and limits

Phase 1 preview source has no internet/storage permissions, runtime URL, network client or telemetry SDK.
Its non-exported AndroidX startup provider retains local lifecycle/profile initialization; manifest source
removes automatic EmojiCompat font initialization. The actual merged-manifest check must confirm this.
Only theme/dynamic-color preferences are saved, using private preferences. No real user data is loaded.
The source policy checks are useful prevention checks; they do not prove the merged manifest, third-party
binaries, Android OS or final runtime has no leak. Dependency resolution/compilation now pass in GitHub CI; resolved-binary privacy analysis and device
traffic capture remain pending.

Telegram necessarily learns account identity, authorization data and some traffic metadata. Its account/file
limits, service availability and retention are outside this app's control. A compromised/rooted OS, external
camera, malicious dependency or implementation bug cannot be ruled out absolutely. Do not promise zero risk,
complete anonymity, audited zero knowledge or unlimited capacity.
