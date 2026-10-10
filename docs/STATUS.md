# TG Photos delivery status — 2026-10-10

This is the historical Phase 1 baseline report. Read **PROGRESS.md** and **HANDOFF.md** for continuation:
the wrapper JAR is now present and 19 Java crypto + 13 backup protocol checks pass. Kotlin/Android checks remain blocked.

## Implemented source

New project under `/workspace/TGPhotos`, feature branch `feature/phase-1-foundation`.
16 modules, pinned version catalog, Android resources/manifest/launcher/system splash and APK packaging guard.
Original Material 3 Expressive design, dynamic colors, saved Light/Dark/System choice, system-bar contrast,
four bottom destinations and top overflow Settings. Separate phone/country, OTP/Open Telegram, Telegram 2FA
and private encryption-password screen previews. Real secret entry and auth submit are disabled.

Photos sample filtering + illustration swipe viewer, Albums folder-policy preview, Files sample search/sort/
filter/selection, permanently locked Vault with FLAG_SECURE source, Settings/About/privacy/license screens.
These are clearly labeled previews, not connected personal files or completed cloud features.

Core foundations: auth state/gateway contract with unavailable adapter, unavailable crypto boundary,
encrypted cache/media contracts, Room encrypted-envelope schema, unscheduled fail-closed WorkManager worker,
independent restore-evidence deletion gate. No production encryption, session, queue, player or vault authentication.

Complete master requirements imported, checklist/roadmap/ADRs/threat/privacy/design/branding/licensing/risk/
test documents created; original source and embedded image hashes recorded. License text/repository-level
checks performed on referenced projects. No GPL/AGPL application source or protected artwork copied.

## Actual checks

- `python scripts/check_source.py`: **PASS**, 12 source policy checks. XML/TOML/module references parse;
  branding/navigation/privacy controls and no generated APK/AAB verified at source level.
- `scripts/check_android.sh`: source checks passed, then **BLOCKED**, exit 2, missing verified wrapper JAR.
- `python scripts/bootstrap_wrapper.py`: **BLOCKED**, sandbox socket permission denied; no binary written.
- `git diff --cached --check`: **PASS** after normalizing trailing blank lines in license/wrapper text snapshots.
- `sh -n gradlew`, `sh -n scripts/check_android.sh`, Python script syntax compilation: **PASS**.
- `./gradlew :app:compileDebugKotlin`: **BLOCKED**, exit 1, missing `org.gradle.wrapper.GradleWrapperMain`.
- Kotlin/Room KSP compilation, debug/release lint, 5 prepared JVM test methods: **NOT RUN**.
- Instrumentation, screenshot/device/accessibility/performance/network-leak tests: **NOT RUN**.
- Direct dependency inventory exists; resolved SBOM/transitive advisory/license/privacy audit: **NOT RUN**.

Environment has Java 21 runtime, but no javac/full JDK17, Kotlin compiler, Gradle, Android SDK, adb or emulator.
GitHub text/documentation connectors worked; sandbox shell downloads did not. Official wrapper scripts,
distribution properties/checksums and a checksum-verified bootstrap script are present; wrapper JAR is missing.
Phase 1 is source-delivered, **not build-verified or production-complete**.

## Changed file groups

- `app/`: app host/navigation/theme preferences + label/icon/splash/backup-exclusion resources.
- `feature/{auth,photos,albums,files,vault,settings}/`: original Compose preview screens.
- `core/{model,designsystem,telegram,crypto,backup,database,media,cache,testing}/`: contracts/design/
  fixtures/schema/worker/deletion safety source and relevant prepared JVM tests.
- root/`gradle/`: modular build configuration, pinned catalog, official wrapper launchers/properties.
- `scripts/`: source checks, verified wrapper bootstrap, non-packaging Android verification command.
- `docs/`, `PLAN.md`, `README.md`, `AGENTS.md`, `NOTICE`: complete handoff and license evidence.

## Next tasks

1. Provision permitted JDK17/SDK36/verified Gradle; compile every module and fix any source/API/version issue.
2. Execute JVM safety tests and lint. Phase 1 visual/device acceptance remains gated on an approved APK workflow.
3. Official TDLib integration with owner-controlled API registration; no private OTP/password given to the agent.
4. Review/implement Argon2id, authenticated key envelopes and second-device recovery before real backup.
5. Implement durable encrypted backup/restore verification, then actual gallery/files/media and separate-key vault.
6. Resolve/full-audit dependencies, inspect merged manifests and traffic; independent security review before release.

No APK/AAB, signing, Play submission, remote repository creation or push was performed.
