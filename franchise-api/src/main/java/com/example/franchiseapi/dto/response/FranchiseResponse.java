package com.example.franchiseapi.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "FranchiseResponse", description = "A franchise")
public record FranchiseResponse(

        @Schema(description = "Franchise identifier", example = "1")
        Long id,

        @Schema(description = "Franchise name", example = "Franquicia Medellin")
        String name,

        @Schema(description = "Creation timestamp", example = "2026-09-30T18:00:00")
        LocalDateTime createdAt,

        @Schema(description = "Last modification timestamp", example = "2026-09-30T18:00:00")
        LocalDateTime updatedAt
) {
}
