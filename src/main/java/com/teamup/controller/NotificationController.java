package com.teamup.controller;

import com.teamup.dto.ApiResponse;
import com.teamup.model.Notification;
import com.teamup.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<Notification>>> getUserNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        List<Notification> notifications;
        if (unreadOnly) {
            notifications = notificationService.getUnreadByUser(userId);
        } else {
            notifications = notificationService.getAllByUser(userId);
        }
        return ResponseEntity.ok(ApiResponse.ok(notifications));
    }

    @GetMapping("/user/{userId}/unread/count")
    public ResponseEntity<ApiResponse<Long>> countUnread(@PathVariable Long userId) {
        long count = notificationService.countUnread(userId);
        return ResponseEntity.ok(ApiResponse.ok(count));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(ApiResponse.ok("Notification marked as read", null));
    }

    @PatchMapping("/user/{userId}/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@PathVariable Long userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.ok("All notifications marked as read", null));
    }
}
