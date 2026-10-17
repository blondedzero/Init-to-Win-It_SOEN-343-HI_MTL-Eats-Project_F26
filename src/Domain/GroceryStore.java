package Domain;

import Domain.GroceryStoreStates.*;

public class GroceryStore {

    private StateGS state = new Closed();

    public void setState(StateGS stateGS)
    {
        this.state = stateGS;
    }

    public StateGS getState()
    {
        return this.state;
    }

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

    public void acceptOrder(){}
    public void markOrderReady(){}
    public void addItem(){}
    public void editItem(){}
    public void deleteItem(){}





}
