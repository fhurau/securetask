package com.securetask.backend.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.securetask.backend.audit.AuditLogResponse;
import com.securetask.backend.audit.AuditLogService;
import com.securetask.backend.audit.AuditRequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Verifies the full async audit path: ProjectService publishes an AuditEvent to Kafka,
 * AuditEventConsumer reads it back off the "audit-events" topic, and AuditLogService
 * persists the resulting row to Postgres.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuditEventStreamingIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("securetask")
            .withUsername("securetask")
            .withPassword("securetask");

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Autowired
    private ProjectService projectService;

    @Autowired
    private AuditLogService auditLogService;

    @Test
    void projectCreationIsAuditedThroughKafka() {
        JwtAuthenticationToken authentication = authentication("user-1", "user1@example.com");
        AuditRequestContext auditContext =
                new AuditRequestContext("127.0.0.1", "integration-test-agent", "corr-1");

        ProjectResponse response = projectService.create(
                new ProjectRequest("Kafka project", "Created via streamed audit test"),
                authentication,
                auditContext);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            List<AuditLogResponse> logs = auditLogService.findAll();
            assertThat(logs).anyMatch(log ->
                    log.resourceId().equals(response.id())
                            && log.action().equals("PROJECT_CREATED")
                            && log.result().equals("SUCCESS")
                            && log.correlationId().equals("corr-1"));
        });
    }

    private JwtAuthenticationToken authentication(String subject, String email) {
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of(
                        "sub", subject,
                        "preferred_username", email,
                        "email", email));
        return new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                email);
    }
}
