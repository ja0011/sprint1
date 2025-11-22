package com.example.socialapp.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.socialapp.model.Post;
import com.example.socialapp.model.PostFlag;
import com.example.socialapp.model.User;
import com.example.socialapp.repository.PostFlagRepository;
import com.example.socialapp.repository.PostRepository;
import com.example.socialapp.repository.UserRepository;

@Service
public class PostFlagService {

    @Autowired
    private PostFlagRepository postFlagRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Transactional
    public PostFlag flagPost(Long postId, Long userId, String reason) {
    // Check if post exists
    Post post = postRepository.findById(postId)
    .orElseThrow(() -> new RuntimeException("Post not found"));

    // Check if user exists
    User user = userRepository.findById(userId)
    .orElseThrow(() -> new RuntimeException("User not found"));

    // REMOVED: Check if post is already flagged - allow multiple flags
    // Multiple users can flag the same post, or same user can flag with different reason

    // Create new flag
    PostFlag postFlag = new PostFlag(post, user, reason);
    PostFlag savedFlag = postFlagRepository.save(postFlag);

    // Create notification for post owner
    notificationService.createFlaggedNotification(post.getUser().getId(), postId);

    return savedFlag;
    }

    public List<PostFlag> getAllFlags() {
    return postFlagRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<PostFlag> getFlagsByStatus(String status) {
    return postFlagRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public Optional<PostFlag> getFlagByPostId(Long postId) {
    return postFlagRepository.findByPostId(postId);
    }

    public boolean isPostFlagged(Long postId) {
    return postFlagRepository.existsByPostId(postId);
    }

    @Transactional
    public PostFlag updateFlagStatus(Long flagId, String status, Long reviewerId) {
    System.out.println("====");
    System.out.println("UPDATE FLAG STATUS IN SERVICE");
    System.out.println("FlagId: " + flagId);
    System.out.println("Status: " + status);
    System.out.println("ReviewerId: " + reviewerId);
    System.out.println("====");
    
    PostFlag flag = postFlagRepository.findById(flagId)
    .orElseThrow(() -> new RuntimeException("Flag not found"));

    User reviewer = userRepository.findById(reviewerId)
    .orElseThrow(() -> new RuntimeException("Reviewer not found"));

    flag.setStatus(status);
    flag.setReviewedAt(Instant.now());
    flag.setReviewedByUser(reviewer);

    PostFlag updatedFlag = postFlagRepository.save(flag);

    // Create notification for post owner based on flag status
    Long postOwnerId = flag.getPost().getUser().getId();
    Long postId = flag.getPost().getId();

    System.out.println("Creating notification for post owner: " + postOwnerId);
    System.out.println("Post ID: " + postId);
    System.out.println("Status: " + status);

    if ("APPROVED".equals(status)) {
    System.out.println("Calling createFlagApprovedNotification");
    notificationService.createFlagApprovedNotification(postOwnerId, postId);
    } else if ("REJECTED".equals(status)) {
    System.out.println("Calling createFlagRejectedNotification");
    notificationService.createFlagRejectedNotification(postOwnerId, postId);
    }

    System.out.println("Notification creation completed");

    return updatedFlag;
    }

    @Transactional
    public void deleteFlag(Long flagId) {
    postFlagRepository.deleteById(flagId);
    }
}