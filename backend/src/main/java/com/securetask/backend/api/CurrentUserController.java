package com.securetask.backend.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.securetask.backend.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "User Profile")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class CurrentUserController {

    @GetMapping("/me")
    @Operation(
            summary = "Get current user",
            description = "Returns identity and realm roles from the validated access token.")
    public CurrentUserResponse me(JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();
        return new CurrentUserResponse(
                jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("email"),
                realmRoles(jwt));
    }

    private List<String> realmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) {
            return List.of();
        }

        Object roles = realmAccess.get("roles");
        if (!(roles instanceof Collection<?> roleNames)) {
            return List.of();
        }

        return roleNames.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .sorted()
                .toList();
    }

    @Schema(description = "Authenticated user details derived from the JWT.")
    public record CurrentUserResponse(
            @Schema(description = "Keycloak preferred username.", example = "user1@example.com")
            String username,
            @Schema(description = "Email claim from the access token.", example = "user1@example.com")
            String email,
            @Schema(description = "Keycloak realm roles.", example = "[\"USER\"]")
            List<String> roles) {
    }
}
