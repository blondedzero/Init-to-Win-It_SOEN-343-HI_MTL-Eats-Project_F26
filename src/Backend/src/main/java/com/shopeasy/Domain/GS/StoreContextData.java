package com.shopeasy.Domain.GS;

public record StoreContextData(Long id, String storeName, String address,
                           double latitude, double longitude, String status) {}