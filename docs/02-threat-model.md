# Threat Model

## Scope

This model covers the Next.js UI and proxy, Spring Boot API, Keycloak
integration, PostgreSQL data, local document storage, and GitHub Actions
security CI. It describes the current local demo, not a production risk
assessment.

## Assets and Actors

**Assets:** access tokens, project data, document content and metadata, audit
records, database credentials, and CI integrity.

**Actors:** project owners, administrators, auditors, unauthenticated users,
authenticated users attempting cross-tenant access, and attackers submitting
malformed input or files.

## Key Threats

| Threat | Existing mitigation | Residual risk / gap |
| --- | --- | --- |
| Unauthenticated API access | Spring Security requires a valid JWT except for health endpoints | Token revocation and identity policy depend on local Keycloak configuration |
| User reads or changes another user's project | Ownership checks run in backend services; administrators are explicitly exempted | Authorization correctness depends on every new object path using the same pattern |
| User accesses another project's document | The backend checks project content access and binds document lookup to both project and document IDs | No automated end-to-end browser test covers the complete flow |
| Auditor downloads file content | Method roles exclude `AUDITOR`; auditors receive metadata-only project access | Audit metadata includes user identifiers and filenames and remains sensitive |
| Malicious or oversized upload | 5 MB application limit, extension allowlist, filename validation, randomized storage names, normalized path containment | No file-signature validation, content disarm, malware scan, or storage quota |
| Path traversal through filenames | Client paths are stripped; generated stored names are resolved and checked under the storage root | Host-volume permissions and isolation are local-demo defaults |
| Injection through project input | Bean Validation limits fields; JPA repositories provide parameterized persistence | No dedicated DAST or injection test suite |
| Token theft in browser | Authorization Code Flow with S256 PKCE; adapter keeps tokens in memory rather than `localStorage` | Any successful browser script injection could still use an in-memory session |
| Audit evasion or log disclosure | Security events are stored in PostgreSQL; audit endpoint is limited to `ADMIN` and `AUDITOR`; correlation IDs are validated | No immutable log sink, alerting, retention policy, or audit-export protection |
| Dependency or committed-secret risk | Locked npm install, Maven tests, Trivy, Gitleaks, and actionlint-validated workflow | CI is point-in-time scanning and does not prove absence of all vulnerable or malicious dependencies |
| Denial of service or automation abuse | Multipart limits constrain individual uploads | No rate limiting, request quotas, global storage quota, or abuse detection |
| CI workflow compromise | Read-only repository permissions and no deployment or application secrets | Third-party actions are version-tagged rather than pinned to commit SHAs |

## Abuse Cases to Re-Test

1. Use one user's token with another user's project UUID.
2. Pair a valid project UUID with a document UUID from another project.
3. Attempt project mutation and document download as `AUDITOR`.
4. Upload empty, oversized, disallowed-extension, and path-like filenames.
5. Call administrative audit endpoints as `USER` and without a token.
6. Send missing, malformed, and valid `X-Correlation-ID` values.

## Out of Scope

Production infrastructure, TLS termination, Keycloak hardening, secrets
management, host security, backups, disaster recovery, privacy compliance,
penetration testing, and formal risk acceptance are not implemented here.

