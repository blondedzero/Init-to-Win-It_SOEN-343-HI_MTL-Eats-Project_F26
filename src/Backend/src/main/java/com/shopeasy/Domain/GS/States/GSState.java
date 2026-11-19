package com.shopeasy.Domain.GS.States;

import com.shopeasy.Domain.GS.StoreContext;
import com.shopeasy.Foundation.entities.StoreStatus;

public interface GSState {

    String openStore(StoreContext store, int queueSize);

    String pauseStore(StoreContext store);

    String closeStore(StoreContext store);

    //simply there so that each state class has the corredct enum associated with it
    StoreStatus getEnum();

    void acceptOrder();

    void markOrderReady();

}
