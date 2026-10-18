package com.shopeasy.foundation.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "vehicles")
public class Vehicle {
    @Id
    private Long courierId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "courier_id")
    private Courier courier;

    @Column(nullable = false)
    private String carMake;

    @Column(nullable = false)
    private String carModel;

    @Column(nullable = false)
    private String carColor;

    @Column(nullable = false)
    private int carYear;

    protected Vehicle() {};

    public Vehicle(Courier courier, String carMake, String carModel, String carColor, int carYear) {
        this.courier =  courier;
        this.carMake = carMake;
        this.carModel = carModel;
        this.carColor = carColor;
        this.carYear = carYear;
    }

    public Long getCourierId() {
        return courierId;
    }

    public Courier getCourier() {
        return courier;
    }

    public String getCarMake() {
        return carMake;
    }
    public void setCarMake(String carMake) {
        this.carMake = carMake;
    }

    public String getCarModel() {
        return carModel;
    }
    public void setCarModel(String carModel) {
        this.carModel = carModel;
    }

    public String getCarColor() {
        return carColor;
    }
    public void setCarColor(String carColor) {
        this.carColor = carColor;
    }

    public int getCarYear() {
        return carYear;
    }
    public void setCarYear(int carYear) {
        this.carYear = carYear;
    }
}

