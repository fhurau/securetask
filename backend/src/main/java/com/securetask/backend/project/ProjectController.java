package com.securetask.backend.project;

import java.net.URI;
import java.util.List;
import java.util.UUID;

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
public class ProjectController {

    private final ProjectService projectService;

    ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    ResponseEntity<ProjectResponse> create(
            @Valid @RequestBody ProjectRequest request,
            JwtAuthenticationToken authentication) {
        ProjectResponse project = projectService.create(request, authentication);
        return ResponseEntity
                .created(URI.create("/api/v1/projects/" + project.id()))
                .body(project);
    }

    @GetMapping
    List<ProjectResponse> findAll(JwtAuthenticationToken authentication) {
        return projectService.findAll(authentication);
    }

    @GetMapping("/{id}")
    ProjectResponse findById(
            @PathVariable UUID id,
            JwtAuthenticationToken authentication) {
        return projectService.findById(id, authentication);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    ProjectResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody ProjectRequest request,
            JwtAuthenticationToken authentication) {
        return projectService.update(id, request, authentication);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    ResponseEntity<Void> delete(
            @PathVariable UUID id,
            JwtAuthenticationToken authentication) {
        projectService.delete(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
