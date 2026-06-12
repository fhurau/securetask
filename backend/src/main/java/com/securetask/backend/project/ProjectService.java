package com.securetask.backend.project;

import java.util.List;
import java.util.UUID;

import com.securetask.backend.audit.AuditLogService;
import com.securetask.backend.audit.AuditRequestContext;
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
    private final AuditLogService auditLogService;

    ProjectService(
            ProjectRepository projectRepository,
            AuditLogService auditLogService) {
        this.projectRepository = projectRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    ProjectResponse create(
            ProjectRequest request,
            JwtAuthenticationToken authentication,
            AuditRequestContext auditContext) {
        Jwt jwt = authentication.getToken();
        Project project = new Project(
                request.name(),
                request.description(),
                jwt.getSubject(),
                jwt.getClaimAsString("email"));
        Project savedProject = projectRepository.save(project);
        auditLogService.record(
                authentication,
                "PROJECT_CREATED",
                savedProject.getId(),
                "SUCCESS",
                auditContext);
        return toResponse(savedProject);
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
    ProjectResponse findById(
            UUID id,
            JwtAuthenticationToken authentication,
            AuditRequestContext auditContext) {
        Project project = getProject(id);
        requireOwnerOrAdmin(project, authentication, auditContext);
        auditLogService.record(
                authentication,
                "PROJECT_VIEWED",
                project.getId(),
                "SUCCESS",
                auditContext);
        return toResponse(project);
    }

    @Transactional
    ProjectResponse update(
            UUID id,
            ProjectRequest request,
            JwtAuthenticationToken authentication,
            AuditRequestContext auditContext) {
        Project project = getProject(id);
        requireOwnerOrAdmin(project, authentication, auditContext);
        project.update(request.name(), request.description());
        auditLogService.record(
                authentication,
                "PROJECT_UPDATED",
                project.getId(),
                "SUCCESS",
                auditContext);
        return toResponse(project);
    }

    @Transactional
    void delete(
            UUID id,
            JwtAuthenticationToken authentication,
            AuditRequestContext auditContext) {
        Project project = getProject(id);
        requireOwnerOrAdmin(project, authentication, auditContext);
        projectRepository.delete(project);
        auditLogService.record(
                authentication,
                "PROJECT_DELETED",
                project.getId(),
                "SUCCESS",
                auditContext);
    }

    private Project getProject(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Project not found"));
    }

    private void requireOwnerOrAdmin(
            Project project,
            JwtAuthenticationToken authentication,
            AuditRequestContext auditContext) {
        if (!isAdmin(authentication)
                && !project.getOwnerUserId().equals(authentication.getToken().getSubject())) {
            auditLogService.record(
                    authentication,
                    "ACCESS_DENIED",
                    project.getId(),
                    "DENIED",
                    auditContext);
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
