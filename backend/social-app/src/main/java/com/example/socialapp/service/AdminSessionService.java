package com.example.socialapp.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class AdminSessionService {

  private final SecureRandom random = new SecureRandom();
  private final Map<String, Long> sessions = new ConcurrentHashMap<>();

  public String createSession(Long adminUserId) {
    byte[] bytes = new byte[24];
    random.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    sessions.put(token, adminUserId);
    return token;
  }

  public Optional<Long> resolve(String token) {
    if (token == null || token.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(sessions.get(token));
  }

  public void revoke(String token) {
    if (token != null) {
      sessions.remove(token);
    }
  }
}
