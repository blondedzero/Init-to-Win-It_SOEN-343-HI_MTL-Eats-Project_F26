package com.shopeasy.domain.reg;

import com.shopeasy.foundation.entities.Customer;
import com.shopeasy.foundation.entities.User;

public class CustomerFactory extends UserFactory {

    @Override
    public User createUser(RegistrationData data) {
        if (!(data instanceof CustomerData)) {
            throw new IllegalArgumentException("Invalid data type");
        }
        String passwordHash = encoder.hashPassword(data.passwordHash());
        double longitude = 45; //write function to get lon,lat from address with Google Maps API
        double latitude = -73;
        return new Customer(
                data.email(),
                passwordHash,
                data.firstName(),
                data.lastName(),
                ((CustomerData) data).address(),
                longitude,
                latitude);
    }
}
