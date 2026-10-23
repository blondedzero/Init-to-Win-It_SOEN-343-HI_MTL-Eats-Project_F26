package com.shopeasy.Domain.GroceryStore.States;

import com.shopeasy.Domain.GroceryStore.StoreContext;
import com.shopeasy.Foundation.entities.StoreStatus;

public interface StateGS {


    String openStore(StoreContext store);

    String pauseStore(StoreContext store);

    String closeStore(StoreContext store);

    //simply there so that each state class has the corredct enum associated with it
    StoreStatus getEnum();

    void acceptOrder();

    void markOrderReady();

}
