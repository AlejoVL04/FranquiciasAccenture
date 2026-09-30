package com.example.franchiseapi.mapper;

import com.example.franchiseapi.dto.response.FranchiseResponse;
import com.example.franchiseapi.entity.Franchise;

import java.util.List;

/**
 * Entity to DTO translation for franchises. Stateless by design: the mapping is
 * a pure function, so there is nothing worth turning into a Spring bean.
 */
public final class FranchiseMapper {

    private FranchiseMapper() {
    }

    public static FranchiseResponse toResponse(Franchise franchise) {
        return new FranchiseResponse(
                franchise.getId(),
                franchise.getName(),
                franchise.getCreatedAt(),
                franchise.getUpdatedAt()
        );
    }

    public static List<FranchiseResponse> toResponseList(List<Franchise> franchises) {
        return franchises.stream().map(FranchiseMapper::toResponse).toList();
    }
}
