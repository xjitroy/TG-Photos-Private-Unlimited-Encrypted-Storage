# ADR 0003 — Random roots, passphrase wrapping and offline recovery

Proposed security decision: random 256-bit root, Argon2id unlock key, AES-256-GCM wraps/chunks,
HKDF domain separation, fresh file/version keys, separate vault hierarchy and offline recovery wrap.
Keystore protects local convenience keys; never make a device-bound key the sole cloud recovery mechanism.
Versioned authenticated manifests are committed last; corruption fails closed before decode/write.
This is a design, not an implemented/reviewed cryptographic protocol. Backend selection, parameters,
associated-data encoding, key lifecycle and native binary provenance require independent security review.
