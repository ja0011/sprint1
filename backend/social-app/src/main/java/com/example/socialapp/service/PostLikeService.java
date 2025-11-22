package com.example.socialapp.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.socialapp.model.Notification;
import com.example.socialapp.model.Post;
import com.example.socialapp.model.PostLike;
import com.example.socialapp.model.User;
import com.example.socialapp.repository.PostDislikeRepository;
import com.example.socialapp.repository.PostLikeRepository;
import com.example.socialapp.repository.PostRepository;
import com.example.socialapp.repository.UserRepository;

@Service
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostDislikeRepository postDislikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    public PostLikeService(PostLikeRepository postLikeRepository, 
                          PostDislikeRepository postDislikeRepository,
                          PostRepository postRepository, 
                          UserRepository userRepository,
                          NotificationService notificationService,
                          SimpMessagingTemplate messagingTemplate) {
        this.postLikeRepository = postLikeRepository;
        this.postDislikeRepository = postDislikeRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public void toggleLike(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user already liked the post
        if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            // Unlike: remove the like
            postLikeRepository.deleteByPostIdAndUserId(postId, userId);
        } else {
            // Like: first remove any dislike if exists
            if (postDislikeRepository.existsByPostIdAndUserId(postId, userId)) {
                postDislikeRepository.deleteByPostIdAndUserId(postId, userId);
            }
            // Add the like
            PostLike like = new PostLike(post, user);
            postLikeRepository.save(like);
            
            // Create notification if the liker is not the post owner
            if (!post.getUser().getId().equals(userId)) {
                Notification notification = notificationService.createLikeNotification(
                    post.getUser().getId(), 
                    userId, 
                    postId
                );
                
                // Send real-time notification via WebSocket
                if (notification != null) {
                    messagingTemplate.convertAndSend(
                        "/topic/notifications/" + post.getUser().getId(),
                        notification
                    );
                }
            }
        }
    }

    public long getLikeCount(Long postId) {
        return postLikeRepository.countByPostId(postId);
    }

    public boolean hasUserLiked(Long postId, Long userId) {
        return postLikeRepository.existsByPostIdAndUserId(postId, userId);
    }
}