package com.devcrafter.Patisserie.App.Exceptions;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String s) {
        super(s);
    }
}
