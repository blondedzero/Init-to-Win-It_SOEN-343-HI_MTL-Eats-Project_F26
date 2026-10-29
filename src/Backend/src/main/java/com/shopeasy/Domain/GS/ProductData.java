package com.shopeasy.Domain.GS;

import java.math.BigDecimal;

public record ProductData(Long productId, String name, String description, String category,
                          BigDecimal price, int stockQuantity, String status) {}
