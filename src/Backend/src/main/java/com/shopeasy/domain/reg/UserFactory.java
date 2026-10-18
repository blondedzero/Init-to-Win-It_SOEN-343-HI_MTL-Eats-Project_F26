package com.shopeasy.domain.reg;
import com.shopeasy.foundation.entities.User;

public abstract class UserFactory {
    public abstract User createUser(RegistrationData data);
}
