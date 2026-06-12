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
infra/keycloak/   Keycloak realm import files (future)
docs/             Project documentation
```

## Quick Start

> Placeholder: setup and startup instructions will be added as each
> application component is introduced.

For now, copy `.env.example` to `.env` before running local infrastructure.
The current `docker-compose.yml` is an initial draft for PostgreSQL and
Keycloak only.
