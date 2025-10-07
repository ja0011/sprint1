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
    // Role defaults to USER in the entity
    users.save(u);

    // UPDATED: Include role in response
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
    return users.findByUsername(username)
      .filter(u -> encoder.matches(body.password(), u.getPasswordHash()))
      // UPDATED: Include role in response
      .<ResponseEntity<?>>map(u -> ResponseEntity.ok(Map.of(
          "id", u.getId(), 
          "username", u.getUsername(), 
          "email", u.getEmail(),
          "role", u.getRole().name()
      )))
      .orElseGet(() -> ResponseEntity.status(401).body(Map.of("error","invalid_credentials")));
  }

  @PostMapping("/logout")
  public ResponseEntity<?> logout() {
    // No server session yet; once you add sessions/JWT, revoke here.
    return ResponseEntity.ok().build();
  }

  @PostMapping("/reset-password")
  public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetReq body) {
    final String email = body.email().trim().toLowerCase();
    final String newPassword = body.newPassword();

    if (newPassword.length() < 6) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "weak_password", "hint", "Use at least 6 characters"));
    }

    java.util.Optional<User> optional = users.findByEmail(email);
    if (optional.isPresent()) {
      User u = optional.get();
      u.setPasswordHash(encoder.encode(newPassword));
      users.save(u);
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.badRequest().body(Map.of("error", "email_not_found"));
    }
  }
}