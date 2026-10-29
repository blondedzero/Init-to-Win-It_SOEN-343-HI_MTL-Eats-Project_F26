package com.shopeasy.Application;

import com.shopeasy.Domain.REG.ParsedToken;
import com.shopeasy.Domain.REG.REGModule;
import com.shopeasy.Domain.REG.exceptions.UnauthenticatedException;
import com.shopeasy.Foundation.entities.User;
import org.springframework.stereotype.Component;

@Component
public class RequestAuthenticator {

    private static final String BEARER_PREFIX = "Bearer ";

    private final REGModule regModule;

    public RequestAuthenticator(REGModule regModule) {
        this.regModule = regModule;
    }

    public User authenticate(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new UnauthenticatedException();
        }
        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();

        ParsedToken parsedToken = regModule.validateToken(token);
        if (!parsedToken.isValid()) {
            throw new UnauthenticatedException();
        }
        return regModule.getAuthenticatedUserData(parsedToken)
                .orElseThrow(UnauthenticatedException::new);
    }
}
