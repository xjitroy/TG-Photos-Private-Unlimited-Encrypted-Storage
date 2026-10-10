# TG Photos implementation plan

The PDF, DOCX, embedded 14 PNGs and original Markdown prompt were reviewed on 2026-10-10.
No existing Android repository was present. The owner authorizes development and local Git commits;
APK creation and Play publishing require separate explicit approval. The latest naming and privacy rules prevail.

| Phase | Work | Exit gate | Current status |
|---|---|---|---|
| P0 Discovery | source inventory, licenses, Telegram policy, threat model, module ADRs | feasibility/risks documented; security decisions reviewed | documents created; source-level licenses checked; transport feasibility pending |
| P1 Foundation | modular Kotlin Compose, Material Expressive, four tabs, overflow Settings, login/UI previews, themes | compile + lint + JVM tests + device/accessibility review | source implemented; source policy checks pass; toolchain/device gates blocked |
| P2 Auth/recovery | official TDLib state updates, phone/code/email/2FA, protected sessions, Argon2id, AES key wrap, recovery export | cross-device encrypted manifest restore; wrong credentials fail | pending |
| P3 Backup | ciphertext chunks, manifest-last commit, folder policies, WorkManager retries/checkpoints, verification | interrupted upload/resume, independent restore hash match, safe deletion consent | pending; fail-closed worker foundation only |
| P4 Gallery/files | MediaStore + SAF consent, real timeline/albums/search/multiselect, file operations/previews/archives | permission revocation, no plaintext leaks or file loss | pending; UI previews only |
| P5 Media | authenticated Media3 source, layouts, tracks/subtitles/resume, bounded prefetch, nondestructive editors | seek/corruption/device codec/cache tests | pending; contracts/dependencies only |
| P6 Vault/hardening | separate keys/auth, biometrics, encrypted previews, background lock, recents protection, wipe | independent leak/threat tests | pending; locked UI + FLAG_SECURE source only |
| P7 Beta/release | resolved SBOM, accessibility, performance, E2E/device matrix, audit, branding | owner approval + audit + verified restore | pending; no APK/publication authorized |

Next implementation sequence: provision/check toolchain without packaging; compile every module; repair
compatibility issues; execute JVM safety tests/lint. Then implement the official TDLib adapter with an
owner-controlled API registration and an ABI-pinned native build. Do not ask the owner to disclose an OTP
or passphrase. Add reviewed crypto before any real backup or session-dependent UI success.

Original references may guide UX behavior. GPL/AGPL app source is reference-only. Candidate permissive
components still need version-specific source/dependency/privacy review before reuse.
No third-party backend, analytics or credential-sharing integration is permitted.
