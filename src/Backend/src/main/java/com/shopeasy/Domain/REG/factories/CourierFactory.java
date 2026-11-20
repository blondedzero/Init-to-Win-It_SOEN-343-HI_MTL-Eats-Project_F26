package com.shopeasy.Domain.REG.factories;

import com.shopeasy.Foundation.entities.Courier;
import com.shopeasy.Foundation.entities.User;

public class CourierFactory extends UserFactory {

    @Override
    public User createUser(RegistrationData data) {
        if (!(data instanceof CourierData courierData)) {
            throw new IllegalArgumentException("Invalid data type");
        }

        Coordinates coords = Coordinates.randomMontrealCoordinates();
        double longitude = coords.longitude();
        double latitude = coords.latitude();

        return new Courier(
                courierData.email(),
                courierData.passwordHash(),
                courierData.firstName(),
                courierData.lastName(),
                longitude,
                latitude);
    }
}
