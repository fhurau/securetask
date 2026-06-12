package com.securetask.backend.document;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.securetask.backend.audit.AuditRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/documents")
public class DocumentController {

    private final DocumentService documentService;

    DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    ResponseEntity<DocumentResponse> upload(
            @PathVariable UUID projectId,
            @RequestPart("file") MultipartFile file,
            JwtAuthenticationToken authentication,
            HttpServletRequest request) {
        DocumentResponse document = documentService.upload(
                projectId,
                file,
                authentication,
                AuditRequestContext.from(request));
        return ResponseEntity
                .created(URI.create(
                        "/api/v1/projects/" + projectId + "/documents/" + document.id()))
                .body(document);
    }

    @GetMapping
    List<DocumentResponse> findAll(
            @PathVariable UUID projectId,
            JwtAuthenticationToken authentication,
            HttpServletRequest request) {
        return documentService.findAll(
                projectId,
                authentication,
                AuditRequestContext.from(request));
    }

    @GetMapping("/{documentId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    ResponseEntity<Resource> download(
            @PathVariable UUID projectId,
            @PathVariable UUID documentId,
            JwtAuthenticationToken authentication,
            HttpServletRequest request) {
        DocumentDownload download = documentService.download(
                projectId,
                documentId,
                authentication,
                AuditRequestContext.from(request));
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.originalFilename())
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(download.resource());
    }
}
