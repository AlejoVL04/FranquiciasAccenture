package com.example.franchiseapi.integration;

import com.example.franchiseapi.controller.BranchController;
import com.example.franchiseapi.controller.FranchiseController;
import com.example.franchiseapi.controller.ProductController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Acceptance criterion 1 and the OpenAPI requirement: the Spring Boot context
 * starts, Flyway migrates the schema, and the generated contract is served.
 */
@DisplayName("Application wiring")
class ApplicationContextIT extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("criterion 1: the Spring Boot context starts with every layer wired")
    void contextLoads() {
        assertThat(applicationContext.getBean(FranchiseController.class)).isNotNull();
        assertThat(applicationContext.getBean(BranchController.class)).isNotNull();
        assertThat(applicationContext.getBean(ProductController.class)).isNotNull();
    }

    @Test
    @DisplayName("Flyway applied all three migrations")
    void flywayMigrationsApplied() {
        Integer applied = applicationContext.getBean(org.springframework.jdbc.core.JdbcTemplate.class)
                .queryForObject(
                        "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1", Integer.class);
        assertThat(applied).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("the stock CHECK constraint is enforced by the database itself")
    void stockCheckConstraintExists() {
        var jdbc = applicationContext.getBean(org.springframework.jdbc.core.JdbcTemplate.class);
        Integer constraints = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_schema = DATABASE()
                  AND table_name = 'products'
                  AND constraint_name = 'chk_products_stock_non_negative'
                """, Integer.class);
        assertThat(constraints).isEqualTo(1);
    }

    @Test
    @DisplayName("the OpenAPI contract is published with the documented title and version")
    void openApiContractIsPublished() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Franchise Management API"))
                .andExpect(jsonPath("$.info.version").value("1.0"))
                .andExpect(jsonPath("$.paths['/api/v1/franchises']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/franchises/{franchiseId}/branches']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/branches/{branchId}/products']").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/branches/{branchId}/products/{productId}/stock']").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/franchises/{franchiseId}/top-stock-products']").exists());
    }

    @Test
    @DisplayName("the health probe used by Docker reports UP")
    void healthProbeIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
