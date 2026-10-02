package com.example.franchiseapi.repository;

import com.example.franchiseapi.entity.Product;
import com.example.franchiseapi.repository.projection.TopStockProductProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Reads go through Spring Data; writes and the top-stock report go through the
 * stored procedures, which enforce the business rules and report violations as
 * described in {@link com.example.franchiseapi.exception.StoredProcedureErrors}.
 * <p>
 * Every procedure acting on an existing product takes the branch as well, so a
 * product addressed through a branch that does not own it is reported as not
 * found instead of being modified.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByBranchIdOrderByIdAsc(Long branchId);

    /** {@code sp_product_create}: inserts the product and returns the stored row. */
    @Query(value = "CALL sp_product_create(:branchId, :name, :stock)", nativeQuery = true)
    Product create(@Param("branchId") Long branchId,
                   @Param("name") String name,
                   @Param("stock") Integer stock);

    /** {@code sp_product_update_stock}: sets the absolute stock and returns the stored row. */
    @Query(value = "CALL sp_product_update_stock(:branchId, :productId, :stock)", nativeQuery = true)
    Product updateStock(@Param("branchId") Long branchId,
                        @Param("productId") Long productId,
                        @Param("stock") Integer stock);

    /** {@code sp_product_update_name}: renames the product and returns the stored row. */
    @Query(value = "CALL sp_product_update_name(:branchId, :productId, :name)", nativeQuery = true)
    Product updateName(@Param("branchId") Long branchId,
                       @Param("productId") Long productId,
                       @Param("name") String name);

    /** {@code sp_product_delete}: removes the product from its branch. */
    @Modifying
    @Query(value = "CALL sp_product_delete(:branchId, :productId)", nativeQuery = true)
    void deleteFromBranch(@Param("branchId") Long branchId, @Param("productId") Long productId);

    /**
     * {@code sp_franchise_top_stock_products}: the highest-stock product of every
     * branch of the franchise, ordered by branch id, in a single round-trip. The
     * ranking (window function), the tie-break and the edge cases are documented
     * in the procedure itself.
     */
    @Query(value = "CALL sp_franchise_top_stock_products(:franchiseId)", nativeQuery = true)
    List<TopStockProductProjection> findTopStockProductPerBranch(@Param("franchiseId") Long franchiseId);
}
