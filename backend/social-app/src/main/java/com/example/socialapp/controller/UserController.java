package com.example.socialapp.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

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
@CrossOrigin(origins = {
  "http://127.0.0.1:5500", "http://localhost:5500",
  "http://127.0.0.1:3000", "http://localhost:3000"
})
public class UserController {

  private final UserRepository userRepository;
  private static final String UPLOAD_DIR = "C:/Users/ethan/.sprint1/sprint1-ethan/sprint1/images/";

  public UserController(UserRepository userRepository) {
    this.userRepository = userRepository;
    // Create images directory if it doesn't exist
    try {
      Files.createDirectories(Paths.get(UPLOAD_DIR));
    } catch (IOException e) {
      throw new RuntimeException("Could not create upload directory", e);
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
    
    if (file.isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("error", "no_file_uploaded"));
    }

    // Validate file type
    String contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      return ResponseEntity.badRequest().body(Map.of("error", "invalid_file_type"));
    }

    try {
      User user = userRepository.findByUsername(username.toLowerCase())
          .orElseThrow(() -> new RuntimeException("User not found"));

      // Generate unique filename
      String originalFilename = file.getOriginalFilename();
      String extension = originalFilename != null && originalFilename.contains(".") 
          ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
          : ".jpg";
      String newFilename = UUID.randomUUID().toString() + extension;

      // Save file to images directory
      Path filePath = Paths.get(UPLOAD_DIR + newFilename);
      Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

      // Update user's profile picture URL
      String imageUrl = "http://localhost:8081/images/" + newFilename;
      user.setProfilePictureUrl(imageUrl);
      userRepository.save(user);

      return ResponseEntity.ok(Map.of(
          "message", "Profile picture uploaded successfully",
          "profilePictureUrl", imageUrl
      ));

    } catch (IOException e) {
      return ResponseEntity.status(500).body(Map.of("error", "upload_failed"));
    }
  }
}