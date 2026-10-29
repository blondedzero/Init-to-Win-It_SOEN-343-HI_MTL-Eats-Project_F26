package com.shopeasy.Domain.ORD;

import com.shopeasy.Foundation.entities.Order;
import com.shopeasy.Foundation.entities.OrderItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderData(Long orderId, Long storeId, String storeName, String customerName, String status,
                        BigDecimal totalCost, Instant placedAt, String rejectionReason, List<OrderLineData> lines) {

    public record OrderLineData(Long productId, String name, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
        static OrderLineData from(OrderItem item) {
            return new OrderLineData(item.getProduct().getProductId(), item.getProduct().getName(),
                    item.getQuantity(), item.getUnitPrice(), item.getLineTotal());
        }
    }

    static OrderData from(Order order) {
        return new OrderData(order.getOrderId(), order.getStore().getAccountId(), order.getStore().getStoreName(),
                order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName(),
                order.getStatus().name(), order.getTotalCost(), order.getPlacedAt(), order.getRejectionReason(),
                order.getItems().stream().map(OrderLineData::from).toList());
    }
}
