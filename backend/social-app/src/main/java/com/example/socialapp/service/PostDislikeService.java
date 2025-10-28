
package com.example.socialapp.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.socialapp.model.Post;
import com.example.socialapp.model.PostDislike;
import com.example.socialapp.model.PostLike;
import com.example.socialapp.model.User;
import com.example.socialapp.repository.PostDislikeRepository;
import com.example.socialapp.repository.PostLikeRepository;
import com.example.socialapp.repository.PostRepository;
import com.example.socialapp.repository.UserRepository;

@Service
public class PostDislikeService {

    private final PostDislikeRepository postDislikeRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PostDislikeService(PostDislikeRepository postDislikeRepository,
                             PostLikeRepository postLikeRepository,
                             PostRepository postRepository,
                             UserRepository userRepository) {
        this.postDislikeRepository = postDislikeRepository;
        this.postLikeRepository = postLikeRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void toggleDislike(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user already disliked the post
        if (postDislikeRepository.existsByPostIdAndUserId(postId, userId)) {
            // Remove dislike
            postDislikeRepository.deleteByPostIdAndUserId(postId, userId);
        } else {
            // Dislike: first remove any like if exists
            if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
                postLikeRepository.deleteByPostIdAndUserId(postId, userId);
            }
            // Add the dislike
            PostDislike dislike = new PostDislike(post, user);
            postDislikeRepository.save(dislike);
        }
    }

    public long getDislikeCount(Long postId) {
        return postDislikeRepository.countByPostId(postId);
    }

    public boolean hasUserDisliked(Long postId, Long userId) {
        return postDislikeRepository.existsByPostIdAndUserId(postId, userId);
    }
}
