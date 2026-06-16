package com.devcrafter.Patisserie.App.repository;

import com.devcrafter.Patisserie.App.enums.Role;
import com.devcrafter.Patisserie.App.models.User;

import java.util.Optional;

public interface UserRepository extends MainEntityRepository<User> {
    Optional<User> findByGoogleSub(String googleSub);
    Optional<User> findByEmail(String email);

    boolean existsByRole(Role role);

    boolean existsByEmail(String email);
}
