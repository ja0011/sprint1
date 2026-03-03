
package com.example.socialapp.service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.socialapp.model.Post;
import com.example.socialapp.model.PostComment;
import com.example.socialapp.model.User;
import com.example.socialapp.repository.PostCommentRepository;
import com.example.socialapp.repository.PostRepository;
import com.example.socialapp.repository.UserRepository;
import com.example.socialapp.model.Notification;
import com.example.socialapp.repository.NotificationRepository;

@Service
public class PostCommentService {

    private final PostCommentRepository postCommentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public PostCommentService(PostCommentRepository postCommentRepository,
                             PostRepository postRepository,
                             UserRepository userRepository,
                             NotificationRepository notificationRepository) {
        this.postCommentRepository = postCommentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    public record CommentResponse(
        Long id,
        Long postId,
        Long userId,
        String username,
        String userProfilePicture,
        String commentText,
        Instant createdAt,
        Instant updatedAt
    ) {}

    @Transactional
    public CommentResponse createComment(Long postId, Long userId, String commentText) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        PostComment comment = new PostComment(post, user, commentText);
        comment = postCommentRepository.save(comment);

        // Create notification for post owner (if not commenting on own post)
        if (!post.getUser().getId().equals(userId)) {
            Notification notification = new Notification();
            notification.setUserId(post.getUser().getId());
            notification.setType("COMMENT");
            notification.setActorId(userId);
            notification.setActorUsername(user.getUsername());
            notification.setPostId(postId);
            notification.setCommentText(commentText);
            notification.setIsRead(false);
            notificationRepository.save(notification);

            System.out.println("[PostCommentService] Created COMMENT notification for user " + post.getUser().getId());
        }

        return mapToResponse(comment);
    }

    public List<CommentResponse> getCommentsByPostId(Long postId) {
        List<PostComment> comments = postCommentRepository.findByPostIdOrderByCreatedAtDesc(postId);
        return comments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public long getCommentCount(Long postId) {
        return postCommentRepository.countByPostId(postId);
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        PostComment comment = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        // Get the current user to check their role
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Allow deletion if user is the comment owner OR has ADMIN role
        boolean isOwner = comment.getUser().getId().equals(userId);
        boolean isAdmin = currentUser.getRole() == User.Role.ADMIN;
        
        if (!isOwner && !isAdmin) {
            throw new RuntimeException("You can only delete your own comments unless you are an admin");
        }

        System.out.println("[PostCommentService] Delete authorized - isOwner: " + isOwner + ", isAdmin: " + isAdmin);

        postCommentRepository.delete(comment);
    }

    private CommentResponse mapToResponse(PostComment comment) {
        return new CommentResponse(
            comment.getId(),
            comment.getPost().getId(),
            comment.getUser().getId(),
            comment.getUser().getUsername(),
            comment.getUser().getProfilePictureUrl(),
            comment.getCommentText(),
            comment.getCreatedAt(),
            comment.getUpdatedAt()
        );
    }
}
