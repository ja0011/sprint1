
package com.example.socialapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.socialapp.model.PostComment;

@Repository
public interface PostCommentRepository extends JpaRepository<PostComment, Long> {
    
    List<PostComment> findByPostIdOrderByCreatedAtDesc(Long postId);
    
    long countByPostId(Long postId);
}
