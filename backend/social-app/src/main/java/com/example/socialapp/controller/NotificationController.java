package com.example.socialapp.controller;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.socialapp.model.Notification;
import com.example.socialapp.model.Post;
import com.example.socialapp.model.User;
import com.example.socialapp.repository.PostRepository;
import com.example.socialapp.repository.UserRepository;
import com.example.socialapp.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "http://127.0.0.1:5500", allowCredentials = "true")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository, PostRepository postRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
    }

    // DTO for notification response
    public record NotificationResponse(
        Long id,
        String type,
        Long actorId,
        String actorUsername,
        String actorProfilePicture,
        Long postId,
        String commentText,
        String postImageUrl,
        String timeAgo,
        Boolean isRead
    ) {}

    // Get all notifications for a user
    @GetMapping
    public ResponseEntity<?> getUserNotifications(@RequestParam Long userId) {
        try {
            List<Notification> notifications = notificationService.getUserNotifications(userId);

            List<NotificationResponse> response = notifications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch notifications"));
        }
    }

    // Get unread notifications
    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadNotifications(@RequestParam Long userId) {
        try {
            List<Notification> notifications = notificationService.getUnreadNotifications(userId);

            List<NotificationResponse> response = notifications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch unread notifications"));
        }
    }

    // Get unread count
    @GetMapping("/unread/count")
    public ResponseEntity<?> getUnreadCount(@RequestParam Long userId) {
        try {
            long count = notificationService.getUnreadCount(userId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch unread count"));
        }
    }

    // Mark notification as read
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long notificationId) {
        try {
            boolean success = notificationService.markAsRead(notificationId);

            if (success) {
                return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Notification not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to mark notification as read"));
        }
    }

    // Mark all notifications as read
    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(@RequestParam Long userId) {
        try {
            notificationService.markAllAsRead(userId);
            return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to mark all notifications as read"));
        }
    }

    // Delete a notification
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long notificationId) {
        try {
            boolean success = notificationService.deleteNotification(notificationId);

            if (success) {
                return ResponseEntity.ok(Map.of("message", "Notification deleted"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Notification not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete notification"));
        }
    }

    // Helper method to map Notification to NotificationResponse
    private NotificationResponse mapToResponse(Notification notification) {
        String actorProfilePicture = "images/default profile picture.jpg";

        if (notification.getActorId() != null) {
            Optional<User> actor = userRepository.findById(notification.getActorId());
            if (actor.isPresent() && actor.get().getProfilePictureUrl() != null) {
                actorProfilePicture = actor.get().getProfilePictureUrl();
            }
        }

        // Get post image URL if notification has a postId
        String postImageUrl = null;
        if (notification.getPostId() != null) {
            Optional<Post> post = postRepository.findById(notification.getPostId());
            if (post.isPresent() && post.get().getImageUrl() != null) {
                postImageUrl = post.get().getImageUrl();
            }
        }

        return new NotificationResponse(
            notification.getId(),
            notification.getType(),
            notification.getActorId(),
            notification.getActorUsername(),
            actorProfilePicture,
            notification.getPostId(),
            notification.getCommentText(),
            postImageUrl,
            getTimeAgo(notification.getCreatedAt()),
            notification.getIsRead()
        );
    }

    // Helper method to calculate time ago
    private String getTimeAgo(LocalDateTime createdAt) {
        Duration duration = Duration.between(createdAt, LocalDateTime.now());

        long seconds = duration.getSeconds();

        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m";
        } else if (seconds < 86400) {
            return (seconds / 3600) + "h";
        } else if (seconds < 604800) {
            return (seconds / 86400) + "d";
        } else {
            return (seconds / 604800) + "w";
        }
    }
}
