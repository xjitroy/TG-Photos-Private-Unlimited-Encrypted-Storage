# ADR 0002 — Approved encrypted transport only

Decision: official TDLib account transport only after owner-controlled API setup and policy/feasibility review.
No backend from a reference application, telemetry, ad SDK, cloud conversion or implicit remote image loading.
No identity/fake encryption fallback. P1 has no network permission and collects no credentials.
Consequence: UI cannot claim authenticated/secured cloud behavior until P2/P3 pass review and tests.
Telegram account/traffic metadata remains visible to Telegram; no absolute anonymity claim.
