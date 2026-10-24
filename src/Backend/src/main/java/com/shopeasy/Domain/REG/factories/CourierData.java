package com.shopeasy.Domain.REG.factories;

public record CourierData(
        String email,
        String passwordHash,
        String firstName,
        String lastName,
        String carMake,
        String carModel,
        int carYear,
        String carColor
) implements RegistrationData {}
