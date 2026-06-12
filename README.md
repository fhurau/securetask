# SecureTask

SecureTask is a portfolio project for demonstrating a secure full-stack
application built with:

- Spring Boot
- Next.js
- PostgreSQL
- Keycloak

The project will be developed in small phases, starting with local
infrastructure and authentication before adding task-management features.
See [docs/00-project-plan.md](docs/00-project-plan.md) for the MVP plan.

## Repository Layout

```text
backend/          Spring Boot resource server
frontend/         Next.js application (future)
infra/keycloak/   Keycloak realm import files
docs/             Project documentation
```

## Quick Start

1. Copy `.env.example` to `.env` and replace the local infrastructure
   passwords.
2. Start PostgreSQL, Keycloak, and the backend:

   ```shell
   docker compose up -d --build
   ```

3. Open Keycloak at `http://localhost:<KEYCLOAK_PORT>`. The default example
   uses port `8081`.
4. Check the public backend endpoint:

   ```shell
   curl http://localhost:8080/api/v1/health
   ```

The `securetask` realm, its roles, the `securetask-frontend` client, and demo
users are imported automatically when Keycloak starts. Keycloak does not
overwrite an existing realm with the same name.

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
