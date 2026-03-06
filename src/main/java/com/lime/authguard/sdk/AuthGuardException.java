package com.lime.authguard.sdk;

public class AuthGuardException extends RuntimeException {

    public AuthGuardException(String message) {
        super(message);
    }

    public AuthGuardException(String message, Throwable cause) {
        super(message, cause);
    }
}
