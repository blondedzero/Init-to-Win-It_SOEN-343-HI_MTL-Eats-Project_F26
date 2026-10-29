package com.shopeasy.Domain.ORD;

import java.util.List;

public record PlaceOrderRequest(Long storeId, List<Line> lines) {
    public record Line(Long productId, int quantity) {}
}
