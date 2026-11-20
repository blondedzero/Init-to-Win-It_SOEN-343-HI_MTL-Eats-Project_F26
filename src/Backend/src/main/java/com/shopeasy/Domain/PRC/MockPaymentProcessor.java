package com.shopeasy.Domain.PRC;

import com.shopeasy.Foundation.entities.PaymentInfo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class MockPaymentProcessor {

    public String charge(PaymentInfo paymentInfo, BigDecimal amount) {
        requireValidPayment(paymentInfo, amount);
        return "mock-charge-" + UUID.randomUUID();
    }

    public String refund(PaymentInfo paymentInfo, Long orderId, BigDecimal amount) {
        requireValidPayment(paymentInfo, amount);
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("A valid order ID is required for a refund");
        }
        return "mock-refund-" + UUID.randomUUID();
    }

    private void requireValidPayment(PaymentInfo paymentInfo, BigDecimal amount) {
        if (paymentInfo == null) {
            throw new IllegalArgumentException("A customer payment method is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
    }
}
