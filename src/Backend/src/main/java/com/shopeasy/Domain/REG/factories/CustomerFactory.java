package com.shopeasy.Domain.REG.factories;

import com.shopeasy.Foundation.entities.Customer;
import com.shopeasy.Foundation.entities.User;

public class CustomerFactory extends UserFactory {

    @Override
    public User createUser(RegistrationData data) {
        if (!(data instanceof CustomerData customerData)) {
            throw new IllegalArgumentException("Invalid data type");
        }
        
        Coordinates coords = Coordinates.randomMontrealCoordinates();
        double longitude = coords.longitude();
        double latitude = coords.latitude();

        return new Customer(
                customerData.email(),
                customerData.passwordHash(),
                customerData.firstName(),
                customerData.lastName(),
                customerData.address(),
                longitude,
                latitude);
    }
}
