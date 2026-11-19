package com.shopeasy.Application.REG;

import com.shopeasy.Domain.REG.*;
import com.shopeasy.Domain.REG.factories.CourierData;
import com.shopeasy.Domain.REG.factories.CustomerData;
import com.shopeasy.Domain.REG.factories.GroceryStoreData;
import com.shopeasy.Domain.REG.factories.OperatorData;
import com.shopeasy.Domain.REG.registrationRequests.CourierRegistrationRequest;
import com.shopeasy.Domain.REG.registrationRequests.CustomerRegistrationRequest;
import com.shopeasy.Domain.REG.registrationRequests.GroceryStoreRegistrationRequest;
import com.shopeasy.Domain.REG.registrationRequests.OperatorRegistrationRequest;
import com.shopeasy.Foundation.entities.GroceryStore;
import com.shopeasy.Foundation.entities.Role;
import com.shopeasy.Foundation.entities.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class AuthController {

    private final REGModule regModule;
    private final AuthTokenUtil authTokenUtil;

    public AuthController(REGModule regModule, AuthTokenUtil authTokenUtil) {
        this.regModule = regModule;
        this.authTokenUtil = authTokenUtil;
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
            //Add code to create auth token for session so that user is automatically logged in after a successful registration (Maybe)
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "Registration successful! Redirecting..."));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Registration failed. Please try again"));
        }
    }

    record LoginRequest(String email, String password) {}
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest req) {
        User user = regModule.login(req.email(), req.password());
        if (user != null) {
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("authToken", authTokenUtil.generateToken(user), "message", "Login successful! Redirecting..."));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Login failed. Please try again"));
        }
    }

    record AuthRequest(String token) {}
    @PostMapping("/auth")
    public ResponseEntity<Map<String, String>> auth(@RequestBody AuthRequest req) {
        if (regModule.validateToken(req.token()).isValid()) {
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "Authentication successful! Redirecting..."));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication failed. Please login again"));
        }
    }

    @PostMapping("/data")
    public ResponseEntity<Map<String, String>> data(@RequestBody AuthRequest req) {
        ParsedToken token = regModule.validateToken(req.token());
        if (!token.isValid()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication failed. Please login again"));
        }

        Optional<User> userEntity = regModule.getAuthenticatedUserData(token);
        if (userEntity.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Failed to find user data"));
        }

        User user = userEntity.get();
        Map<String, String> body = new LinkedHashMap<>();
        body.put("firstName", user.getFirstName());
        body.put("lastName", user.getLastName());
        body.put("email", user.getEmail());
        body.put("role", user.getRole().toString());
        body.put("userId", user.getAccountId().toString());

        if (user instanceof GroceryStore store) {
            body.put("storeName", store.getStoreName());
        }

        return ResponseEntity.ok(body);
    }
}
