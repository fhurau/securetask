package com.securetask.backend.document;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID projectId,
        String originalFilename,
        String contentType,
        long sizeBytes,
        String uploadedByUserId,
        String uploadedByEmail,
        Instant createdAt) {
}
