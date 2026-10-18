package com.shopeasy.domain.reg;

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
        String address,
        String cardBrand,
        String cardLast4Digits,
        int cardExpMonth,
        int cardExpYear,
        String paymentProcessorToken,
        String stripeCustomerId
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
        int carYear,
        String carColor
) implements RegistrationData {}

record OperatorData(
        String email,
        String passwordHash,
        String firstName,
        String lastName
) implements RegistrationData {}
