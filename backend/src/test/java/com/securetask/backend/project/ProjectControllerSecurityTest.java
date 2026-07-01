package com.securetask.backend.project;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProjectController.class)
class ProjectControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    @Test
    void unauthenticatedRequestGetsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userGetsForbiddenForAnotherUsersProject() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(projectService.findById(eq(projectId), any(), any()))
                .thenThrow(new AccessDeniedException("You cannot access this project"));

        mockMvc.perform(get("/api/v1/projects/{id}", projectId)
                        .with(jwt()
                                .jwt(token -> token.subject("user-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanGetOwnProject() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(projectService.findById(eq(projectId), any(), any()))
                .thenReturn(new ProjectResponse(
                        projectId, "My project", null,
                        "user-1", "user1@example.com", null, null));

        mockMvc.perform(get("/api/v1/projects/{id}", projectId)
                        .with(jwt()
                                .jwt(token -> token.subject("user-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk());
    }
}
