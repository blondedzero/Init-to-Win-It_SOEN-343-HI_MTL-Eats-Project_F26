package com.shopeasy.Domain.PRC;

import java.math.BigDecimal;

public record RefundRequest(BigDecimal amount, String reason) {}
