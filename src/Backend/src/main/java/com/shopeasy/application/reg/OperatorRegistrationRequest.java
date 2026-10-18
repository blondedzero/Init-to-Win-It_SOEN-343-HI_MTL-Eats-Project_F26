package com.shopeasy.application.reg;

public record OperatorRegistrationRequest(
        String email,
        String password,
        String firstName,
        String lastName
) implements RegistrationRequest {}
