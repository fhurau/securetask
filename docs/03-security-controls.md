# Security Controls

This inventory records controls visible in the repository. It does not claim
production readiness or certification.

## Identity and Access

| Control | Implementation |
| --- | --- |
| OAuth 2.0/OIDC login | Keycloak public client using Authorization Code Flow and S256 PKCE |
| Token storage | `keycloak-js` keeps tokens in memory; application code does not use `localStorage` |
| API authentication | Spring Boot resource server validates Keycloak-issued JWTs |
| Role mapping | Keycloak realm roles become `ROLE_*` Spring authorities |
| Function authorization | `@PreAuthorize` restricts project mutation, document content operations, and audit-log access |
| Object authorization | Project owner subject IDs are checked in backend services; `ADMIN` can access all projects |
| Auditor boundary | `AUDITOR` can read audit logs and document metadata, but cannot mutate projects or upload/download files |

Frontend route guards and hidden controls improve usability only. The Spring
Boot backend is the security enforcement point.

## Input and Document Handling

- Project names are required and limited to 100 characters; descriptions are
  limited to 1,000 characters.
- Uploads must be non-empty, no larger than 5 MB, and use `pdf`, `txt`, `png`,
  `jpg`, or `jpeg`.
- Original filenames are reduced to a basename, trimmed, length-limited, and
  rejected when they contain control characters.
- Stored filenames are random UUIDs. Normalized storage paths must remain
  beneath the configured storage root.
- Downloads use attachment content disposition and metadata selected by the
  server.
- JPA repositories and typed UUID path parameters avoid hand-built SQL.

The allowlist is extension-based. The application does not inspect file
signatures or scan content for malware.

## Audit and Traceability

- The backend records project create, view, update, delete, access-denied, and
  document upload/download/rejection events.
- Events contain actor ID/email, action, resource, result, timestamp, IP
  address, user agent, and correlation ID.
- A caller-supplied correlation ID is accepted only when it matches
  `[A-Za-z0-9._-]{1,100}`; otherwise the backend generates a UUID.
- Audit records do not include request bodies, tokens, passwords, secret keys,
  or document bytes.
- Only `ADMIN` and `AUDITOR` roles can list audit records.

## Browser and Response Headers

- Frontend and backend responses set `X-Content-Type-Options: nosniff`,
  `Referrer-Policy: same-origin`, a restrictive `Permissions-Policy`, and
  `X-Frame-Options: DENY`.
- Both surfaces set a basic Content Security Policy with same-origin defaults,
  blocked plugins, restricted base URIs, and `frame-ancestors 'none'`.
- The frontend allows connections to the configured local Keycloak origin.
- The backend allows same-origin Swagger assets and Swagger's required inline
  styles.

The policies are designed for the HTTP localhost demo. They do not use
per-response nonces or hashes and are not a production deployment policy.

## Delivery Controls

GitHub Actions runs on pushes to `main`, pull requests, and manual dispatch:

- Maven backend tests on Java 21.
- `npm ci`, ESLint, and a production Next.js build on Node.js 24.
- Gitleaks over full Git history with redacted findings.
- Trivy filesystem dependency scanning for fixable high and critical
  vulnerabilities.
- Workflow-level read-only repository permissions and no deployment job.

## Controls Not Implemented

TLS configuration, rate limiting, malware scanning, production CSP design,
encryption-at-rest design, centralized monitoring and alerting,
tamper-resistant audit storage, automated backup/restore, runtime container
scanning, DAST, SAST beyond lint/tests, and production secrets management.
