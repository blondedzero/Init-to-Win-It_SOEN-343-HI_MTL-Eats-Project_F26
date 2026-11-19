package com.shopeasy.Domain.GS;


import com.shopeasy.Domain.GS.States.*;
import com.shopeasy.Foundation.entities.GroceryStore;
import com.shopeasy.Foundation.entities.StoreStatus;

public class StoreContext {

    private final GroceryStore entity;

    private GSState state;


    public StoreContext(GroceryStore entity) {
        this.entity = entity;
        this.state = setContextState(entity);
    }

    private GSState setContextState(GroceryStore entity) {
        switch (entity.getStatus()) {
            case OPEN -> {
                return new Open();
            }
            case CLOSED -> {
                return new Closed();
            }
            case PAUSED -> {
                return new Paused();
            }
            default -> {
                entity.setStatus(StoreStatus.CLOSED);
                return new Closed();
            }
        }
    }

    public void acceptOrder(){}
    public void declineOrder() {}
    public void markOrderReady(){}
    public void addItem(){}
    public void editItem(){}
    public void deleteItem(){}


    //when a user wants to change the state simply call the methods and no matter what state the store is currently in it will switch
    //if store is paused, check that queue size is < threshold before allowing re-open, otherwise pause is useless
    public String openStore(int queueSize) {
        return state.openStore(this, queueSize);
    }

    public String pauseStore()
    {
        return state.pauseStore(this);
    }

    public String closeStore() {
        return state.closeStore(this);
    }



    public void setState(GSState GSState)
    {
        this.state = GSState;
        entity.setStatus(state.getEnum());
    }

    public GSState getState()
    {
        return this.state;
    }

}
