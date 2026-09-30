package com.example.franchiseapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "CreateFranchiseRequest", description = "Payload to register a new franchise")
public record CreateFranchiseRequest(

        @Schema(description = "Franchise name. Must be unique across all franchises.",
                example = "Franquicia Medellin", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Franchise name is required")
        @Size(max = 100, message = "Franchise name must not exceed 100 characters")
        String name
) {
}
