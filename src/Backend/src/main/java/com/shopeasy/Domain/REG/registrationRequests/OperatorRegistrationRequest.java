package com.shopeasy.Domain.REG.registrationRequests;

public record OperatorRegistrationRequest(
        String email,
        String password,
        String firstName,
        String lastName
) implements RegistrationRequest {}
