package com.example.socialapp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.socialapp.model.PostFlag;

@Repository
public interface PostFlagRepository extends JpaRepository<PostFlag, Long> {
    
    Optional<PostFlag> findByPostId(Long postId);
    
    boolean existsByPostId(Long postId);
    
    List<PostFlag> findByStatus(String status);
    
    List<PostFlag> findAllByOrderByCreatedAtDesc();
    
    @Query("SELECT pf FROM PostFlag pf WHERE pf.status = :status ORDER BY pf.createdAt DESC")
    List<PostFlag> findByStatusOrderByCreatedAtDesc(@Param("status") String status);
}