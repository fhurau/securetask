package com.securetask.backend.auth;

import java.time.Duration;
import java.time.Instant;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "token-blacklist:";

    private final StringRedisTemplate redisTemplate;

    TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void blacklist(Jwt jwt) {
        Duration remaining = Duration.between(Instant.now(), jwt.getExpiresAt());
        if (remaining.isNegative() || remaining.isZero()) {
            return;
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + jwt.getId(), "revoked", remaining);
    }

    public boolean isBlacklisted(String jti) {
        return jti != null && Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
    }
}
