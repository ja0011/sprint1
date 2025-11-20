package com.example.socialapp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.socialapp.model.UserFollow;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {
    
    // Check if follower is following followed
    boolean existsByFollowerIdAndFollowedId(Long followerId, Long followedId);
    
    // Find a specific follow relationship
    Optional<UserFollow> findByFollowerIdAndFollowedId(Long followerId, Long followedId);
    
    // Count followers of a user
    long countByFollowedId(Long followedId);
    
    // Count users that a user is following
    long countByFollowerId(Long followerId);
    
    // Delete a follow relationship
    void deleteByFollowerIdAndFollowedId(Long followerId, Long followedId);
    
    // Get all followers of a user (NEW)
    List<UserFollow> findByFollowedId(Long followedId);
}