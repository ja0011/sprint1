package com.example.socialapp.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.socialapp.model.User;
import com.example.socialapp.repository.UserRepository;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

  private final UserRepository userRepository;
  private static final String UPLOAD_DIR = "target/classes/static/images/";
  private static final Logger logger = LoggerFactory.getLogger(UserController.class);

  public UserController(UserRepository userRepository) {
    this.userRepository = userRepository;
    // Create images directory if it doesn't exist
    try {
      File uploadDir = new File(UPLOAD_DIR);
      if (!uploadDir.exists()) {
        boolean created = uploadDir.mkdirs();
        System.out.println("[UserController] Upload directory created: " + created + " at " + uploadDir.getAbsolutePath());
      } else {
        System.out.println("[UserController] Upload directory exists at: " + uploadDir.getAbsolutePath());
      }
    } catch (Exception e) {
      System.err.println("[UserController] Could not create upload directory: " + e.getMessage());
      logger.error("[UserController] Could not create upload directory", e);
    }
  }

  // ===== DTOs =====
  public record UserProfileResponse(
      Long id,
      String username,
      String email,
      String bio,
      Integer graduationYear,
      String major,
      String minor,
      String profilePictureUrl
  ) {}

  public record UpdateProfileRequest(
      String bio,
      Integer graduationYear,
      String major,
      String minor
  ) {}

  public record UserSearchResult(
      Long id,
      String username,
      String profilePictureUrl,
      String role
  ) {}

  // ===== Endpoints =====

  // GET user profile by username
  @GetMapping("/{username}")
  public ResponseEntity<?> getUserProfile(@PathVariable String username) {
    return userRepository.findByUsername(username.toLowerCase())
        .<ResponseEntity<?>>map(u -> ResponseEntity.ok(new UserProfileResponse(
            u.getId(),
            u.getUsername(),
            u.getEmail(),
            u.getBio(),
            u.getGraduationYear(),
            u.getMajor(),
            u.getMinor(),
            u.getProfilePictureUrl()
        )))
        .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "user_not_found")));
  }

  // UPDATE user profile
  @PutMapping("/{username}")
  public ResponseEntity<?> updateUserProfile(
      @PathVariable String username,
      @Valid @RequestBody UpdateProfileRequest request) {
    
    User user = userRepository.findByUsername(username.toLowerCase())
        .orElseThrow(() -> new RuntimeException("User not found"));

    // Update profile fields
    user.setBio(request.bio());
    user.setGraduationYear(request.graduationYear());
    user.setMajor(request.major());
    user.setMinor(request.minor());

    userRepository.save(user);

    return ResponseEntity.ok(new UserProfileResponse(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getBio(),
        user.getGraduationYear(),
        user.getMajor(),
        user.getMinor(),
        user.getProfilePictureUrl()
    ));
  }

  // UPLOAD profile picture
  @PostMapping("/{username}/profile-picture")
  public ResponseEntity<?> uploadProfilePicture(
      @PathVariable String username,
      @RequestParam("file") MultipartFile file) {
    
    System.out.println("[UserController] Upload request received for user: " + username);
    System.out.println("[UserController] File: " + file.getOriginalFilename() + ", Size: " + file.getSize() + ", Type: " + file.getContentType());
    
    if (file.isEmpty()) {
      System.err.println("[UserController] File is empty");
      return ResponseEntity.badRequest().body(Map.of("error", "no_file_uploaded"));
    }

    // Validate file type
    String contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      System.err.println("[UserController] Invalid file type: " + contentType);
      return ResponseEntity.badRequest().body(Map.of("error", "invalid_file_type"));
    }

    try {
      User user = userRepository.findByUsername(username.toLowerCase())
          .orElseThrow(() -> new RuntimeException("User not found"));

      System.out.println("[UserController] User found: " + user.getUsername());

      // Keep original filename
      String originalFilename = file.getOriginalFilename();
      if (originalFilename == null || originalFilename.isEmpty()) {
        System.err.println("[UserController] Invalid filename");
        return ResponseEntity.badRequest().body(Map.of("error", "invalid_filename"));
      }

      System.out.println("[UserController] Saving file: " + originalFilename);

      // Save file to images directory with original name (will overwrite if exists)
      Path filePath = Paths.get(UPLOAD_DIR + originalFilename);
      System.out.println("[UserController] Full path: " + filePath.toAbsolutePath());
      
      Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
      System.out.println("[UserController] File saved successfully");

      // Update user's profile picture URL
      String imageUrl = "images/" + originalFilename;
      user.setProfilePictureUrl(imageUrl);
      userRepository.save(user);
      
      System.out.println("[UserController] User profile updated with image URL: " + imageUrl);

      return ResponseEntity.ok(Map.of(
          "message", "Profile picture uploaded successfully",
          "profilePictureUrl", imageUrl
      ));

    } catch (IOException e) {
      System.err.println("[UserController] File I/O error during upload: " + e.getMessage());
      logger.error("File I/O error during upload", e);
      return ResponseEntity.status(500).body(Map.of("error", "upload_failed", "details", e.getMessage()));
    } catch (RuntimeException e) {
      System.err.println("[UserController] User not found: " + e.getMessage());
      logger.error("User not found during upload", e);
      return ResponseEntity.status(404).body(Map.of("error", "user_not_found", "details", e.getMessage()));
    }
  }

  // SEARCH users with role-based filtering
  @GetMapping("/search")
  public ResponseEntity<?> searchUsers(
      @RequestParam String query,
      @RequestParam(required = false) Long currentUserId) {
    
    System.out.println("[UserController] Search request - query: " + query + ", currentUserId: " + currentUserId);
    
    if (query == null || query.trim().isEmpty()) {
      return ResponseEntity.ok(List.of());
    }

    try {
      // Get all matching users
      List<User> allMatches = userRepository.searchByUsername(query.trim());
      
      // Apply role-based filtering
      List<User> filteredUsers = allMatches;
      
      if (currentUserId != null) {
        // Get the current user's role
        User currentUser = userRepository.findById(currentUserId).orElse(null);
        
        if (currentUser != null) {
          // If USER, filter out ADMIN accounts
          if (currentUser.getRole() == User.Role.USER) {
            filteredUsers = allMatches.stream()
                .filter(u -> u.getRole() != User.Role.ADMIN)
                .collect(Collectors.toList());
            System.out.println("[UserController] USER role - filtered out ADMIN accounts");
          }
          // If ADMIN, show all users (no filtering needed)
          else if (currentUser.getRole() == User.Role.ADMIN) {
            System.out.println("[UserController] ADMIN role - showing all users");
          }
        }
      }
      
      // Convert to search result DTOs
      List<UserSearchResult> results = filteredUsers.stream()
          .map(u -> new UserSearchResult(
              u.getId(),
              u.getUsername(),
              u.getProfilePictureUrl(),
              u.getRole().toString()
          ))
          .limit(10) // Limit to 10 results
          .collect(Collectors.toList());
      
      System.out.println("[UserController] Returning " + results.size() + " search results");
      return ResponseEntity.ok(results);
      
    } catch (Exception e) {
      System.err.println("[UserController] Error during search: " + e.getMessage());
      logger.error("Error during user search", e);
      return ResponseEntity.status(500).body(Map.of("error", "search_failed"));
    }
  }
}