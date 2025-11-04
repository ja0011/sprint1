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

    @Transactional
    public PostFlag flagPost(Long postId, Long userId, String reason) {
        // Check if post exists
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        // Check if user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if post is already flagged
        if (postFlagRepository.existsByPostId(postId)) {
            throw new RuntimeException("Post is already flagged");
        }

        // Create new flag
        PostFlag postFlag = new PostFlag(post, user, reason);
        return postFlagRepository.save(postFlag);
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
        PostFlag flag = postFlagRepository.findById(flagId)
                .orElseThrow(() -> new RuntimeException("Flag not found"));

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new RuntimeException("Reviewer not found"));

        flag.setStatus(status);
        flag.setReviewedAt(Instant.now());
        flag.setReviewedByUser(reviewer);

        return postFlagRepository.save(flag);
    }

    @Transactional
    public void deleteFlag(Long flagId) {
        postFlagRepository.deleteById(flagId);
    }
}