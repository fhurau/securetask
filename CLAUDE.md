# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

SecureTask is a local portfolio app demonstrating security controls (OAuth2/OIDC via Keycloak,
object-level authorization, audit logging, controlled file upload/download, security headers) in a
full-stack system: Next.js frontend, Spring Boot backend, PostgreSQL, Keycloak. It is explicitly a
demo, not a production blueprint — see "Local-Demo Limitations" in README.md before "fixing" things
that are intentional demo simplifications (plain HTTP, committed demo credentials, local file storage).

## Local-first constraint

Every new feature must work 100% via `docker compose up` on a plain local machine — no cloud account,
no paid API key, no external service dependency required for full functionality. If a feature would
normally reach for a cloud API (e.g. email delivery, malware scanning, object storage, LLM calls),
either skip it, stub it, or use a container/local equivalent added to `docker-compose.yml`. Optional
integrations with external paid services are fine only if the feature is fully usable without them.

## Commands

Run everything (Docker Desktop required):

```powershell
Copy-Item .env.example .env
docker compose up -d --build
```

Reviewer/security smoke test (auth, object-level authz, document restrictions, audit-log access):

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1
```

Backend (from `backend/`):
- Run all tests: `mvn test`
- Run a single test class: `mvn test -Dtest=ProjectServiceTest`
- Run a single test method: `mvn test -Dtest=ProjectServiceTest#methodName`

Frontend (from `frontend/`):
- `npm run dev` / `npm run build` / `npm run start`
- `npm run lint`

CI (`.github/workflows/security-ci.yml`) runs on push/PR to `main`: Maven tests (Java 21), frontend
lint+build (Node 24), Gitleaks secret scan, Trivy fs vuln scan (HIGH/CRITICAL, fails build),
actionlint.

## Architecture

**Request path**: Browser → Next.js server route `frontend/app/api/backend/[...path]/route.ts`
(BFF proxy) → Spring Boot backend at `BACKEND_API_BASE_URL`. The frontend never talks to the backend
directly from the browser; the Next.js route forwards only `authorization`, `content-type`, and
`x-correlation-id` headers and only returns a fixed allowlist of response headers. Any change to what
gets proxied must be made in that one route file.

**Auth**: `keycloak-js` runs in the browser (`components/auth-provider.tsx`), using Authorization
Code + PKCE (S256) against Keycloak. Access tokens are kept in memory only (no `localStorage`); the
token is attached client-side and passed through the BFF proxy above. The backend independently
validates the JWT as an OAuth2 resource server (`SecurityConfig.java`) and derives Spring authorities
from the Keycloak `realm_access.roles` claim (prefixed `ROLE_`), keyed on `preferred_username`. The
backend does not trust the frontend for authorization — every protected endpoint re-checks
ownership/role server-side.

**Object-level authorization**: `ProjectAuthorizationService` is the single choke point for
"can this JWT touch this project" — `requireContentAccess` (owner or ADMIN) vs `requireMetadataAccess`
(owner, ADMIN, or AUDITOR). Both throw `AccessDeniedException` and audit-log the denial before
throwing. Any new project-scoped endpoint (including documents, since documents hang off a project)
should route through this service rather than reimplementing the owner/role check.

**Audit logging**: `AuditLogService` + `AuditRequestContext` (populated via `CorrelationIdFilter`)
record actor, action, resource type/id, outcome, and correlation id. Audit records deliberately
exclude passwords, tokens, secret keys, file content, and full request bodies — keep new audit calls
consistent with that (log identifiers/outcomes, not payloads). `AuditLogController` is read access
restricted to ADMIN/AUDITOR roles.

**Documents**: `DocumentService`/`DocumentStorageService` enforce a 5 MB limit, an extension
allowlist (`.txt`, `.pdf`, `.png`, `.jpg`, `.jpeg`), randomized storage filenames, and path-traversal
checks before touching the filesystem (`DOCUMENT_STORAGE_PATH`). Document access follows the same
project-ownership rules as `ProjectAuthorizationService`.

**Backend module layout** (`backend/src/main/java/com/securetask/backend/`): package-per-domain
(`project`, `document`, `audit`, `config`, `api`), each with its own Controller/Service/Repository —
follow this convention for new domains rather than layering by technical type.

**DB migrations**: Flyway, `backend/src/main/resources/db/migration/V{n}__description.sql`, applied
to the `securetask` schema. `ddl-auto: validate` — schema changes must go through a new Flyway
migration, JPA will not auto-generate schema.

**Frontend structure**: Next.js App Router (`frontend/app/`) with route-colocated pages; shared
logic in `frontend/components/`, `frontend/hooks/` (`use-api.ts`, `use-projects.ts`), and
`frontend/lib/api.ts` (the `apiRequest` helper used by `auth-provider.tsx` and elsewhere to call the
BFF proxy with the bearer token attached).

**Security headers/CSP**: defined once in `SecurityConfig.java` for the backend. The CSP is
localhost-scoped on purpose (see README "Local-Demo Limitations") — don't harden it to
production-grade without being asked, that's a documented, deliberate gap for this demo.

## Docs

Design detail lives in `docs/`: `01-architecture.md`, `02-threat-model.md`,
`03-security-controls.md`, `04-owasp-mapping.md`, `05-audit-logging.md`, `06-security-tradeoffs.md`,
`07-demo-scenarios.md`. Check these before changing security-relevant behavior — many "gaps" are
intentional and documented as tradeoffs.
