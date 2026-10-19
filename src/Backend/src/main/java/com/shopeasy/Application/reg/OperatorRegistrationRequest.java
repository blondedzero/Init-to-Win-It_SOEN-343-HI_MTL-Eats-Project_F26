package com.shopeasy.Application.reg;

public record OperatorRegistrationRequest(
        String email,
        String password,
        String firstName,
        String lastName
) implements RegistrationRequest {}
