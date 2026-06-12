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
backend/          Spring Boot application (future)
frontend/         Next.js application (future)
infra/keycloak/   Keycloak realm import files
docs/             Project documentation
```

## Quick Start

1. Copy `.env.example` to `.env` and replace the local infrastructure
   passwords.
2. Start PostgreSQL and Keycloak:

   ```shell
   docker compose up -d
   ```

3. Open Keycloak at `http://localhost:<KEYCLOAK_PORT>`. The default example
   uses port `8081`.

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
