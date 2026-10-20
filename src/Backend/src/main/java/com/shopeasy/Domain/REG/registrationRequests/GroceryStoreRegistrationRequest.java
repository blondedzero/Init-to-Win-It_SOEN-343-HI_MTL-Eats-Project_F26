package com.shopeasy.Domain.REG.registrationRequests;

public record GroceryStoreRegistrationRequest(
        String email,
        String password,
        String firstName,
        String lastName,
        String address,
        String storeName
) implements RegistrationRequest {}
