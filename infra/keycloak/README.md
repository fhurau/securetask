# Keycloak Configuration

Place exportable realm configuration files in `realms/`.

The local Keycloak container mounts that directory read-only and attempts to
import realm JSON files when it starts. Realm configuration will be added in a
later project phase.
