package com.shopeasy.Domain.ORD.exceptions;

public class OrderRuleViolationException extends RuntimeException {
    public OrderRuleViolationException(String message) {
        super(message);
    }
}
