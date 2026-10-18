package com.shopeasy.application.reg;


import com.shopeasy.domain.reg.*;
import com.shopeasy.foundation.entities.Role;
import com.shopeasy.foundation.entities.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class AuthController {

    private final REGModule regModule;
    public AuthController(REGModule regModule) {
        this.regModule = regModule;
    }

    @PostMapping("/register/customer")
    public ResponseEntity<Map<String, String>> register(@RequestBody CustomerRegistrationRequest req) {
        User user = regModule.register(Role.CUSTOMER, req);
        return registerResponse(user);
    }

    @PostMapping("/register/store")
    public ResponseEntity<Map<String, String>> register(@RequestBody GroceryStoreRegistrationRequest req) {
        User user = regModule.register(Role.STORE, req);
        return registerResponse(user);
    }

    @PostMapping("/register/courier")
    public ResponseEntity<Map<String, String>> register(@RequestBody CourierRegistrationRequest req) {
        User user = regModule.register(Role.COURIER, req);
        return registerResponse(user);
    }

    @PostMapping("/register/operator")
    public ResponseEntity<Map<String, String>> register(@RequestBody OperatorRegistrationRequest req) {
        User user = regModule.register(Role.OPERATOR, req);
        return registerResponse(user);
    }

    private ResponseEntity<Map<String, String>> registerResponse(User user) {
        if (user != null) {
            //Add code to create auth token for session
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("token", "Registration successful!"));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Registration failed. Please try again"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest req) {
        User user = regModule.login(req.email(), req.password());
        if (user != null) {
            //Add code to create auth token for session
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("token", "this is an authentication token"));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Login failed. Please try again"));
        }
    }
}
