package com.shopeasy.Domain.REG.exceptions;

public class StoreAlreadyExistsException extends RuntimeException {
    public StoreAlreadyExistsException() {
        super("This store is already registered on the platform");
    }
}
