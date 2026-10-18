package com.shopeasy.foundation.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "couriers")
@PrimaryKeyJoinColumn(name = "account_id")
public class Courier extends User {

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private double latitude;

    protected Courier() {}

    //Constructor
    public Courier(String email, String passwordHash, String firstName, String lastName, double longitude, double latitude) {
        super(email, passwordHash, firstName, lastName, Role.COURIER);
        this.longitude = longitude;
        this.latitude = latitude;
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