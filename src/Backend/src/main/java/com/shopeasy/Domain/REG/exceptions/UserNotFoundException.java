package com.shopeasy.Domain.REG.exceptions;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String email) {
        super("User with" + email + " not found");
    }
}
