package com.shopeasy.Domain.REG.exceptions;

public class UnauthenticatedException extends RuntimeException {
    public UnauthenticatedException() {
        super("You must be logged in to do this. Please login again.");
    }
}
