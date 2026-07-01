---
description: Run a security check of the current change against this repo's security invariants
---

Review the current diff (or the area the user names) against SecureTask's security model. This repo
exists to demonstrate these controls correctly, so treat regressions here as high-severity even when
small.

Check for:
- **Object-level authorization**: any new project- or document-scoped endpoint must route through
  `ProjectAuthorizationService` (`requireContentAccess` / `requireMetadataAccess`) rather than
  reimplementing an owner/role check inline.
- **AuthN**: protected endpoints stay behind the JWT resource-server filter chain in
  `SecurityConfig.java`; nothing new gets added to the `permitAll()` matcher list without a clear
  reason (health/docs endpoints only).
- **Audit logging**: security-relevant actions (especially denials) are recorded via
  `AuditLogService`, and logged fields never include passwords, tokens, secret keys, file content,
  or full request bodies.
- **Document handling**: uploads stay within the 5 MB limit, the extension allowlist (`.txt`, `.pdf`,
  `.png`, `.jpg`, `.jpeg`), randomized storage filenames, and path-traversal checks in
  `DocumentStorageService`.
- **Headers/CSP**: no loosening of `SecurityConfig.java`'s headers/CSP beyond what's already
  documented as a localhost-only tradeoff in `docs/06-security-tradeoffs.md`.
- **Secrets**: no new hardcoded secrets outside `.env`/`.env.example` conventions (Gitleaks runs in
  CI, but don't rely on CI to catch it).
- **Local-first**: the change still works fully via `docker compose up` with no cloud account or
  paid API key (see CLAUDE.md's Local-first constraint).

Then, if containers are running locally, run the smoke test for a live check:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1
```

For a deeper, multi-agent review of the whole branch, tell the user they can run `/security-review`
or ultrareview (`/code-review ultra`) instead of this quick pass.

Report findings as: file/line, what's wrong, why it matters for this repo specifically. Don't repeat
the "Local-Demo Limitations" items from README.md as findings — those are intentional.
