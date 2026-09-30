package com.example.franchiseapi.service.impl;

import com.example.franchiseapi.dto.request.CreateFranchiseRequest;
import com.example.franchiseapi.dto.response.FranchiseResponse;
import com.example.franchiseapi.dto.response.TopStockProductResponse;
import com.example.franchiseapi.entity.Franchise;
import com.example.franchiseapi.exception.BusinessException;
import com.example.franchiseapi.exception.ResourceNotFoundException;
import com.example.franchiseapi.mapper.FranchiseMapper;
import com.example.franchiseapi.mapper.ProductMapper;
import com.example.franchiseapi.repository.FranchiseRepository;
import com.example.franchiseapi.repository.ProductRepository;
import com.example.franchiseapi.repository.projection.TopStockProductProjection;
import com.example.franchiseapi.service.FranchiseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class FranchiseServiceImpl implements FranchiseService {

    private static final String FRANCHISE = "Franchise";

    private final FranchiseRepository franchiseRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public FranchiseResponse create(CreateFranchiseRequest request) {
        String name = request.name().trim();
        if (franchiseRepository.existsByNameIgnoreCase(name)) {
            throw BusinessException.conflict(
                    "A franchise named '%s' already exists".formatted(name));
        }

        Franchise saved = franchiseRepository.save(Franchise.builder().name(name).build());
        log.info("Franchise created: id={}, name={}", saved.getId(), saved.getName());
        return FranchiseMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FranchiseResponse> findAll() {
        return FranchiseMapper.toResponseList(franchiseRepository.findAllByOrderByIdAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public FranchiseResponse findById(Long franchiseId) {
        Franchise franchise = franchiseRepository.findById(franchiseId)
                .orElseThrow(() -> ResourceNotFoundException.of(FRANCHISE, franchiseId));
        return FranchiseMapper.toResponse(franchise);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopStockProductResponse> findTopStockProductPerBranch(Long franchiseId,
                                                                      boolean includeBranchesWithoutProducts) {
        // Fail with 404 for an unknown franchise rather than returning an empty
        // list: an empty list is already the correct answer for a franchise that
        // exists but has no branches, so the two cases must stay distinguishable.
        if (!franchiseRepository.existsById(franchiseId)) {
            throw ResourceNotFoundException.of(FRANCHISE, franchiseId);
        }

        List<TopStockProductProjection> rows = productRepository.findTopStockProductPerBranch(franchiseId);

        List<TopStockProductResponse> result = rows.stream()
                .filter(row -> includeBranchesWithoutProducts || Objects.nonNull(row.getProductId()))
                .map(ProductMapper::toTopStockResponse)
                .toList();

        log.info("Top-stock report for franchise {}: {} branch(es) scanned, {} entry(ies) returned",
                franchiseId, rows.size(), result.size());
        return result;
    }
}
