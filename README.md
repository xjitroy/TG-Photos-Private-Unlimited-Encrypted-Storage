# TG Photos

Native Android project for private Telegram-backed photo, video and file backup.
Official application name: **TG Photos**. Proposed new application ID: `app.tgphotos.android`.
Play working title: **TG Photos: Private Backup**. Independent project, not affiliated with Telegram.

## Current delivery

Phase 1 source foundation: Kotlin/Compose, Material 3 Expressive theme, dynamic colors,
Light/Dark/System preferences, launch splash/icon, four bottom tabs and overflow Settings.
Login screen previews separately show phone/country, OTP, Telegram 2FA and private encryption password.
Photos include original landscape previews and swipe navigation; Albums show folder-policy previews;
Files support sample search/filter/sort/selection; Vault stays locked and applies `FLAG_SECURE`.
All sample surfaces identify themselves as design previews. Authentication and encryption are unavailable,
not simulated as successful. Room, WorkManager and Media3 modules are foundations, not connected features.

**Android/Kotlin compilation, JVM tests and Debug/Release lint pass in private GitHub CI.**
The local workspace still lacks Android SDK, Gradle distribution and Kotlin compiler.
The verified official wrapper JAR is now committed. Java compiler-module access supports offline primitive checks.
No APK/AAB was created. Phase 1's device/accessibility exit gates remain open.

## Privacy

There is no Internet/storage permission, telemetry SDK, analytics, ad SDK, crash-upload SDK,
remote media source, backend endpoint or FileProvider in the Phase 1 app.
Only non-sensitive appearance preferences are persisted. Referenced applications receive no app data.
Future approved Telegram uploads must contain authenticated encrypted content, names, manifests and thumbnails.
Telegram can still observe account and traffic metadata. Network and independent security review are mandatory
before claiming production encryption or an absence of leaks. See [privacy contract](docs/PRIVACY_CONTRACT.md).

## Read the project

- [Requirements checklist](docs/REQUIREMENTS.md)
- [Implementation plan and roadmap](PLAN.md)
- [Module architecture](docs/ARCHITECTURE.md)
- [Threat model](docs/THREAT_MODEL.md)
- [Source/license decisions](docs/LICENSING.md)
- [Current continuation and checks](docs/PROGRESS.md)
- [Next developer/agent handoff](docs/HANDOFF.md)
- [Phase 1 source report](docs/STATUS.md)
- [Full imported plan, with naming override](docs/MASTER_PLAN.md)
- [Original documents and all 14 UI reference images](docs/reference/README.md)

## Check without creating an APK

Run `python3 scripts/check_source.py` for source policy checks. On the continuation branch,
`scripts/check_crypto_offline.sh` and `scripts/check_backup_offline.sh` run the recorded 28 primitive/key-hierarchy and 13
protocol checks. These do not replace Kotlin/Android compilation or independent security testing.
The official Gradle 8.13 wrapper JAR is present; `scripts/bootstrap_wrapper.py` also verifies it against its
pinned checksum. On a prepared machine, install Android SDK platform 36/build tools
and set `sdk.dir` in ignored `local.properties`, or set `ANDROID_HOME` to your existing SDK.

```sh
./gradlew :app:compileDebugKotlin :core:backup:compileDebugKotlin :core:database:compileDebugKotlin :core:media:compileDebugKotlin
./gradlew :core:model:test :core:crypto:test
./gradlew :app:lintDebug :app:lintRelease
```

Packaging/signing/publishing tasks are blocked by default. Do not override the approval property without
explicit owner permission. Instrumentation/device screenshots require a later approved installation workflow.
Owner-authorized repository: https://github.com/xjitroy/TG-Photos-Private-Unlimited-Encrypted-Storage.
`main` contains the Phase 1 foundation; latest unmerged security work is on
`feature/phase-2-security-foundation`. See HANDOFF.md before continuing. The owner authorized eventual APK delivery after production gates; Play publication remains unapproved.
