package com.shopeasy.Domain.exceptions;

public class OrderAssignmentFailedException extends RuntimeException {
    public OrderAssignmentFailedException(String message) {
        super(message);
    }
}
