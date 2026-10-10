# TG Photos remaining risks and blockers

| Issue | Impact | Required next action |
|---|---|---|
| No Android SDK/Gradle/full JDK/Kotlin compiler | cannot claim compile/lint/JVM/device success | provision JDK17 + SDK36 + verified wrapper; compile without APK |
| Sandbox cannot download binaries | wrapper JAR and dependencies missing | run pinned bootstrap with permitted network on prepared development machine |
| No TDLib native artifact or API registration | real auth and cloud transport unavailable | reproducible official JNI build, own API registration, policy/feasibility review |
| Crypto protocol not implemented/audited | no live encrypted backup/restore/vault | review backend/key hierarchy/vectors before enabling storage |
| Expressive dependency is prerelease | compatibility/API/behavior risk | compile isolated UI modules, device-review, reassess production version |
| Dependencies not resolved | transitive manifest/license/advisory/telemetry behavior unknown | resolved SBOM/hashes/merged manifest/privacy review |
| Telegram quotas/limits/account availability | backup interruption or availability loss | authoritative limits, FloodWait, deleted-message/account recovery UX |
| Partial country picker in preview | not a complete international auth UI | comprehensive authoritative calling-code data and E.164 validation |
| Namespace is proposed | uniqueness/domain ownership not established | owner confirms before distribution; preserve existing installs thereafter |
| No runtime visual/security/performance checks | source appearance/security assumptions unproven | later approved installation, screenshots, accessibility and network/leak QA |
| No remote repo configured | local commit only, no PR/push | attach owner repository when available; do not publish implicitly |

No APK/AAB/signing/Play publishing is authorized. No unlimited storage or zero-risk security claim is made.
