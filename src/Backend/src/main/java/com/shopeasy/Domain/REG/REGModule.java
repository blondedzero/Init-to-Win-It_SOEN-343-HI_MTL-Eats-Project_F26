package com.shopeasy.Domain.REG;

import com.shopeasy.Domain.REG.exceptions.*;
import com.shopeasy.Domain.REG.factories.*;
import com.shopeasy.Domain.REG.registrationRequests.CourierRegistrationRequest;
import com.shopeasy.Domain.REG.registrationRequests.CustomerRegistrationRequest;
import com.shopeasy.Domain.REG.registrationRequests.GroceryStoreRegistrationRequest;
import com.shopeasy.Domain.REG.registrationRequests.RegistrationRequest;
import com.shopeasy.Foundation.entities.*;
import com.shopeasy.Foundation.repositories.*;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class REGModule {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final GroceryStoreRepository groceryStoreRepository;
    private final PaymentInfoRepository paymentInfoRepository;
    private final PasswordEncoder encoder;
    private final AuthTokenUtil authTokenUtil;

    public REGModule(UserRepository userRepository, VehicleRepository vehicleRepository, AuthTokenUtil authTokenUtil, GroceryStoreRepository groceryStoreRepository, PaymentInfoRepository paymentInfoRepository) {
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.groceryStoreRepository = groceryStoreRepository;
        this.paymentInfoRepository = paymentInfoRepository;
        this.encoder = new PasswordEncoder();
        this.authTokenUtil = authTokenUtil;
    }

    public RegistrationData processRegistrationRequest(Role role, RegistrationRequest req) {
        String passwordHash = encoder.hashPassword(req.password());
        RegistrationData registrationData;

        switch (role) {
            case CUSTOMER -> {
                if (!(req instanceof CustomerRegistrationRequest customerRequest)) {
                    throw new IllegalArgumentException("Invalid Request");
                }

                //Add card validation with Stripe API later
                //Mock payment info validation
                String cardBrand = "VISA";
                String cardLast4Digits = customerRequest.cardNumber().substring(12);
                int cardExpMonth = customerRequest.cardExpMonth();
                int cardExpYear = customerRequest.cardExpYear();
                String paymentProcessorToken = UUID.randomUUID().toString();
                String stripeCustomerId = UUID.randomUUID().toString();

                registrationData = new CustomerData(
                        req.email(),
                        passwordHash,
                        req.firstName(),
                        req.lastName(),
                        customerRequest.address(),
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
            throw new UserAlreadyExistsException();
        }

        UserFactory factory;
        RegistrationData registrationData = processRegistrationRequest(role, req);

        switch (role) {
            case CUSTOMER -> factory = new CustomerFactory();
            case STORE -> factory = new GroceryStoreFactory();
            case COURIER -> factory = new CourierFactory();
            case OPERATOR -> factory = new OperatorFactory();
            default -> throw new IllegalArgumentException("Invalid role");
        }

        User user = factory.createUser(registrationData);

        if (user instanceof GroceryStore store && groceryStoreRepository.existsByAddressAndStoreName(store.getAddress(), store.getStoreName())) {
            throw new StoreAlreadyExistsException();
        }

        User savedUser = userRepository.save(user);

        if (savedUser instanceof Customer customer) {
            CustomerData customerData = (CustomerData) registrationData;
            paymentInfoRepository.save(new PaymentInfo(
                    customer,
                    customerData.cardBrand(),
                    customerData.cardLast4Digits(),
                    customerData.cardExpMonth(),
                    customerData.cardExpYear(),
                    customerData.paymentProcessorToken(),
                    customerData.stripeCustomerId()
                    ));
        }

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
            } else throw  new InvalidCredentialsException();
        } else {
            throw new InvalidCredentialsException();
        }
    }

    public Optional<User> getAuthenticatedUserData(ParsedToken token) {
        Long accountId = Long.valueOf(token.claims().getSubject());
        return userRepository.findById(accountId);
    }

    public ParsedToken validateToken(String token) {
        Claims claims = authTokenUtil.parseToken(token);
        Optional<User> userEntry = userRepository.findById(Long.valueOf(claims.getSubject()));
        return new ParsedToken(userEntry.isPresent(), claims);
    }
}
