package com.example.franchiseapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(name = "UpdateStockRequest", description = "Payload to set the new absolute stock of a product")
public record UpdateStockRequest(

        @Schema(description = "New absolute stock value. Must be zero or greater.",
                example = "50", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Stock is required")
        @Min(value = 0, message = "Stock must be greater than or equal to 0")
        Integer stock
) {
}
