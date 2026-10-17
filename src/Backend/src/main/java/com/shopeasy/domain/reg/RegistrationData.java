package com.shopeasy.domain.reg;

import com.shopeasy.foundation.entities.Role;

public interface RegistrationData {
        String email();
        String passwordHash();
        String firstName();
        String lastName();
}

record CustomerData(
        String email,
        String passwordHash,
        String firstName,
        String lastName,
        String address
) implements RegistrationData {}

record GroceryStoreData(
        String email,
        String passwordHash,
        String firstName,
        String lastName,
        String address,
        String storeName
) implements RegistrationData {}

record CourierData(
        String email,
        String passwordHash,
        String firstName,
        String lastName,
        String carMake,
        String carModel,
        String carYear,
        String carColor
) implements RegistrationData {}

