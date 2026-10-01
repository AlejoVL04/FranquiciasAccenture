package com.example.franchiseapi.controller;

import com.example.franchiseapi.dto.request.CreateBranchRequest;
import com.example.franchiseapi.dto.request.CreateFranchiseRequest;
import com.example.franchiseapi.dto.request.UpdateNameRequest;
import com.example.franchiseapi.dto.response.BranchResponse;
import com.example.franchiseapi.dto.response.FranchiseResponse;
import com.example.franchiseapi.dto.response.TopStockProductResponse;
import com.example.franchiseapi.exception.ErrorResponse;
import com.example.franchiseapi.service.BranchService;
import com.example.franchiseapi.service.FranchiseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/franchises", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Franchises", description = "Franchise registration, renaming, lookup and the highest-stock report")
public class FranchiseController {

    private final FranchiseService franchiseService;
    private final BranchService branchService;

    @Operation(
            summary = "Create a franchise",
            description = "Registers a new franchise. The name must be unique across all franchises."
    )
    @ApiResponse(responseCode = "201", description = "Franchise created",
            content = @Content(schema = @Schema(implementation = FranchiseResponse.class)))
    @ApiResponse(responseCode = "400", description = "Name missing, blank or longer than 100 characters",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "A franchise with that name already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FranchiseResponse> create(@Valid @RequestBody CreateFranchiseRequest request) {
        FranchiseResponse created = franchiseService.create(request);
        URI location = UriComponentsBuilder.fromPath("/api/v1/franchises/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "List franchises", description = "Returns every franchise, ordered by id.")
    @ApiResponse(responseCode = "200", description = "Franchise list")
    @GetMapping
    public List<FranchiseResponse> findAll() {
        return franchiseService.findAll();
    }

    @Operation(summary = "Get a franchise by id")
    @ApiResponse(responseCode = "200", description = "Franchise found",
            content = @Content(schema = @Schema(implementation = FranchiseResponse.class)))
    @ApiResponse(responseCode = "404", description = "Franchise not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{franchiseId}")
    public FranchiseResponse findById(
            @Parameter(description = "Franchise identifier", example = "1")
            @PathVariable Long franchiseId) {
        return franchiseService.findById(franchiseId);
    }

    @Operation(
            summary = "Rename a franchise",
            description = "The new name must be unique across all franchises. Renaming a franchise to "
                    + "its current name, or to a different capitalisation of it, is accepted."
    )
    @ApiResponse(responseCode = "200", description = "Franchise renamed",
            content = @Content(schema = @Schema(implementation = FranchiseResponse.class)))
    @ApiResponse(responseCode = "400", description = "Name missing, blank or longer than 100 characters",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Franchise not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Another franchise already uses that name",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping(value = "/{franchiseId}/name", consumes = MediaType.APPLICATION_JSON_VALUE)
    public FranchiseResponse updateName(
            @Parameter(description = "Franchise identifier", example = "1")
            @PathVariable Long franchiseId,
            @Valid @RequestBody UpdateNameRequest request) {
        return franchiseService.updateName(franchiseId, request);
    }

    @Operation(
            summary = "Create a branch inside a franchise",
            description = "Registers a new branch. The name must be unique within the owning franchise."
    )
    @ApiResponse(responseCode = "201", description = "Branch created",
            content = @Content(schema = @Schema(implementation = BranchResponse.class)))
    @ApiResponse(responseCode = "400", description = "Name missing, blank or longer than 100 characters",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Franchise not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "That branch name is already used in this franchise",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(value = "/{franchiseId}/branches", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BranchResponse> createBranch(
            @Parameter(description = "Franchise identifier", example = "1")
            @PathVariable Long franchiseId,
            @Valid @RequestBody CreateBranchRequest request) {

        BranchResponse created = branchService.create(franchiseId, request);
        URI location = UriComponentsBuilder.fromPath("/api/v1/branches/{id}/products")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.status(HttpStatus.CREATED).location(location).body(created);
    }

    @Operation(summary = "List the branches of a franchise")
    @ApiResponse(responseCode = "200", description = "Branch list")
    @ApiResponse(responseCode = "404", description = "Franchise not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{franchiseId}/branches")
    public List<BranchResponse> findBranches(
            @Parameter(description = "Franchise identifier", example = "1")
            @PathVariable Long franchiseId) {
        return branchService.findByFranchise(franchiseId);
    }

    @Operation(
            summary = "Highest-stock product of each branch of a franchise",
            description = """
                    Returns, for the given franchise, the product holding the most stock in each of \
                    its branches. At most one entry per branch.

                    Resolved by a single native query using a window function \
                    (ROW_NUMBER partitioned by branch, ordered by stock descending), so the ranking \
                    happens inside MySQL: no product entity is loaded into the application and the \
                    cost does not grow with the number of branches, which rules out N+1.

                    Tie-break: when several products of the same branch share the maximum stock, the \
                    oldest one (lowest product id) is returned, which makes the result deterministic.

                    A stock of 0 is a valid maximum and is reported normally.

                    Branches holding no products are omitted by default; pass \
                    includeBranchesWithoutProducts=true to have them listed with null product fields."""
    )
    @ApiResponse(responseCode = "200", description = "Report rows, ordered by branch id")
    @ApiResponse(responseCode = "404", description = "Franchise not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{franchiseId}/top-stock-products")
    public List<TopStockProductResponse> findTopStockProducts(
            @Parameter(description = "Franchise identifier", example = "1")
            @PathVariable Long franchiseId,
            @Parameter(description = "Also report branches that hold no products", example = "false")
            @RequestParam(defaultValue = "false") boolean includeBranchesWithoutProducts) {
        return franchiseService.findTopStockProductPerBranch(franchiseId, includeBranchesWithoutProducts);
    }
}
