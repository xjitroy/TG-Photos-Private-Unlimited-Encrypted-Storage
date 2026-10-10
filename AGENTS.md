# TG Photos engineering rules

Keep the app name TG Photos. Communicate with the owner in Bengali; use English identifiers.
Preserve applicationId app.tgphotos.android. The owner authorized eventual APK creation/delivery on
2026-10-10 after original requirements/design and production gates pass. This is not permission to
publish to Play or externally deploy. Compile, lint, JVM tests and development toward that APK are allowed.
No credentials, passphrases, OTPs or production keys in source, logs, saved state or fixtures.
Phase 1 is a clearly labelled design preview with unavailable transport/crypto adapters.
Never report an unavailable adapter as successful authentication, backup, restore or unlock.
Use original assets and Compose implementations. Check licenses before copying source.
Do not merge security-sensitive integrations without independent human security review.
Keep changes on a feature branch; document checks honestly, including blocked checks.
No device deletion until a complete independently verified restore plus explicit confirmation.
The owner authorizes source/documentation updates to xjitroy/TG-Photos-Private-Unlimited-Encrypted-Storage.
Keep it private. Exclude .private/, credentials and credential screenshots from every upload/archive.
Read docs/HANDOFF.md and docs/PROGRESS.md before resuming; security drafts remain unmerged pending review.
