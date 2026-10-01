package com.example.franchiseapi.repository;

import com.example.franchiseapi.entity.Franchise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FranchiseRepository extends JpaRepository<Franchise, Long> {

    /** Enforces the "no duplicated franchise name" rule before inserting. */
    boolean existsByNameIgnoreCase(String name);

    /** Same rule on rename: the franchise being renamed does not collide with itself. */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<Franchise> findAllByOrderByIdAsc();
}
