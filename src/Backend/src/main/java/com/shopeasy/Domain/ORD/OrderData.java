package com.shopeasy.Domain.ORD;

import com.shopeasy.Foundation.entities.Order;
import com.shopeasy.Foundation.entities.OrderItem;
import com.shopeasy.Foundation.entities.Refund;
import com.shopeasy.Domain.PRC.PricingSummary;
import com.shopeasy.Domain.RVW.RatingData;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderData(Long orderId, Long storeId, String storeName, Long courierId, String courierName,
                        boolean courierAccepted,
                        String customerName, String status, String deliveryAddress,
                        BigDecimal totalCost, Instant placedAt, Instant readyAt, Instant canceledAt, Instant pickedUpAt, Instant deliveredAt, String rejectionReason,
                        RefundData refund, RatingData rating, List<OrderLineData> lines, PricingSummary pricing) {

    public record OrderLineData(Long productId, String name, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
        static OrderLineData from(OrderItem item) {
            return new OrderLineData(item.getProduct().getProductId(), item.getProduct().getName(),
                    item.getQuantity(), item.getUnitPrice(), item.getLineTotal());
        }
    }

    public record RefundData(Long refundId, Long orderId, BigDecimal amount, String reason,
                             Long operatorId, Instant refundedAt, String processorReference) {
        static RefundData from(Refund refund) {
            return new RefundData(refund.getRefundId(), refund.getOrder().getOrderId(),
                    refund.getRefundAmount(), refund.getRefundReason(), refund.getOperatorId(),
                    refund.getRefundDate(), refund.getProcessorRefundReference());
        }
    }

    public static OrderData from(Order order) {
        var courier = order.getCourier();
        var refund = order.getRefund();
        var rating = order.getRating();
        return new OrderData(order.getOrderId(), order.getStore().getAccountId(), order.getStore().getStoreName(),
                courier == null ? null : courier.getAccountId(), courier == null ? null : courier.getFullName(),
                order.isCourierAccepted(),
                order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName(),
                order.getStatus().name(), order.getDeliveryAddress(), order.getTotalCost(), order.getPlacedAt(), order.getReadyAt(), order.getCancelledAt(), order.getPickedUpAt(), order.getDeliveredAt(),
                order.getRejectionReason(), refund == null ? null : RefundData.from(refund),
                rating == null ? null : RatingData.from(rating),
                order.getItems().stream().map(OrderLineData::from).toList(), PricingSummary.from(order));
    }
}
