package com.shopeasy.Domain.REG.factories;

public record OperatorData(
        String email,
        String passwordHash,
        String firstName,
        String lastName
) implements RegistrationData {}
