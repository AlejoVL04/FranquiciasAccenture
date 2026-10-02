package com.example.franchiseapi.repository;

import com.example.franchiseapi.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Reads go through Spring Data; writes go through the stored procedures, which
 * enforce the business rules and report violations as described in
 * {@link com.example.franchiseapi.exception.StoredProcedureErrors}.
 */
@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {

    List<Branch> findByFranchiseIdOrderByIdAsc(Long franchiseId);

    /** {@code sp_branch_create}: inserts the branch and returns the stored row. */
    @Query(value = "CALL sp_branch_create(:franchiseId, :name)", nativeQuery = true)
    Branch create(@Param("franchiseId") Long franchiseId, @Param("name") String name);

    /** {@code sp_branch_update_name}: renames the branch and returns the stored row. */
    @Query(value = "CALL sp_branch_update_name(:branchId, :name)", nativeQuery = true)
    Branch updateName(@Param("branchId") Long branchId, @Param("name") String name);
}
