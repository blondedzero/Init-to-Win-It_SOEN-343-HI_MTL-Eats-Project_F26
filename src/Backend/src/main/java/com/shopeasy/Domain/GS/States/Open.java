package com.shopeasy.Domain.GS.States;

import com.shopeasy.Domain.GS.StoreContext;
import com.shopeasy.Foundation.entities.StoreStatus;

public class Open implements GSState {


    public String openStore(StoreContext store) {
        return "[error] this store is already open";
    }

    public String pauseStore(StoreContext store) {
        store.setState(new Paused());
        return "[msg] store is pausing";
    }

    public String closeStore(StoreContext store) {
        store.setState(new Closed());
        return "[msg] store is closing";

    }

    public void acceptOrder() {
    }

    public void markOrderReady() {
    }

    public StoreStatus getEnum()
    {
        return StoreStatus.OPEN;
    }


}
