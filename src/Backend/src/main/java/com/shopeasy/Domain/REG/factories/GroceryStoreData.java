package com.shopeasy.Domain.REG.factories;

public record GroceryStoreData(
        String email,
        String passwordHash,
        String firstName,
        String lastName,
        String address,
        String storeName
) implements RegistrationData {}
