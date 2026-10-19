package com.shopeasy.Domain.REG;

import com.shopeasy.Application.reg.CustomerRegistrationRequest;
import com.shopeasy.Application.reg.CourierRegistrationRequest;
import com.shopeasy.Application.reg.GroceryStoreRegistrationRequest;
import com.shopeasy.Application.reg.RegistrationRequest;
import com.shopeasy.Domain.REG.exceptions.UserAlreadyExistsException;
import com.shopeasy.Domain.REG.exceptions.UserNotFoundException;
import com.shopeasy.Foundation.entities.Courier;
import com.shopeasy.Foundation.entities.Vehicle;
import com.shopeasy.Foundation.entities.Role;
import com.shopeasy.Foundation.entities.User;
import com.shopeasy.Foundation.repositories.UserRepository;
import com.shopeasy.Foundation.repositories.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class REGModule {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final PasswordEncoder encoder;

    public REGModule(UserRepository userRepository, VehicleRepository vehicleRepository) {
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.encoder = new PasswordEncoder();
    }

    public RegistrationData createRegistrationData(Role role, RegistrationRequest req) {
        String passwordHash = encoder.hashPassword(req.password());
        RegistrationData registrationData;

        switch (role) {
            case CUSTOMER -> {
                if (!(req instanceof CustomerRegistrationRequest)) {
                    throw new IllegalArgumentException("Invalid Request");
                }

                //Add card validation with Stripe API later
                //Mock payment info validation
                String cardBrand = "VISA";
                String cardLast4Digits = ((CustomerRegistrationRequest) req).cardNumber().substring(11);
                int cardExpMonth = ((CustomerRegistrationRequest) req).cardExpMonth();
                int cardExpYear = ((CustomerRegistrationRequest) req).cardExpYear();
                String paymentProcessorToken = "this is a placeholder token";
                String stripeCustomerId = UUID.randomUUID().toString();

                registrationData = new CustomerData(
                        req.email(),
                        passwordHash,
                        req.firstName(),
                        req.lastName(),
                        ((CustomerRegistrationRequest) req).address(),
                        cardBrand,
                        cardLast4Digits,
                        cardExpMonth,
                        cardExpYear,
                        paymentProcessorToken,
                        stripeCustomerId);
            }
            case STORE -> {
                if (!(req instanceof GroceryStoreRegistrationRequest storeRequest)) {
                    throw new IllegalArgumentException("Invalid Request");
                }

                registrationData = new GroceryStoreData(
                        req.email(),
                        passwordHash,
                        req.firstName(),
                        req.lastName(),
                        storeRequest.address(),
                        storeRequest.storeName());
            }
            case COURIER -> {
                if (!(req instanceof CourierRegistrationRequest courierRequest)) {
                    throw new IllegalArgumentException("Invalid Request");
                }

                registrationData = new CourierData(
                        req.email(),
                        passwordHash,
                        req.firstName(),
                        req.lastName(),
                        courierRequest.carMake(),
                        courierRequest.carModel(),
                        courierRequest.carYear(),
                        courierRequest.carColor());
            }
            case OPERATOR -> {
                registrationData = new OperatorData(
                        req.email(),
                        passwordHash,
                        req.firstName(),
                        req.lastName());
            }
            default -> {
                throw new IllegalArgumentException("Invalid role");
            }
        }
        return registrationData;
    }

    @Transactional
    public User register(Role role, RegistrationRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new UserAlreadyExistsException(req.email());
        }

        UserFactory factory;
        RegistrationData registrationData = createRegistrationData(role, req);

        switch (role) {
            case CUSTOMER -> factory = new CustomerFactory();
            case STORE -> factory = new GroceryStoreFactory();
            case COURIER -> factory = new CourierFactory();
            case OPERATOR -> factory = new OperatorFactory();
            default -> throw new IllegalArgumentException("Invalid role");
        }

        User user = factory.createUser(registrationData);
        User savedUser = userRepository.save(user);
        if (savedUser instanceof Courier courier) {
            CourierData courierData = (CourierData) registrationData;
            vehicleRepository.save(new Vehicle(
                    courier,
                    courierData.carMake(),
                    courierData.carModel(),
                    courierData.carColor(),
                    courierData.carYear()));
        }
        return savedUser;
    }

    public User login(String email, String password) {
        Optional<User> userEntry = userRepository.findByEmail(email);
        if (userEntry.isPresent()) {
            User user = userEntry.get();
            if (encoder.checkPassword(password, user.getPasswordHash())) {
                return user;
            } else return null;
        } else {
            throw new UserNotFoundException(email);
        }
    }
}
