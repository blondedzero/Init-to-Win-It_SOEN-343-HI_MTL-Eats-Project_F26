package com.shopeasy.application;

import com.shopeasy.domain.reg.RegistrationData;

public interface RegistrationRequest {
    String email();
    String password();
    String firstName();
    String lastName();
}

