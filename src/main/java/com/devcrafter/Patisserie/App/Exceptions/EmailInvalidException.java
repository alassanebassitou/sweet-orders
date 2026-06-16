package com.devcrafter.Patisserie.App.Exceptions;

public class EmailInvalidException extends RuntimeException {

    public EmailInvalidException(String emailInvalid) {
        super(emailInvalid);
    }
}
