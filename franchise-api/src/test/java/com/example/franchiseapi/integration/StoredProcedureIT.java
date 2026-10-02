package com.example.franchiseapi.integration;

import com.example.franchiseapi.exception.StoredProcedureErrors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Calls the stored procedures directly over JDBC, bypassing the API, to prove
 * that MySQL enforces the business rules on its own and reports violations
 * with the agreed error numbers.
 */
@DisplayName("Stored procedures (called directly)")
class StoredProcedureIT extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Flyway created every procedure the API relies on")
    void proceduresExist() {
        assertThat(jdbc.queryForList("""
                SELECT routine_name
                FROM information_schema.routines
                WHERE routine_schema = DATABASE()
                  AND routine_type = 'PROCEDURE'
                """, String.class))
                .contains(
                        "sp_franchise_create", "sp_franchise_update_name", "sp_franchise_top_stock_products",
                        "sp_branch_create", "sp_branch_update_name",
                        "sp_product_create", "sp_product_update_stock", "sp_product_update_name",
                        "sp_product_delete");
    }

    @Test
    @DisplayName("sp_franchise_create trims the name and returns the stored row")
    void createsFranchiseTrimmingName() {
        Map<String, Object> row = jdbc.queryForMap("CALL sp_franchise_create(?)", "  Franquicia A  ");

        assertThat(row.get("name")).isEqualTo("Franquicia A");
        assertThat(row.get("id")).isNotNull();
        assertThat(row.get("created_at")).isNotNull();
        assertThat(row.get("updated_at")).isNotNull();
    }

    @Test
    @DisplayName("a name differing only in case is rejected with 50409")
    void rejectsDuplicateNameIgnoringCase() {
        jdbc.queryForMap("CALL sp_franchise_create(?)", "Franquicia A");

        assertThat(errnoOf(() -> jdbc.queryForMap("CALL sp_franchise_create(?)", "FRANQUICIA A")))
                .isEqualTo(StoredProcedureErrors.CONFLICT);
    }

    @Test
    @DisplayName("a blank or over-long name is rejected with 50400")
    void rejectsInvalidNames() {
        assertThat(errnoOf(() -> jdbc.queryForMap("CALL sp_franchise_create(?)", "   ")))
                .isEqualTo(StoredProcedureErrors.BAD_REQUEST);
        assertThat(errnoOf(() -> jdbc.queryForMap("CALL sp_franchise_create(?)", "x".repeat(101))))
                .isEqualTo(StoredProcedureErrors.BAD_REQUEST);
    }

    @Test
    @DisplayName("a branch on an unknown franchise is rejected with 50404")
    void rejectsBranchOnUnknownFranchise() {
        assertThat(errnoOf(() -> jdbc.queryForMap("CALL sp_branch_create(?, ?)", 999_999L, "Sucursal")))
                .isEqualTo(StoredProcedureErrors.NOT_FOUND);
    }

    @Test
    @DisplayName("a negative or missing stock is rejected with 50400 and nothing is inserted")
    void rejectsInvalidStock() {
        long branchId = newBranch(newFranchise(), "Sucursal Norte");

        assertThat(errnoOf(() -> jdbc.queryForMap("CALL sp_product_create(?, ?, ?)", branchId, "Mouse", -1)))
                .isEqualTo(StoredProcedureErrors.BAD_REQUEST);
        assertThat(errnoOf(() -> jdbc.queryForMap("CALL sp_product_create(?, ?, ?)", branchId, "Mouse", null)))
                .isEqualTo(StoredProcedureErrors.BAD_REQUEST);
        assertThat(productRepository.count()).isZero();
    }

    @Test
    @DisplayName("a product addressed through a branch that does not own it is rejected with 50404 and left untouched")
    void rejectsProductThroughWrongBranch() {
        long franchiseId = newFranchise();
        long ownerBranch = newBranch(franchiseId, "Sucursal Norte");
        long otherBranch = newBranch(franchiseId, "Sucursal Sur");
        long productId = idOf(jdbc.queryForMap("CALL sp_product_create(?, ?, ?)", ownerBranch, "Mouse", 10));

        assertThat(errnoOf(() -> jdbc.queryForMap("CALL sp_product_update_stock(?, ?, ?)",
                otherBranch, productId, 99)))
                .isEqualTo(StoredProcedureErrors.NOT_FOUND);
        assertThat(errnoOf(() -> jdbc.update("CALL sp_product_delete(?, ?)", otherBranch, productId)))
                .isEqualTo(StoredProcedureErrors.NOT_FOUND);

        assertThat(productRepository.findById(productId))
                .get()
                .extracting(product -> product.getStock())
                .isEqualTo(10);
    }

    @Test
    @DisplayName("the top-stock report of an unknown franchise is rejected with 50404")
    void rejectsReportOfUnknownFranchise() {
        assertThat(errnoOf(() -> jdbc.queryForList("CALL sp_franchise_top_stock_products(?)", 999_999L)))
                .isEqualTo(StoredProcedureErrors.NOT_FOUND);
    }

    // ---------------------------------------------------------------- helpers

    private long newFranchise() {
        return idOf(jdbc.queryForMap("CALL sp_franchise_create(?)", "Franquicia A"));
    }

    private long newBranch(long franchiseId, String name) {
        return idOf(jdbc.queryForMap("CALL sp_branch_create(?, ?)", franchiseId, name));
    }

    private static long idOf(Map<String, Object> row) {
        return ((Number) row.get("id")).longValue();
    }

    private static int errnoOf(Runnable procedureCall) {
        Throwable thrown = catchThrowable(procedureCall::run);
        assertThat(thrown).as("the procedure call should have been rejected").isNotNull();
        Throwable root = NestedExceptionUtils.getRootCause(thrown);
        assertThat(root).isInstanceOf(SQLException.class);
        return ((SQLException) root).getErrorCode();
    }
}
