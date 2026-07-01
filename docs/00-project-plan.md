# distributed-auth-platform MVP Project Plan

distributed-auth-platform will be built incrementally so that each phase remains small,
reviewable, and testable.

## Phase 0: Repository Setup

- Add project documentation and environment-variable examples.
- Add a local Docker Compose draft for PostgreSQL and Keycloak.
- Reserve directories for backend, frontend, and Keycloak configuration.

## Phase 1: Local Identity Infrastructure

- Define and import a development Keycloak realm.
- Configure clients, roles, and test users without committing secrets.
- Document local startup and identity-provider setup.

## Phase 2: Backend Foundation

- Create the Spring Boot application.
- Connect to PostgreSQL through environment variables.
- Configure Keycloak-issued JWT validation.
- Add health checks, database migrations, and focused tests.

## Phase 3: Task API

- Add authenticated task create, read, update, and delete operations.
- Scope task access to the authenticated owner.
- Add validation, consistent error responses, and authorization tests.

## Phase 4: Frontend Foundation

- Create the Next.js application.
- Add Keycloak login and logout flows.
- Establish an authenticated API client and basic application layout.

## Phase 5: Task User Interface

- Add task listing, creation, editing, completion, and deletion.
- Handle loading, empty, validation, and error states.
- Verify that users cannot access another user's tasks.

## Phase 6: Security and Delivery

- Review authentication, authorization, CORS, headers, logging, and secret
  handling.
- Add end-to-end coverage for critical user journeys.
- Finalize Docker-based local setup and deployment documentation.
- Add portfolio screenshots and architecture notes.

## MVP Completion Criteria

- A user can authenticate through Keycloak.
- An authenticated user can manage only their own tasks.
- PostgreSQL persists application and identity data locally.
- The project can be configured without committed secrets.
- Setup, architecture, and security decisions are documented.
