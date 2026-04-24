package com.zdravdom.notification.adapters.inbound.rest;

import com.zdravdom.auth.adapters.inbound.security.JwtAuthenticationFilter.JwtAuthenticatedPrincipal;
import com.zdravdom.notification.application.dto.*;
import com.zdravdom.notification.application.service.NotificationService;
import com.zdravdom.notification.domain.Platform;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for push notification management.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Push notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/push-token")
    @Operation(summary = "Register device push token")
    public ResponseEntity<MessageResponse> registerPushToken(
            @AuthenticationPrincipal JwtAuthenticatedPrincipal principal,
            @Valid @RequestBody RegisterPushTokenRequest request) {
        notificationService.registerPushToken(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponse("Token registered"));
    }

    @GetMapping
    @Operation(summary = "List notifications for current user")
    public ResponseEntity<NotificationListResponse> getNotifications(
            @AuthenticationPrincipal JwtAuthenticatedPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        NotificationListResponse response = notificationService.getNotifications(
            principal.userId(), page, size);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtAuthenticatedPrincipal principal) {
        NotificationResponse response = notificationService.markAsRead(id, principal.userId());
        return ResponseEntity.ok(response);
    }

    public record MessageResponse(String message) {}
}