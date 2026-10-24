package com.shopeasy.Foundation.entities;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "customers")
@PrimaryKeyJoinColumn(name = "account_id")
public class Customer extends User {

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private double latitude;

    protected Customer() {}

    //Constructor
    public Customer(String email, String passwordHash, String firstName, String lastName, String address, double longitude, double latitude) {
        super(email, passwordHash, firstName, lastName, Role.CUSTOMER);
        this.address = address;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    @Override
    public List<Order> getHistory() {
        return null;
    }

    //Methods to implement
    public void placeOrder() {
    }

    public void cancelOrder() {
    }

    //Getters & Setters
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
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

}