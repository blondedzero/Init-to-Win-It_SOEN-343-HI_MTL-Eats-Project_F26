package com.shopeasy.Domain;

import com.shopeasy.Domain.GroceryStoreStates.*;

public class GroceryStore {


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



    //state  transitions, these are only used by the internal implemented states, Closed Paused and Open
    private StateGS state = new Closed();

    public void setState(StateGS stateGS)
    {
        this.state = stateGS;
    }

    public StateGS getState()
    {
        return this.state;
    }





}
