package com.devcrafter.Patisserie.App.Exceptions;

public class SessionCreationFailedException extends RuntimeException {
    public SessionCreationFailedException(String sessionCreationFailed) {
        super(sessionCreationFailed);
    }
}
