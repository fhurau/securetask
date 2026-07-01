package com.securetask.backend.audit;

import java.io.Serializable;
import java.util.UUID;

public record AuditEvent(
        String actorUserId,
        String actorEmail,
        String action,
        String resourceType,
        UUID resourceId,
        String result,
        String ipAddress,
        String userAgent,
        String correlationId) implements Serializable {
}
