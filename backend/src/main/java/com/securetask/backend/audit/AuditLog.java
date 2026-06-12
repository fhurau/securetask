package com.securetask.backend.audit;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_logs")
class AuditLog {

    @Id
    private UUID id;

    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    @Column(name = "actor_user_id", nullable = false)
    private String actorUserId;

    @Column(name = "actor_email", length = 320)
    private String actorEmail;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 100)
    private String resourceType;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(nullable = false, length = 50)
    private String result;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    protected AuditLog() {
    }

    AuditLog(
            String actorUserId,
            String actorEmail,
            String action,
            String resourceType,
            UUID resourceId,
            String result,
            String ipAddress,
            String userAgent,
            String correlationId) {
        this.id = UUID.randomUUID();
        this.actorUserId = actorUserId;
        this.actorEmail = actorEmail;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.result = result;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.correlationId = correlationId;
    }

    @PrePersist
    void onCreate() {
        timestamp = Instant.now();
    }

    UUID getId() {
        return id;
    }

    Instant getTimestamp() {
        return timestamp;
    }

    String getActorUserId() {
        return actorUserId;
    }

    String getActorEmail() {
        return actorEmail;
    }

    String getAction() {
        return action;
    }

    String getResourceType() {
        return resourceType;
    }

    UUID getResourceId() {
        return resourceId;
    }

    String getResult() {
        return result;
    }

    String getIpAddress() {
        return ipAddress;
    }

    String getUserAgent() {
        return userAgent;
    }

    String getCorrelationId() {
        return correlationId;
    }
}
