package com.example.socialapp.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.socialapp.model.User;
import com.example.socialapp.model.UserFollow;
import com.example.socialapp.repository.UserFollowRepository;
import com.example.socialapp.repository.UserRepository;

@Service
public class UserFollowService {
    
    @Autowired
    private UserFollowRepository userFollowRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    // Follow a user
    @Transactional
    public boolean followUser(Long followerId, Long followedId) {
        // Prevent self-following
        if (followerId.equals(followedId)) {
            return false;
        }
        
        // Check if already following
        if (userFollowRepository.existsByFollowerIdAndFollowedId(followerId, followedId)) {
            return false;
        }
        
        UserFollow userFollow = new UserFollow(followerId, followedId);
        userFollowRepository.save(userFollow);
        return true;
    }
    
    // Unfollow a user
    @Transactional
    public boolean unfollowUser(Long followerId, Long followedId) {
        if (userFollowRepository.existsByFollowerIdAndFollowedId(followerId, followedId)) {
            userFollowRepository.deleteByFollowerIdAndFollowedId(followerId, followedId);
            return true;
        }
        return false;
    }
    
    // Check if a user is following another user
    public boolean isFollowing(Long followerId, Long followedId) {
        return userFollowRepository.existsByFollowerIdAndFollowedId(followerId, followedId);
    }
    
    // Get follower count
    public long getFollowerCount(Long userId) {
        return userFollowRepository.countByFollowedId(userId);
    }
    
    // Get following count
    public long getFollowingCount(Long userId) {
        return userFollowRepository.countByFollowerId(userId);
    }

    // Get list of users who follow the given user
    public List<User> getFollowers(Long userId) {
        List<UserFollow> follows = userFollowRepository.findByFollowedId(userId);
        return follows.stream()
                .map(follow -> userRepository.findById(follow.getFollowerId()).orElse(null))
                .filter(user -> user != null)
                .collect(Collectors.toList());
    }
}