package com.example.franchiseapi.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "BranchResponse", description = "A branch belonging to a franchise")
public record BranchResponse(

        @Schema(description = "Branch identifier", example = "1")
        Long id,

        @Schema(description = "Branch name", example = "Sucursal El Poblado")
        String name,

        @Schema(description = "Identifier of the owning franchise", example = "1")
        Long franchiseId,

        @Schema(description = "Creation timestamp", example = "2026-09-30T18:00:00")
        LocalDateTime createdAt,

        @Schema(description = "Last modification timestamp", example = "2026-09-30T18:00:00")
        LocalDateTime updatedAt
) {
}
