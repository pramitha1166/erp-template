package com.eudext.erp.testsupport;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests that need a real Postgres and Redis
 * (NFR-M2).
 *
 * <p>The containers are deliberately started by hand here — the singleton
 * pattern — rather than through {@code @Testcontainers}/{@code @Container}.
 * That extension stops a static container when its *class* finishes, but
 * every subclass here shares the same Spring context configuration, so the
 * framework's context cache hands the second and later classes the first
 * class's {@code DataSource} — still pointing at the container that was
 * just stopped. The suite then fails from the second IT class onwards with
 * "connection refused" on a stale port. Started once per JVM and never
 * stopped (Ryuk reaps them at exit), the URL stays valid for the cached
 * context's whole life.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("eudext_erp")
                    .withUsername("eudext")
                    .withPassword("eudext");

    static final RedisContainer REDIS = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }
}
