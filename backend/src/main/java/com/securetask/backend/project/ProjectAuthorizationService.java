package com.securetask.backend.project;

import java.util.UUID;

import com.securetask.backend.audit.AuditLogService;
import com.securetask.backend.audit.AuditRequestContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ProjectAuthorizationService {

    private final ProjectRepository projectRepository;
    private final AuditLogService auditLogService;

    ProjectAuthorizationService(
            ProjectRepository projectRepository,
            AuditLogService auditLogService) {
        this.projectRepository = projectRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public void requireContentAccess(
            UUID projectId,
            JwtAuthenticationToken authentication,
            AuditRequestContext auditContext) {
        Project project = getProject(projectId);
        if (!isAdmin(authentication)
                && !project.getOwnerUserId().equals(authentication.getToken().getSubject())) {
            deny(projectId, authentication, auditContext);
        }
    }

    @Transactional(readOnly = true)
    public void requireMetadataAccess(
            UUID projectId,
            JwtAuthenticationToken authentication,
            AuditRequestContext auditContext) {
        Project project = getProject(projectId);
        if (!isAdmin(authentication)
                && !isAuditor(authentication)
                && !project.getOwnerUserId().equals(authentication.getToken().getSubject())) {
            deny(projectId, authentication, auditContext);
        }
    }

    private Project getProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Project not found"));
    }

    private void deny(
            UUID projectId,
            JwtAuthenticationToken authentication,
            AuditRequestContext auditContext) {
        auditLogService.record(
                authentication,
                "ACCESS_DENIED",
                "PROJECT",
                projectId,
                "DENIED",
                auditContext);
        throw new AccessDeniedException("You cannot access this project");
    }

    private boolean isAdmin(JwtAuthenticationToken authentication) {
        return hasRole(authentication, "ROLE_ADMIN");
    }

    private boolean isAuditor(JwtAuthenticationToken authentication) {
        return hasRole(authentication, "ROLE_AUDITOR");
    }

    private boolean hasRole(
            JwtAuthenticationToken authentication,
            String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(role));
    }
}
