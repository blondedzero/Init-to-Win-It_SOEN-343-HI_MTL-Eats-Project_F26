package com.shopeasy.Foundation.entities;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity(name = "CustomerOrder")
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private GroceryStore store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id")          // nullable: no courier until one is assigned
    private Courier courier;

    @Enumerated(EnumType.STRING)
    @Column(name = "seed_initial_status")
    private OrderStatus seedInitialStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seed_initial_courier_id")
    private Courier seedInitialCourier;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private BigDecimal subtotal;
    private BigDecimal totalCost;
    private BigDecimal serviceFee;
    private BigDecimal smallOrderFee;
    private BigDecimal deliveryFee;
    private BigDecimal taxAmount;
    private String pricingRuleVersion;
    private String deliveryAddress;
    private Instant placedAt;
    private Instant acceptedAt;
    private Instant readyAt;
    private Instant pickedUpAt;
    private Instant cancelledAt;
    private Instant deliveredAt;
    private String rejectionReason;
    private Boolean isCharged;

    private String chargeReference;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Refund refund;

    protected Order() {}

    public Order(Customer customer, GroceryStore store) {
        this.customer = customer;
        this.store = store;
        this.deliveryAddress = customer.getAddress();
        this.placedAt = Instant.now();
        this.status = OrderStatus.PLACED;
        this.subtotal = BigDecimal.ZERO;
        this.totalCost = BigDecimal.ZERO;
        this.smallOrderFee = BigDecimal.ZERO;
        this.isCharged = false;
    }

    public void addItem(Product product, int quantity) {
        OrderItem item = new OrderItem(this, product, quantity);
        items.add(item);
        subtotal = subtotal.add(item.getLineTotal());
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    private void requireStatus(OrderStatus expected, OrderStatus target) {
        if (this.status != expected) {
            throw new IllegalStateException("Order " + orderId + " cannot go from " + status + " to " + target
                    + " (it must be " + expected + " first)");
        }
    }

    public void markAccepted () {
        requireStatus(OrderStatus.PLACED, OrderStatus.ACCEPTED);
        this.acceptedAt = Instant.now();
        this.status = OrderStatus.ACCEPTED;
    }

    public void markRejected (String reason) {
        requireStatus(OrderStatus.PLACED, OrderStatus.REJECTED);
        this.cancelledAt = Instant.now();
        this.rejectionReason = reason;
        this.status = OrderStatus.REJECTED;
    }

    public void markCancelled () {
        this.cancelledAt = Instant.now();
        this.status = OrderStatus.CANCELED;
    }

    public void markPreparing () {
        requireStatus(OrderStatus.ACCEPTED, OrderStatus.PREPARING);
        this.status = OrderStatus.PREPARING;
    }

    public void markReady () {
        requireStatus(OrderStatus.PREPARING, OrderStatus.READY);
        this.readyAt = Instant.now();
        this.status = OrderStatus.READY;
    }

    public void markPickedUp () {
        requireStatus(OrderStatus.READY, OrderStatus.PICKED_UP);
        this.pickedUpAt = Instant.now();
        this.status = OrderStatus.PICKED_UP;
    }

    public void markDelivered () {
        requireStatus(OrderStatus.PICKED_UP, OrderStatus.DELIVERED);
        this.deliveredAt = Instant.now();
        this.status = OrderStatus.DELIVERED;
    }

    public void markCharged(String chargeReference) {
        if (chargeReference == null || chargeReference.isBlank() || Boolean.TRUE.equals(isCharged)) {
            throw new IllegalStateException("Order cannot be charged");
        }
        this.chargeReference = chargeReference;
        this.isCharged = true;
    }

    public void markRefunded(Refund refund) {
        if (status != OrderStatus.DELIVERED && status != OrderStatus.CANCELED) {
            throw new IllegalStateException("Only delivered or canceled orders can be refunded");
        }
        if (!Boolean.TRUE.equals(isCharged) || this.refund != null || refund == null || refund.getOrder() != this) {
            throw new IllegalStateException("Order is not eligible for a refund");
        }
        this.refund = refund;
        this.status = OrderStatus.REFUNDED;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public GroceryStore getStore() {
        return store;
    }

    public Courier getCourier() {
        return courier;
    }

    public void setCourier(Courier courier) {
        this.courier = courier;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public BigDecimal getSubtotal() {return subtotal;}

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }
    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }
    public void setDeliveryFee(BigDecimal deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public BigDecimal getServiceFee() {
        return serviceFee;
    }
    public void setServiceFee(BigDecimal serviceFee) {
        this.serviceFee = serviceFee;
    }

    public BigDecimal getSmallOrderFee() {
        return smallOrderFee;
    }
    public void setSmallOrderFee(BigDecimal smallOrderFee) {
        this.smallOrderFee = smallOrderFee;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public String getPricingRuleVersion() {
        return pricingRuleVersion;
    }

    public void setPricingRuleVersion(String pricingRuleVersion) {
        this.pricingRuleVersion = pricingRuleVersion;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public Instant getReadyAt() {
        return readyAt;
    }

    public Instant getPickedUpAt() {
        return pickedUpAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public Boolean getIsCharged() {
        return isCharged;
    }
    public void setIsCharged(Boolean isCharged) {
        this.isCharged = isCharged;
    }
    public String getChargeReference() {
        return chargeReference;
    }

    public Refund getRefund() {
        return refund;
    }

    public OrderStatus getSeedInitialStatus() {
        return seedInitialStatus;
    }

    public void captureSeedInitialState() {
        if (seedInitialStatus == null) {
            seedInitialStatus = status;
            seedInitialCourier = courier;
        }
    }

    public void restoreSeedInitialState() {
        if (seedInitialStatus == null) {
            return;
        }

        status = refund != null && seedInitialStatus != OrderStatus.REFUNDED
                ? OrderStatus.REFUNDED
                : seedInitialStatus;
        courier = seedInitialCourier;
        acceptedAt = switch (status) {
            case ACCEPTED, PREPARING, READY, PICKED_UP, DELIVERED -> placedAt;
            default -> null;
        };
        readyAt = switch (status) {
            case READY, PICKED_UP, DELIVERED -> placedAt;
            default -> null;
        };
        pickedUpAt = switch (status) {
            case PICKED_UP, DELIVERED -> placedAt;
            default -> null;
        };
        deliveredAt = status == OrderStatus.DELIVERED ? placedAt : null;
        cancelledAt = switch (status) {
            case REJECTED, CANCELED, REFUNDED -> placedAt;
            default -> null;
        };
        rejectionReason = status == OrderStatus.REJECTED ? "Demo seed rejected order" : null;
    }
}
