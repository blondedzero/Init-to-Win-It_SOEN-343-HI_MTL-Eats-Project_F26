package com.shopeasy.domain.GroceryStoreStates;

import com.shopeasy.domain.GroceryStore;

public class Closed implements StateGS {

    public String openStore(GroceryStore store) {
        store.setState(new Open());
        return "[msg] opening Store";
    }

    public String pauseStore(GroceryStore store) {
        store.setState(new Paused());
        return "[msg] store is pausing";
    }

    public String closeStore(GroceryStore store) {
        return "[error] store is already closed";

    }

    public void acceptOrder() {
    }

    public void markOrderReady() {
    }

}
