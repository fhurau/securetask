package com.securetask.backend.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.securetask.backend.config.OpenApiConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class LogoutController {

    private final TokenBlacklistService tokenBlacklistService;

    LogoutController(TokenBlacklistService tokenBlacklistService) {
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Revoke the current token",
            description = "Blacklists the caller's current access token for its remaining lifetime.")
    ResponseEntity<Void> logout(JwtAuthenticationToken authentication) {
        tokenBlacklistService.blacklist(authentication.getToken());
        return ResponseEntity.noContent().build();
    }
}
