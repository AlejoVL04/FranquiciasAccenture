package com.example.franchiseapi.repository;

import com.example.franchiseapi.entity.Product;
import com.example.franchiseapi.repository.projection.TopStockProductProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Enforces "no duplicated product name inside one branch". */
    boolean existsByBranchIdAndNameIgnoreCase(Long branchId, String name);

    /** Same rule on rename: the product being renamed does not collide with itself. */
    boolean existsByBranchIdAndNameIgnoreCaseAndIdNot(Long branchId, String name, Long id);

    /**
     * Scoped lookup: the product must exist *and* belong to the given branch.
     * A product addressed through the wrong branch is indistinguishable from a
     * missing product, which is what criteria 5 and 6 require.
     */
    Optional<Product> findByIdAndBranchId(Long id, Long branchId);

    List<Product> findByBranchIdOrderByIdAsc(Long branchId);

    /**
     * Returns the highest-stock product of every branch of one franchise, in a
     * single round-trip.
     *
     * <p><b>How it works.</b> The derived table ranks the products of the
     * franchise with {@code ROW_NUMBER()} partitioned by branch and ordered by
     * stock descending; keeping only {@code row_num = 1} leaves exactly one row
     * per branch. The ranking and the reduction both happen inside MySQL — no
     * product entity is ever loaded into the JVM, and there is no per-branch
     * query, so the endpoint is immune to N+1 regardless of how many branches
     * or products the franchise has.
     *
     * <p><b>Index usage.</b> {@code idx_products_branch_id_stock
     * (branch_id, stock DESC)} feeds both the partitioning and the ordering
     * directly from the index.
     *
     * <p><b>Edge cases.</b>
     * <ul>
     *   <li><i>Branch without products</i> — the {@code LEFT JOIN} keeps the
     *       branch with null product columns; the service layer decides whether
     *       to report or omit it.</li>
     *   <li><i>Stock 0</i> — a legitimate maximum. Zero is never confused with
     *       "no product", because the product columns are non-null in that case.</li>
     *   <li><i>Tie on the maximum stock</i> — broken deterministically by
     *       {@code id ASC}: the oldest product wins. Without that tiebreaker the
     *       winner would vary between executions.</li>
     * </ul>
     */
    @Query(value = """
            SELECT f.id      AS franchiseId,
                   f.name    AS franchiseName,
                   b.id      AS branchId,
                   b.name    AS branchName,
                   tp.id     AS productId,
                   tp.name   AS productName,
                   tp.stock  AS stock
            FROM branches b
            INNER JOIN franchises f
                    ON f.id = b.franchise_id
            LEFT JOIN (
                SELECT p.id,
                       p.name,
                       p.stock,
                       p.branch_id,
                       ROW_NUMBER() OVER (
                           PARTITION BY p.branch_id
                           ORDER BY p.stock DESC, p.id ASC
                       ) AS row_num
                FROM products p
                INNER JOIN branches rb
                        ON rb.id = p.branch_id
                WHERE rb.franchise_id = :franchiseId
            ) tp
                    ON tp.branch_id = b.id
                   AND tp.row_num = 1
            WHERE b.franchise_id = :franchiseId
            ORDER BY b.id ASC
            """, nativeQuery = true)
    List<TopStockProductProjection> findTopStockProductPerBranch(@Param("franchiseId") Long franchiseId);
}
