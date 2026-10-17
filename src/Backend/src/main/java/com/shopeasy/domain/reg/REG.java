package com.shopeasy.domain.reg;

import com.shopeasy.application.CustomerRegistrationRequest;
import com.shopeasy.application.RegistrationRequest;
import com.shopeasy.foundation.entities.Role;
import com.shopeasy.foundation.entities.User;
import com.shopeasy.foundation.repositories.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class REG {

    private final UserRepository userRepository;
    private PasswordEncoder encoder;

    public REG(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.encoder = new PasswordEncoder();
    }

    public User register(Role role, RegistrationRequest req) {

        UserFactory factory;
        RegistrationData registrationData;

        String passwordHash = encoder.hashPassword(req.password());

        switch (role) {
            case CUSTOMER ->  {
                if (!(req instanceof CustomerRegistrationRequest)) {
                    throw new IllegalArgumentException("Invalid Request");
                }
                factory = new CustomerFactory();
                registrationData = new CustomerData(
                        req.email(),
                        passwordHash,
                        req.firstName(),
                        req.lastName(),
                        ((CustomerRegistrationRequest) req).address());
            }
            default ->  {
                throw new IllegalArgumentException("Invalid role");
            }
        }

        User user = factory.createUser(registrationData);
        return userRepository.save(user);
    }
}


