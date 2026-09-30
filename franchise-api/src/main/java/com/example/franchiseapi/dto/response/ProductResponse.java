package com.example.franchiseapi.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "ProductResponse", description = "A product belonging to a branch")
public record ProductResponse(

        @Schema(description = "Product identifier", example = "1")
        Long id,

        @Schema(description = "Product name", example = "Laptop Lenovo")
        String name,

        @Schema(description = "Current stock", example = "25")
        Integer stock,

        @Schema(description = "Identifier of the owning branch", example = "1")
        Long branchId,

        @Schema(description = "Creation timestamp", example = "2026-09-30T18:00:00")
        LocalDateTime createdAt,

        @Schema(description = "Last modification timestamp", example = "2026-09-30T18:00:00")
        LocalDateTime updatedAt
) {
}
