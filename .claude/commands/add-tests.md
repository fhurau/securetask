---
description: Add tests for the current change, following this repo's existing test conventions
---

Add tests for the code changed in the current diff (or the file/feature the user names).

Backend (`backend/src/test/java/...`, mirrors the main package structure):
- Unit/service tests: JUnit 5 + Mockito, following `ProjectServiceTest.java` / `DocumentServiceTest.java`.
- Controller/security tests: `@WebMvcTest` + `MockMvc` + `spring-security-test`'s `jwt()` request
  postprocessor to simulate roles/subject, following `ProjectControllerSecurityTest.java` /
  `DocumentControllerSecurityTest.java` / `AuditLogControllerSecurityTest.java`. Always cover: no
  auth → 401, wrong owner/role → 403, correct owner/role → 200.
- Run with `mvn test` (from `backend/`), or `mvn test -Dtest=ClassName#method` for a single test.

Frontend: there is currently no test framework installed (no Jest/Vitest/RTL in
`frontend/package.json`). Do not add one speculatively. If the user explicitly asks for frontend
tests, stop and ask which framework to introduce before adding dependencies.

Rules:
- Match the existing file naming and package placement exactly (test class next to the code it
  tests, same package).
- Prefer extending an existing test class over creating a new one if the behavior is closely related.
- For anything touching `ProjectAuthorizationService`, audit logging, or document upload
  constraints, include both an allowed-access and a denied-access case — those are the security
  invariants this codebase is built to demonstrate.
- Don't test framework/library behavior (Spring wiring, JPA itself) — test this project's logic.
