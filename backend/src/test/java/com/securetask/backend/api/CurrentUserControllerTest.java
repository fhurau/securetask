package com.securetask.backend.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class CurrentUserControllerTest {

    @Test
    void returnsIdentityAndRealmRolesFromJwt() {
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of(
                        "sub", "user-id",
                        "preferred_username", "user1@example.com",
                        "email", "user1@example.com",
                        "realm_access", Map.of("roles", List.of("USER", "offline_access"))));

        var response = new CurrentUserController().me(new JwtAuthenticationToken(jwt));

        assertThat(response.username()).isEqualTo("user1@example.com");
        assertThat(response.email()).isEqualTo("user1@example.com");
        assertThat(response.roles()).containsExactly("USER", "offline_access");
    }
}
