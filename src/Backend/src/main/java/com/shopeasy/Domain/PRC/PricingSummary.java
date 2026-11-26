package com.shopeasy.Domain.PRC;

import com.shopeasy.Foundation.entities.Order;
import com.shopeasy.Foundation.entities.OrderItem;

import java.math.BigDecimal;

public record PricingSummary(
        BigDecimal subtotal,
        BigDecimal smallOrderFee,
        BigDecimal serviceFee,
        BigDecimal deliveryFee,
        BigDecimal taxAmount,
        BigDecimal totalCost,
        String ruleVersion
) {
    public static PricingSummary from(Order order) {
        boolean hasCalculatedFees = order.getServiceFee() != null
                && order.getDeliveryFee() != null
                && order.getTaxAmount() != null;
        if (!hasCalculatedFees) {
            return null;
        }

        BigDecimal subtotal = order.getItems().stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PricingSummary(subtotal, order.getSmallOrderFee(), order.getServiceFee(),
                order.getDeliveryFee(), order.getTaxAmount(), order.getTotalCost(),
                order.getPricingRuleVersion());
    }
}
