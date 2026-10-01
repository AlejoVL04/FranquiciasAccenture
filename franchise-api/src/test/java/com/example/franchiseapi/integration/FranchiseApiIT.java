package com.example.franchiseapi.integration;

import com.example.franchiseapi.dto.request.CreateBranchRequest;
import com.example.franchiseapi.dto.request.CreateFranchiseRequest;
import com.example.franchiseapi.dto.request.CreateProductRequest;
import com.example.franchiseapi.dto.request.UpdateNameRequest;
import com.example.franchiseapi.dto.request.UpdateStockRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage of acceptance criteria 2 to 6 and 8, against a real MySQL
 * instance: every request goes through the web layer, the services, Hibernate
 * and the migrated schema.
 */
@DisplayName("Franchise API (end to end)")
class FranchiseApiIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("criterion 2: creating a franchise returns 201 with its generated id and timestamps")
    void createsFranchise() throws Exception {
        mockMvc.perform(post("/api/v1/franchises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateFranchiseRequest("Franquicia Medellin"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Franquicia Medellin"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("criterion 2: a duplicated franchise name returns 409 with the standard error body")
    void rejectsDuplicatedFranchise() throws Exception {
        createFranchise("Franquicia Medellin");

        mockMvc.perform(post("/api/v1/franchises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateFranchiseRequest("Franquicia Medellin"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.path").value("/api/v1/franchises"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("a blank franchise name returns 400 with the offending field reported")
    void rejectsBlankFranchiseName() throws Exception {
        mockMvc.perform(post("/api/v1/franchises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.validationErrors.name").value("Franchise name is required"));
    }

    @Test
    @DisplayName("criterion 3: creating a branch returns 201 linked to its franchise")
    void createsBranch() throws Exception {
        long franchiseId = createFranchise("Franquicia Medellin");

        mockMvc.perform(post("/api/v1/franchises/{id}/branches", franchiseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateBranchRequest("Sucursal El Poblado"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Sucursal El Poblado"))
                .andExpect(jsonPath("$.franchiseId").value((int) franchiseId));
    }

    @Test
    @DisplayName("criterion 3: creating a branch on an unknown franchise returns 404")
    void rejectsBranchOnUnknownFranchise() throws Exception {
        mockMvc.perform(post("/api/v1/franchises/{id}/branches", 9999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateBranchRequest("Sucursal El Poblado"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Franchise with id 9999 not found"));
    }

    @Test
    @DisplayName("criterion 3: a duplicated branch name inside one franchise returns 409")
    void rejectsDuplicatedBranchInFranchise() throws Exception {
        long franchiseId = createFranchise("Franquicia Medellin");
        createBranch(franchiseId, "Sucursal El Poblado");

        mockMvc.perform(post("/api/v1/franchises/{id}/branches", franchiseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateBranchRequest("Sucursal El Poblado"))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("the same branch name is accepted in a different franchise")
    void allowsSameBranchNameInAnotherFranchise() throws Exception {
        long medellin = createFranchise("Franquicia Medellin");
        long bogota = createFranchise("Franquicia Bogota");
        createBranch(medellin, "Sucursal Centro");

        mockMvc.perform(post("/api/v1/franchises/{id}/branches", bogota)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateBranchRequest("Sucursal Centro"))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("criterion 4: creating a product returns 201 linked to its branch")
    void createsProduct() throws Exception {
        long branchId = createBranch(createFranchise("Franquicia Medellin"), "Sucursal El Poblado");

        mockMvc.perform(post("/api/v1/branches/{id}/products", branchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateProductRequest("Laptop Lenovo", 25))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Laptop Lenovo"))
                .andExpect(jsonPath("$.stock").value(25))
                .andExpect(jsonPath("$.branchId").value((int) branchId));
    }

    @Test
    @DisplayName("criterion 4: creating a product on an unknown branch returns 404")
    void rejectsProductOnUnknownBranch() throws Exception {
        mockMvc.perform(post("/api/v1/branches/{id}/products", 9999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateProductRequest("Laptop Lenovo", 25))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Branch with id 9999 not found"));
    }

    @Test
    @DisplayName("criterion 4: a duplicated product name inside one branch returns 409")
    void rejectsDuplicatedProductInBranch() throws Exception {
        long branchId = createBranch(createFranchise("Franquicia Medellin"), "Sucursal El Poblado");
        createProduct(branchId, "Laptop Lenovo", 25);

        mockMvc.perform(post("/api/v1/branches/{id}/products", branchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateProductRequest("Laptop Lenovo", 10))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("a negative initial stock returns 400 before the product is created")
    void rejectsNegativeInitialStock() throws Exception {
        long branchId = createBranch(createFranchise("Franquicia Medellin"), "Sucursal El Poblado");

        mockMvc.perform(post("/api/v1/branches/{id}/products", branchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateProductRequest("Laptop Lenovo", -1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.stock")
                        .value("Stock must be greater than or equal to 0"));

        mockMvc.perform(get("/api/v1/branches/{id}/products", branchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("criterion 5: deleting a product returns 204 and removes it")
    void deletesProduct() throws Exception {
        long branchId = createBranch(createFranchise("Franquicia Medellin"), "Sucursal El Poblado");
        long productId = createProduct(branchId, "Laptop Lenovo", 25);

        mockMvc.perform(delete("/api/v1/branches/{branchId}/products/{productId}", branchId, productId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(status().isNotFound());
        assertThat(productRepository.findById(productId)).isEmpty();
    }

    @Test
    @DisplayName("criterion 5: deleting an unknown product returns 404")
    void rejectsDeleteOfUnknownProduct() throws Exception {
        long branchId = createBranch(createFranchise("Franquicia Medellin"), "Sucursal El Poblado");

        mockMvc.perform(delete("/api/v1/branches/{branchId}/products/{productId}", branchId, 9999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Product with id 9999 not found in branch " + branchId));
    }

    @Test
    @DisplayName("criterion 5: deleting a product through a branch that does not own it returns 404 "
            + "and leaves the product untouched")
    void rejectsDeleteThroughWrongBranch() throws Exception {
        long franchiseId = createFranchise("Franquicia Medellin");
        long ownerBranch = createBranch(franchiseId, "Sucursal El Poblado");
        long otherBranch = createBranch(franchiseId, "Sucursal Laureles");
        long productId = createProduct(ownerBranch, "Laptop Lenovo", 25);

        mockMvc.perform(delete("/api/v1/branches/{branchId}/products/{productId}", otherBranch, productId))
                .andExpect(status().isNotFound());

        assertThat(productRepository.findById(productId)).isPresent();
    }

    @Test
    @DisplayName("criterion 6: updating the stock returns 200 with the new value persisted")
    void updatesStock() throws Exception {
        long branchId = createBranch(createFranchise("Franquicia Medellin"), "Sucursal El Poblado");
        long productId = createProduct(branchId, "Laptop Lenovo", 25);

        mockMvc.perform(patch("/api/v1/branches/{branchId}/products/{productId}/stock", branchId, productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdateStockRequest(50))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) productId))
                .andExpect(jsonPath("$.name").value("Laptop Lenovo"))
                .andExpect(jsonPath("$.stock").value(50))
                .andExpect(jsonPath("$.branchId").value((int) branchId));

        assertThat(productRepository.findById(productId))
                .get()
                .extracting(product -> product.getStock())
                .isEqualTo(50);
    }

    @Test
    @DisplayName("criterion 6: the stock may be set to 0")
    void allowsZeroStock() throws Exception {
        long branchId = createBranch(createFranchise("Franquicia Medellin"), "Sucursal El Poblado");
        long productId = createProduct(branchId, "Laptop Lenovo", 25);

        mockMvc.perform(patch("/api/v1/branches/{branchId}/products/{productId}/stock", branchId, productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdateStockRequest(0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(0));
    }

    @Test
    @DisplayName("criterion 6: a negative stock returns 400 and the stored value is unchanged")
    void rejectsNegativeStockUpdate() throws Exception {
        long branchId = createBranch(createFranchise("Franquicia Medellin"), "Sucursal El Poblado");
        long productId = createProduct(branchId, "Laptop Lenovo", 25);

        mockMvc.perform(patch("/api/v1/branches/{branchId}/products/{productId}/stock", branchId, productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdateStockRequest(-5))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.stock")
                        .value("Stock must be greater than or equal to 0"));

        assertThat(productRepository.findById(productId))
                .get()
                .extracting(product -> product.getStock())
                .isEqualTo(25);
    }

    @Test
    @DisplayName("criterion 6: a missing stock field returns 400")
    void rejectsMissingStock() throws Exception {
        long branchId = createBranch(createFranchise("Franquicia Medellin"), "Sucursal El Poblado");
        long productId = createProduct(branchId, "Laptop Lenovo", 25);

        mockMvc.perform(patch("/api/v1/branches/{branchId}/products/{productId}/stock", branchId, productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.stock").value("Stock is required"));
    }

    @Test
    @DisplayName("criterion 6: updating a product through a branch that does not own it returns 404")
    void rejectsStockUpdateThroughWrongBranch() throws Exception {
        long franchiseId = createFranchise("Franquicia Medellin");
        long ownerBranch = createBranch(franchiseId, "Sucursal El Poblado");
        long otherBranch = createBranch(franchiseId, "Sucursal Laureles");
        long productId = createProduct(ownerBranch, "Laptop Lenovo", 25);

        mockMvc.perform(patch("/api/v1/branches/{branchId}/products/{productId}/stock", otherBranch, productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdateStockRequest(50))))
                .andExpect(status().isNotFound());

        assertThat(productRepository.findById(productId))
                .get()
                .extracting(product -> product.getStock())
                .isEqualTo(25);
    }

    @Test
    @DisplayName("renaming a franchise returns 200 and persists the new name")
    void renamesFranchise() throws Exception {
        long franchiseId = createFranchise("Franquicia Medellin");

        mockMvc.perform(patch("/api/v1/franchises/{id}/name", franchiseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdateNameRequest("Franquicia Antioquia"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) franchiseId))
                .andExpect(jsonPath("$.name").value("Franquicia Antioquia"));

        assertThat(franchiseRepository.findById(franchiseId))
                .get()
                .extracting(franchise -> franchise.getName())
                .isEqualTo("Franquicia Antioquia");
    }

    @Test
    @DisplayName("a franchise may be renamed to a different capitalisation of its own name")
    void renamesFranchiseToOwnNameInOtherCase() throws Exception {
        long franchiseId = createFranchise("Franquicia Medellin");

        mockMvc.perform(patch("/api/v1/franchises/{id}/name", franchiseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdateNameRequest("FRANQUICIA MEDELLIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("FRANQUICIA MEDELLIN"));
    }

    @Test
    @DisplayName("renaming a franchise to the name of another one returns 409")
    void rejectsFranchiseRenameToTakenName() throws Exception {
        createFranchise("Franquicia Bogota");
        long franchiseId = createFranchise("Franquicia Medellin");

        mockMvc.perform(patch("/api/v1/franchises/{id}/name", franchiseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdateNameRequest("franquicia bogota"))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("renaming an unknown franchise returns 404 and a blank name returns 400")
    void rejectsInvalidFranchiseRename() throws Exception {
        mockMvc.perform(patch("/api/v1/franchises/{id}/name", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdateNameRequest("Otra"))))
                .andExpect(status().isNotFound());

        long franchiseId = createFranchise("Franquicia Medellin");
        mockMvc.perform(patch("/api/v1/franchises/{id}/name", franchiseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name").value("Name is required"));
    }

    @Test
    @DisplayName("renaming a branch returns 200; a name taken in the same franchise returns 409")
    void renamesBranch() throws Exception {
        long franchiseId = createFranchise("Franquicia Medellin");
        long branchId = createBranch(franchiseId, "Sucursal El Poblado");
        createBranch(franchiseId, "Sucursal Laureles");

        mockMvc.perform(patch("/api/v1/branches/{id}/name", branchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdateNameRequest("Sucursal Envigado"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) branchId))
                .andExpect(jsonPath("$.name").value("Sucursal Envigado"))
                .andExpect(jsonPath("$.franchiseId").value((int) franchiseId));

        mockMvc.perform(patch("/api/v1/branches/{id}/name", branchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdateNameRequest("Sucursal Laureles"))))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/v1/branches/{id}/name", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdateNameRequest("Otra"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a branch may take a name used in a different franchise")
    void renamesBranchToNameUsedInAnotherFranchise() throws Exception {
        createBranch(createFranchise("Franquicia Bogota"), "Sucursal Centro");
        long branchId = createBranch(createFranchise("Franquicia Medellin"), "Sucursal El Poblado");

        mockMvc.perform(patch("/api/v1/branches/{id}/name", branchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdateNameRequest("Sucursal Centro"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("renaming a product returns 200 and keeps its stock; a taken name returns 409")
    void renamesProduct() throws Exception {
        long branchId = createBranch(createFranchise("Franquicia Medellin"), "Sucursal El Poblado");
        long productId = createProduct(branchId, "Laptop Lenovo", 25);
        createProduct(branchId, "Mouse Logitech", 10);

        mockMvc.perform(patch("/api/v1/branches/{branchId}/products/{productId}/name", branchId, productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdateNameRequest("Laptop Lenovo X1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) productId))
                .andExpect(jsonPath("$.name").value("Laptop Lenovo X1"))
                .andExpect(jsonPath("$.stock").value(25));

        mockMvc.perform(patch("/api/v1/branches/{branchId}/products/{productId}/name", branchId, productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdateNameRequest("Mouse Logitech"))))
                .andExpect(status().isConflict());

        assertThat(productRepository.findById(productId))
                .get()
                .extracting(product -> product.getName())
                .isEqualTo("Laptop Lenovo X1");
    }

    @Test
    @DisplayName("renaming a product through a branch that does not own it returns 404")
    void rejectsProductRenameThroughWrongBranch() throws Exception {
        long franchiseId = createFranchise("Franquicia Medellin");
        long ownerBranch = createBranch(franchiseId, "Sucursal El Poblado");
        long otherBranch = createBranch(franchiseId, "Sucursal Laureles");
        long productId = createProduct(ownerBranch, "Laptop Lenovo", 25);

        mockMvc.perform(patch("/api/v1/branches/{branchId}/products/{productId}/name", otherBranch, productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdateNameRequest("Otro"))))
                .andExpect(status().isNotFound());

        assertThat(productRepository.findById(productId))
                .get()
                .extracting(product -> product.getName())
                .isEqualTo("Laptop Lenovo");
    }

    @Test
    @DisplayName("read endpoints expose the franchise, branch and product tree")
    void readEndpoints() throws Exception {
        long franchiseId = createFranchise("Franquicia Medellin");
        long branchId = createBranch(franchiseId, "Sucursal El Poblado");
        long productId = createProduct(branchId, "Laptop Lenovo", 25);

        mockMvc.perform(get("/api/v1/franchises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Franquicia Medellin"));

        mockMvc.perform(get("/api/v1/franchises/{id}", franchiseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Franquicia Medellin"));

        mockMvc.perform(get("/api/v1/franchises/{id}/branches", franchiseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Sucursal El Poblado"));

        mockMvc.perform(get("/api/v1/branches/{id}/products", branchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Laptop Lenovo"));

        mockMvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(25));
    }

    @Test
    @DisplayName("an unknown franchise id returns 404 with the request path echoed back")
    void unknownFranchiseReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/franchises/{id}", 10))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Franchise with id 10 not found"))
                .andExpect(jsonPath("$.path").value("/api/v1/franchises/10"));
    }

    @Test
    @DisplayName("a non-numeric id returns 400 rather than 500")
    void nonNumericIdReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/franchises/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("a malformed JSON body returns 400")
    void malformedBodyReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/franchises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or missing request body"));
    }

    @Test
    @DisplayName("criterion 8: data written by the API survives in MySQL and is read back")
    void persistenceIsBackedByMySql() throws Exception {
        long franchiseId = createFranchise("Franquicia Persistente");
        long branchId = createBranch(franchiseId, "Sucursal Unica");
        createProduct(branchId, "Producto Unico", 42);

        // Read straight through the repositories, bypassing the web layer, to
        // confirm the rows really are in the database and not in a cache.
        assertThat(franchiseRepository.findById(franchiseId)).isPresent();
        assertThat(branchRepository.findByFranchiseIdOrderByIdAsc(franchiseId)).hasSize(1);
        assertThat(productRepository.findByBranchIdOrderByIdAsc(branchId))
                .singleElement()
                .satisfies(product -> {
                    assertThat(product.getName()).isEqualTo("Producto Unico");
                    assertThat(product.getStock()).isEqualTo(42);
                    assertThat(product.getCreatedAt()).isNotNull();
                    assertThat(product.getUpdatedAt()).isNotNull();
                });
    }

    // ---------------------------------------------------------------- helpers

    private String json(Object payload) throws Exception {
        return objectMapper.writeValueAsString(payload);
    }

    private long createFranchise(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/franchises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateFranchiseRequest(name))))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private long createBranch(long franchiseId, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/franchises/{id}/branches", franchiseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateBranchRequest(name))))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private long createProduct(long branchId, String name, int stock) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/branches/{id}/products", branchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateProductRequest(name, stock))))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private long idOf(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }
}
