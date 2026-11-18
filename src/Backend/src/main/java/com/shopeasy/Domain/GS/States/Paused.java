package com.shopeasy.Domain.GS.States;

import com.shopeasy.Domain.GS.StoreContext;
import com.shopeasy.Foundation.entities.StoreStatus;

public class Paused implements GSState {

    public String openStore(StoreContext store, int queueSize) {
        if (queueSize >= 6) {
            throw new IllegalStateException("Store cannot be reopened while the preparation queue has 6 or more orders.");
        }
        store.setState(new Open());
        return "[msg] unPausing Store";
    }

    public String pauseStore(StoreContext store) {
        throw new IllegalStateException("Store is already paused");
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
