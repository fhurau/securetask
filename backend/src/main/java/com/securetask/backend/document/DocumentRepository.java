package com.securetask.backend.document;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findAllByProjectIdOrderByCreatedAtDesc(UUID projectId);

    Optional<Document> findByIdAndProjectId(UUID id, UUID projectId);
}
