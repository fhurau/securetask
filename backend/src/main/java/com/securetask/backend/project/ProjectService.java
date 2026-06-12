package com.securetask.backend.project;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
class ProjectService {

    private final ProjectRepository projectRepository;

    ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional
    ProjectResponse create(ProjectRequest request, JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();
        Project project = new Project(
                request.name(),
                request.description(),
                jwt.getSubject(),
                jwt.getClaimAsString("email"));
        return toResponse(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    List<ProjectResponse> findAll(JwtAuthenticationToken authentication) {
        List<Project> projects = isAdmin(authentication)
                ? projectRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                : projectRepository.findAllByOwnerUserIdOrderByCreatedAtDesc(
                        authentication.getToken().getSubject());

        return projects.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    ProjectResponse findById(UUID id, JwtAuthenticationToken authentication) {
        Project project = getProject(id);
        requireOwnerOrAdmin(project, authentication);
        return toResponse(project);
    }

    @Transactional
    ProjectResponse update(
            UUID id,
            ProjectRequest request,
            JwtAuthenticationToken authentication) {
        Project project = getProject(id);
        requireOwnerOrAdmin(project, authentication);
        project.update(request.name(), request.description());
        return toResponse(project);
    }

    @Transactional
    void delete(UUID id, JwtAuthenticationToken authentication) {
        Project project = getProject(id);
        requireOwnerOrAdmin(project, authentication);
        projectRepository.delete(project);
    }

    private Project getProject(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Project not found"));
    }

    private void requireOwnerOrAdmin(
            Project project,
            JwtAuthenticationToken authentication) {
        if (!isAdmin(authentication)
                && !project.getOwnerUserId().equals(authentication.getToken().getSubject())) {
            throw new AccessDeniedException("You cannot access this project");
        }
    }

    private boolean isAdmin(JwtAuthenticationToken authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getOwnerUserId(),
                project.getOwnerEmail(),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }
}
