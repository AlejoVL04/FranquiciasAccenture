package com.example.franchiseapi.integration;

import com.example.franchiseapi.entity.Branch;
import com.example.franchiseapi.entity.Franchise;
import com.example.franchiseapi.entity.Product;
import com.example.franchiseapi.repository.projection.TopStockProductProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Acceptance criterion 7, exercised against real MySQL so the window function
 * in the native query is genuinely executed by the database engine.
 */
@DisplayName("Top stock product per branch (criterion 7)")
class TopStockProductIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("criterion 23: returns Mouse 50, Laptop 40 and Monitor 30 for the reference scenario")
    void resolvesTheReferenceScenario() throws Exception {
        Franchise franchiseA = franchise("Franquicia A");

        Branch north = branch(franchiseA, "Sucursal Norte");
        product(north, "Laptop", 20);
        product(north, "Mouse", 50);
        product(north, "Teclado", 30);

        Branch centre = branch(franchiseA, "Sucursal Centro");
        product(centre, "Monitor", 15);
        product(centre, "Laptop", 40);
        product(centre, "Mouse", 25);

        Branch south = branch(franchiseA, "Sucursal Sur");
        product(south, "Teclado", 10);
        product(south, "Mouse", 5);
        product(south, "Monitor", 30);

        mockMvc.perform(get("/api/v1/franchises/{id}/top-stock-products", franchiseA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))

                .andExpect(jsonPath("$[0].franchiseName").value("Franquicia A"))
                .andExpect(jsonPath("$[0].branchName").value("Sucursal Norte"))
                .andExpect(jsonPath("$[0].productName").value("Mouse"))
                .andExpect(jsonPath("$[0].stock").value(50))

                .andExpect(jsonPath("$[1].branchName").value("Sucursal Centro"))
                .andExpect(jsonPath("$[1].productName").value("Laptop"))
                .andExpect(jsonPath("$[1].stock").value(40))

                .andExpect(jsonPath("$[2].branchName").value("Sucursal Sur"))
                .andExpect(jsonPath("$[2].productName").value("Monitor"))
                .andExpect(jsonPath("$[2].stock").value(30));
    }

    @Test
    @DisplayName("the payload identifies franchise, branch, product and stock on every row")
    void payloadIdentifiesTheWholeChain() throws Exception {
        Franchise franchiseA = franchise("Franquicia A");
        Branch north = branch(franchiseA, "Sucursal Norte");
        Product mouse = product(north, "Mouse", 50);

        mockMvc.perform(get("/api/v1/franchises/{id}/top-stock-products", franchiseA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].franchiseId").value(franchiseA.getId().intValue()))
                .andExpect(jsonPath("$[0].franchiseName").value("Franquicia A"))
                .andExpect(jsonPath("$[0].branchId").value(north.getId().intValue()))
                .andExpect(jsonPath("$[0].branchName").value("Sucursal Norte"))
                .andExpect(jsonPath("$[0].productId").value(mouse.getId().intValue()))
                .andExpect(jsonPath("$[0].productName").value("Mouse"))
                .andExpect(jsonPath("$[0].stock").value(50));
    }

    @Test
    @DisplayName("returns at most one product per branch even when a branch holds many")
    void returnsAtMostOneProductPerBranch() throws Exception {
        Franchise franchiseA = franchise("Franquicia A");
        Branch north = branch(franchiseA, "Sucursal Norte");
        for (int i = 1; i <= 25; i++) {
            product(north, "Producto " + i, i);
        }

        mockMvc.perform(get("/api/v1/franchises/{id}/top-stock-products", franchiseA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productName").value("Producto 25"))
                .andExpect(jsonPath("$[0].stock").value(25));
    }

    @Test
    @DisplayName("omits a branch that holds no products by default")
    void omitsBranchWithoutProducts() throws Exception {
        Franchise franchiseA = franchise("Franquicia A");
        Branch north = branch(franchiseA, "Sucursal Norte");
        product(north, "Mouse", 50);
        branch(franchiseA, "Sucursal Vacia");

        mockMvc.perform(get("/api/v1/franchises/{id}/top-stock-products", franchiseA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].branchName").value("Sucursal Norte"));
    }

    @Test
    @DisplayName("reports a branch that holds no products with null product fields when asked to")
    void reportsBranchWithoutProductsOnDemand() throws Exception {
        Franchise franchiseA = franchise("Franquicia A");
        Branch north = branch(franchiseA, "Sucursal Norte");
        product(north, "Mouse", 50);
        Branch empty = branch(franchiseA, "Sucursal Vacia");

        mockMvc.perform(get("/api/v1/franchises/{id}/top-stock-products", franchiseA.getId())
                        .param("includeBranchesWithoutProducts", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].branchId").value(empty.getId().intValue()))
                .andExpect(jsonPath("$[1].branchName").value("Sucursal Vacia"))
                // Absent from the payload because null fields are stripped by Jackson.
                .andExpect(jsonPath("$[1].productId").doesNotExist())
                .andExpect(jsonPath("$[1].productName").doesNotExist())
                .andExpect(jsonPath("$[1].stock").doesNotExist());
    }

    @Test
    @DisplayName("a stock of 0 is a valid maximum and is reported, not treated as missing")
    void reportsZeroStockAsMaximum() throws Exception {
        Franchise franchiseA = franchise("Franquicia A");
        Branch branch = branch(franchiseA, "Sucursal Agotada");
        product(branch, "Mouse", 0);
        product(branch, "Teclado", 0);

        mockMvc.perform(get("/api/v1/franchises/{id}/top-stock-products", franchiseA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productName").value("Mouse"))
                .andExpect(jsonPath("$[0].stock").value(0))
                .andExpect(jsonPath("$[0].productId").exists());
    }

    @Test
    @DisplayName("breaks a tie on the maximum stock by returning the oldest product")
    void breaksTieByOldestProduct() throws Exception {
        Franchise franchiseA = franchise("Franquicia A");
        Branch branch = branch(franchiseA, "Sucursal Empate");
        Product first = product(branch, "Mouse", 50);
        Product second = product(branch, "Teclado", 50);
        product(branch, "Monitor", 10);

        assertThat(first.getId()).isLessThan(second.getId());

        mockMvc.perform(get("/api/v1/franchises/{id}/top-stock-products", franchiseA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productId").value(first.getId().intValue()))
                .andExpect(jsonPath("$[0].productName").value("Mouse"))
                .andExpect(jsonPath("$[0].stock").value(50));
    }

    @Test
    @DisplayName("the tie-break is stable across repeated executions")
    void tieBreakIsDeterministic() {
        Franchise franchiseA = franchise("Franquicia A");
        Branch branch = branch(franchiseA, "Sucursal Empate");
        Product first = product(branch, "Mouse", 50);
        product(branch, "Teclado", 50);
        product(branch, "Monitor", 50);

        for (int attempt = 0; attempt < 5; attempt++) {
            List<TopStockProductProjection> rows =
                    productRepository.findTopStockProductPerBranch(franchiseA.getId());
            assertThat(rows).singleElement()
                    .extracting(TopStockProductProjection::getProductId)
                    .isEqualTo(first.getId());
        }
    }

    @Test
    @DisplayName("ignores branches and products belonging to other franchises")
    void isScopedToTheRequestedFranchise() throws Exception {
        Franchise franchiseA = franchise("Franquicia A");
        Branch northA = branch(franchiseA, "Sucursal Norte");
        product(northA, "Mouse", 50);

        Franchise franchiseB = franchise("Franquicia B");
        Branch northB = branch(franchiseB, "Sucursal Norte");
        product(northB, "Monitor", 9999);

        mockMvc.perform(get("/api/v1/franchises/{id}/top-stock-products", franchiseA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].franchiseName").value("Franquicia A"))
                .andExpect(jsonPath("$[0].productName").value("Mouse"))
                .andExpect(jsonPath("$[0].stock").value(50));
    }

    @Test
    @DisplayName("returns an empty list for a franchise that has no branches")
    void returnsEmptyListForFranchiseWithoutBranches() throws Exception {
        Franchise franchiseA = franchise("Franquicia Sin Sucursales");

        mockMvc.perform(get("/api/v1/franchises/{id}/top-stock-products", franchiseA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("returns 404 for an unknown franchise, which an empty list would not distinguish")
    void returnsNotFoundForUnknownFranchise() throws Exception {
        mockMvc.perform(get("/api/v1/franchises/{id}/top-stock-products", 9999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Franchise with id 9999 not found"));
    }

    @Test
    @DisplayName("resolves the whole report with a single query, independent of the branch count")
    void resolvesReportInASingleQuery() {
        Franchise franchiseA = franchise("Franquicia Grande");
        for (int b = 1; b <= 10; b++) {
            Branch branch = branch(franchiseA, "Sucursal " + b);
            for (int p = 1; p <= 10; p++) {
                product(branch, "Producto " + p, b * p);
            }
        }

        List<TopStockProductProjection> rows =
                productRepository.findTopStockProductPerBranch(franchiseA.getId());

        // 10 branches, 100 products: one row per branch comes back, and the winner
        // of each branch is "Producto 10" because stock grows with p.
        assertThat(rows).hasSize(10);
        assertThat(rows)
                .extracting(TopStockProductProjection::getProductName,
                        TopStockProductProjection::getStock)
                .contains(tuple("Producto 10", 10), tuple("Producto 10", 100));
        assertThat(rows)
                .extracting(TopStockProductProjection::getBranchId)
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("reflects a stock update immediately")
    void reflectsStockUpdates() throws Exception {
        Franchise franchiseA = franchise("Franquicia A");
        Branch north = branch(franchiseA, "Sucursal Norte");
        product(north, "Mouse", 50);
        Product teclado = product(north, "Teclado", 30);

        mockMvc.perform(get("/api/v1/franchises/{id}/top-stock-products", franchiseA.getId()))
                .andExpect(jsonPath("$[0].productName").value("Mouse"));

        teclado.setStock(99);
        productRepository.saveAndFlush(teclado);

        mockMvc.perform(get("/api/v1/franchises/{id}/top-stock-products", franchiseA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productName").value("Teclado"))
                .andExpect(jsonPath("$[0].stock").value(99));
    }

    // ---------------------------------------------------------------- fixtures

    private Franchise franchise(String name) {
        return franchiseRepository.saveAndFlush(Franchise.builder().name(name).build());
    }

    private Branch branch(Franchise franchise, String name) {
        return branchRepository.saveAndFlush(
                Branch.builder().name(name).franchise(franchise).build());
    }

    private Product product(Branch branch, String name, int stock) {
        return productRepository.saveAndFlush(
                Product.builder().name(name).stock(stock).branch(branch).build());
    }
}
