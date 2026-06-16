package com.devcrafter.Patisserie.App.Exceptions;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MultipleUsersFoundWithEmailException extends RuntimeException {
    private String email;
    public MultipleUsersFoundWithEmailException(String email) {
        super(String.format(
                "Multiple users found with the email '%s'.",
                email
        ));
        this.email = email;
    }
}
