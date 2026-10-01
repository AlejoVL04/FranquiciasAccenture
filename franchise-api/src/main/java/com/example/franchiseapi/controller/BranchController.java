package com.example.franchiseapi.controller;

import com.example.franchiseapi.dto.request.CreateProductRequest;
import com.example.franchiseapi.dto.request.UpdateNameRequest;
import com.example.franchiseapi.dto.request.UpdateStockRequest;
import com.example.franchiseapi.dto.response.BranchResponse;
import com.example.franchiseapi.dto.response.ProductResponse;
import com.example.franchiseapi.exception.ErrorResponse;
import com.example.franchiseapi.service.BranchService;
import com.example.franchiseapi.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Branch renaming, plus the products of a branch. Products are addressed as a
 * sub-resource of their branch, because every operation on a product is scoped
 * by the branch that owns it.
 */
@RestController
@RequestMapping(value = "/api/v1/branches", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Branches", description = "Branch renaming and the products it holds: creation, removal, "
        + "renaming and stock changes")
public class BranchController {

    private final BranchService branchService;
    private final ProductService productService;

    @Operation(
            summary = "Rename a branch",
            description = "The new name must be unique within the owning franchise. Renaming a branch "
                    + "to its current name, or to a different capitalisation of it, is accepted."
    )
    @ApiResponse(responseCode = "200", description = "Branch renamed",
            content = @Content(schema = @Schema(implementation = BranchResponse.class)))
    @ApiResponse(responseCode = "400", description = "Name missing, blank or longer than 100 characters",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Branch not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Another branch of the franchise already uses that name",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping(value = "/{branchId}/name", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BranchResponse updateName(
            @Parameter(description = "Branch identifier", example = "1")
            @PathVariable Long branchId,
            @Valid @RequestBody UpdateNameRequest request) {
        return branchService.updateName(branchId, request);
    }

    @Operation(
            summary = "Create a product inside a branch",
            description = "Registers a new product. The name must be unique within the owning branch "
                    + "and the stock must be zero or greater."
    )
    @ApiResponse(responseCode = "201", description = "Product created",
            content = @Content(schema = @Schema(implementation = ProductResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid name or negative stock",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Branch not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "That product name is already used in this branch",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(value = "/{branchId}/products", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductResponse> createProduct(
            @Parameter(description = "Branch identifier", example = "1")
            @PathVariable Long branchId,
            @Valid @RequestBody CreateProductRequest request) {

        ProductResponse created = productService.create(branchId, request);
        URI location = UriComponentsBuilder.fromPath("/api/v1/products/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "List the products of a branch")
    @ApiResponse(responseCode = "200", description = "Product list")
    @ApiResponse(responseCode = "404", description = "Branch not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{branchId}/products")
    public List<ProductResponse> findProducts(
            @Parameter(description = "Branch identifier", example = "1")
            @PathVariable Long branchId) {
        return productService.findByBranch(branchId);
    }

    @Operation(
            summary = "Delete a product from a branch",
            description = "The product must belong to the given branch. A product addressed through a "
                    + "branch that does not own it is reported as 404, never deleted."
    )
    @ApiResponse(responseCode = "204", description = "Product deleted")
    @ApiResponse(responseCode = "404", description = "The product does not exist in that branch",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{branchId}/products/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(
            @Parameter(description = "Branch identifier", example = "1")
            @PathVariable Long branchId,
            @Parameter(description = "Product identifier", example = "1")
            @PathVariable Long productId) {
        productService.delete(branchId, productId);
    }

    @Operation(
            summary = "Update the stock of a product",
            description = "Sets the new absolute stock value. PATCH is used rather than PUT because "
                    + "only one field of the product is being modified."
    )
    @ApiResponse(responseCode = "200", description = "Stock updated",
            content = @Content(schema = @Schema(implementation = ProductResponse.class)))
    @ApiResponse(responseCode = "400", description = "Stock missing or negative",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "The product does not exist in that branch",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping(value = "/{branchId}/products/{productId}/stock",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ProductResponse updateStock(
            @Parameter(description = "Branch identifier", example = "1")
            @PathVariable Long branchId,
            @Parameter(description = "Product identifier", example = "1")
            @PathVariable Long productId,
            @Valid @RequestBody UpdateStockRequest request) {
        return productService.updateStock(branchId, productId, request);
    }

    @Operation(
            summary = "Rename a product",
            description = "The product must belong to the given branch and the new name must be unique "
                    + "within that branch."
    )
    @ApiResponse(responseCode = "200", description = "Product renamed",
            content = @Content(schema = @Schema(implementation = ProductResponse.class)))
    @ApiResponse(responseCode = "400", description = "Name missing, blank or longer than 100 characters",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "The product does not exist in that branch",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Another product of the branch already uses that name",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping(value = "/{branchId}/products/{productId}/name",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ProductResponse updateProductName(
            @Parameter(description = "Branch identifier", example = "1")
            @PathVariable Long branchId,
            @Parameter(description = "Product identifier", example = "1")
            @PathVariable Long productId,
            @Valid @RequestBody UpdateNameRequest request) {
        return productService.updateName(branchId, productId, request);
    }
}
