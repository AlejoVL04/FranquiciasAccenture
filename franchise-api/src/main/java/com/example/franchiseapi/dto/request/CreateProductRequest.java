package com.example.franchiseapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "CreateProductRequest", description = "Payload to register a new product inside a branch")
public record CreateProductRequest(

        @Schema(description = "Product name. Must be unique within the owning branch.",
                example = "Laptop Lenovo", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Product name is required")
        @Size(max = 100, message = "Product name must not exceed 100 characters")
        String name,

        @Schema(description = "Initial stock. Must be zero or greater.",
                example = "25", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Stock is required")
        @Min(value = 0, message = "Stock must be greater than or equal to 0")
        Integer stock
) {
}
