# Security Tradeoffs

SecureTask favors a small, inspectable local demo over production operational
complexity.

| Decision | Benefit | Tradeoff / production follow-up |
| --- | --- | --- |
| Keycloak public browser client with PKCE | No client secret is embedded in the frontend | Requires HTTPS, hardened redirects/origins, session policy, and production Keycloak operations |
| Tokens held in memory | Avoids persistent browser token storage | Page reloads depend on the identity session; script injection could still use the active token |
| Next.js same-origin API proxy | Avoids exposing the internal backend URL and forwards only selected headers | It is not a security gateway; the backend must enforce every authorization rule |
| Owner ID stored from JWT `sub` | Stable object ownership independent of user-supplied fields | Identity migration or realm replacement needs an ownership migration strategy |
| Explicit role plus service ownership checks | Keeps function and object authorization visible near operations | Repetition can drift as endpoints grow; broader policy tests would be needed |
| Auditor metadata access | Supports review without exposing file content | Metadata can still be sensitive and currently has no masking policy |
| Files on local storage; metadata in PostgreSQL | Simple demo setup and avoids database BLOB handling | No malware scan, encryption, object-store controls, backup, quota, or multi-instance coordination |
| Extension allowlist and server-generated filenames | Blocks obvious executable/archive types and traversal through stored names | Extensions do not prove content type; production upload pipelines need signature inspection and malware scanning |
| Database audit table | Easy to query in the application demo | Not immutable, centralized, independently retained, or connected to alerts |
| Trivy ignores unfixed findings and fails on fixable high/critical issues | Keeps CI actionable for remediable severe dependency findings | Lower severities and unfixed vulnerabilities still require periodic review |
| Version-tagged GitHub Actions | Readable and receives upstream fixes | Commit-SHA pinning would provide stronger workflow dependency integrity |
| Local-demo CSP and response headers | Blocks framing, plugins, broad referrer leakage, and unexpected content sources without breaking Keycloak or Swagger | Allows inline styles and frontend inline scripts; production should use HTTPS and deployment-specific nonce/hash policies |
| Separate direct-grant smoke client | Enables a one-command, non-interactive local security test without weakening the PKCE frontend client | Resource-owner password grants and committed demo credentials are local-only; production automation needs dedicated identities, secret management, rotation, and stronger grant policies |
| Local HTTP and committed demo users | Fast, reproducible recruiter demo | Credentials and transport are unsuitable for shared or production environments |

## Deliberately Not Claimed

The project does not claim zero vulnerabilities, OWASP compliance,
production-grade identity configuration, secure hosting, regulatory
compliance, penetration-test coverage, or a complete secure software
development lifecycle.
