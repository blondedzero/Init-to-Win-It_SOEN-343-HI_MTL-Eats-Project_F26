package com.shopeasy.Domain.GS.States;

import com.shopeasy.Domain.GS.StoreContext;
import com.shopeasy.Foundation.entities.StoreStatus;

public class Closed implements GSState {

    public String openStore(StoreContext store) {
        store.setState(new Open());
        return "[msg] opening Store";
    }

    public String pauseStore(StoreContext store) {
        store.setState(new Paused());
        return "[msg] store is pausing";
    }

    public String closeStore(StoreContext store) {
        return "[error] store is already closed";

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
