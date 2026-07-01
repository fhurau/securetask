# OWASP Mapping

This is a project control cross-reference, not an OWASP assessment,
certification, or claim that a category is fully mitigated. Category names are
from the [OWASP Top 10:2025](https://owasp.org/Top10/2025/) and
[OWASP API Security Top 10:2023](https://owasp.org/API-Security/editions/2023/en/0x11-t10/).

Status meanings:

- **Implemented:** a directly relevant control exists in this repository.
- **Partial:** some relevant controls exist, with material local-demo gaps.
- **Not implemented / N/A:** no specific control exists, or the application
  does not currently perform the risky behavior.

## OWASP Top 10:2025

| Category | Status | distributed-auth-platform evidence and limits |
| --- | --- | --- |
| A01 Broken Access Control | Implemented | Backend role checks and project-owner checks protect Project CRUD and documents; tests cover key denial paths. New endpoints would still require careful review. |
| A02 Security Misconfiguration | Partial | Environment-based configuration, restricted CI permissions, disabled Keycloak direct grants, narrow proxy header forwarding, and a basic security-header baseline help. Local HTTP, Keycloak dev mode, demo credentials, and non-production CSP allowances remain. |
| A03 Software Supply Chain Failures | Partial | Locked npm dependencies, Maven/Node builds, Trivy, Gitleaks, and free CI checks exist. There is no SBOM, provenance verification, or commit-SHA pinning for actions. |
| A04 Cryptographic Failures | Partial | OIDC/JWT validation and PKCE rely on established libraries. The local demo uses HTTP and defines no encryption-at-rest or key-management design. |
| A05 Injection | Partial | Bean Validation, UUID parsing, JPA repositories, and framework encoding reduce common injection paths. No dedicated SAST/DAST or adversarial injection suite is present. |
| A06 Insecure Design | Partial | The design separates identity, role checks, object checks, document metadata, file bytes, and audit records. This threat model is lightweight and no formal secure-design review is claimed. |
| A07 Authentication Failures | Implemented | Keycloak handles authentication; the backend validates JWTs; the frontend uses Authorization Code Flow with S256 PKCE and in-memory tokens. Local identity policy is not production-hardened. |
| A08 Software or Data Integrity Failures | Partial | Flyway controls schema changes, locked npm installs support reproducibility, and CI validates builds. No artifact signing, provenance enforcement, or tamper-resistant data store exists. |
| A09 Security Logging and Alerting Failures | Partial | Security-relevant audit events and correlation IDs are stored and access-controlled. There is no alerting, centralized aggregation, retention policy, or immutable sink. |
| A10 Mishandling of Exceptional Conditions | Partial | Validation failures, forbidden access, missing objects, invalid correlation IDs, and storage cleanup have explicit paths and focused tests. There is no global resilience or fault-injection program. |

## OWASP API Security Top 10:2023

| Category | Status | distributed-auth-platform evidence and limits |
| --- | --- | --- |
| API1 Broken Object Level Authorization | Implemented | Project ownership is checked for object access; document lookup is scoped to project and document IDs; administrators are explicit exceptions. |
| API2 Broken Authentication | Implemented | Spring validates Keycloak JWTs and all non-health API routes require authentication. Keycloak remains a local demo configuration. |
| API3 Broken Object Property Level Authorization | Partial | Request DTOs expose only project name/description; owner and audit fields are server-controlled. Responses intentionally include owner/document metadata, with no field-level policy framework. |
| API4 Unrestricted Resource Consumption | Partial | Upload size and multipart limits exist. There is no rate limiting, pagination, request quota, or total storage quota. |
| API5 Broken Function Level Authorization | Implemented | Method roles separate user/admin mutations, auditor/admin audit access, and document content access. |
| API6 Unrestricted Access to Sensitive Business Flows | Partial | Sensitive operations require roles and ownership, but automated abuse controls and rate limits are absent. |
| API7 Server Side Request Forgery | N/A currently | The API does not fetch caller-supplied remote URLs. This must be reassessed if URL import or webhook features are added. |
| API8 Security Misconfiguration | Partial | The application uses explicit JWT, proxy, persistence, CI, and response-header configuration. Development-mode services, local HTTP, and missing production TLS/CSP hardening remain. |
| API9 Improper Inventory Management | Partial | `/api/v1` endpoints are listed in the README and architecture docs. There is no generated OpenAPI inventory, lifecycle policy, or deployed-environment inventory. |
| API10 Unsafe Consumption of APIs | Partial | Keycloak integration uses maintained libraries and configured issuer/JWK endpoints. There is no general third-party API validation policy because no other external API is consumed. |
