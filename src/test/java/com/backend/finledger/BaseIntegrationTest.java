package com.backend.finledger;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;

/**
 * Base integration test class utilizing Testcontainers MySQL 8 if Docker daemon is running,
 * seamlessly falling back to local database configuration when running on non-Docker hosts.
 */
@SpringBootTest
public abstract class BaseIntegrationTest {

    public static final MySQLContainer<?> MYSQL_CONTAINER;

    static {
        MySQLContainer<?> container = null;
        try {
            if (DockerClientFactory.instance().isDockerAvailable()) {
                container = new MySQLContainer<>("mysql:8.0.36")
                        .withDatabaseName("finledger_test")
                        .withUsername("test")
                        .withPassword("test");
                container.start();
            }
        } catch (Throwable t) {
            // Docker daemon not detected on host; tests will execute against local spring.datasource
            container = null;
        }
        MYSQL_CONTAINER = container;
    }

    @DynamicPropertySource
    static void configureTestProperties(DynamicPropertyRegistry registry) {
        if (MYSQL_CONTAINER != null && MYSQL_CONTAINER.isRunning()) {
            registry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);
            registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
            registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
            registry.add("spring.datasource.driver-class-name", MYSQL_CONTAINER::getDriverClassName);
            registry.add("spring.flyway.url", MYSQL_CONTAINER::getJdbcUrl);
            registry.add("spring.flyway.user", MYSQL_CONTAINER::getUsername);
            registry.add("spring.flyway.password", MYSQL_CONTAINER::getPassword);
        }
    }
}
