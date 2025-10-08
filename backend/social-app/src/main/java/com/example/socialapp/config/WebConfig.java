package com.example.socialapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // Serve uploaded profile pictures from the project's images directory
    registry.addResourceHandler("/images/**")
        .addResourceLocations("file:C:/Users/ethan/.sprint1/sprint1-ethan/sprint1/images/");
  }
}