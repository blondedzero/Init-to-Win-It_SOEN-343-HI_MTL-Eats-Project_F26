package com.shopeasy.Domain.REG.factories;
import com.shopeasy.Foundation.entities.User;

public abstract class UserFactory {
    public abstract User createUser(RegistrationData data);
}
