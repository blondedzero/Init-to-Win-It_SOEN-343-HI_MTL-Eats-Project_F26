package com.shopeasy.Foundation.entities;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "couriers")
@PrimaryKeyJoinColumn(name = "account_id")
public class Courier extends User {

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private double latitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourierStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "seed_initial_status")
    private CourierStatus seedInitialStatus;

    protected Courier() {}

    //Constructor
    public Courier(String email, String passwordHash, String firstName, String lastName, double longitude, double latitude) {
        super(email, passwordHash, firstName, lastName, Role.COURIER);
        this.longitude = longitude;
        this.latitude = latitude;
        this.status = CourierStatus.IDLE;
    }

    @Override
    public List<Order> getHistory() {
        return null;
    }

    public void goOnline() {
        requireStatus(CourierStatus.OFFLINE, CourierStatus.IDLE);
        this.status = CourierStatus.IDLE;
    }

    public void goOffline() {
        if (status == CourierStatus.ASSIGNED || status == CourierStatus.CARRYING) {
            throw new IllegalStateException("Courier " + getAccountId()
                    + " cannot go offline while an order is assigned or being carried");
        }
        this.status = CourierStatus.OFFLINE;
    }

    public void goOfflineWithPendingOffer() {
        requireStatus(CourierStatus.ASSIGNED, CourierStatus.OFFLINE);
        this.status = CourierStatus.OFFLINE;
    }

    public void assignOrder() {
        requireStatus(CourierStatus.IDLE, CourierStatus.ASSIGNED);
        this.status = CourierStatus.ASSIGNED;
    }

    public void cancelAssignment() {
        requireStatus(CourierStatus.ASSIGNED, CourierStatus.IDLE);
        this.status = CourierStatus.IDLE;
    }

    public void declineOrder() {
        if (status != CourierStatus.IDLE) {
            throw new IllegalStateException("Courier " + getAccountId() + " is " + status + " and has no offer to decline");
        }
    }

    public void confirmPickup() {
        requireStatus(CourierStatus.ASSIGNED, CourierStatus.CARRYING);
        this.status = CourierStatus.CARRYING;
    }

    public void confirmDelivery() {
        requireStatus(CourierStatus.CARRYING, CourierStatus.IDLE);
        this.status = CourierStatus.IDLE;
    }

    private void requireStatus(CourierStatus expected, CourierStatus target) {
        if (this.status != expected) {
            throw new IllegalStateException("Courier " + getAccountId() + " cannot go from " + status + " to " + target
                    + " (it must be " + expected + " first)");
        }
    }

    //Getters & Setters
    public double getLongitude() {
        return longitude;
    }
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public CourierStatus getStatus() {
        return status;
    }
    public void setStatus(CourierStatus status) {
        this.status = status;
    }

    public CourierStatus getSeedInitialStatus() {
        return seedInitialStatus;
    }

    public void setSeedInitialStatus(CourierStatus seedInitialStatus) {
        this.seedInitialStatus = seedInitialStatus;
    }

}