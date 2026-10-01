package com.example.franchiseapi.service;

import com.example.franchiseapi.dto.request.CreateProductRequest;
import com.example.franchiseapi.dto.request.UpdateNameRequest;
import com.example.franchiseapi.dto.request.UpdateStockRequest;
import com.example.franchiseapi.dto.response.ProductResponse;

import java.util.List;

public interface ProductService {

    /**
     * @throws com.example.franchiseapi.exception.ResourceNotFoundException 404, unknown branch
     * @throws com.example.franchiseapi.exception.BusinessException         409, name already used in this branch
     */
    ProductResponse create(Long branchId, CreateProductRequest request);

    /**
     * @throws com.example.franchiseapi.exception.ResourceNotFoundException 404, the product does not
     *                                                                     exist in that branch
     */
    void delete(Long branchId, Long productId);

    /**
     * Sets the new absolute stock of a product.
     *
     * @throws com.example.franchiseapi.exception.ResourceNotFoundException 404, the product does not
     *                                                                     exist in that branch
     * @throws com.example.franchiseapi.exception.BusinessException         400, negative stock
     */
    ProductResponse updateStock(Long branchId, Long productId, UpdateStockRequest request);

    /**
     * @throws com.example.franchiseapi.exception.ResourceNotFoundException 404, the product does not
     *                                                                     exist in that branch
     * @throws com.example.franchiseapi.exception.BusinessException         409, name already used in this branch
     */
    ProductResponse updateName(Long branchId, Long productId, UpdateNameRequest request);

    /**
     * @throws com.example.franchiseapi.exception.ResourceNotFoundException 404, unknown branch
     */
    List<ProductResponse> findByBranch(Long branchId);

    /**
     * @throws com.example.franchiseapi.exception.ResourceNotFoundException 404, unknown product
     */
    ProductResponse findById(Long productId);
}
