package com.shopeasy.Foundation.entities;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity(name = "Refund")
@Table(name = "refunds")
public class Refund {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long refundId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(nullable = false, length = 500)
    private String refundReason;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(nullable = false)
    private Long operatorId;

    @Column(nullable = false)
    private Instant refundDate;

    @Column(nullable = false)
    private String processorRefundReference;

    protected Refund() {}

    public Refund(Order order, String refundReason, BigDecimal refundAmount,
                  Long operatorId, String processorRefundReference) {
        this.order = order;
        this.refundReason = refundReason;
        this.refundAmount = refundAmount;
        this.operatorId = operatorId;
        this.processorRefundReference = processorRefundReference;
        this.refundDate = Instant.now();
    }

    public Long getRefundId() {
        return refundId;
    }
    public void setRefundId(Long refundId) {
        this.refundId = refundId;
    }
    public Order getOrder() {
        return order;
    }
    public void setOrder(Order order) {
        this.order = order;
    }
    public String getRefundReason() {
        return refundReason;
    }
    public BigDecimal getRefundAmount() {
        return refundAmount;
    }
    public Long getOperatorId() {
        return operatorId;
    }
    public Instant getRefundDate() {
        return refundDate;
    }
    public String getProcessorRefundReference() {
        return processorRefundReference;
    }
}
