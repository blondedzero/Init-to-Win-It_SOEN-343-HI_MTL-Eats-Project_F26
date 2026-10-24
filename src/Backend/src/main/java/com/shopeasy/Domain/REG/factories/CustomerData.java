package com.shopeasy.Domain.REG.factories;

public record CustomerData(
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
