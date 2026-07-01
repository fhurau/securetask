package com.securetask.backend.document;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.securetask.backend.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DocumentController.class)
@Import(SecurityConfig.class)
class DocumentControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    private StringRedisTemplate redisTemplate;

    @Test
    void unauthenticatedRequestGetsUnauthorized() throws Exception {
        mockMvc.perform(get(
                        "/api/v1/projects/{projectId}/documents",
                        UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedUploadGetsUnauthorized() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "content".getBytes());
        // Without a CSRF token, Spring's CsrfFilter returns 403 before auth runs; supply one so
        // the auth check executes and returns 401 as expected.
        mockMvc.perform(multipart(
                        "/api/v1/projects/{projectId}/documents",
                        UUID.randomUUID())
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void auditorCannotUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "content".getBytes());

        mockMvc.perform(multipart(
                        "/api/v1/projects/{projectId}/documents",
                        UUID.randomUUID())
                        .file(file)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_AUDITOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditorCannotDownloadFileContent() throws Exception {
        mockMvc.perform(get(
                        "/api/v1/projects/{projectId}/documents/{documentId}",
                        UUID.randomUUID(),
                        UUID.randomUUID())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_AUDITOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void crossUserDownloadGetsForbidden() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        when(documentService.download(
                org.mockito.ArgumentMatchers.eq(projectId),
                org.mockito.ArgumentMatchers.eq(documentId),
                any(),
                any()))
                .thenThrow(new AccessDeniedException("denied"));

        mockMvc.perform(get(
                        "/api/v1/projects/{projectId}/documents/{documentId}",
                        projectId,
                        documentId)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }
}
