package com.shopeasy.Domain.REG.exceptions;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Invalid credentials. Please try again.");
    }
}
