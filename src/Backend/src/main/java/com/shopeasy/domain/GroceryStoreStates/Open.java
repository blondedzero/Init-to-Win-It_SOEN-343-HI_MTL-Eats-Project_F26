package com.shopeasy.domain.GroceryStoreStates;

import com.shopeasy.domain.GroceryStore;

public class Open implements StateGS {


    public String openStore(GroceryStore store) {
        return "[error] this store is already open";
    }

    public String pauseStore(GroceryStore store) {
        store.setState(new Paused());
        return "[msg] store is pausing";
    }

    public String closeStore(GroceryStore store) {
        store.setState(new Closed());
        return "[msg] store is closing";

    }

    public void acceptOrder() {
    }

    public void markOrderReady() {
    }

}
