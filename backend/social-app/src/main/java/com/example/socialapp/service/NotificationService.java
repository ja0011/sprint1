package com.example.socialapp.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.socialapp.model.Notification;
import com.example.socialapp.model.User;
import com.example.socialapp.repository.NotificationRepository;
import com.example.socialapp.repository.UserRepository;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
    this.notificationRepository = notificationRepository;
    this.userRepository = userRepository;
    }

    // Create a follow notification
    @Transactional
    public Notification createFollowNotification(Long followedUserId, Long followerUserId) {
    Optional<User> follower = userRepository.findById(followerUserId);
    
    if (follower.isPresent()) {
    Notification notification = new Notification();
    notification.setUserId(followedUserId);
    notification.setType("FOLLOW");
    notification.setActorId(followerUserId);
    notification.setActorUsername(follower.get().getUsername());
    
    return notificationRepository.save(notification);
    }
    
    return null;
    }

    // Create a like notification
    @Transactional
    public Notification createLikeNotification(Long postOwnerId, Long likerUserId, Long postId) {
    Optional<User> liker = userRepository.findById(likerUserId);
    
    if (liker.isPresent()) {
    Notification notification = new Notification();
    notification.setUserId(postOwnerId);
    notification.setType("LIKE");
    notification.setActorId(likerUserId);
    notification.setActorUsername(liker.get().getUsername());
    notification.setPostId(postId);
    
    return notificationRepository.save(notification);
    }
    
    return null;
    }

    // Create a flagged post notification
    @Transactional
    public Notification createFlaggedNotification(Long postOwnerId, Long postId) {
    Notification notification = new Notification();
    notification.setUserId(postOwnerId);
    notification.setType("FLAG_CREATED");
    notification.setActorId(null); // System notification
    notification.setActorUsername("Moderation");
    notification.setPostId(postId);
    
    return notificationRepository.save(notification);
    }

    // Create a flag approved notification
    @Transactional
    public Notification createFlagApprovedNotification(Long postOwnerId, Long postId) {
    Notification notification = new Notification();
    notification.setUserId(postOwnerId);
    notification.setType("FLAG_APPROVED");
    notification.setActorId(null); // System notification
    notification.setActorUsername("Admin");
    notification.setPostId(postId);
    
    return notificationRepository.save(notification);
    }

    // Create a flag rejected notification
    @Transactional
    public Notification createFlagRejectedNotification(Long postOwnerId, Long postId) {
    Notification notification = new Notification();
    notification.setUserId(postOwnerId);
    notification.setType("FLAG_REJECTED");
    notification.setActorId(null); // System notification
    notification.setActorUsername("Admin");
    notification.setPostId(postId);
    
    return notificationRepository.save(notification);
    }

    // Get all notifications for a user
    public List<Notification> getUserNotifications(Long userId) {
    return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // Get unread notifications for a user
    public List<Notification> getUnreadNotifications(Long userId) {
    return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    // Mark notification as read
    @Transactional
    public boolean markAsRead(Long notificationId) {
    Optional<Notification> notification = notificationRepository.findById(notificationId);
    
    if (notification.isPresent()) {
    notification.get().setIsRead(true);
    notificationRepository.save(notification.get());
    return true;
    }
    
    return false;
    }

    // Mark all notifications as read for a user
    @Transactional
    public void markAllAsRead(Long userId) {
    List<Notification> notifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    
    for (Notification notification : notifications) {
    notification.setIsRead(true);
    notificationRepository.save(notification);
    }
    }

    // Get unread count
    public long getUnreadCount(Long userId) {
    return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    // Delete a notification
    @Transactional
    public boolean deleteNotification(Long notificationId) {
    if (notificationRepository.existsById(notificationId)) {
    notificationRepository.deleteById(notificationId);
    return true;
    }
    return false;
    }
}