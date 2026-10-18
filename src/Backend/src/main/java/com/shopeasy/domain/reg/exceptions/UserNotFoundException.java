package com.shopeasy.domain.reg.exceptions;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String email) {
        super("User with" + email + " not found");
    }
}
