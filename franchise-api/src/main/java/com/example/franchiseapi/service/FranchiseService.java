package com.example.franchiseapi.service;

import com.example.franchiseapi.dto.request.CreateFranchiseRequest;
import com.example.franchiseapi.dto.response.FranchiseResponse;
import com.example.franchiseapi.dto.response.TopStockProductResponse;

import java.util.List;

public interface FranchiseService {

    /**
     * @throws com.example.franchiseapi.exception.BusinessException 409, name already taken
     */
    FranchiseResponse create(CreateFranchiseRequest request);

    List<FranchiseResponse> findAll();

    /**
     * @throws com.example.franchiseapi.exception.ResourceNotFoundException 404, unknown franchise
     */
    FranchiseResponse findById(Long franchiseId);

    /**
     * Highest-stock product of each branch of the franchise, at most one entry
     * per branch.
     *
     * @param includeBranchesWithoutProducts when {@code true}, branches holding
     *                                       no products are reported with null
     *                                       product fields instead of being omitted
     * @throws com.example.franchiseapi.exception.ResourceNotFoundException 404, unknown franchise
     */
    List<TopStockProductResponse> findTopStockProductPerBranch(Long franchiseId,
                                                               boolean includeBranchesWithoutProducts);
}
