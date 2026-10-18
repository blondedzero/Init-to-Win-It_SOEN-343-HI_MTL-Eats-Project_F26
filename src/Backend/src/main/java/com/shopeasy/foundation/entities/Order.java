package com.shopeasy.foundation.entities;

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

    public void markAccepted () {
        if (this.status != OrderStatus.PLACED) throw new IllegalStateException();
        this.acceptedAt = Instant.now();
        this.status = OrderStatus.PREPARING;
    }

    public void markRejected () {
        if (this.status != OrderStatus.PLACED) throw new IllegalStateException();
        this.cancelledAt = Instant.now();
        this.status = OrderStatus.REJECTED;
    }

    public void markCancelled () {
        this.cancelledAt = Instant.now();
        this.status = OrderStatus.CANCELED;
    }

    public void markReady () {
        if  (this.status != OrderStatus.PREPARING) throw new IllegalStateException();
        this.readyAt = Instant.now();
        this.status = OrderStatus.READY;
    }

    public void markPickedUp () {
        if  (this.status != OrderStatus.READY) throw new IllegalStateException();
        this.pickedUpAt = Instant.now();
        this.status = OrderStatus.PICKED_UP;
    }

    public void markDelivered () {
        if   (this.status != OrderStatus.PICKED_UP) throw new IllegalStateException();
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
}
