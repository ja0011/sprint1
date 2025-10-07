package com.example.socialapp.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.socialapp.model.User;
import com.example.socialapp.model.User.Role;
import com.example.socialapp.repository.UserRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Validated
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {
  "http://127.0.0.1:5500", "http://localhost:5500",
  "http://127.0.0.1:3000", "http://localhost:3000"
})
public class AdminController {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public AdminController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  // ===== DTOs =====
  public record UserResponse(
      Long id,
      String username,
      String email,
      String role,
      String createdAt
  ) {}

  public record CreateUserRequest(
      @NotBlank String username,
      @NotBlank @Email String email,
      @NotBlank String password,
      String role  // "USER" or "ADMIN", defaults to USER
  ) {}

  public record UpdateRoleRequest(
      @NotBlank String role  // "USER" or "ADMIN"
  ) {}

  // ===== Endpoints =====

  // GET all users
  @GetMapping("/users")
  public ResponseEntity<?> getAllUsers() {
    List<UserResponse> users = userRepository.findAll().stream()
        .map(u -> new UserResponse(
            u.getId(),
            u.getUsername(),
            u.getEmail(),
            u.getRole().name(),
            u.getCreatedAt().toString()
        ))
        .collect(Collectors.toList());
    
    return ResponseEntity.ok(users);
  }

  // DELETE a user by ID
  @DeleteMapping("/users/{id}")
  public ResponseEntity<?> deleteUser(@PathVariable Long id) {
    if (!userRepository.existsById(id)) {
      return ResponseEntity.status(404).body(Map.of("error", "user_not_found"));
    }
    
    userRepository.deleteById(id);
    return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
  }

  // CREATE a new user (with optional role)
  @PostMapping("/users")
  public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
    String username = request.username().trim().toLowerCase();
    String email = request.email().trim().toLowerCase();
    
    if (userRepository.existsByUsername(username)) {
      return ResponseEntity.badRequest().body(Map.of("error", "username_taken"));
    }
    
    if (userRepository.existsByEmail(email)) {
      return ResponseEntity.badRequest().body(Map.of("error", "email_taken"));
    }
    
    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    
    // Set role (default to USER if not specified or invalid)
    try {
      if (request.role() != null && !request.role().isBlank()) {
        user.setRole(Role.valueOf(request.role().toUpperCase()));
      }
    } catch (IllegalArgumentException e) {
      // Invalid role, keep default USER
    }
    
    userRepository.save(user);
    
    return ResponseEntity.ok(new UserResponse(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getRole().name(),
        user.getCreatedAt().toString()
    ));
  }

  // UPDATE user role
  @PatchMapping("/users/{id}/role")
  public ResponseEntity<?> updateUserRole(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("User not found"));
    
    try {
      user.setRole(Role.valueOf(request.role().toUpperCase()));
      userRepository.save(user);
      
      return ResponseEntity.ok(new UserResponse(
          user.getId(),
          user.getUsername(),
          user.getEmail(),
          user.getRole().name(),
          user.getCreatedAt().toString()
      ));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("error", "invalid_role"));
    }
  }
}