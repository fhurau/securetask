package com.securetask.backend.audit;

import java.util.List;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            JwtAuthenticationToken authentication,
            String action,
            UUID resourceId,
            String result,
            AuditRequestContext context) {
        Jwt jwt = authentication.getToken();
        AuditLog auditLog = new AuditLog(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                action,
                "PROJECT",
                resourceId,
                result,
                context.ipAddress(),
                context.userAgent(),
                context.correlationId());
        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> findAll() {
        return auditLogRepository.findAllByOrderByTimestampDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getTimestamp(),
                auditLog.getActorUserId(),
                auditLog.getActorEmail(),
                auditLog.getAction(),
                auditLog.getResourceType(),
                auditLog.getResourceId(),
                auditLog.getResult(),
                auditLog.getIpAddress(),
                auditLog.getUserAgent(),
                auditLog.getCorrelationId());
    }
}
