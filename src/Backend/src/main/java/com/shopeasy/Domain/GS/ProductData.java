package com.shopeasy.Domain.GS;

import com.shopeasy.Foundation.entities.Product;

import java.math.BigDecimal;

public record ProductData(Long productId, String name, String description, String category,
                          BigDecimal price, int stockQuantity, String status) {

    public static ProductData from(Product p) {
        return new ProductData(p.getProductId(), p.getName(), p.getDescription(),
                p.getCategory().getCategoryName(), p.getPrice(), p.getStockQuantity(),
                p.getStatus().name());
    }
}
