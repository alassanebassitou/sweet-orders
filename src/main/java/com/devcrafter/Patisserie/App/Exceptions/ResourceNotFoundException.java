package com.devcrafter.Patisserie.App.Exceptions;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String s) {
        super(s);
    }

    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " introuvable avec l'id: " + id);
    }
}
