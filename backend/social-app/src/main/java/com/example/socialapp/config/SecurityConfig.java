package com.example.socialapp.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
    .cors(cors -> cors.configurationSource(corsConfigurationSource()))
    .csrf(csrf -> csrf.disable())
    .authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**").permitAll()
    .requestMatchers("/api/users/**").permitAll()
    .requestMatchers("/api/admin/**").permitAll()
    .requestMatchers("/api/posts/**").permitAll()
    .requestMatchers("/api/flags/**").permitAll()
    .requestMatchers("/api/messages/**").permitAll()
    .requestMatchers("/api/notifications/**").permitAll()
    .requestMatchers("/api/warnings/**").permitAll()  // ADDED THIS LINE
    .requestMatchers("/ws/**").permitAll()
    .requestMatchers("/images/**").permitAll()
    .requestMatchers("/uploads/**").permitAll()
    .anyRequest().authenticated()
    );
    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    // Changed from setAllowedOrigins to setAllowedOriginPatterns
    configuration.setAllowedOriginPatterns(Arrays.asList(
    "http://127.0.0.1:*", 
    "http://localhost:*"
    ));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}