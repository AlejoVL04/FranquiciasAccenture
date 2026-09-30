package com.example.franchiseapi.mapper;

import com.example.franchiseapi.dto.response.ProductResponse;
import com.example.franchiseapi.dto.response.TopStockProductResponse;
import com.example.franchiseapi.entity.Product;
import com.example.franchiseapi.repository.projection.TopStockProductProjection;

import java.util.List;

public final class ProductMapper {

    private ProductMapper() {
    }

    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getStock(),
                // Reads the proxy identifier: no SELECT is issued for the branch.
                product.getBranch().getId(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public static List<ProductResponse> toResponseList(List<Product> products) {
        return products.stream().map(ProductMapper::toResponse).toList();
    }

    public static TopStockProductResponse toTopStockResponse(TopStockProductProjection projection) {
        return new TopStockProductResponse(
                projection.getFranchiseId(),
                projection.getFranchiseName(),
                projection.getBranchId(),
                projection.getBranchName(),
                projection.getProductId(),
                projection.getProductName(),
                projection.getStock()
        );
    }
}
