package com.example.socialapp.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.socialapp.model.Post;
import com.example.socialapp.model.User;
import com.example.socialapp.repository.PostRepository;
import com.example.socialapp.repository.UserRepository;
import com.example.socialapp.service.PostCommentService;
import com.example.socialapp.service.PostDislikeService;
import com.example.socialapp.service.PostLikeService;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = {
    "http://127.0.0.1:5500", "http://localhost:5500",
    "http://127.0.0.1:3000", "http://localhost:3000"
})
public class PostController {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostLikeService postLikeService;
    private final PostDislikeService postDislikeService;
    private final PostCommentService postCommentService;
    private static final String UPLOAD_DIR = "target/classes/static/images/posts/";

    public PostController(PostRepository postRepository, UserRepository userRepository,
                         PostLikeService postLikeService, PostDislikeService postDislikeService,
                         PostCommentService postCommentService) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.postLikeService = postLikeService;
        this.postDislikeService = postDislikeService;
        this.postCommentService = postCommentService;
        
        try {
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                boolean created = uploadDir.mkdirs();
                System.out.println("[PostController] Upload directory created: " + created + " at " + uploadDir.getAbsolutePath());
            } else {
                System.out.println("[PostController] Upload directory exists at: " + uploadDir.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("[PostController] Could not create upload directory: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public record PostResponse(
        Long id,
        Long userId,
        String username,
        String userProfilePicture,
        String content,
        String imageUrl,
        Instant createdAt,
        Instant updatedAt,
        long likeCount,
        long dislikeCount,
        long commentCount,
        boolean userLiked,
        boolean userDisliked
    ) {}

    private PostResponse mapToResponse(Post post, Long currentUserId) {
        return new PostResponse(
            post.getId(),
            post.getUser().getId(),
            post.getUser().getUsername(),
            post.getUser().getProfilePictureUrl(),
            post.getContent(),
            post.getImageUrl(),
            post.getCreatedAt(),
            post.getUpdatedAt(),
            postLikeService.getLikeCount(post.getId()),
            postDislikeService.getDislikeCount(post.getId()),
            postCommentService.getCommentCount(post.getId()),
            currentUserId != null ? postLikeService.hasUserLiked(post.getId(), currentUserId) : false,
            currentUserId != null ? postDislikeService.hasUserDisliked(post.getId(), currentUserId) : false
        );
    }

    @PostMapping
    public ResponseEntity<?> createPost(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        
        System.out.println("[PostController] Create post request - userId: " + userId);
        
        try {
            if ((content == null || content.trim().isEmpty()) && (image == null || image.isEmpty())) {
                return ResponseEntity.badRequest().body("Post must contain either text content or an image");
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String imageUrl = null;
            
            if (image != null && !image.isEmpty()) {
    String originalFilename = image.getOriginalFilename();
    String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
    String newFilename = UUID.randomUUID().toString() + fileExtension;
    
    System.out.println("[PostController] Saving post image: " + newFilename);
    
    Path filePath = Paths.get(UPLOAD_DIR + newFilename);
    System.out.println("[PostController] Full path: " + filePath.toAbsolutePath());
    
    Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
    
    imageUrl = "/images/posts/" + newFilename;  // ← ADD THE LEADING SLASH HERE
    System.out.println("[PostController] Image URL: " + imageUrl);
}

            Post post = new Post(user, content, imageUrl);
            post = postRepository.save(post);

            PostResponse response = mapToResponse(post, userId);

            System.out.println("[PostController] Post created successfully with ID: " + post.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            System.err.println("[PostController] Error creating post: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating post: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<PostResponse>> getAllPosts(
            @RequestParam(value = "userId", required = false) Long userId) {
        List<Post> posts = postRepository.findAllByOrderByCreatedAtDesc();
        List<PostResponse> response = posts.stream()
                .map(post -> mapToResponse(post, userId))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostResponse>> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(value = "currentUserId", required = false) Long currentUserId) {
        List<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<PostResponse> response = posts.stream()
                .map(post -> mapToResponse(post, currentUserId))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{postId}")
    public ResponseEntity<?> updatePost(
            @PathVariable Long postId,
            @RequestParam("userId") Long userId,
            @RequestParam("content") String content) {
        
        try {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("Post not found"));

            if (!post.getUser().getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You can only edit your own posts");
            }

            post.setContent(content);
            post.setUpdatedAt(Instant.now());
            post = postRepository.save(post);

            PostResponse response = mapToResponse(post, userId);

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating post: " + e.getMessage());
        }
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<?> deletePost(
            @PathVariable Long postId,
            @RequestParam("userId") Long userId) {
        
        try {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("Post not found"));

            // Get the current user to check their role
            User currentUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Allow deletion if user is the post owner OR has ADMIN role
            boolean isOwner = post.getUser().getId().equals(userId);
            boolean isAdmin = currentUser.getRole() == User.Role.ADMIN;
            
            if (!isOwner && !isAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You can only delete your own posts unless you are an admin");
            }

            System.out.println("[PostController] Delete authorized - isOwner: " + isOwner + ", isAdmin: " + isAdmin);

            // Delete the image file if it exists
            if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
                try {
                    // Extract filename from URL (e.g., "images/posts/uuid.jpg" -> "uuid.jpg")
                    String filename = post.getImageUrl().replace("images/posts/", "");
                    Path filePath = Paths.get(UPLOAD_DIR + filename);
                    Files.deleteIfExists(filePath);
                    System.out.println("[PostController] Deleted post image: " + filename);
                } catch (IOException e) {
                    System.err.println("[PostController] Could not delete post image: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            postRepository.delete(post);
            System.out.println("[PostController] Post deleted successfully: " + postId);
            return ResponseEntity.ok("Post deleted successfully");
            
        } catch (Exception e) {
            System.err.println("[PostController] Error deleting post: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting post: " + e.getMessage());
        }
    }
}
