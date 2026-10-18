package com.shopeasy.domain.reg;

import com.shopeasy.foundation.entities.Role;
import com.shopeasy.foundation.entities.User;

public class OperatorFactory extends UserFactory {

    @Override
    public User createUser(RegistrationData data) {
        if (!(data instanceof OperatorData operatorData)) {
            throw new IllegalArgumentException("Invalid data type");
        }

        return new User(
                operatorData.email(),
                operatorData.passwordHash(),
                operatorData.firstName(),
                operatorData.lastName(),
                Role.OPERATOR);
    }
}
