package com.shopeasy.Domain.GS;

import java.math.BigDecimal;

public record ProductRequest(
        String name,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        String categoryName
) {}
