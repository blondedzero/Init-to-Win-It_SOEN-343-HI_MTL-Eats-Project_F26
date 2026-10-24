package com.shopeasy.Domain.REG.factories;

import com.shopeasy.Foundation.entities.Operator;

public class OperatorFactory extends UserFactory {

    @Override
    public Operator createUser(RegistrationData data) {
        if (!(data instanceof OperatorData operatorData)) {
            throw new IllegalArgumentException("Invalid data type");
        }

        return new Operator(
                operatorData.email(),
                operatorData.passwordHash(),
                operatorData.firstName(),
                operatorData.lastName());
    }
}
