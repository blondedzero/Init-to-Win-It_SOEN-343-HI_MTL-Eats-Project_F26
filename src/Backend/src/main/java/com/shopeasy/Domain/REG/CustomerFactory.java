package com.shopeasy.Domain.REG;

import com.shopeasy.Foundation.entities.Customer;
import com.shopeasy.Foundation.entities.User;

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
