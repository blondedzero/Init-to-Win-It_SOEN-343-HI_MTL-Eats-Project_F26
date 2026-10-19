package com.shopeasy.Foundation.entities;

public enum OrderStatus {
    PLACED,
    PREPARING, //set status to preparing when order is accepted
    REJECTED,
    CANCELED,
    READY,
    PICKED_UP,
    DELIVERED
}
