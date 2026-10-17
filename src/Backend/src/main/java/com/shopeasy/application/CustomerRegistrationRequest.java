package com.shopeasy.application;

public record CustomerRegistrationRequest(
        String email,
        String password,
        String firstName,
        String lastName,
        String address
) implements RegistrationRequest {}
