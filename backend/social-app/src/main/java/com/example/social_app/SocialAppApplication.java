package com.example.social_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example")
public class SocialAppApplication {
  public static void main(String[] args) {
    SpringApplication.run(SocialAppApplication.class, args);
  }
}
