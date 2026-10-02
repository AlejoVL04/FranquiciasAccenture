package com.example.franchiseapi.service.impl;

import com.example.franchiseapi.dto.request.CreateBranchRequest;
import com.example.franchiseapi.dto.request.UpdateNameRequest;
import com.example.franchiseapi.dto.response.BranchResponse;
import com.example.franchiseapi.entity.Branch;
import com.example.franchiseapi.exception.ResourceNotFoundException;
import com.example.franchiseapi.mapper.BranchMapper;
import com.example.franchiseapi.repository.BranchRepository;
import com.example.franchiseapi.repository.FranchiseRepository;
import com.example.franchiseapi.service.BranchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.franchiseapi.exception.StoredProcedureErrors.call;

@Slf4j
@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {

    private static final String FRANCHISE = "Franchise";

    private final BranchRepository branchRepository;
    private final FranchiseRepository franchiseRepository;

    @Override
    @Transactional
    public BranchResponse create(Long franchiseId, CreateBranchRequest request) {
        Branch saved = call(() -> branchRepository.create(franchiseId, request.name()));
        log.info("Branch created: id={}, name={}, franchiseId={}", saved.getId(), saved.getName(), franchiseId);
        return BranchMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BranchResponse updateName(Long branchId, UpdateNameRequest request) {
        Branch updated = call(() -> branchRepository.updateName(branchId, request.name()));
        log.info("Branch renamed: id={}, name={}", branchId, updated.getName());
        return BranchMapper.toResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchResponse> findByFranchise(Long franchiseId) {
        if (!franchiseRepository.existsById(franchiseId)) {
            throw ResourceNotFoundException.of(FRANCHISE, franchiseId);
        }
        return BranchMapper.toResponseList(branchRepository.findByFranchiseIdOrderByIdAsc(franchiseId));
    }
}
