package com.example.franchiseapi.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "TopStockProductResponse",
        description = """
                The single highest-stock product of one branch, resolved for a given franchise.
                At most one entry is returned per branch. When several products of the same
                branch tie on the maximum stock, the oldest one (lowest product id) wins."""
)
public record TopStockProductResponse(

        @Schema(description = "Identifier of the owning franchise", example = "1")
        Long franchiseId,

        @Schema(description = "Franchise name", example = "Franquicia A")
        String franchiseName,

        @Schema(description = "Branch identifier", example = "1")
        Long branchId,

        @Schema(description = "Branch name", example = "Sucursal Norte")
        String branchName,

        @Schema(description = "Identifier of the highest-stock product", example = "10")
        Long productId,

        @Schema(description = "Name of the highest-stock product", example = "Mouse")
        String productName,

        @Schema(description = "Stock of the highest-stock product", example = "50")
        Integer stock
) {
}
