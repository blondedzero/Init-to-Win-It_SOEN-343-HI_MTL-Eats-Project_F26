package com.shopeasy.domain.reg;

import com.shopeasy.foundation.entities.Customer;
import com.shopeasy.foundation.entities.User;

public class CustomerFactory extends UserFactory {

    @Override
    public User createUser(RegistrationData data) {
        if (!(data instanceof CustomerData customerData)) {
            throw new IllegalArgumentException("Invalid data type");
        }
        double longitude = 45; //write function to get lon,lat from address with Google Maps API
        double latitude = -73;
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
