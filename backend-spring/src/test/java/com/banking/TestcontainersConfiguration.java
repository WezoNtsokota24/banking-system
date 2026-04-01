package com.banking;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;

/**
 * TestContainers configuration for Spring Boot 2.7.
 * 
 * This configuration is automatically picked up by Spring Boot test context
 * to configure datasource properties when TestContainers are used.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /**
     * Creates a MySQL container for testing.
     * 
     * Returns a MySQLContainer that will be used by the test suite.
     * 
     * @return MySQLContainer configured and started for testing
     */
    @Bean
    public MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>("mysql:8.0")
                .withDatabaseName("banking_test_db")
                .withUsername("test_user")
                .withPassword("test_password");
    }
}

