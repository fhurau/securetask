package com.securetask.backend.audit;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Security-relevant audit event.")
public record AuditLogResponse(
        @Schema(description = "Audit event identifier.")
        UUID id,
        @Schema(description = "Event time in UTC.")
        Instant timestamp,
        @Schema(description = "Keycloak subject identifier of the actor.")
        String actorUserId,
        @Schema(description = "Actor email.", example = "user1@example.com")
        String actorEmail,
        @Schema(description = "Recorded action.", example = "PROJECT_VIEWED")
        String action,
        @Schema(description = "Resource category.", example = "PROJECT")
        String resourceType,
        @Schema(description = "Affected resource identifier, when applicable.")
        UUID resourceId,
        @Schema(description = "Event result.", example = "SUCCESS")
        String result,
        @Schema(description = "Observed client IP address.", example = "127.0.0.1")
        String ipAddress,
        @Schema(description = "Observed user agent.")
        String userAgent,
        @Schema(description = "Request correlation identifier.")
        String correlationId) {
}
