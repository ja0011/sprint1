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

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = {
    "http://127.0.0.1:5500", "http://localhost:5500",
    "http://127.0.0.1:3000", "http://localhost:3000"
})
public class PostController {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private static final String UPLOAD_DIR = "target/classes/static/images/posts/";

    public PostController(PostRepository postRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        
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
        Instant updatedAt
    ) {}

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

            PostResponse response = new PostResponse(
                post.getId(),
                user.getId(),
                user.getUsername(),
                user.getProfilePictureUrl(),
                post.getContent(),
                post.getImageUrl(),
                post.getCreatedAt(),
                post.getUpdatedAt()
            );

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
    public ResponseEntity<List<PostResponse>> getAllPosts() {
        List<Post> posts = postRepository.findAllByOrderByCreatedAtDesc();
        List<PostResponse> response = posts.stream()
                .map(post -> new PostResponse(
                    post.getId(),
                    post.getUser().getId(),
                    post.getUser().getUsername(),
                    post.getUser().getProfilePictureUrl(),
                    post.getContent(),
                    post.getImageUrl(),
                    post.getCreatedAt(),
                    post.getUpdatedAt()
                ))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostResponse>> getUserPosts(@PathVariable Long userId) {
        List<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<PostResponse> response = posts.stream()
                .map(post -> new PostResponse(
                    post.getId(),
                    post.getUser().getId(),
                    post.getUser().getUsername(),
                    post.getUser().getProfilePictureUrl(),
                    post.getContent(),
                    post.getImageUrl(),
                    post.getCreatedAt(),
                    post.getUpdatedAt()
                ))
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

            PostResponse response = new PostResponse(
                post.getId(),
                post.getUser().getId(),
                post.getUser().getUsername(),
                post.getUser().getProfilePictureUrl(),
                post.getContent(),
                post.getImageUrl(),
                post.getCreatedAt(),
                post.getUpdatedAt()
            );

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

            if (!post.getUser().getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You can only delete your own posts");
            }

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