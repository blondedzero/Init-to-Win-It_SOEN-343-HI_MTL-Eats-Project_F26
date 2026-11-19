package com.shopeasy.Application;

public abstract class Controller {
    final RequestAuthenticator authenticator;

    public Controller(RequestAuthenticator authenticator) {
        this.authenticator = authenticator;
    }
}
