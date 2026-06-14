# Keycloak Configuration

Realm import files live in `realms/`.

The local Keycloak container mounts that directory read-only and imports the
`securetask` realm when it starts. Keycloak skips an import when a realm with
the same name already exists in its database.

The committed users and passwords are local demo data only. They must not be
used in a shared or production environment.

The realm also contains two intentionally separate clients:

- `securetask-frontend` is the browser client. It uses Authorization Code Flow
  with S256 PKCE and does not allow direct access grants.
- `securetask-smoke-test` is a public, local-demo testing client that allows
  direct grants so the reviewer smoke test can obtain non-interactive tokens.
  It is not used by the frontend and is not a production credential design.
