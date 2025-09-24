package com.example.social_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main Spring Boot application class.
 *
 * NOTE:
 * - Your other code lives under package "com.example.socialapp" (no underscore),
 *   while this class is in "com.example.social_app" (with underscore).
 * - The annotations below explicitly tell Spring where to find your controllers,
 *   services, entities, and repositories so everything wires up correctly.
 */
@SpringBootApplication(scanBasePackages = "com.example")
@EnableJpaRepositories(basePackages = "com.example.socialapp.repository")
@EntityScan(basePackages = "com.example.socialapp.model")
public class SocialAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(SocialAppApplication.class, args);
    }
}
