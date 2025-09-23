package com.example.socialapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.Instant;

@Entity @Table(name = "users")
public class User {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable=false, unique=true, length=40)
  @NotBlank private String username;

  @Column(nullable=false, unique=true, length=120)
  @NotBlank @Email private String email;

  @Column(name="password_hash", nullable=false, length=100)
  @NotBlank private String passwordHash;

  @Column(name="created_at", nullable=false)
  private Instant createdAt = Instant.now();

  public Long getId() { return id; }
  public String getUsername() { return username; }
  public void setUsername(String v) { username = v; }
  public String getEmail() { return email; }
  public void setEmail(String v) { email = v; }
  public String getPasswordHash() { return passwordHash; }
  public void setPasswordHash(String v) { passwordHash = v; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant v) { createdAt = v; }
}
