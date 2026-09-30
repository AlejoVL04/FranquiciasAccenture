package com.example.franchiseapi.controller;

import com.example.franchiseapi.dto.response.ProductResponse;
import com.example.franchiseapi.exception.ErrorResponse;
import com.example.franchiseapi.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Direct, branch-independent access to a single product. Mutating operations
 * deliberately live on {@link BranchController} only, so that the branch
 * ownership check can never be bypassed.
 */
@RestController
@RequestMapping(value = "/api/v1/products", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Products", description = "Read-only access to an individual product")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Get a product by id")
    @ApiResponse(responseCode = "200", description = "Product found",
            content = @Content(schema = @Schema(implementation = ProductResponse.class)))
    @ApiResponse(responseCode = "404", description = "Product not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{productId}")
    public ProductResponse findById(
            @Parameter(description = "Product identifier", example = "1")
            @PathVariable Long productId) {
        return productService.findById(productId);
    }
}
