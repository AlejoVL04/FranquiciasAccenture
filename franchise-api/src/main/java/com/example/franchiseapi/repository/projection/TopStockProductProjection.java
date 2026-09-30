package com.example.franchiseapi.repository.projection;

/**
 * Read-only projection for the "highest stock per branch" report.
 * <p>
 * Backed by a native query: only these seven columns leave the database, so no
 * entity graph is materialised and nothing has to be reduced in Java.
 * The product fields are {@code null} for a branch that holds no products.
 */
public interface TopStockProductProjection {

    Long getFranchiseId();

    String getFranchiseName();

    Long getBranchId();

    String getBranchName();

    Long getProductId();

    String getProductName();

    Integer getStock();
}
