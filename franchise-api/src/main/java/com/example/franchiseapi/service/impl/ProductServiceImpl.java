package com.example.franchiseapi.service.impl;

import com.example.franchiseapi.dto.request.CreateProductRequest;
import com.example.franchiseapi.dto.request.UpdateNameRequest;
import com.example.franchiseapi.dto.request.UpdateStockRequest;
import com.example.franchiseapi.dto.response.ProductResponse;
import com.example.franchiseapi.entity.Product;
import com.example.franchiseapi.exception.ResourceNotFoundException;
import com.example.franchiseapi.mapper.ProductMapper;
import com.example.franchiseapi.repository.BranchRepository;
import com.example.franchiseapi.repository.ProductRepository;
import com.example.franchiseapi.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.franchiseapi.exception.StoredProcedureErrors.call;
import static com.example.franchiseapi.exception.StoredProcedureErrors.run;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final String BRANCH = "Branch";
    private static final String PRODUCT = "Product";

    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;

    @Override
    @Transactional
    public ProductResponse create(Long branchId, CreateProductRequest request) {
        Product saved = call(() -> productRepository.create(branchId, request.name(), request.stock()));
        log.info("Product created: id={}, name={}, stock={}, branchId={}",
                saved.getId(), saved.getName(), saved.getStock(), branchId);
        return ProductMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long branchId, Long productId) {
        run(() -> productRepository.deleteFromBranch(branchId, productId));
        log.info("Product deleted: id={}, branchId={}", productId, branchId);
    }

    @Override
    @Transactional
    public ProductResponse updateStock(Long branchId, Long productId, UpdateStockRequest request) {
        Product updated = call(() -> productRepository.updateStock(branchId, productId, request.stock()));
        log.info("Stock updated: productId={}, branchId={}, stock={}", productId, branchId, updated.getStock());
        return ProductMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public ProductResponse updateName(Long branchId, Long productId, UpdateNameRequest request) {
        Product updated = call(() -> productRepository.updateName(branchId, productId, request.name()));
        log.info("Product renamed: id={}, branchId={}, name={}", productId, branchId, updated.getName());
        return ProductMapper.toResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> findByBranch(Long branchId) {
        if (!branchRepository.existsById(branchId)) {
            throw ResourceNotFoundException.of(BRANCH, branchId);
        }
        return ProductMapper.toResponseList(productRepository.findByBranchIdOrderByIdAsc(branchId));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse findById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.of(PRODUCT, productId));
        return ProductMapper.toResponse(product);
    }
}
