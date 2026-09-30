package com.example.franchiseapi.mapper;

import com.example.franchiseapi.dto.response.BranchResponse;
import com.example.franchiseapi.entity.Branch;

import java.util.List;

public final class BranchMapper {

    private BranchMapper() {
    }

    public static BranchResponse toResponse(Branch branch) {
        return new BranchResponse(
                branch.getId(),
                branch.getName(),
                // Reads the proxy identifier: no SELECT is issued for the franchise.
                branch.getFranchise().getId(),
                branch.getCreatedAt(),
                branch.getUpdatedAt()
        );
    }

    public static List<BranchResponse> toResponseList(List<Branch> branches) {
        return branches.stream().map(BranchMapper::toResponse).toList();
    }
}
