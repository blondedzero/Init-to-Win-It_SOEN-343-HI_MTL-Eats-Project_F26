package com.shopeasy.Domain.GroceryStore.States;

import com.shopeasy.Domain.GroceryStore.StoreContext;
import com.shopeasy.Foundation.entities.StoreStatus;

public class Paused implements StateGS {


    public String openStore(StoreContext store) {
        store.setState(new Open());

        return "[msg] unPausing Store";
    }

    public String pauseStore(StoreContext store) {
        return "[error] store is already paused";
    }

    public String closeStore(StoreContext store) {
        store.setState(new Closed());
        return "[msg] store is closed";
    }

    public void acceptOrder() {
    }

    public void markOrderReady() {
    }

    public StoreStatus getEnum()
    {
        return StoreStatus.PAUSED;
    }


}
