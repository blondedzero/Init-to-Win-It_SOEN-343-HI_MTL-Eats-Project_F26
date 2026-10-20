package com.shopeasy.Domain.REG.registrationRequests;

public record CustomerRegistrationRequest(
        String email,
        String password,
        String firstName,
        String lastName,
        String address,
        String cardNumber,
        int cardExpMonth,
        int cardExpYear,
        int cardCVV
) implements RegistrationRequest {}
