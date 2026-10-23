package com.shopeasy.Domain.GroceryStore;


import com.shopeasy.Domain.GroceryStore.States.*;
import com.shopeasy.Foundation.entities.GroceryStore;
import com.shopeasy.Foundation.entities.StoreStatus;

public class StoreContext {

    private final GroceryStore entity;

    private StateGS state;


    public StoreContext(GroceryStore entity) {
        this.entity = entity;
        this.state=new Closed();
        entity.setStatus(StoreStatus.CLOSED);
    }

    public void acceptOrder(){}
    public void markOrderReady(){}
    public void addItem(){}
    public void editItem(){}
    public void deleteItem(){}


    //when a user wants to change the state simply call the methods and no matter what state the store is currently in it will switch
    public String openStore() {
        return state.openStore(this);
    }

    public String pauseStore()
    {
        return state.pauseStore(this);
    }

    public String closeStore() {
        return state.closeStore(this);
    }



    public void setState(StateGS stateGS)
    {
        this.state = stateGS;
        entity.setStatus(state.getEnum());

    }

    public StateGS getState()
    {
        return this.state;
    }





}
