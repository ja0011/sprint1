package com.example.socialapp.controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.socialapp.model.User;
import com.example.socialapp.repository.UserRepository;
import com.example.socialapp.service.AdminSessionService;

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

  private static final String REQUIRED_DOMAIN = "@student.dcccd.edu";

  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final AdminSessionService sessions;

  public AdminController(UserRepository users, PasswordEncoder encoder, AdminSessionService sessions) {
    this.users = users;
    this.encoder = encoder;
    this.sessions = sessions;
  }

  public record AdminLoginReq(@NotBlank @Email String email, @NotBlank String password) {}
  public record AdminRegisterReq(@NotBlank String username, @NotBlank @Email String email, @NotBlank String password) {}
  public record AdminLoginRes(Long id, String username, String email, String token) {}
  public record UserRow(Long id, String username, String email, boolean admin, Instant createdAt) {}

  @PostMapping("/register")
  public ResponseEntity<?> register(@Valid @RequestBody AdminRegisterReq body) {
    String username = body.username().trim().toLowerCase();
    String email = body.email().trim().toLowerCase();
    if (!email.endsWith(REQUIRED_DOMAIN)) {
      return ResponseEntity.status(403).body(Map.of("error", "invalid_admin_domain"));
    }
    if (username.length() < 3) {
      return ResponseEntity.badRequest().body(Map.of("error", "username_too_short"));
    }
    if (users.existsByUsername(username)) {
      return ResponseEntity.badRequest().body(Map.of("error", "username_taken"));
    }
    if (users.existsByEmail(email)) {
      return ResponseEntity.badRequest().body(Map.of("error", "email_taken"));
    }

    User admin = new User();
    admin.setUsername(username);
    admin.setEmail(email);
    admin.setPasswordHash(encoder.encode(body.password()));
    admin.setAdmin(true);
    users.save(admin);

    return ResponseEntity.ok(Map.of(
      "id", admin.getId(),
      "username", admin.getUsername(),
      "email", admin.getEmail()
    ));
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@Valid @RequestBody AdminLoginReq body) {
    String email = body.email().trim().toLowerCase();
    if (!email.endsWith(REQUIRED_DOMAIN)) {
      return ResponseEntity.status(403).body(Map.of("error", "invalid_admin_domain"));
    }
    return users.findByEmailAndAdminTrue(email)
      .filter(u -> encoder.matches(body.password(), u.getPasswordHash()))
      .map(u -> {
        String token = sessions.createSession(u.getId());
        return ResponseEntity.ok((Object) new AdminLoginRes(u.getId(), u.getUsername(), u.getEmail(), token));
      })
      .orElseGet(() -> ResponseEntity.status(401).body(Map.of("error", "invalid_admin_credentials")));
  }

  @PostMapping("/logout")
  public ResponseEntity<?> logout(@RequestHeader(name = "X-Admin-Token", required = false) String token) {
    sessions.revoke(token);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/users")
  public ResponseEntity<?> listUsers(@RequestHeader("X-Admin-Token") String token) {
    return sessions.resolve(token)
      .flatMap(adminId -> users.findById(adminId))
      .filter(User::isAdmin)
      .<ResponseEntity<?>>map(admin -> {
        List<UserRow> rows = users.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
            .stream()
            .map(u -> new UserRow(u.getId(), u.getUsername(), u.getEmail(), u.isAdmin(), u.getCreatedAt()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(rows);
      })
      .orElseGet(() -> ResponseEntity.status(401).body(Map.of("error", "invalid_admin_session")));
  }
}
