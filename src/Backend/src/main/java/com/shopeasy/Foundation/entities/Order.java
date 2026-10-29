package com.shopeasy.Foundation.entities;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
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

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private BigDecimal totalCost;
    private String deliveryAddress;
    private Instant placedAt;
    private Instant acceptedAt;
    private Instant readyAt;
    private Instant pickedUpAt;
    private Instant cancelledAt;
    private Instant deliveredAt;
    private String rejectionReason;

    protected Order() {}

    public Order(Customer customer, GroceryStore store) {
        this.customer = customer;
        this.store = store;
        this.deliveryAddress = customer.getAddress();
        this.placedAt = Instant.now();
        this.status = OrderStatus.PLACED;
        this.totalCost = BigDecimal.ZERO;
    }

    public void addItem(Product product, int quantity) {
        OrderItem item = new OrderItem(this, product, quantity);
        items.add(item);
        totalCost = totalCost.add(item.getLineTotal());
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

    public BigDecimal getTotalCost() {
        return totalCost;
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
}
