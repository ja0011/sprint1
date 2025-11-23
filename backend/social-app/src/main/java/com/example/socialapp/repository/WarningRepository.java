package com.example.socialapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.socialapp.model.Warning;

@Repository
public interface WarningRepository extends JpaRepository<Warning, Long> {
    
    // Find all warnings for a specific user
    List<Warning> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    // Find all warnings sent by a specific admin
    List<Warning> findByAdminIdOrderByCreatedAtDesc(Long adminId);
    
    // Count warnings for a user
    long countByUserId(Long userId);
}