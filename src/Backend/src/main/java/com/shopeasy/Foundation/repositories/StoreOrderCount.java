package com.shopeasy.Foundation.repositories;

import com.shopeasy.Foundation.entities.OrderStatus;

public interface StoreOrderCount {
    Long getStoreId();
    OrderStatus getStatus();
    long getCount();
}

