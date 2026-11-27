package com.shopeasy.Foundation.entities;

import jakarta.persistence.*;

import java.time.Instant;

@Entity(name = "Rating")
@Table(name = "ratings")
public class Rating {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ratingId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private GroceryStore store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id")
    private Courier courier;

    @Column(nullable = false)
    private int storeScore;

    @Column(nullable = false)
    private int courierScore;

    @Column(nullable = false)
    private Instant ratedAt;

    protected Rating() {}

    public Rating(Order order, GroceryStore store, Courier courier, int storeScore, int courierScore) {
        this.order = order;
        this.store = store;
        this.courier = courier;
        this.storeScore = storeScore;
        this.courierScore = courierScore;
        this.ratedAt = Instant.now();
    }

    public Long getRatingId() {
        return ratingId;
    }
    public Order getOrder() {
        return order;
    }
    public GroceryStore getStore() {
        return store;
    }
    public Courier getCourier() {
        return courier;
    }
    public int getStoreScore() {
        return storeScore;
    }
    public int getCourierScore() {
        return courierScore;
    }
    public Instant getRatedAt() {
        return ratedAt;
    }
}
