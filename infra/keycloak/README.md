# Keycloak Configuration

Realm import files live in `realms/`.

The local Keycloak container mounts that directory read-only and imports the
`securetask` realm when it starts. Keycloak skips an import when a realm with
the same name already exists in its database.

The committed users and passwords are local demo data only. They must not be
used in a shared or production environment.
