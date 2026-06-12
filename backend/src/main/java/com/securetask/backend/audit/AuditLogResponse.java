package com.securetask.backend.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        Instant timestamp,
        String actorUserId,
        String actorEmail,
        String action,
        String resourceType,
        UUID resourceId,
        String result,
        String ipAddress,
        String userAgent,
        String correlationId) {
}
