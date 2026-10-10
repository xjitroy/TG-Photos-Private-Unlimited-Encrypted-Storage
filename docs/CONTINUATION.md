# Latest TG Photos development

Main retains the Phase 1 foundation. Before resuming, switch to `feature/phase-2-security-foundation` and read its AGENTS.md, docs/HANDOFF.md, PROGRESS.md, REQUIREMENTS.md, MASTER_PLAN_RECONCILIATION.md and docs/evidence/android-ci.txt. Security integrations remain unmerged pending independent human review.

The owner authorized eventual APK delivery on 2026-10-10 after original requirements/design and production checks pass. Play publication is not authorized. App branding is TG Photos. No APK has been delivered; UI still contains labelled design previews.

At this update, the feature head is fab88efa38f67bb60b54d918e13394bc4794d80b. Android compilation, Room KSP, 17 JVM tests and Debug/Release lint passed for source d9490c3becbf43e0a12192084f56a8e6af609c03 in run 38067943717. Later disk-cache/email-OTP changes require new CI verification. Native workflow 38067274352 is still running. Check actual newer heads/runs before making claims.

Original master-plan/design archive and extraction instructions are tracked. Public source-reference snapshots are reproducible from the feature branch manifest; private API registration/screenshots, credentials, keys and user files are excluded from Git/archive. A fresh clone does not include private configuration.
