package com.example.socialapp.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.socialapp.model.User;
import com.example.socialapp.repository.UserRepository;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

  @PostMapping("/logout")
public ResponseEntity<?> logout() {
  // When you add sessions/JWT, revoke here. For now, no-op is fine.
  return ResponseEntity.ok().build();
}


  private final UserRepository users;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  public AuthController(UserRepository users) {
    this.users = users;
  }

  public record RegisterReq(@NotBlank String username,
                            @NotBlank @Email String email,
                            @NotBlank String password) {}

  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody RegisterReq body) {
    if (users.existsByUsername(body.username())) {
      return ResponseEntity.badRequest().body(Map.of("error", "username_taken"));
    }
    if (users.existsByEmail(body.email())) {
      return ResponseEntity.badRequest().body(Map.of("error", "email_taken"));
    }
    User u = new User();
    u.setUsername(body.username());
    u.setEmail(body.email());
    u.setPasswordHash(encoder.encode(body.password()));
    users.save(u);
    return ResponseEntity.ok(Map.of("id", u.getId(), "username", u.getUsername(), "email", u.getEmail()));
  }

  public record LoginReq(@NotBlank String username, @NotBlank String password) {}

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginReq body) {
    return users.findByUsername(body.username())
      .filter(u -> encoder.matches(body.password(), u.getPasswordHash()))
      .<ResponseEntity<?>>map(u -> ResponseEntity.ok(Map.of("id", u.getId(), "username", u.getUsername())))
      .orElseGet(() -> ResponseEntity.status(401).body(Map.of("error", "invalid_credentials")));
  }
}
