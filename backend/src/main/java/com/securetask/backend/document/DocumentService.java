package com.securetask.backend.document;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.securetask.backend.audit.AuditLogService;
import com.securetask.backend.audit.AuditRequestContext;
import com.securetask.backend.project.ProjectAuthorizationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
class DocumentService {

    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "pdf", "application/pdf",
            "txt", "text/plain",
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg");

    private final DocumentRepository documentRepository;
    private final DocumentStorageService storageService;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final AuditLogService auditLogService;
    private final long maxSizeBytes;

    DocumentService(
            DocumentRepository documentRepository,
            DocumentStorageService storageService,
            ProjectAuthorizationService projectAuthorizationService,
            AuditLogService auditLogService,
            @Value("${securetask.documents.max-size-bytes}") long maxSizeBytes) {
        this.documentRepository = documentRepository;
        this.storageService = storageService;
        this.projectAuthorizationService = projectAuthorizationService;
        this.auditLogService = auditLogService;
        this.maxSizeBytes = maxSizeBytes;
    }

    @Transactional
    DocumentResponse upload(
            UUID projectId,
            MultipartFile file,
            JwtAuthenticationToken authentication,
            AuditRequestContext auditContext) {
        projectAuthorizationService.requireContentAccess(
                projectId,
                authentication,
                auditContext);

        ValidatedFile validatedFile;
        try {
            validatedFile = validate(file);
        } catch (ResponseStatusException exception) {
            auditLogService.record(
                    authentication,
                    "DOCUMENT_UPLOAD_REJECTED",
                    "PROJECT",
                    projectId,
                    "REJECTED",
                    auditContext);
            throw exception;
        }

        Jwt jwt = authentication.getToken();
        Document document = new Document(
                projectId,
                validatedFile.originalFilename(),
                validatedFile.storedFilename(),
                validatedFile.contentType(),
                file.getSize(),
                jwt.getSubject(),
                jwt.getClaimAsString("email"));

        try {
            storageService.store(validatedFile.storedFilename(), file.getInputStream());
            Document savedDocument = documentRepository.saveAndFlush(document);
            auditLogService.record(
                    authentication,
                    "DOCUMENT_UPLOADED",
                    "DOCUMENT",
                    savedDocument.getId(),
                    "SUCCESS",
                    auditContext);
            return toResponse(savedDocument);
        } catch (IOException exception) {
            storageService.deleteQuietly(validatedFile.storedFilename());
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Could not read uploaded document",
                    exception);
        } catch (RuntimeException exception) {
            storageService.deleteQuietly(validatedFile.storedFilename());
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    List<DocumentResponse> findAll(
            UUID projectId,
            JwtAuthenticationToken authentication,
            AuditRequestContext auditContext) {
        projectAuthorizationService.requireMetadataAccess(
                projectId,
                authentication,
                auditContext);
        return documentRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    DocumentDownload download(
            UUID projectId,
            UUID documentId,
            JwtAuthenticationToken authentication,
            AuditRequestContext auditContext) {
        projectAuthorizationService.requireContentAccess(
                projectId,
                authentication,
                auditContext);
        Document document = documentRepository.findByIdAndProjectId(documentId, projectId)
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "Document not found"));
        DocumentDownload download = new DocumentDownload(
                storageService.load(document.getStoredFilename()),
                document.getOriginalFilename(),
                document.getContentType(),
                document.getSizeBytes());
        auditLogService.record(
                authentication,
                "DOCUMENT_DOWNLOADED",
                "DOCUMENT",
                document.getId(),
                "SUCCESS",
                auditContext);
        return download;
    }

    private ValidatedFile validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "File is required");
        }
        if (file.getSize() > maxSizeBytes) {
            throw new ResponseStatusException(BAD_REQUEST, "File exceeds the 5 MB limit");
        }

        String originalFilename = safeOriginalFilename(file.getOriginalFilename());
        int extensionSeparator = originalFilename.lastIndexOf('.');
        if (extensionSeparator < 1 || extensionSeparator == originalFilename.length() - 1) {
            throw new ResponseStatusException(BAD_REQUEST, "File type is not allowed");
        }

        String extension = originalFilename.substring(extensionSeparator + 1)
                .toLowerCase(Locale.ROOT);
        String contentType = ALLOWED_TYPES.get(extension);
        if (contentType == null) {
            throw new ResponseStatusException(BAD_REQUEST, "File type is not allowed");
        }

        return new ValidatedFile(
                originalFilename,
                UUID.randomUUID() + "." + extension,
                contentType);
    }

    private String safeOriginalFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Filename is required");
        }
        String normalized = originalFilename.replace('\\', '/');
        String filename = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        if (filename.isBlank()
                || filename.length() > 255
                || filename.chars().anyMatch(Character::isISOControl)) {
            throw new ResponseStatusException(BAD_REQUEST, "Filename is invalid");
        }
        return filename;
    }

    private DocumentResponse toResponse(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getProjectId(),
                document.getOriginalFilename(),
                document.getContentType(),
                document.getSizeBytes(),
                document.getUploadedByUserId(),
                document.getUploadedByEmail(),
                document.getCreatedAt());
    }

    private record ValidatedFile(
            String originalFilename,
            String storedFilename,
            String contentType) {
    }
}
