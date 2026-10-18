package com.shopeasy.application.reg;

public record CourierRegistrationRequest(
        String email,
        String password,
        String firstName,
        String lastName,
        String carMake,
        String carModel,
        int carYear,
        String carColor
) implements RegistrationRequest {}
