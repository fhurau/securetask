package com.securetask.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class SecurityConfigTest {

    @Test
    void convertsKeycloakRealmRolesToSpringAuthorities() {
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of(
                        "sub", "user-id",
                        "realm_access", Map.of("roles", List.of("USER", "ADMIN"))));

        var authorities = new SecurityConfig.KeycloakRealmRoleConverter().convert(jwt);

        assertThat(authorities)
                .extracting("authority")
                .containsExactly("ROLE_USER", "ROLE_ADMIN");
    }
}
