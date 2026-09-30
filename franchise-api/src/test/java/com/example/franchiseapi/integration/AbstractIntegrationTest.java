package com.example.franchiseapi.integration;

import com.example.franchiseapi.repository.BranchRepository;
import com.example.franchiseapi.repository.FranchiseRepository;
import com.example.franchiseapi.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared setup for the integration tests: a real MySQL 8 instance provided by
 * Testcontainers, migrated by Flyway exactly as in production.
 * <p>
 * H2 is deliberately not used: the top-stock report relies on a MySQL window
 * function and on the schema written by the Flyway scripts, so testing it
 * against a different engine would prove nothing about production behaviour.
 * <p>
 * The container is started once per JVM from a static initialiser and shared by
 * every subclass, so the whole integration suite pays the startup cost only once.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class AbstractIntegrationTest {

    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
                    .withDatabaseName("franchise_db")
                    .withUsername("franchise_user")
                    .withPassword("franchise_password");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected FranchiseRepository franchiseRepository;

    @Autowired
    protected BranchRepository branchRepository;

    @Autowired
    protected ProductRepository productRepository;

    /**
     * Each test starts from an empty schema. Children are removed before their
     * parents so the foreign keys are respected without relying on cascades.
     */
    @BeforeEach
    void resetDatabase() {
        productRepository.deleteAll();
        branchRepository.deleteAll();
        franchiseRepository.deleteAll();
    }
}
