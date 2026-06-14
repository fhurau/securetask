# SecureTask

SecureTask is a portfolio project for demonstrating a secure full-stack
application built with:

- Spring Boot
- Next.js
- PostgreSQL
- Keycloak

The current local demo includes authentication, Project CRUD, object-level
authorization, audit logging, controlled document handling, and security CI.

## Documentation

- [MVP project plan](docs/00-project-plan.md)
- [Architecture](docs/01-architecture.md)
- [Threat model](docs/02-threat-model.md)
- [Security controls](docs/03-security-controls.md)
- [OWASP Top 10 mappings](docs/04-owasp-mapping.md)
- [Audit logging](docs/05-audit-logging.md)
- [Security tradeoffs](docs/06-security-tradeoffs.md)
- [AppSec demo scenarios](docs/07-demo-scenarios.md)

## Repository Layout

```text
backend/          Spring Boot resource server
frontend/         Next.js TypeScript application
infra/keycloak/   Keycloak realm import files
docs/             Project documentation
```

## Quick Start

1. Copy `.env.example` to `.env` and replace the local infrastructure
   passwords.
2. Start PostgreSQL, Keycloak, the backend, and the frontend:

   ```shell
   docker compose up -d --build
   ```

3. Open Keycloak at `http://localhost:<KEYCLOAK_PORT>`. The default example
   uses port `8081`.
4. Open SecureTask at `http://localhost:3000`.
5. Check the public backend endpoint:

   ```shell
   curl http://localhost:8080/api/v1/health
   ```

The `securetask` realm, its roles, the `securetask-frontend` client, and demo
users are imported automatically when Keycloak starts. Keycloak does not
overwrite an existing realm with the same name.

## Reviewer Smoke Test

With Docker Compose services already running, execute:

```powershell
docker compose up -d --build
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1
```

The script checks service reachability, protected endpoint authentication,
frontend-client direct-grant rejection, object-level authorization, allowed
and rejected uploads, downloads, and audit-log roles. It creates temporary
data and removes the project, local upload bytes, and temporary files even
when a check fails.

URLs, realm, client IDs, demo users, passwords, and upload storage path can be
overridden with PowerShell parameters. The defaults match this repository's
local Docker Compose environment. The script never prints tokens or passwords.

The `securetask-smoke-test` Keycloak client exists only for this local
non-interactive test. It is separate from `securetask-frontend`; the frontend
client remains Authorization Code Flow with S256 PKCE and direct access grants
disabled.

If the `securetask` realm already existed before the smoke client was added,
Keycloak will skip the updated import. Recreate the local demo realm/database
before running the script if the smoke client is missing. This removes local
demo data, so do it only when that reset is acceptable.

## Security CI

GitHub Actions runs the workflow in
`.github/workflows/security-ci.yml` for pushes to `main`, pull requests, and
manual runs. It uses only free, public tooling and read-only repository
permissions:

- Maven runs the backend test suite on Java 21.
- npm installs the locked frontend dependencies, then runs lint and the
  production build on Node.js 24.
- Gitleaks scans the complete Git history for committed secrets and redacts
  detected values from its output.
- Trivy scans the repository filesystem for high and critical dependency
  vulnerabilities with available fixes.

Any failed test, build, secret finding, or qualifying Trivy finding fails its
job. The workflow does not deploy the application and does not require paid
services or application secrets.

## Frontend

The frontend runs at `http://localhost:3000`. It uses the public
`securetask-frontend` Keycloak client with Authorization Code Flow and PKCE.
Access tokens are held in memory by the Keycloak adapter and are not stored in
`localStorage`.

To run the frontend outside Docker:

```shell
cd frontend
copy .env.local.example .env.local
npm install
npm run dev
```

The default local settings use:

| Variable | Default |
| --- | --- |
| `NEXT_PUBLIC_KEYCLOAK_URL` | `http://localhost:8081` |
| `NEXT_PUBLIC_KEYCLOAK_REALM` | `securetask` |
| `NEXT_PUBLIC_KEYCLOAK_CLIENT_ID` | `securetask-frontend` |
| `NEXT_PUBLIC_API_PROXY_PATH` | `/api/backend` |
| `BACKEND_API_BASE_URL` | `http://localhost:8080/api/v1` |

`NEXT_PUBLIC_*` values contain public URLs and identifiers only. The
server-side `BACKEND_API_BASE_URL` points the Next.js proxy to the backend; in
Docker Compose it uses `http://backend:8080/api/v1`. No client secret is
required or included because the Keycloak client is public.

### Demo Flow

1. Open `http://localhost:3000` and sign in through Keycloak.
2. Use `user1@example.com` / `User123!` to create a project, edit it, upload an
   allowed document, and download it.
3. Sign out and use `user2@example.com` / `User123!`. A direct request for the
   first user's project or document is rejected by the backend and the
   frontend shows the Forbidden page.
4. Sign in as `auditor@example.com` / `Auditor123!` to open Audit logs.
   Auditors can view audit data and document metadata made accessible by the
   backend, but upload and download controls are not shown.
5. Sign in as `admin@example.com` / `Admin123!` to view all projects, manage
   documents, and inspect audit logs.

## Keycloak Login

Select the `securetask` realm before signing in. The demo accounts are:

| Username | Password | Role |
| --- | --- | --- |
| `admin@example.com` | `Admin123!` | `ADMIN` |
| `manager@example.com` | `Manager123!` | `MANAGER` |
| `user1@example.com` | `User123!` | `USER` |
| `user2@example.com` | `User123!` | `USER` |
| `auditor@example.com` | `Auditor123!` | `AUDITOR` |

These credentials are intentionally committed for local demonstration only.
Do not reuse them in shared or production environments.

The `securetask-frontend` client is configured as a public OpenID Connect
client for `http://localhost:3000`, with Authorization Code Flow and PKCE.

## Backend

The backend uses Java 21, Spring Boot 3, and Maven. It runs at
`http://localhost:8080` and exposes:

| Endpoint | Access | Description |
| --- | --- | --- |
| `GET /api/v1/health` | Public | Returns `{"status":"UP"}` |
| `GET /api/v1/me` | Bearer token | Returns the JWT username, email, and realm roles |
| `POST /api/v1/projects` | `USER`, `ADMIN` | Creates a project |
| `GET /api/v1/projects` | Authenticated | Lists accessible projects |
| `GET /api/v1/projects/{id}` | Authenticated | Gets an accessible project |
| `PUT /api/v1/projects/{id}` | `USER`, `ADMIN` | Updates an accessible project |
| `DELETE /api/v1/projects/{id}` | `USER`, `ADMIN` | Deletes an accessible project |
| `POST /api/v1/projects/{id}/documents` | `USER`, `ADMIN` | Uploads a document |
| `GET /api/v1/projects/{id}/documents` | Authenticated | Lists accessible document metadata |
| `GET /api/v1/projects/{id}/documents/{documentId}` | `USER`, `ADMIN` | Downloads accessible file content |
| `GET /api/v1/audit-logs` | `ADMIN`, `AUDITOR` | Lists audit events newest first |

Swagger UI is available locally at
`http://localhost:8080/swagger-ui.html`. The generated OpenAPI document is
available at `http://localhost:8080/v3/api-docs`. Documentation endpoints are
public for local review; protected API operations still require a Keycloak
access token through Swagger UI's **Authorize** button.

## Security Headers

The Next.js frontend and Spring Boot backend send a local-demo header baseline:

- `X-Content-Type-Options: nosniff`
- `Referrer-Policy: same-origin`
- `Permissions-Policy: camera=(), microphone=(), geolocation=()`
- `X-Frame-Options: DENY`
- Content Security Policy restricting content to expected local sources

The frontend CSP permits browser connections to the configured Keycloak
origin, which defaults to `http://localhost:8081`. The backend CSP permits
same-origin Swagger UI assets and inline styles required by Swagger UI.

These policies are deliberately compatible with the HTTP-based local demo.
They are not a production CSP or a substitute for HTTPS, nonce/hash-based
script policies, deployment-specific origins, or reverse-proxy hardening.

To run the backend outside Docker, start Keycloak first and then run:

```shell
cd backend
mvn spring-boot:run
```

The default local configuration validates tokens issued by
`http://localhost:8081/realms/securetask`. Configuration can be overridden
without changing source files:

```shell
SECURITY_JWT_ISSUER_URI=http://localhost:8081/realms/securetask
SECURITY_JWT_JWK_SET_URI=http://localhost:8081/realms/securetask/protocol/openid-connect/certs
```

Call the authenticated endpoint with an access token obtained through the
`securetask-frontend` authorization flow:

```shell
curl -H "Authorization: Bearer <access-token>" http://localhost:8080/api/v1/me
```

## Project API

Projects are stored in the `securetask` PostgreSQL schema. Flyway creates and
updates the schema automatically when the backend starts.

Users can create projects and can read, update, or delete only projects they
own. Administrators can access all projects. Auditors have read-only access,
but ownership rules still apply.

Set an access token in your shell before calling the examples:

```shell
ACCESS_TOKEN=<access-token>
```

Create a project:

```shell
curl -X POST http://localhost:8080/api/v1/projects \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Portfolio launch","description":"Prepare the SecureTask demo"}'
```

List accessible projects:

```shell
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/api/v1/projects
```

Get, update, or delete one project:

```shell
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/api/v1/projects/<project-id>

curl -X PUT http://localhost:8080/api/v1/projects/<project-id> \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Updated name","description":"Updated description"}'

curl -X DELETE \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/api/v1/projects/<project-id>
```

Project names are required and limited to 100 characters. Descriptions are
optional and limited to 1000 characters.

## Audit Logs

The backend writes audit records for:

- `PROJECT_CREATED`
- `PROJECT_UPDATED`
- `PROJECT_DELETED`
- `PROJECT_VIEWED`
- `ACCESS_DENIED`

Each record contains the actor ID and email, action, resource identifier,
result, timestamp, remote IP address, user agent, and correlation ID. Clients
may send an `X-Correlation-ID` header containing up to 100 letters, numbers,
dots, underscores, or hyphens. The backend generates one when the header is
missing or invalid and returns it in the response.

Audit logging never stores passwords, access tokens, refresh tokens, secret
keys, or full request bodies.

Administrators and auditors can list audit records:

```shell
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/api/v1/audit-logs
```

Users receive `403 Forbidden` from the audit log endpoint.

## Documents

Documents are stored under `DOCUMENT_STORAGE_PATH`, which defaults to
`./data/uploads` for local development. Docker Compose mounts the repository's
`./data/uploads` directory into the backend container. Uploaded file bytes are
not stored in PostgreSQL; the database contains document metadata only.

Uploads are limited to 5 MB and must use one of these extensions:

- `.pdf`
- `.txt`
- `.png`
- `.jpg`
- `.jpeg`

Executable and archive types such as `.exe`, `.js`, `.bat`, `.sh`, `.jar`, and
`.zip` are rejected. Stored filenames are randomized, normalized paths are
checked against the storage root, and original filenames are used only as
validated display/download metadata.

Upload a document:

```shell
curl -X POST \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -F "file=@./example.pdf" \
  http://localhost:8080/api/v1/projects/<project-id>/documents
```

List document metadata:

```shell
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/api/v1/projects/<project-id>/documents
```

Download a document:

```shell
curl -L \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -o downloaded-file \
  http://localhost:8080/api/v1/projects/<project-id>/documents/<document-id>
```

Project owners and administrators can upload, list, and download documents.
Auditors can list document metadata for any project but cannot upload or
download file content.

Document operations write `DOCUMENT_UPLOADED`, `DOCUMENT_DOWNLOADED`,
`DOCUMENT_UPLOAD_REJECTED`, and applicable `ACCESS_DENIED` audit events. File
content and authentication secrets are never written to audit logs.
