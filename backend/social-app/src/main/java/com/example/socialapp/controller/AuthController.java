package com.example.socialapp.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.socialapp.model.User;
import com.example.socialapp.repository.UserRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Validated
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {
  "http://127.0.0.1:5500", "http://localhost:5500",
  "http://127.0.0.1:3000", "http://localhost:3000"
})
public class AuthController {

  private final UserRepository users;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  public AuthController(UserRepository users) {
    this.users = users;
  }

  // ===== DTOs =====
  public record RegisterReq(
      @NotBlank String username,
      @NotBlank @Email String email,
      @NotBlank String password
  ) {}
  
  public record LoginReq(
      @NotBlank String username,
      @NotBlank String password
  ) {}

  public record ResetReq(
      @NotBlank @Email String email,
      @NotBlank String newPassword
  ) {}

  // ===== Endpoints =====

  @PostMapping("/register")
  public ResponseEntity<?> register(@Valid @RequestBody RegisterReq body) {
    final String username = body.username().trim().toLowerCase();
    final String email = body.email().trim().toLowerCase();
    final String raw = body.password();

    if (username.length() < 3) {
      return ResponseEntity.badRequest().body(Map.of("error", "username_too_short"));
    }
    if (users.existsByUsername(username)) {
      return ResponseEntity.badRequest().body(Map.of("error", "username_taken"));
    }
    if (users.existsByEmail(email)) {
      return ResponseEntity.badRequest().body(Map.of("error", "email_taken"));
    }

    User u = new User();
    u.setUsername(username);
    u.setEmail(email);
    u.setPasswordHash(encoder.encode(raw));
    u.setActive(true); // New users are active by default
    // Role defaults to USER in the entity
    users.save(u);

    return ResponseEntity.ok(Map.of(
        "id", u.getId(), 
        "username", u.getUsername(), 
        "email", u.getEmail(),
        "role", u.getRole().name()
    ));
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@Valid @RequestBody LoginReq body){
    final String username = body.username().trim().toLowerCase();
    
    // First, check if user exists and password matches
    var userOpt = users.findByUsername(username);
    if (userOpt.isEmpty()) {
      return ResponseEntity.status(401).body(Map.of("error", "invalid_credentials"));
    }
    
    User user = userOpt.get();
    
    // Check password
    if (!encoder.matches(body.password(), user.getPasswordHash())) {
      return ResponseEntity.status(401).body(Map.of("error", "invalid_credentials"));
    }
    
    // Check if account is active
    if (!user.getActive()) {
      return ResponseEntity.status(403).body(Map.of(
          "error", "account_suspended", 
          "message", "Your account has been suspended. Please contact an administrator."
      ));
    }
    
    // All checks passed - return success
    return ResponseEntity.ok(Map.of(
        "id", user.getId(),
        "username", user.getUsername(),
        "email", user.getEmail(),
        "role", user.getRole().name()
    ));
  }

  @PostMapping("/reset")
  public ResponseEntity<?> reset(@Valid @RequestBody ResetReq body) {
    final String email = body.email().trim().toLowerCase();
    return users.findByEmail(email)
      .<ResponseEntity<?>>map(u -> {
        u.setPasswordHash(encoder.encode(body.newPassword()));
        users.save(u);
        return ResponseEntity.ok(Map.of("message", "password_reset_success"));
      })
      .orElseGet(() -> ResponseEntity.status(404)
          .body(Map.of("error", "email_not_found")));
  }
}