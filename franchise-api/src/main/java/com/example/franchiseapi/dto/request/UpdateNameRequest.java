package com.example.franchiseapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Shared by the rename operations of franchises, branches and products: the
 * three names follow the same length rule, only their uniqueness scope differs,
 * and that scope is enforced by each service.
 */
@Schema(name = "UpdateNameRequest", description = "Payload to rename a franchise, branch or product")
public record UpdateNameRequest(

        @Schema(description = "New name. Must stay unique within the scope of the resource "
                + "(globally for franchises, per franchise for branches, per branch for products).",
                example = "Nuevo nombre", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name
) {
}
