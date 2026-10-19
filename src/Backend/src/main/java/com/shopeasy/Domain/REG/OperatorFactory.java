package com.shopeasy.Domain.REG;

import com.shopeasy.Foundation.entities.Role;
import com.shopeasy.Foundation.entities.User;

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
