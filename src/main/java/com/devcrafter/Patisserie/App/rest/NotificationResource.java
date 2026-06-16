package com.devcrafter.Patisserie.App.rest;

import com.devcrafter.Patisserie.App.dto.response.NotificationResponse;
import com.devcrafter.Patisserie.App.services.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@Tag(name = "Notifications", description = "Gestion des notifications de l'utilisateur connecté")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
@SecurityRequirement(name = "BearerAuth")
public class NotificationResource {

    private final NotificationService notificationService;

    @Operation(summary = "Toutes les notifications")
    @ApiResponse(responseCode = "200", description = "Liste complète des notifications")
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getAll() {
        return ResponseEntity.ok(notificationService.getToutesNotifications());
    }

    @Operation(summary = "Notifications non lues")
    @ApiResponse(responseCode = "200", description = "Notifications non lues uniquement")
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnRead() {
        return ResponseEntity.ok(notificationService.getUnRead());
    }

    @Operation(summary = "Nombre de notifications non lues")
    @ApiResponse(responseCode = "200", description = "Compteur — ex: { \"nonLues\": 3 }")
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> count() {
        return ResponseEntity.ok(Map.of("nonLues", notificationService.countNonLues()));
    }

    @Operation(summary = "Marquer une notification comme lue")
    @ApiResponse(responseCode = "200", description = "Notification marquée comme lue")
    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(
            @Parameter(description = "ID de la notification") @PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markRead(id));
    }

    @Operation(summary = "Marquer toutes les notifications comme lues")
    @ApiResponse(responseCode = "204", description = "Toutes les notifications lues")
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead() {
        notificationService.markAllRead();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Supprimer une notification")
    @ApiResponse(responseCode = "204", description = "Notification supprimée")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(
            @Parameter(description = "ID de la notification") @PathVariable Long id) {
        notificationService.remove(id);
        return ResponseEntity.noContent().build();
    }
}
