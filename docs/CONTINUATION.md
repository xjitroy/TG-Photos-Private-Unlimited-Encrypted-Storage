# Latest TG Photos development

Main retains the historical Phase 1 foundation. Switch to `feature/phase-2-security-foundation` before continuing implementation and read its AGENTS.md, docs/HANDOFF.md, PROGRESS.md, PRODUCTION_READINESS.md, REQUIREMENTS.md and MASTER_PLAN_RECONCILIATION.md. Security source remains unmerged pending independent human review.

The owner authorized eventual APK creation/delivery on 2026-10-10 after requirements/design and production gates. Play publication is not authorized. App branding remains TG Photos. No production APK has been delivered; real login/backup/gallery/player/Vault integration remains incomplete.

At this update the feature head is b7befe232f57dbeb31671fbf3432ee8d0ade565b; fetch the actual branch head because later updates may exist. Android compilation, Room KSP, 22 JVM tests and Debug/Release lint passed for prior named sources. Both official TDLib/OpenSSL ABI builds pass in native run 38071501341. Four isolated Android 35 Keystore tests pass in run 38072657836. Native load/UTF-8/bounds pass in run 38073154300, but its immediate-reownership test exposed a shutdown race; corrected source is awaiting runs 38074390463 and 38074390506. Never report the failed overall runtime run or a pending fix as passing acceptance.

The source now includes public-dependency download caching and complete-range CI classification; documentation-only updates retain privacy/source/ZIP checks without Android recompilation. Read actual latest run results, retained evidence and the six remaining production phases on the feature branch.

Original ZIP/PDF/DOCX/Markdown and all 14 design references are preserved. Selected public source/license snapshots from all 14 reference repositories are reproducible using the feature-branch manifest. Private API registration/screenshots, credentials, OTPs, passphrases, signing/encryption keys and user files are excluded from source/archive; a clone does not contain private configuration.
