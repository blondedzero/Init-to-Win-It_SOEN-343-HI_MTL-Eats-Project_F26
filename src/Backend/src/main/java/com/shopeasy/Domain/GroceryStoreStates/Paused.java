package com.shopeasy.Domain.GroceryStoreStates;

import com.shopeasy.Domain.GroceryStore;

public class Paused implements StateGS {

    @Override
    public String openStore(GroceryStore store) {
        store.setState(new Open());
        return "[msg] unPausing Store";
    }

    @Override
    public String pauseStore(GroceryStore store) {
        return "[error] store is already paused";
    }

    @Override
    public String closeStore(GroceryStore store) {
        store.setState(new Closed());
        return "[msg] store is closed";
    }

    public void acceptOrder() {
    }

    public void markOrderReady() {
    }

}
