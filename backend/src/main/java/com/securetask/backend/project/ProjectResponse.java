package com.securetask.backend.project;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Project data visible to an authorized caller.")
public record ProjectResponse(
        @Schema(description = "Project identifier.")
        UUID id,
        @Schema(description = "Project name.", example = "Portfolio launch")
        String name,
        @Schema(description = "Optional project description.")
        String description,
        @Schema(description = "Keycloak subject identifier of the owner.")
        String ownerUserId,
        @Schema(description = "Owner email.", example = "user1@example.com")
        String ownerEmail,
        @Schema(description = "Creation time in UTC.")
        Instant createdAt,
        @Schema(description = "Last update time in UTC.")
        Instant updatedAt) {
}
