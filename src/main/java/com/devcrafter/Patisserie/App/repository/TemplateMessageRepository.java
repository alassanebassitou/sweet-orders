package com.devcrafter.Patisserie.App.repository;

import com.devcrafter.Patisserie.App.models.TemplateMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TemplateMessageRepository extends JpaRepository<TemplateMessage, Long> {

    Optional<TemplateMessage> findByType(String type);
    List<TemplateMessage> findAll();
}
