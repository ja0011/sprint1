
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

@Service
public class PostCommentService {

    private final PostCommentRepository postCommentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PostCommentService(PostCommentRepository postCommentRepository,
                             PostRepository postRepository,
                             UserRepository userRepository) {
        this.postCommentRepository = postCommentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
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
