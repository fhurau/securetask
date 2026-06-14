package com.securetask.backend.audit;

import java.util.List;

import com.securetask.backend.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-logs")
@Tag(name = "Audit Logs")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class AuditLogController {

    private final AuditLogService auditLogService;

    AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    @Operation(
            summary = "List audit events",
            description = "Returns security-relevant events newest first. Requires ADMIN or AUDITOR.")
    List<AuditLogResponse> findAll() {
        return auditLogService.findAll();
    }
}
