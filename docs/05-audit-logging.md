# Audit Logging

## What Is Recorded

Audit records are stored in PostgreSQL and returned newest first to
authenticated `ADMIN` and `AUDITOR` users.

| Field | Purpose |
| --- | --- |
| `timestamp` | Server-side event time |
| `actorUserId`, `actorEmail` | Identity from the validated JWT |
| `action` | Security or business event name |
| `resourceType`, `resourceId` | Affected project or document |
| `result` | `SUCCESS`, `DENIED`, or `REJECTED` |
| `ipAddress`, `userAgent` | Request context |
| `correlationId` | Request tracing value |

Implemented action names:

- `PROJECT_CREATED`, `PROJECT_VIEWED`, `PROJECT_UPDATED`, `PROJECT_DELETED`
- `ACCESS_DENIED`
- `DOCUMENT_UPLOADED`, `DOCUMENT_DOWNLOADED`
- `DOCUMENT_UPLOAD_REJECTED`

The backend accepts `X-Correlation-ID` only when it contains 1-100 letters,
numbers, dots, underscores, or hyphens. It generates a UUID otherwise and
returns the selected value in the response header.

## Data-Minimization Rules

Audit calls pass structured event fields, not complete requests. The current
implementation does not write passwords, access tokens, refresh tokens,
secret keys, request bodies, or document content to audit records.

Actor email, IP address, user agent, filenames in document metadata, and
resource identifiers can still be sensitive. Access to the audit endpoint is
therefore role-restricted.

## Demo Checks

1. Perform a successful project create and view.
2. Attempt to open another user's project.
3. Upload an allowed document, reject a disallowed one, and download the
   allowed document.
4. Sign in as the auditor and confirm the expected action, result, actor,
   resource, and correlation ID records.
5. Confirm a normal user receives `403 Forbidden` from `/api/v1/audit-logs`.

## Limitations

- Audit rows are mutable by database administrators and are not signed or
  exported to immutable storage.
- There is no alerting, dashboarding, anomaly detection, retention/deletion
  policy, pagination, or archival process.
- Not every framework-level authentication failure or unexpected exception is
  converted into an application audit event.
- Client IP accuracy depends on the direct local request path; trusted-proxy
  handling is not designed for production.
- Audit availability depends on PostgreSQL and is not a substitute for
  operational application logs.

