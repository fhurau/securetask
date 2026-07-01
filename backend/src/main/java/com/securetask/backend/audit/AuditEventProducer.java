package com.securetask.backend.audit;

import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuditEventProducer {

    static final String TOPIC = "audit-events";

    private final KafkaTemplate<String, AuditEvent> kafkaTemplate;

    AuditEventProducer(KafkaTemplate<String, AuditEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(
            JwtAuthenticationToken authentication,
            String action,
            UUID resourceId,
            String result,
            AuditRequestContext context) {
        publish(authentication, action, "PROJECT", resourceId, result, context);
    }

    public void publish(
            JwtAuthenticationToken authentication,
            String action,
            String resourceType,
            UUID resourceId,
            String result,
            AuditRequestContext context) {
        Jwt jwt = authentication.getToken();
        AuditEvent event = new AuditEvent(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                action,
                resourceType,
                resourceId,
                result,
                context.ipAddress(),
                context.userAgent(),
                context.correlationId());
        kafkaTemplate.send(TOPIC, event.correlationId(), event);
    }
}
