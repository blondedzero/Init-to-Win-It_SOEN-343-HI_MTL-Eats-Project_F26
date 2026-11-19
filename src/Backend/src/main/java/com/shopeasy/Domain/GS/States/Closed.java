package com.shopeasy.Domain.GS.States;

import com.shopeasy.Domain.GS.StoreContext;
import com.shopeasy.Foundation.entities.StoreStatus;

public class Closed implements GSState {

    public String openStore(StoreContext store, int queueSize) {
        store.setState(new Open());
        return "[msg] opening Store";
    }

    public String pauseStore(StoreContext store) {
        throw new IllegalStateException("Cannot pause a closed Store");
    }

    public String closeStore(StoreContext store) {
        throw new IllegalStateException("Cannot close a closed Store");

    }

    public void acceptOrder() {
    }

    public void markOrderReady() {
    }

    public StoreStatus getEnum()
    {
        return StoreStatus.CLOSED;
    }

}
