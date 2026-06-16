package com.devcrafter.Patisserie.App.repository;

import com.devcrafter.Patisserie.App.models.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationRepositories extends JpaRepository<Notification, Long> {
    List<Notification> findByIsReadFalseOrderByCreatedAtDesc();

    List<Notification> findAllByOrderByCreatedAtDesc();

    long countByIsReadFalse();

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true")
    void markAllAsRead();
}
