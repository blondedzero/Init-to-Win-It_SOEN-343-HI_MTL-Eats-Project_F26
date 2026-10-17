package com.shopeasy.domain.reg;

import org.springframework.security.crypto.bcrypt.BCrypt;

public class PasswordEncoder {

    public String hashPassword(String password) {
        String salt = BCrypt.gensalt();
        return BCrypt.hashpw(password, salt);
    }

    public boolean checkPassword(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }

}
