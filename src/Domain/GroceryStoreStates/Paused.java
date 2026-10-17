package Domain.GroceryStoreStates;

import Domain.GroceryStore;

public class Paused implements StateGS {


    public String openStore(GroceryStore store) {
        store.setState(new Open());
        return "[msg] unPausing Store";
    }

    public String pauseStore(GroceryStore store) {
        return "[error] store is already paused";
    }

    public String closeStore(GroceryStore store) {
        store.setState(new Closed());
        return "[msg] store is closed";
    }

    public void acceptOrder() {
    }

    public void markOrderReady() {
    }

}
