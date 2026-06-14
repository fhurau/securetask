package com.securetask.backend.document;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Stored document metadata. File content is returned only by download.")
public record DocumentResponse(
        @Schema(description = "Document identifier.")
        UUID id,
        @Schema(description = "Owning project identifier.")
        UUID projectId,
        @Schema(description = "Validated original filename.", example = "design.pdf")
        String originalFilename,
        @Schema(description = "Server-selected media type.", example = "application/pdf")
        String contentType,
        @Schema(description = "File size in bytes.", example = "2048")
        long sizeBytes,
        @Schema(description = "Keycloak subject identifier of the uploader.")
        String uploadedByUserId,
        @Schema(description = "Uploader email.", example = "user1@example.com")
        String uploadedByEmail,
        @Schema(description = "Upload time in UTC.")
        Instant createdAt) {
}
