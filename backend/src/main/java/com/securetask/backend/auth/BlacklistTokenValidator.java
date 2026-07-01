package com.securetask.backend.auth;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class BlacklistTokenValidator implements OAuth2TokenValidator<Jwt> {

    private final TokenBlacklistService tokenBlacklistService;

    public BlacklistTokenValidator(TokenBlacklistService tokenBlacklistService) {
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (tokenBlacklistService.isBlacklisted(jwt.getId())) {
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "token_revoked",
                    "Token has been revoked",
                    null));
        }
        return OAuth2TokenValidatorResult.success();
    }
}
