package com.securetask.backend.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CurrentUserController {

    @GetMapping("/me")
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

    public record CurrentUserResponse(String username, String email, List<String> roles) {
    }
}
