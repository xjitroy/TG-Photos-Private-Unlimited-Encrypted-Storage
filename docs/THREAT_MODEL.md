# TG Photos threat model and security gates

Assets: plaintext media/names/EXIF, passphrases/recovery credentials, root/scoped keys, TDLib session,
vault index, file version identity, folder grants and deletion eligibility. No real assets enter P1.

| Threat | Required control | P1 status / later evidence |
|---|---|---|
| Telegram account/cloud compromise | client AEAD ciphertext + encrypted manifests, no plaintext captions | no transport; P2/P3 tests pending |
| Password guessing | Argon2id unique salt, bounded calibrated memory/time, strength UX | crypto unavailable; no password collected |
| Wrong chunk/version/order/replay | authenticate IDs, version, index, schema, total lengths/pointers; verify manifest | protocol and vectors pending |
| Truncated/tampered streaming | authenticate bounded chunk before giving decoder bytes | media contract only; corruption tests pending |
| Lost phone/session theft | Keystore local key wrap, auth timeout, app-private encrypted session/cache | backup/transfer excluded; session integration pending |
| Third-party backend/tracking | strict dependency/source review, no telemetry, traffic captures | no endpoints/permission in source; resolved/merged/runtime checks pending |
| Vault preview/screenshot leak | separate keys, lifecycle lock, secure screen/recents, encrypted previews | locked surface + FLAG_SECURE; device QA pending |
| Unsafe local deletion | full independent restore evidence + version/hash + explicit scoped-storage consent | conservative domain gate + test source; no deletion operation |
| Recovery loss/backdoor | offline recovery wrap, second-device restore, no hidden recovery access | no keys generated; implementation pending |
| Archive/path traversal/bombs | canonical bounded extraction, ratio/size/path limits, app-private staging | archives not implemented |
| Export leaks | explicit decrypt consent, ephemeral URI grants, cleanup, no implicit FileProvider | no export/provider |
| Root/OS compromise | document limitations, best-effort key lifecycle; no absolute guarantees | unavoidable boundary |

Review findings for the current source: no production crypto code, native TDLib binary, actual session,
database, cache or upload queue is enabled. Consequently neither E2EE nor secure restore has been delivered.
Use unavailable adapters rather than identity encryption or synthetic authentication.

Before P2/P3 merge: independent human review of crypto/session/transport boundaries; golden vectors,
wrong password/corrupt AEAD failure, nonce uniqueness, bounded memory, recovery on a fresh device.
Before release: full dependency/native binary review, merged manifest inspection, network capture,
local forensic/leak tests, UI/accessibility/performance/device matrix and external security audit.
