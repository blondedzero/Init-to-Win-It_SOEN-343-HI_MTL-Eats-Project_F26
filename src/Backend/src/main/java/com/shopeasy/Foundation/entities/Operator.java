package com.shopeasy.Foundation.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import java.util.List;

@Entity
@Table(name = "operators")
@PrimaryKeyJoinColumn(name = "account_id")
public class Operator extends User {
    protected Operator() {}

    public Operator(String email, String passwordHash, String firstName, String lastName) {
        super(email, passwordHash, firstName, lastName, Role.OPERATOR);
    }

    @Override
    public List<Order> getHistory() {
        return null;
    }

    public void assignOrder() {
    }

    public void openStore() {
    }

    public void pauseStore() {
    }

    public void restoreSystem() {
    }
}
