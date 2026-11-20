package com.shopeasy.Domain.REG.factories;

import com.shopeasy.Foundation.entities.GroceryStore;
import com.shopeasy.Foundation.entities.User;

import static com.shopeasy.Foundation.entities.StoreStatus.CLOSED;

public class GroceryStoreFactory extends UserFactory {

    @Override
    public User createUser(RegistrationData data) {
        if (!(data instanceof GroceryStoreData storeData)) {
            throw new IllegalArgumentException("Invalid data type");
        }

        Coordinates coords = Coordinates.randomMontrealCoordinates();
        double longitude = coords.longitude();
        double latitude = coords.latitude();

        GroceryStore store = new GroceryStore(
                storeData.email(),
                storeData.passwordHash(),
                storeData.firstName(),
                storeData.lastName(),
                storeData.address(),
                longitude,
                latitude,
                storeData.storeName());
        store.setStatus(CLOSED);
        return store;
    }
}
