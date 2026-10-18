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
        this.acceptedAt = Instant.now();
        this.status = OrderStatus.PREPARING;
    }

    public void markRejected () {
        this.cancelledAt = Instant.now();
        this.status = OrderStatus.REJECTED;
    }

    public void markCancelled () {
        this.cancelledAt = Instant.now();
        this.status = OrderStatus.CANCELED;
    }

    public void markReady () {
        this.readyAt = Instant.now();
        this.status = OrderStatus.READY;
    }

    public void markPickedUp () {
        this.pickedUpAt = Instant.now();
        this.status = OrderStatus.PICKED_UP;
    }

    public void markDelivered () {
        this.deliveredAt = Instant.now();
        this.status = OrderStatus.DELIVERED;
    }
}
