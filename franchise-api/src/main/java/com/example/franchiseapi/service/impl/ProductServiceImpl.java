package com.example.franchiseapi.service.impl;

import com.example.franchiseapi.dto.request.CreateProductRequest;
import com.example.franchiseapi.dto.request.UpdateNameRequest;
import com.example.franchiseapi.dto.request.UpdateStockRequest;
import com.example.franchiseapi.dto.response.ProductResponse;
import com.example.franchiseapi.entity.Branch;
import com.example.franchiseapi.entity.Product;
import com.example.franchiseapi.exception.BusinessException;
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
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> ResourceNotFoundException.of(BRANCH, branchId));

        requireNonNegativeStock(request.stock());

        String name = request.name().trim();
        if (productRepository.existsByBranchIdAndNameIgnoreCase(branchId, name)) {
            throw BusinessException.conflict(
                    "A product named '%s' already exists in branch %d".formatted(name, branchId));
        }

        Product saved = productRepository.save(Product.builder()
                .name(name)
                .stock(request.stock())
                .branch(branch)
                .build());

        log.info("Product created: id={}, name={}, stock={}, branchId={}",
                saved.getId(), saved.getName(), saved.getStock(), branchId);
        return ProductMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long branchId, Long productId) {
        Product product = requireProductInBranch(branchId, productId);
        productRepository.delete(product);
        log.info("Product deleted: id={}, branchId={}", productId, branchId);
    }

    @Override
    @Transactional
    public ProductResponse updateStock(Long branchId, Long productId, UpdateStockRequest request) {
        requireNonNegativeStock(request.stock());

        Product product = requireProductInBranch(branchId, productId);
        int previousStock = product.getStock();
        product.setStock(request.stock());

        // The entity is managed, so the UPDATE would flush on commit anyway.
        // Flushing here forces Hibernate to write the @UpdateTimestamp before the
        // response is mapped, so updatedAt in the payload matches the stored row.
        Product updated = productRepository.saveAndFlush(product);

        log.info("Stock updated: productId={}, branchId={}, {} -> {}",
                productId, branchId, previousStock, updated.getStock());
        return ProductMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public ProductResponse updateName(Long branchId, Long productId, UpdateNameRequest request) {
        Product product = requireProductInBranch(branchId, productId);

        String name = request.name().trim();
        if (productRepository.existsByBranchIdAndNameIgnoreCaseAndIdNot(branchId, name, productId)) {
            throw BusinessException.conflict(
                    "A product named '%s' already exists in branch %d".formatted(name, branchId));
        }

        String previousName = product.getName();
        product.setName(name);
        // Flushed so the @UpdateTimestamp is written before the response is mapped.
        Product updated = productRepository.saveAndFlush(product);

        log.info("Product renamed: id={}, branchId={}, '{}' -> '{}'",
                productId, branchId, previousName, updated.getName());
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

    /**
     * Resolves a product by id <em>and</em> owning branch, so addressing a
     * product through a branch that does not own it returns 404 instead of
     * silently mutating another branch's data.
     */
    private Product requireProductInBranch(Long branchId, Long productId) {
        return productRepository.findByIdAndBranchId(productId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product with id %d not found in branch %d".formatted(productId, branchId)));
    }

    /**
     * Second line of defence behind {@code @Min(0)} on the request DTOs: this one
     * guards the service contract itself, for any caller that bypasses the web layer.
     */
    private void requireNonNegativeStock(Integer stock) {
        if (stock == null || stock < 0) {
            throw new BusinessException("Stock must be greater than or equal to 0");
        }
    }
}
