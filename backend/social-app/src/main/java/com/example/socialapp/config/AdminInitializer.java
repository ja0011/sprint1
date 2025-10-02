package com.example.socialapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.socialapp.model.User;
import com.example.socialapp.repository.UserRepository;

@Configuration
public class AdminInitializer {

  private static final String REQUIRED_DOMAIN = "@student.dcccd.edu";

  @Bean
  public CommandLineRunner ensureDefaultAdmin(
      UserRepository users,
      PasswordEncoder encoder,
      @Value("${ADMIN_USERNAME:admin}") String adminUsername,
      @Value("${ADMIN_EMAIL:admin@student.dcccd.edu}") String adminEmail,
      @Value("${ADMIN_PASSWORD:admin123}") String adminPassword) {
    return args -> {
      String username = adminUsername == null ? "" : adminUsername.trim().toLowerCase();
      String email = adminEmail == null ? "" : adminEmail.trim().toLowerCase();
      if (username.isBlank() || email.isBlank() || !email.endsWith(REQUIRED_DOMAIN)) {
        return;
      }

      users.findByUsername(username).ifPresentOrElse(existing -> {
        if (!existing.isAdmin()) {
          existing.setAdmin(true);
          users.save(existing);
        }
      }, () -> {
        User admin = new User();
        admin.setUsername(username);
        admin.setEmail(email);
        admin.setPasswordHash(encoder.encode(adminPassword));
        admin.setAdmin(true);
        users.save(admin);
      });
    };
  }
}
