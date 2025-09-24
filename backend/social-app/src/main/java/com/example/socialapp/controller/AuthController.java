package com.example.socialapp.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
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
// Allow your static site on 127.0.0.1:5500 to call the API during dev.
// Adjust origins as needed (e.g., http://localhost:5500 if that's your URL).
@CrossOrigin(origins = {"http://127.0.0.1:5500", "http://localhost:5500"}, allowCredentials = "false")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AuthController(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    // ======== DTOs ========
    public record RegisterReq(
            @NotBlank String username,
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record LoginReq(
            @NotBlank String username,
            @NotBlank String password
    ) {}

    public record LoginResp(
            Long id,
            String username,
            String email
    ) {}

    public record ResetReq(
            @NotBlank @Email String email,
            @NotBlank String newPassword
    ) {}

    // ======== Endpoints ========

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterReq body) {
        final String username = body.username().trim().toLowerCase();
        final String email = body.email().trim().toLowerCase();
        final String rawPassword = body.password();

        if (username.length() < 3) {
            return ResponseEntity.badRequest().body(Map.of("error", "username_too_short"));
        }
        if (users.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username_taken"));
        }
        Optional<User> emailOpt = (Optional<User>) users.findByEmail(email);
        if (emailOpt.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "email_taken"));
        }

        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(rawPassword));
        users.save(u);

        return ResponseEntity.ok(new LoginResp(u.getId(), u.getUsername(), u.getEmail()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginReq body) {
        final String username = body.username().trim().toLowerCase();
        final String password = body.password();

        Optional<User> opt = users.findByUsername(username);
        if (opt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_credentials"));
        }
        User u = opt.get();
        if (!encoder.matches(password, u.getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_credentials"));
        }
        return ResponseEntity.ok(new LoginResp(u.getId(), u.getUsername(), u.getEmail()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // No server session yet; once you add sessions/JWT, revoke here.
        return ResponseEntity.ok().build();
    }

    /**
     * DEV-SIMPLE reset: user enters email + new password (no token).
     * For production, replace with request-reset + token confirmation flow.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetReq body) {
        final String email = body.email().trim().toLowerCase();
        final String newPassword = body.newPassword();

        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "weak_password", "hint", "Use at least 6 characters"));
        }

        Optional<User> userOpt = (Optional<User>) users.findByEmail(email);
        return userOpt
                .map(u -> {
                    u.setPasswordHash(encoder.encode(newPassword));
                    users.save(u);
                    return ResponseEntity.ok().build();
                })
                .orElseGet(() -> ResponseEntity.badRequest().body(Map.of("error", "email_not_found")));
    }
}
