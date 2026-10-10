# ADR 0001 — Native modular Android

Decision: Kotlin, Compose, Material 3 Expressive, native Android storage/lifecycle APIs; no web wrapper/backend.
Separate features, pure domain contracts, operational cores and app composition. Phase 1 uses original preview
fixtures and unavailable integrations. New applicationId `app.tgphotos.android`; minimum SDK 26, compile/target 36.
Consequence: more module configuration but clear privacy/security boundaries. Package uniqueness/domain
ownership and actual compatibility remain to be checked before distribution.
