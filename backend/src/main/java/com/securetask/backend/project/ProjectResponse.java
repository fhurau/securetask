package com.securetask.backend.project;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        String description,
        String ownerUserId,
        String ownerEmail,
        Instant createdAt,
        Instant updatedAt) {
}
