package com.shopeasy.Domain.GroceryStoreStates;

import com.shopeasy.Domain.GroceryStore;

public interface StateGS {
    String openStore(GroceryStore store);

    String pauseStore(GroceryStore store);

    String closeStore(GroceryStore store);

    void acceptOrder();

    void markOrderReady();
}
