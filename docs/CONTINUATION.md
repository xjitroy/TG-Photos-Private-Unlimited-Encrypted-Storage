# Latest TG Photos development

Main retains the historical Phase 1 foundation. Switch to `feature/phase-2-security-foundation` before continuing implementation and read its AGENTS.md, docs/HANDOFF.md, PROGRESS.md, PRODUCTION_READINESS.md, REQUIREMENTS.md and MASTER_PLAN_RECONCILIATION.md. Security source remains unmerged pending independent human review.

The owner authorized eventual APK creation/delivery on 2026-10-10 after requirements/design and production gates. Play publication is not authorized. App branding remains TG Photos. No production APK has been delivered; real login/backup/gallery/player/Vault integration remains incomplete.

At this update the feature head is 3bf7faab1705bd6c3b57b113159537f259ae402e; fetch the actual branch head because later updates may exist. Source b7befe232f57dbeb31671fbf3432ee8d0ade565b passes Android compilation, Room KSP, 24 JVM tests and Debug/Release lint in run 38074390463. Both official TDLib/OpenSSL ABI builds pass in native run 38071501341. Run 38074390506 attempt 2 passes all six Android 35 provider/native tests, including the corrected immediate-reownership shutdown regression. Its overall UI run FAILED at Photos hierarchy acquisition after passing login. The harness now preserves raw dump/crash/window diagnostics with bounded retries; runs 38087302893 and 38087302897 check that update. Do not confuse partial runtime success with complete UI or production acceptance.

The source now includes public-dependency download caching and complete-range CI classification; documentation-only updates retain privacy/source/ZIP checks without Android recompilation. Read actual latest run results, retained evidence and the six remaining production phases on the feature branch.

Original ZIP/PDF/DOCX/Markdown and all 14 design references are preserved. Selected public source/license snapshots from all 14 reference repositories are reproducible using the feature-branch manifest. Private API registration/screenshots, credentials, OTPs, passphrases, signing/encryption keys and user files are excluded from source/archive; a clone does not contain private configuration.
