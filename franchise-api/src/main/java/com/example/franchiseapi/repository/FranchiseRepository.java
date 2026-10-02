package com.example.franchiseapi.repository;

import com.example.franchiseapi.entity.Franchise;
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
public interface FranchiseRepository extends JpaRepository<Franchise, Long> {

    List<Franchise> findAllByOrderByIdAsc();

    /** {@code sp_franchise_create}: inserts the franchise and returns the stored row. */
    @Query(value = "CALL sp_franchise_create(:name)", nativeQuery = true)
    Franchise create(@Param("name") String name);

    /** {@code sp_franchise_update_name}: renames the franchise and returns the stored row. */
    @Query(value = "CALL sp_franchise_update_name(:franchiseId, :name)", nativeQuery = true)
    Franchise updateName(@Param("franchiseId") Long franchiseId, @Param("name") String name);
}
