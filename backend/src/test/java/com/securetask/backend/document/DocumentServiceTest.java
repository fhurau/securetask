package com.securetask.backend.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.securetask.backend.audit.AuditEventProducer;
import com.securetask.backend.audit.AuditRequestContext;
import com.securetask.backend.project.ProjectAuthorizationService;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentStorageService storageService;

    @Mock
    private ProjectAuthorizationService projectAuthorizationService;

    @Mock
    private AuditEventProducer auditEventProducer;

    private final AuditRequestContext auditContext =
            new AuditRequestContext("127.0.0.1", "test-agent", "test-correlation");

    @Test
    void ownerCanUploadAllowedFileAndAuditIsWritten() {
        UUID projectId = UUID.randomUUID();
        JwtAuthenticationToken authentication = authentication("user-1", "ROLE_USER");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "secure content".getBytes(StandardCharsets.UTF_8));
        when(documentRepository.saveAndFlush(any(Document.class))).thenAnswer(invocation -> {
            Document document = invocation.getArgument(0);
            document.onCreate();
            return document;
        });
        DocumentService service = service();

        DocumentResponse response = service.upload(
                projectId,
                file,
                authentication,
                auditContext);

        ArgumentCaptor<String> storedFilename = ArgumentCaptor.forClass(String.class);
        verify(storageService).store(storedFilename.capture(), any());
        assertThat(storedFilename.getValue()).endsWith(".txt").doesNotContain("notes");
        assertThat(response.originalFilename()).isEqualTo("notes.txt");
        verify(auditEventProducer).publish(
                authentication,
                "DOCUMENT_UPLOADED",
                "DOCUMENT",
                response.id(),
                "SUCCESS",
                auditContext);
    }

    @Test
    void ownerCanDownloadOwnDocument() {
        UUID projectId = UUID.randomUUID();
        Document document = document(projectId, "report.pdf", "stored.pdf");
        when(documentRepository.findByIdAndProjectId(document.getId(), projectId))
                .thenReturn(Optional.of(document));
        when(storageService.load("stored.pdf"))
                .thenReturn(new ByteArrayResource("pdf".getBytes(StandardCharsets.UTF_8)));
        JwtAuthenticationToken authentication = authentication("user-1", "ROLE_USER");
        DocumentService service = service();

        DocumentDownload download = service.download(
                projectId,
                document.getId(),
                authentication,
                auditContext);

        assertThat(download.originalFilename()).isEqualTo("report.pdf");
        verify(auditEventProducer).publish(
                authentication,
                "DOCUMENT_DOWNLOADED",
                "DOCUMENT",
                document.getId(),
                "SUCCESS",
                auditContext);
    }

    @Test
    void crossUserDownloadIsDenied() {
        UUID projectId = UUID.randomUUID();
        JwtAuthenticationToken authentication = authentication("user-2", "ROLE_USER");
        org.mockito.Mockito.doThrow(new AccessDeniedException("denied"))
                .when(projectAuthorizationService)
                .requireContentAccess(projectId, authentication, auditContext);
        DocumentService service = service();

        assertThatThrownBy(() -> service.download(
                projectId,
                UUID.randomUUID(),
                authentication,
                auditContext))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void pathTraversalFilenameIsRejected() {
        UUID projectId = UUID.randomUUID();
        JwtAuthenticationToken authentication = authentication("user-1", "ROLE_USER");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../../etc/passwd",
                "text/plain",
                "content".getBytes(StandardCharsets.UTF_8));
        DocumentService service = service();

        assertThatThrownBy(() -> service.upload(projectId, file, authentication, auditContext))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void executableUploadIsRejectedAndAudited() {
        assertRejectedUpload("malware.exe", 10);
    }

    @Test
    void oversizedUploadIsRejectedAndAudited() {
        assertRejectedUpload("large.pdf", 101);
    }

    private void assertRejectedUpload(String filename, int size) {
        UUID projectId = UUID.randomUUID();
        JwtAuthenticationToken authentication = authentication("user-1", "ROLE_USER");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                filename,
                "application/octet-stream",
                new byte[size]);
        DocumentService service = new DocumentService(
                documentRepository,
                storageService,
                projectAuthorizationService,
                auditEventProducer,
                100);

        assertThatThrownBy(() -> service.upload(
                projectId,
                file,
                authentication,
                auditContext))
                .isInstanceOf(ResponseStatusException.class);
        verify(auditEventProducer).publish(
                authentication,
                "DOCUMENT_UPLOAD_REJECTED",
                "PROJECT",
                projectId,
                "REJECTED",
                auditContext);
    }

    private DocumentService service() {
        return new DocumentService(
                documentRepository,
                storageService,
                projectAuthorizationService,
                auditEventProducer,
                5 * 1024 * 1024);
    }

    private Document document(
            UUID projectId,
            String originalFilename,
            String storedFilename) {
        Document document = new Document(
                projectId,
                originalFilename,
                storedFilename,
                "application/pdf",
                3,
                "user-1",
                "user1@example.com");
        document.onCreate();
        return document;
    }

    private JwtAuthenticationToken authentication(String subject, String authority) {
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of(
                        "sub", subject,
                        "email", subject + "@example.com"));
        return new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority(authority)));
    }
}
