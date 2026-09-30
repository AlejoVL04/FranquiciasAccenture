package com.example.franchiseapi.service;

import com.example.franchiseapi.dto.request.CreateBranchRequest;
import com.example.franchiseapi.dto.response.BranchResponse;

import java.util.List;

public interface BranchService {

    /**
     * @throws com.example.franchiseapi.exception.ResourceNotFoundException 404, unknown franchise
     * @throws com.example.franchiseapi.exception.BusinessException         409, name already used in this franchise
     */
    BranchResponse create(Long franchiseId, CreateBranchRequest request);

    /**
     * @throws com.example.franchiseapi.exception.ResourceNotFoundException 404, unknown franchise
     */
    List<BranchResponse> findByFranchise(Long franchiseId);
}
