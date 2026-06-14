# AppSec Demo Scenarios

These scenarios are designed for a short portfolio walkthrough using the
local Docker Compose environment and the demo accounts listed in the README.

## 1. Authentication and Role Enforcement

1. Open the application without signing in and confirm protected pages route
   to login.
2. Sign in as `user1@example.com`.
3. Explain that Keycloak performs OIDC Authorization Code Flow with S256 PKCE
   and Spring Boot validates the resulting JWT.
4. Attempt to open Audit logs and confirm a normal user cannot access them.
5. Sign in as `auditor@example.com` and confirm the audit view is available.

**Evidence:** frontend Keycloak adapter, Spring resource-server configuration,
method role checks, and controller security tests.

## 2. Object-Level Authorization

1. As user 1, create a project and retain its URL/UUID.
2. Sign out and sign in as `user2@example.com`.
3. Navigate directly to user 1's project URL.
4. Confirm the backend returns forbidden and the UI shows the Forbidden page.
5. Sign in as `admin@example.com` and confirm administrators can access the
   project.

**Security point:** changing an object identifier does not bypass the backend
owner check. The frontend is not trusted to enforce ownership.

## 3. Secure Document Handling

1. As a project owner, upload a small PDF, TXT, PNG, JPG, or JPEG file.
2. Download it and confirm attachment behavior and the original display name.
3. Attempt an empty, over-5-MB, or disallowed-extension upload such as `.exe`
   or `.zip`.
4. Confirm the upload is rejected.
5. As the auditor, call the document-metadata endpoint with the known project
   ID and confirm metadata is returned while upload and download are rejected.
   This is an API boundary demonstration; the normal project-detail UI remains
   owner/admin scoped.

**Security point:** the implementation combines ownership checks, role checks,
size/extension validation, filename handling, randomized stored names, and
storage-root containment. It does not include malware scanning or file-signature
inspection.

## 4. Audit Trail and Correlation

1. Generate successful project and document operations plus one denied access
   and one rejected upload.
2. Sign in as the auditor and review the resulting events.
3. Show actor, action, resource, result, timestamp, request context, and
   correlation ID.
4. Send a valid `X-Correlation-ID` in an API request and confirm it appears in
   the response and audit record.

**Security point:** structured security events support investigation without
logging request bodies, tokens, or file content.

## 5. Security CI

Open `.github/workflows/security-ci.yml` and the latest workflow run:

- Backend tests execute on Java 21.
- Frontend dependencies install from the lockfile, then lint and build.
- Gitleaks scans full Git history with redacted output.
- Trivy checks fixable high and critical dependency vulnerabilities.
- Repository permissions are read-only and there is no deployment job.

**Security point:** CI supplies repeatable preventive checks, not a guarantee
that the application is vulnerability-free.

## Close With the Limitations

State plainly that this is a local demonstration: HTTP and Keycloak dev mode,
committed demo credentials, local file storage, and no rate limiting, malware
scanning, centralized alerting, immutable audit sink, production secrets
manager, or deployment hardening.
