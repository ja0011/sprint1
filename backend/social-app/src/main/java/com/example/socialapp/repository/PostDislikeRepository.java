
package com.example.socialapp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.socialapp.model.PostDislike;

@Repository
public interface PostDislikeRepository extends JpaRepository<PostDislike, Long> {
    
    Optional<PostDislike> findByPostIdAndUserId(Long postId, Long userId);
    
    long countByPostId(Long postId);
    
    void deleteByPostIdAndUserId(Long postId, Long userId);
    
    boolean existsByPostIdAndUserId(Long postId, Long userId);
}
