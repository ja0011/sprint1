
package com.example.socialapp.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.socialapp.service.PostCommentService;
import com.example.socialapp.service.PostDislikeService;
import com.example.socialapp.service.PostLikeService;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = {
    "http://127.0.0.1:5500", "http://localhost:5500",
    "http://127.0.0.1:3000", "http://localhost:3000"
})
public class PostInteractionController {

    private final PostLikeService postLikeService;
    private final PostDislikeService postDislikeService;
    private final PostCommentService postCommentService;

    public PostInteractionController(PostLikeService postLikeService,
                                     PostDislikeService postDislikeService,
                                     PostCommentService postCommentService) {
        this.postLikeService = postLikeService;
        this.postDislikeService = postDislikeService;
        this.postCommentService = postCommentService;
    }

    // ========== LIKE ENDPOINTS ==========

    @PostMapping("/{postId}/like")
    public ResponseEntity<?> toggleLike(
            @PathVariable Long postId,
            @RequestParam("userId") Long userId) {
        try {
            postLikeService.toggleLike(postId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("likeCount", postLikeService.getLikeCount(postId));
            response.put("dislikeCount", postDislikeService.getDislikeCount(postId));
            response.put("userLiked", postLikeService.hasUserLiked(postId, userId));
            response.put("userDisliked", postDislikeService.hasUserDisliked(postId, userId));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("[PostInteractionController] Error toggling like: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error toggling like: " + e.getMessage());
        }
    }

    // ========== DISLIKE ENDPOINTS ==========

    @PostMapping("/{postId}/dislike")
    public ResponseEntity<?> toggleDislike(
            @PathVariable Long postId,
            @RequestParam("userId") Long userId) {
        try {
            postDislikeService.toggleDislike(postId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("likeCount", postLikeService.getLikeCount(postId));
            response.put("dislikeCount", postDislikeService.getDislikeCount(postId));
            response.put("userLiked", postLikeService.hasUserLiked(postId, userId));
            response.put("userDisliked", postDislikeService.hasUserDisliked(postId, userId));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("[PostInteractionController] Error toggling dislike: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error toggling dislike: " + e.getMessage());
        }
    }

    // ========== COMMENT ENDPOINTS ==========

    @PostMapping("/{postId}/comments")
    public ResponseEntity<?> createComment(
            @PathVariable Long postId,
            @RequestParam("userId") Long userId,
            @RequestBody Map<String, String> payload) {
        try {
            String commentText = payload.get("commentText");
            if (commentText == null || commentText.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Comment text cannot be empty");
            }

            PostCommentService.CommentResponse comment = postCommentService.createComment(postId, userId, commentText);
            return ResponseEntity.status(HttpStatus.CREATED).body(comment);
        } catch (Exception e) {
            System.err.println("[PostInteractionController] Error creating comment: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating comment: " + e.getMessage());
        }
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<?> getComments(@PathVariable Long postId) {
        try {
            List<PostCommentService.CommentResponse> comments = postCommentService.getCommentsByPostId(postId);
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            System.err.println("[PostInteractionController] Error fetching comments: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching comments: " + e.getMessage());
        }
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable Long commentId,
            @RequestParam("userId") Long userId) {
        try {
            postCommentService.deleteComment(commentId, userId);
            return ResponseEntity.ok("Comment deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            System.err.println("[PostInteractionController] Error deleting comment: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting comment: " + e.getMessage());
        }
    }

    // ========== STATS ENDPOINT ==========

    @GetMapping("/{postId}/stats")
    public ResponseEntity<?> getPostStats(
            @PathVariable Long postId,
            @RequestParam(value = "userId", required = false) Long userId) {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("likeCount", postLikeService.getLikeCount(postId));
            stats.put("dislikeCount", postDislikeService.getDislikeCount(postId));
            stats.put("commentCount", postCommentService.getCommentCount(postId));
            
            if (userId != null) {
                stats.put("userLiked", postLikeService.hasUserLiked(postId, userId));
                stats.put("userDisliked", postDislikeService.hasUserDisliked(postId, userId));
            }
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.err.println("[PostInteractionController] Error fetching post stats: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching post stats: " + e.getMessage());
        }
    }
}
