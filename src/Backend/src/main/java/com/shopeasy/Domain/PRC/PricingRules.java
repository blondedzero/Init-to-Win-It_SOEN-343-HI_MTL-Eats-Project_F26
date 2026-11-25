package com.shopeasy.Domain.PRC;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
/*
Pricing rules:
- Add smallOrderFee to subtotal if items <= smallOrderThreshold
- Service fee is computed on subtotal (items cost + small order fee if applicable)
- Delivery fee is computed on subtotal (items cost + small order fee if applicable)
- Tax is computed on subtotal (items cost + small order fee if applicable)
*/
@ConfigurationProperties(prefix = "prc")
public record PricingRules(
        String ruleVersion,
        int smallOrderThreshold,
        BigDecimal smallOrderFee,
        BigDecimal serviceFeePct,
        BigDecimal deliveryFeePct,
        BigDecimal taxPct
) {
    @Override
    public BigDecimal serviceFeePct() {
        return this.serviceFeePct.divide(BigDecimal.valueOf(100));
    }

    @Override
    public BigDecimal deliveryFeePct() {
        return this.deliveryFeePct.divide(BigDecimal.valueOf(100));
    }

    @Override
    public BigDecimal taxPct() {
        return this.taxPct.divide(BigDecimal.valueOf(100));
    }
}
