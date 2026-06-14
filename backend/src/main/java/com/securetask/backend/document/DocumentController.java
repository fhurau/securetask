package com.securetask.backend.document;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.securetask.backend.audit.AuditRequestContext;
import com.securetask.backend.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Documents")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class DocumentController {

    private final DocumentService documentService;

    DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
            summary = "Upload a project document",
            description = "Uploads an allowed file up to 5 MB. Requires project content access.")
    ResponseEntity<DocumentResponse> upload(
            @PathVariable UUID projectId,
            @Parameter(description = "PDF, TXT, PNG, JPG, or JPEG file.", required = true)
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
    @Operation(
            summary = "List document metadata",
            description = "Lists metadata for an accessible project without returning file content.")
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
    @Operation(
            summary = "Download a project document",
            description = "Downloads file content when the caller has project content access.")
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
