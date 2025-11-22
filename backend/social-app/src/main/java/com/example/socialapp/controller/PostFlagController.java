package com.example.socialapp.controller; 

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.socialapp.model.PostFlag;
import com.example.socialapp.service.PostFlagService;

@RestController
@RequestMapping("/api/flags")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"})
public class PostFlagController {

    @Autowired
    private PostFlagService postFlagService;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    .withZone(ZoneId.systemDefault());

    @PostMapping
    public ResponseEntity<?> flagPost(@RequestParam Long postId, 
    @RequestParam Long userId, 
    @RequestParam String reason) {
    System.out.println("====");
    System.out.println("FLAG POST REQUEST RECEIVED");
    System.out.println("PostId: " + postId);
    System.out.println("UserId: " + userId);
    System.out.println("Reason: " + reason);
    System.out.println("====");
    
    try {
    PostFlag flag = postFlagService.flagPost(postId, userId, reason);
    
    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("message", "Post flagged successfully");
    response.put("flagId", flag.getId());
    response.put("reason", flag.getReason());
    
    System.out.println("SUCCESS: Flag created with ID: " + flag.getId());
    
    return ResponseEntity.ok(response);
    } catch (RuntimeException e) {
    System.err.println("ERROR: " + e.getMessage());
    e.printStackTrace();
    
    Map<String, Object> error = new HashMap<>();
    error.put("success", false);
    error.put("message", e.getMessage());
    return ResponseEntity.badRequest().body(error);
    }
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllFlags(@RequestParam(required = false) String status) {
    try {
    List<PostFlag> flags;
    
    if (status != null && !status.equals("ALL")) {
    flags = postFlagService.getFlagsByStatus(status);
    } else {
    flags = postFlagService.getAllFlags();
    }

    List<Map<String, Object>> response = flags.stream().map(flag -> {
    Map<String, Object> flagData = new HashMap<>();
    flagData.put("id", flag.getId());
    flagData.put("postId", flag.getPost().getId());
    flagData.put("postContent", flag.getPost().getContent());
    flagData.put("postImageUrl", flag.getPost().getImageUrl());
    flagData.put("postAuthor", flag.getPost().getUser().getUsername());
    flagData.put("postAuthorId", flag.getPost().getUser().getId());
    flagData.put("flaggedBy", flag.getFlaggedByUser().getUsername());
    flagData.put("flaggedById", flag.getFlaggedByUser().getId());
    flagData.put("reason", flag.getReason());
    flagData.put("status", flag.getStatus());
    flagData.put("createdAt", formatter.format(flag.getCreatedAt()));
    
    if (flag.getReviewedAt() != null) {
    flagData.put("reviewedAt", formatter.format(flag.getReviewedAt()));
    }
    if (flag.getReviewedByUser() != null) {
    flagData.put("reviewedBy", flag.getReviewedByUser().getUsername());
    }
    
    return flagData;
    }).collect(Collectors.toList());

    return ResponseEntity.ok(response);
    } catch (Exception e) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<?> getFlagByPostId(@PathVariable Long postId) {
    try {
    return postFlagService.getFlagByPostId(postId)
    .map(flag -> {
    Map<String, Object> response = new HashMap<>();
    response.put("flagged", true);
    response.put("reason", flag.getReason());
    response.put("status", flag.getStatus());
    return ResponseEntity.ok(response);
    })
    .orElseGet(() -> {
    Map<String, Object> response = new HashMap<>();
    response.put("flagged", false);
    return ResponseEntity.ok(response);
    });
    } catch (Exception e) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
    }

    @PutMapping("/{flagId}/status")
    public ResponseEntity<?> updateFlagStatus(@PathVariable Long flagId,
    @RequestParam String status,
    @RequestParam Long reviewerId) {
    System.out.println("====");
    System.out.println("UPDATE FLAG STATUS REQUEST RECEIVED");
    System.out.println("FlagId: " + flagId);
    System.out.println("Status: " + status);
    System.out.println("ReviewerId: " + reviewerId);
    System.out.println("====");
    
    try {
    PostFlag flag = postFlagService.updateFlagStatus(flagId, status, reviewerId);
    
    System.out.println("SUCCESS: Flag status updated to: " + flag.getStatus());
    System.out.println("Post Owner ID: " + flag.getPost().getUser().getId());
    System.out.println("Post ID: " + flag.getPost().getId());
    
    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("message", "Flag status updated successfully");
    response.put("status", flag.getStatus());
    
    return ResponseEntity.ok(response);
    } catch (RuntimeException e) {
    System.err.println("ERROR: " + e.getMessage());
    e.printStackTrace();
    
    Map<String, Object> error = new HashMap<>();
    error.put("success", false);
    error.put("message", e.getMessage());
    return ResponseEntity.badRequest().body(error);
    }
    }

    @DeleteMapping("/{flagId}")
    public ResponseEntity<?> deleteFlag(@PathVariable Long flagId) {
    try {
    postFlagService.deleteFlag(flagId);
    
    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("message", "Flag deleted successfully");
    
    return ResponseEntity.ok(response);
    } catch (Exception e) {
    Map<String, Object> error = new HashMap<>();
    error.put("success", false);
    error.put("message", e.getMessage());
    return ResponseEntity.badRequest().body(error);
    }
    }
}