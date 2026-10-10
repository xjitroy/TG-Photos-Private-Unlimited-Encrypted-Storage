# TG Photos implementation plan

The PDF, DOCX, embedded 14 PNGs and original Markdown prompt were reviewed on 2026-10-10.
No existing Android repository was present. The owner authorizes development and local Git commits;
The owner subsequently authorized eventual APK creation/delivery after production gates. Play publishing
remains unapproved. The latest naming and privacy rules prevail.

| Phase | Work | Exit gate | Current status |
|---|---|---|---|
| P0 Discovery | source inventory, licenses, Telegram policy, threat model, module ADRs | feasibility/risks documented; security decisions reviewed | documents created; source-level licenses checked; transport feasibility pending |
| P1 Foundation | modular Kotlin Compose, Material Expressive, four tabs, overflow Settings, login/UI previews, themes | compile + lint + JVM tests + device/accessibility review | Android compilation/lint/JVM checks pass; emulator UI/accessibility acceptance pending |
| P2 Auth/recovery | official TDLib state updates, phone/code/email/2FA, protected sessions, Argon2id, AES key wrap, recovery export | cross-device encrypted manifest restore; wrong credentials fail | compiled/tested crypto/session drafts and both native ABI builds; live UI/root/recovery wiring pending |
| P3 Backup | ciphertext chunks, manifest-last commit, folder policies, WorkManager retries/checkpoints, verification | interrupted upload/resume, independent restore hash match, safe deletion consent | coordinator protocol fault tests pass; durable outbox/checkpoints and scheduling pending |
| P4 Gallery/files | MediaStore + SAF consent, real timeline/albums/search/multiselect, file operations/previews/archives | permission revocation, no plaintext leaks or file loss | pending; UI previews only |
| P5 Media | authenticated Media3 source, layouts, tracks/subtitles/resume, bounded prefetch, nondestructive editors | seek/corruption/device codec/cache tests | authenticated cache/range/DataSource drafts compiled/fault-tested; player/editor wiring pending |
| P6 Vault/hardening | separate keys/auth, biometrics, encrypted previews, background lock, recents protection, wipe | independent leak/threat tests | pending; locked UI + FLAG_SECURE source only |
| P7 Beta/release | resolved SBOM, accessibility, performance, E2E/device matrix, audit, branding | owner approval + audit + verified restore | pending; eventual APK authorized; no production APK/publication |

Next implementation sequence: finish Android Keystore/JNI runtime and preview acceptance; review
secure API provisioning and session/root/recovery persistence; connect the official TDLib adapter with an
owner-controlled API registration and the ABI-pinned native build. Do not ask the owner to disclose an OTP
or passphrase. Add reviewed crypto before any real backup or session-dependent UI success.

Original references may guide UX behavior. GPL/AGPL app source is reference-only. Candidate permissive
components still need version-specific source/dependency/privacy review before reuse.
No third-party backend, analytics or credential-sharing integration is permitted.
