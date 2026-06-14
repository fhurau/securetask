package com.securetask.backend.project;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.securetask.backend.audit.AuditRequestContext;
import com.securetask.backend.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Projects")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class ProjectController {

    private final ProjectService projectService;

    ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
            summary = "Create a project",
            description = "Creates a project owned by the authenticated user. Requires USER or ADMIN.")
    ResponseEntity<ProjectResponse> create(
            @Valid @RequestBody ProjectRequest request,
            JwtAuthenticationToken authentication,
            HttpServletRequest httpRequest) {
        ProjectResponse project = projectService.create(
                request,
                authentication,
                AuditRequestContext.from(httpRequest));
        return ResponseEntity
                .created(URI.create("/api/v1/projects/" + project.id()))
                .body(project);
    }

    @GetMapping
    @Operation(
            summary = "List accessible projects",
            description = "Lists owned projects, or all projects for an administrator.")
    List<ProjectResponse> findAll(JwtAuthenticationToken authentication) {
        return projectService.findAll(authentication);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get a project",
            description = "Returns a project when the caller is its owner or an administrator.")
    ProjectResponse findById(
            @PathVariable UUID id,
            JwtAuthenticationToken authentication,
            HttpServletRequest httpRequest) {
        return projectService.findById(
                id,
                authentication,
                AuditRequestContext.from(httpRequest));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
            summary = "Update a project",
            description = "Updates an accessible project. Requires USER or ADMIN.")
    ProjectResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody ProjectRequest request,
            JwtAuthenticationToken authentication,
            HttpServletRequest httpRequest) {
        return projectService.update(
                id,
                request,
                authentication,
                AuditRequestContext.from(httpRequest));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
            summary = "Delete a project",
            description = "Deletes an accessible project and its document metadata. Requires USER or ADMIN.")
    ResponseEntity<Void> delete(
            @PathVariable UUID id,
            JwtAuthenticationToken authentication,
            HttpServletRequest httpRequest) {
        projectService.delete(
                id,
                authentication,
                AuditRequestContext.from(httpRequest));
        return ResponseEntity.noContent().build();
    }
}
