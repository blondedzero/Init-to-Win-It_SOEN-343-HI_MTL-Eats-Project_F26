package com.shopeasy.application;


import com.shopeasy.domain.reg.*;
import com.shopeasy.foundation.entities.Role;
import com.shopeasy.foundation.entities.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class AuthController {

    private final REG reg;
    public AuthController(REG reg) {
        this.reg = reg;
    }

    @PostMapping("/register/customer")
    public String register(@RequestBody CustomerRegistrationRequest req) {
        User user = reg.register(Role.CUSTOMER, req);
        System.out.println("CustomerRegistrationRequest");
        return "Hit CustomerRegistration Endpoint";
    }

    @PostMapping("/register/store")
    public void register(@RequestBody GroceryStoreRegistrationRequest req) {
        User user = reg.register(Role.STORE, req);
    }

    @PostMapping("/register/courier")
    public void register(@RequestBody CourierRegistrationRequest req) {
        User user = reg.register(Role.COURIER, req);
    }

    @PostMapping("/register/operator")
    public void register(@RequestBody RegistrationRequest req) {
        User user = reg.register(Role.OPERATOR, req);
    }

    public void login() {

    }

}

