package com.securetask.backend.audit;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class AuditEventConsumer {

    private final AuditLogService auditLogService;

    AuditEventConsumer(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @KafkaListener(topics = AuditEventProducer.TOPIC, groupId = "audit-log-writer")
    void consume(AuditEvent event) {
        auditLogService.record(event);
    }
}
