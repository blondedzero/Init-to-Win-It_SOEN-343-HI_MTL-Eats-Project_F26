package com.shopeasy.Domain.REG.exceptions;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException() {
        super("A user with email already exists");
    }
}
