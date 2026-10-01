package com.example.franchiseapi.repository;

import com.example.franchiseapi.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {

    /**
     * Enforces "no two branches with the same name inside one franchise".
     * Resolves to a lookup on the {@code (franchise_id, name)} unique index.
     */
    boolean existsByFranchiseIdAndNameIgnoreCase(Long franchiseId, String name);

    /** Same rule on rename: the branch being renamed does not collide with itself. */
    boolean existsByFranchiseIdAndNameIgnoreCaseAndIdNot(Long franchiseId, String name, Long id);

    List<Branch> findByFranchiseIdOrderByIdAsc(Long franchiseId);
}
