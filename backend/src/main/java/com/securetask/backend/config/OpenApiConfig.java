package com.securetask.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(
        info = @Info(
                title = "SecureTask API",
                version = "v1",
                description = "Local demo API for projects, documents, and audit events."),
        tags = {
                @Tag(name = "Health", description = "Public service health endpoint."),
                @Tag(name = "User Profile", description = "Authenticated user identity."),
                @Tag(name = "Projects", description = "Authorized project operations."),
                @Tag(name = "Documents", description = "Authorized project document operations."),
                @Tag(name = "Audit Logs", description = "Administrator and auditor events.")
        })
@SecurityScheme(
        name = OpenApiConfig.BEARER_SCHEME,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Keycloak access token.")
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearer-jwt";
}
