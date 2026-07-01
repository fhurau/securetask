package com.securetask.backend.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.securetask.backend.auth.BlacklistTokenValidator;
import com.securetask.backend.auth.TokenBlacklistService;
import com.securetask.backend.audit.AuditRequestContext;
import com.securetask.backend.config.RateLimitFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Verifies the three Redis-backed features end to end against real containers:
 * token blacklist rejection, project-list caching with mutation eviction,
 * and per-user fixed-window rate limiting.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class RedisFeaturesIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("securetask")
            .withUsername("securetask")
            .withPassword("securetask");

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("securetask.rate-limit.requests-per-window", () -> 3);
        registry.add("securetask.rate-limit.window-seconds", () -> 60);
    }

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void revokedTokenIsRejectedBeforeExpiry() {
        Jwt jwt = jwt("user-blacklist-1", "jti-blacklist-1", Instant.now().plusSeconds(60));
        BlacklistTokenValidator validator = new BlacklistTokenValidator(tokenBlacklistService);

        assertThat(validator.validate(jwt).hasErrors()).isFalse();

        tokenBlacklistService.blacklist(jwt);

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
        assertThat(jwt.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void cachedProjectListIsServedUntilMutationEvicts() {
        JwtAuthenticationToken authentication = authentication("user-cache-1", "jti-cache-1");

        assertThat(projectService.findAll(authentication)).isEmpty();

        projectRepository.saveAndFlush(
                new Project("Bypassed project", null, "user-cache-1", "user-cache-1@example.com"));

        assertThat(projectService.findAll(authentication)).isEmpty();

        projectService.create(
                new ProjectRequest("Created project", null),
                authentication,
                new AuditRequestContext("127.0.0.1", "test-agent", "corr-cache-1"));

        List<ProjectResponse> afterEviction = projectService.findAll(authentication);
        assertThat(afterEviction).hasSize(2);
        assertThat(afterEviction).extracting(ProjectResponse::name)
                .containsExactlyInAnyOrder("Bypassed project", "Created project");
    }

    @Test
    void rateLimiterReturns429AfterThreshold() throws Exception {
        JwtAuthenticationToken authentication = authentication("user-rate-1", "jti-rate-1");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/projects");
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 3; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            rateLimitFilter.doFilter(request, response, chain);
            assertThat(response.getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse throttledResponse = new MockHttpServletResponse();
        rateLimitFilter.doFilter(request, throttledResponse, chain);
        assertThat(throttledResponse.getStatus()).isEqualTo(429);
    }

    private Jwt jwt(String subject, String jti, Instant expiresAt) {
        return new Jwt(
                "token-" + jti,
                Instant.now(),
                expiresAt,
                Map.of("alg", "none"),
                Map.of(
                        "sub", subject,
                        "jti", jti,
                        "preferred_username", subject + "@example.com",
                        "email", subject + "@example.com"));
    }

    private JwtAuthenticationToken authentication(String subject, String jti) {
        return new JwtAuthenticationToken(
                jwt(subject, jti, Instant.now().plusSeconds(300)),
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                subject + "@example.com");
    }
}
