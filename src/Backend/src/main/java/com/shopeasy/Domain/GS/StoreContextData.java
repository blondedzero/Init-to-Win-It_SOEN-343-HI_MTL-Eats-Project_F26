package com.shopeasy.Domain.GS;

import com.shopeasy.Foundation.entities.GroceryStore;

public record StoreContextData(Long id, String storeName, String address,
                               double latitude, double longitude, String status, long ordersInProgress, long ordersReady) {

    public static StoreContextData from(GroceryStore store) {
        return from(store, 0, 0);
    }

    public static StoreContextData from(GroceryStore store, long ordersInProgress, long ordersReady) {
        return new StoreContextData(store.getAccountId(), store.getStoreName(), store.getAddress(),
                store.getLatitude(), store.getLongitude(), store.getStatus().name(),
                ordersInProgress, ordersReady);
    }
}

