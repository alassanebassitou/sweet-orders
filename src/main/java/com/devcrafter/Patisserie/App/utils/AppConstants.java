package com.devcrafter.Patisserie.App.utils;

import java.math.BigDecimal;

public final class AppConstants {

    private AppConstants() {}

    public static final String CURRENT_USER = "currentUser";
    public static final String SESSION_ID = "sessionId";
    public static final String CAT_NOT_FOUND = "Category not found";
    public static final BigDecimal ACOMPTE_PERCENT = new BigDecimal("0.50"); // 50%
    public static final String PRODUCT_NOT_FOUND = "Produit";
    public static final String COMMANDE_NOT_FOUND = "Commande";
    public static final String EMAIL_INVALID = "Email Invalide";
    public static final String USER_NOT_FOUND = "Utilisateur";
    public static final String INVALID_ROLE = "Role Invalide";
    public static final String PREFIX = "session:";
    public static final String USER_SESSION = "user-session:";
}
