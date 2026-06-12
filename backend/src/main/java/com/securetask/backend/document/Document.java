package com.securetask.backend.document;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "documents")
class Document {

    @Id
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, unique = true, length = 255)
    private String storedFilename;

    @Column(name = "content_type", nullable = false, length = 255)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "uploaded_by_user_id", nullable = false)
    private String uploadedByUserId;

    @Column(name = "uploaded_by_email", nullable = false, length = 320)
    private String uploadedByEmail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Document() {
    }

    Document(
            UUID projectId,
            String originalFilename,
            String storedFilename,
            String contentType,
            long sizeBytes,
            String uploadedByUserId,
            String uploadedByEmail) {
        this.id = UUID.randomUUID();
        this.projectId = projectId;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.uploadedByUserId = uploadedByUserId;
        this.uploadedByEmail = uploadedByEmail;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    UUID getId() {
        return id;
    }

    UUID getProjectId() {
        return projectId;
    }

    String getOriginalFilename() {
        return originalFilename;
    }

    String getStoredFilename() {
        return storedFilename;
    }

    String getContentType() {
        return contentType;
    }

    long getSizeBytes() {
        return sizeBytes;
    }

    String getUploadedByUserId() {
        return uploadedByUserId;
    }

    String getUploadedByEmail() {
        return uploadedByEmail;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
