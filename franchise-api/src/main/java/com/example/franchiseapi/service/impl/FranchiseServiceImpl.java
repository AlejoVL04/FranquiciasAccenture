package com.example.franchiseapi.service.impl;

import com.example.franchiseapi.dto.request.CreateFranchiseRequest;
import com.example.franchiseapi.dto.request.UpdateNameRequest;
import com.example.franchiseapi.dto.response.FranchiseResponse;
import com.example.franchiseapi.dto.response.TopStockProductResponse;
import com.example.franchiseapi.entity.Franchise;
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

import static com.example.franchiseapi.exception.StoredProcedureErrors.call;

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
        Franchise saved = call(() -> franchiseRepository.create(request.name()));
        log.info("Franchise created: id={}, name={}", saved.getId(), saved.getName());
        return FranchiseMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public FranchiseResponse updateName(Long franchiseId, UpdateNameRequest request) {
        Franchise updated = call(() -> franchiseRepository.updateName(franchiseId, request.name()));
        log.info("Franchise renamed: id={}, name={}", franchiseId, updated.getName());
        return FranchiseMapper.toResponse(updated);
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
        // The procedure raises 404 for an unknown franchise, so an empty list
        // here always means a franchise that exists but has no branches.
        List<TopStockProductProjection> rows =
                call(() -> productRepository.findTopStockProductPerBranch(franchiseId));

        List<TopStockProductResponse> result = rows.stream()
                .filter(row -> includeBranchesWithoutProducts || Objects.nonNull(row.getProductId()))
                .map(ProductMapper::toTopStockResponse)
                .toList();

        log.info("Top-stock report for franchise {}: {} branch(es) scanned, {} entry(ies) returned",
                franchiseId, rows.size(), result.size());
        return result;
    }
}
