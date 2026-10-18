package com.shopeasy.domain.reg;

import com.shopeasy.foundation.entities.Courier;
import com.shopeasy.foundation.entities.User;

public class CourierFactory extends UserFactory {

    @Override
    public User createUser(RegistrationData data) {
        if (!(data instanceof CourierData courierData)) {
            throw new IllegalArgumentException("Invalid data type");
        }
        double longitude = 45; //write function to get lon,lat from address with Google Maps API
        double latitude = -73;

        return new Courier(
                courierData.email(),
                courierData.passwordHash(),
                courierData.firstName(),
                courierData.lastName(),
                longitude,
                latitude);
    }
}
