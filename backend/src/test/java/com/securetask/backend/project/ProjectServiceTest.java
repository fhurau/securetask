package com.securetask.backend.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Test
    void userCanCreateOwnProject() {
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.onCreate();
            return project;
        });
        ProjectService service = new ProjectService(projectRepository);

        ProjectResponse response = service.create(
                new ProjectRequest("My project", "Description"),
                authentication("user-1", "user1@example.com", "ROLE_USER"));

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(captor.capture());
        assertThat(captor.getValue().getOwnerUserId()).isEqualTo("user-1");
        assertThat(captor.getValue().getOwnerEmail()).isEqualTo("user1@example.com");
        assertThat(response.name()).isEqualTo("My project");
    }

    @Test
    void userCanListOnlyOwnProjects() {
        Project ownProject = project("Own project", "user-1", "user1@example.com");
        when(projectRepository.findAllByOwnerUserIdOrderByCreatedAtDesc("user-1"))
                .thenReturn(List.of(ownProject));
        ProjectService service = new ProjectService(projectRepository);

        List<ProjectResponse> response = service.findAll(
                authentication("user-1", "user1@example.com", "ROLE_USER"));

        assertThat(response).extracting(ProjectResponse::name).containsExactly("Own project");
        verify(projectRepository, never()).findAll(any(Sort.class));
    }

    @Test
    void userCannotAccessAnotherUsersProject() {
        Project otherProject = project(
                "Other project",
                "user-2",
                "user2@example.com");
        when(projectRepository.findById(otherProject.getId()))
                .thenReturn(Optional.of(otherProject));
        ProjectService service = new ProjectService(projectRepository);

        assertThatThrownBy(() -> service.findById(
                otherProject.getId(),
                authentication("user-1", "user1@example.com", "ROLE_USER")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void adminCanAccessAllProjects() {
        Project first = project("First", "user-1", "user1@example.com");
        Project second = project("Second", "user-2", "user2@example.com");
        when(projectRepository.findAll(any(Sort.class))).thenReturn(List.of(first, second));
        ProjectService service = new ProjectService(projectRepository);

        List<ProjectResponse> response = service.findAll(
                authentication("admin-1", "admin@example.com", "ROLE_ADMIN"));

        assertThat(response).extracting(ProjectResponse::name)
                .containsExactly("First", "Second");
        verify(projectRepository, never())
                .findAllByOwnerUserIdOrderByCreatedAtDesc(any());
    }

    private Project project(String name, String ownerUserId, String ownerEmail) {
        Project project = new Project(name, null, ownerUserId, ownerEmail);
        project.onCreate();
        return project;
    }

    private JwtAuthenticationToken authentication(
            String subject,
            String email,
            String authority) {
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of(
                        "sub", subject,
                        "preferred_username", email,
                        "email", email));
        return new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority(authority)),
                email);
    }
}
