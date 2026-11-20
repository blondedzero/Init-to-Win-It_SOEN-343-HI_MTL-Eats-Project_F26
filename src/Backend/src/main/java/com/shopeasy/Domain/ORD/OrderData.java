package com.shopeasy.Domain.ORD;

import com.shopeasy.Foundation.entities.Order;
import com.shopeasy.Foundation.entities.OrderItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderData(Long orderId, Long storeId, String storeName, Long courierId, String courierName,
                        String customerName, String status, String deliveryAddress,
                        BigDecimal totalCost, Instant placedAt, Instant readyAt, Instant canceledAt, Instant pickedUpAt, Instant deliveredAt, String rejectionReason,
                        List<OrderLineData> lines) {

    public record OrderLineData(Long productId, String name, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
        static OrderLineData from(OrderItem item) {
            return new OrderLineData(item.getProduct().getProductId(), item.getProduct().getName(),
                    item.getQuantity(), item.getUnitPrice(), item.getLineTotal());
        }
    }

    public static OrderData from(Order order) {
        var courier = order.getCourier();
        return new OrderData(order.getOrderId(), order.getStore().getAccountId(), order.getStore().getStoreName(),
                courier == null ? null : courier.getAccountId(), courier == null ? null : courier.getFullName(),
                order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName(),
                order.getStatus().name(), order.getDeliveryAddress(), order.getTotalCost(), order.getPlacedAt(), order.getReadyAt(), order.getCancelledAt(), order.getPickedUpAt(), order.getDeliveredAt(),
                order.getRejectionReason(),
                order.getItems().stream().map(OrderLineData::from).toList());
    }
}
