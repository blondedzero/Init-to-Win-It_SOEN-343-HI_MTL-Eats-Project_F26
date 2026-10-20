package com.shopeasy.Foundation.entities;

import jakarta.persistence.*;

@Entity
@Table(
        name = "grocery_stores",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_store_name_address",
                columnNames = {"store_name", "address"}))
@PrimaryKeyJoinColumn(name = "account_id")
public class GroceryStore extends User {

    @Column(nullable = false)
    private String storeName;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private double latitude;

    protected GroceryStore() {}

    //Constructor
    public GroceryStore(String email, String passwordHash, String firstName, String lastName, String address, double longitude, double latitude, String storeName) {
        super(email, passwordHash, firstName, lastName, Role.STORE);
        this.longitude = longitude;
        this.latitude = latitude;
        this.storeName = storeName;
        this.address = address;
    }

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

    public String getStoreName() {
        return storeName;
    }
    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}