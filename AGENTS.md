# TG Photos engineering rules

Keep the app name TG Photos. Communicate with the owner in Bengali; use English identifiers.
Preserve applicationId app.tgphotos.android. The owner authorized eventual APK creation/delivery on
2026-10-10 after original requirements/design and production gates pass. This is not permission to
publish to Play or externally deploy. Compile, lint, JVM tests and development toward that APK are allowed.
No credentials, passphrases, OTPs or production keys in source, logs, saved state or fixtures.
On 2026-10-11 the owner explicitly requested the current APK. Delivery of an honestly labelled
development preview is authorized; this does not waive production gates or authorize Play publication.
Phase 1 is a clearly labelled design preview with unavailable transport/crypto adapters.
Never report an unavailable adapter as successful authentication, backup, restore or unlock.
Use original assets and Compose implementations. Check licenses before copying source.
On 2026-10-11 the owner explicitly requested agent-led security review instead of independent human review.
Complete and document that review plus actual checks before merging security integrations. Do not call
agent self-review an independent human or third-party audit.
Keep changes on a feature branch; document checks honestly, including blocked checks.
No device deletion until a complete independently verified restore plus explicit confirmation.
The owner authorizes source/documentation updates to xjitroy/TG-Photos-Private-Unlimited-Encrypted-Storage.
Keep it private. Exclude .private/, credentials and credential screenshots from every upload/archive.
Read docs/HANDOFF.md and docs/PROGRESS.md before resuming; security drafts remain unmerged pending documented agent review and passing checks.

Compatibility target: Android 8/API26 through Android 16/API36 phones/tablets, including the owner's
Android16 device. Future Android support requires new-SDK/device testing and maintenance, not an
unverified universal guarantee. Production native coverage still needs 32-bit ARM/x86 work.
Keep resumable source/build/test/architecture documentation and owner-requested APKs private on GitHub.
Workspace retention is temporary; GitHub plus owner backups are the durable record. Never upload
private API registration, passwords, production encryption/signing keys as ordinary source/artifacts.
