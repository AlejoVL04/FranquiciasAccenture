package com.example.franchiseapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "CreateBranchRequest", description = "Payload to register a new branch inside a franchise")
public record CreateBranchRequest(

        @Schema(description = "Branch name. Must be unique within the owning franchise.",
                example = "Sucursal El Poblado", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Branch name is required")
        @Size(max = 100, message = "Branch name must not exceed 100 characters")
        String name
) {
}
