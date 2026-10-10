# Original ZIP and owner-instruction reconciliation

Checked against the original owner ZIP on 2026-10-10. Run `scripts/extract_references.py` to verify the
PDF, DOCX, Markdown and 14 embedded image hashes. Original archive SHA-256:
`12bb3d53d5c719a9fff5dfbd5dc4d66bc5570ed144521fe7bd227ca107dc1fec`.

The original 19 master-plan sections remain represented below; source/tests do not mean a production
feature is complete. Detailed per-feature implementation status is in `REQUIREMENTS.md` and `PROGRESS.md`.

| Master-plan section | Retained requirements and evidence |
|---|---|
| 01 Vision/constraints | F01–F10, S02–S05, BRANDING/PRIVACY_CONTRACT: TG Photos, truthful capacity, Telegram metadata limits |
| 02 Moodboards | F12, DESIGN, original archived DOCX with all 14 images; original runtime assets |
| 03 Onboarding | A01–A08, E01–E12: phone/OTP/2FA/email states, separate private passphrase, recovery; preview remains honest |
| 04 Gallery/file actions | G01–G07, B01–B14, M01–M08: cloud/local availability, folders, consent and queue gates |
| 05 Viewers/vault/settings | V01–V08, P01–P05, S01, F08/F13: ratios/tracks/secure screens/appearance/accessibility |
| 06 Information architecture | F09/F10, S01; exactly four tabs and overflow Settings |
| 07 Module architecture | ARCHITECTURE, 16 existing modules, Kotlin/Compose/Room/WorkManager/Media3; TDLib/JNI both ABI compile pass; live integration pending |
| 08 Telegram contract/schema | A01/A07/A08, E06/E07, B06/B10/B13, TDLib drafts; opaque ciphertext transport only |
| 09 Keys/threats | E01–E13, P02/P04, THREAT_MODEL; separate roots/HKDF drafts and tested AEAD; independent review gate |
| 10 Automatic backup/deletion | B01–B14, LocalDeletionGate/coordinator tests; no deletion or successful scheduled mock worker |
| 11 Cache/prefetch | E08, G07, V04–V06, P04/P05; authenticated disk cache/range reader drafts and fault checks; app prefetch/lifecycle pending |
| 12 Files/editors | G06, V03/V07/V08, M01–M08; document/archive safety, codecs and nondestructive editors remain pending |
| 13 Source projects | O01–O03, LICENSING/upstream records; no copied GPL/AGPL app source/backend |
| 14 Licensing | NOTICE, declared inventory; resolved SBOM/security scan and distribution obligations remain gates |
| 15 Agent workflow | AGENTS/HANDOFF; feature branch, real test logs, no secrets, no synthetic success, review before merge |
| 16 Phases/acceptance | PLAN/TEST_MATRIX; P1 exit still requires Android/device checks; later gates remain open |
| 17 Historical prompts | Retained in MASTER_PLAN; latest owner overrides take precedence; not permission to package/publish |
| 18 Setup/repository | Wrapper verified; private GitHub source uploaded; pinned source-only CI plus temporary emulator test APKs; no release APK/signing/publication |
| 19 API references | Original links retained; official TDLib/native/API references checked before drafting |

## Latest owner overrides applied

- Official app name **TG Photos**; Play working title **TG Photos: Private Backup**. The old API registration
  title/repository identifier is not a verified unlimited-storage claim or the application display name.
- Continue authorized development and source updates without repetitive permission questions.
- Upload resumable source/documents/test evidence to the owner's private repository; exclude private API
  values, registration screenshots, user files and keys. Referenced application servers receive no app data.
- The owner's latest instruction authorizes eventual APK creation/delivery after requirements/design and
  production gates pass. Play publication remains unapproved. Source-only compile/lint/test CI continues
  without packaging; a separate account-free QA workflow builds/removes temporary debug APKs. No production APK is delivered.
- Do not mark previews or tested individual primitives as completed live login, backup, vault or audited E2EE.

## Explicit detail checks still required

Advanced subtitle timing/editing, full country catalogue/E.164 library validation, cloud notification privacy,
10GB/device fault tests, SAF grant revocation, archive bomb/traversal protection, cache eviction on lock,
cross-device password/recovery/key rotation and accessibility screenshots must be implemented/verified.
These are retained requirements, not waived because the initial UI source exists.
