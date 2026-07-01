package com.securetask.backend.audit;

import java.util.List;

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
    public void record(AuditEvent event) {
        AuditLog auditLog = new AuditLog(
                event.actorUserId(),
                event.actorEmail(),
                event.action(),
                event.resourceType(),
                event.resourceId(),
                event.result(),
                event.ipAddress(),
                event.userAgent(),
                event.correlationId());
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
