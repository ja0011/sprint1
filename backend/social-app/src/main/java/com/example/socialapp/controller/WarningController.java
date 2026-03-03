package com.example.socialapp.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.socialapp.model.Warning;
import com.example.socialapp.service.WarningService;

@RestController
@RequestMapping("/api/warnings")
public class WarningController {

    private final WarningService warningService;

    public WarningController(WarningService warningService) {
        this.warningService = warningService;
    }

    // Send a warning
    @PostMapping("/send")
    public ResponseEntity<?> sendWarning(@RequestBody Map<String, Object> request) {
        try {
            Long adminId = Long.parseLong(request.get("adminId").toString());
            Long userId = Long.parseLong(request.get("userId").toString());
            String message = request.get("message").toString();

            System.out.println("====");
            System.out.println("SEND WARNING REQUEST");
            System.out.println("Admin ID: " + adminId);
            System.out.println("User ID: " + userId);
            System.out.println("Message: " + message);
            System.out.println("====");

            Warning warning = warningService.sendWarning(adminId, userId, message);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Warning sent successfully");
            response.put("warningId", warning.getId());

            System.out.println("SUCCESS: Warning sent with ID: " + warning.getId());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to send warning");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Get warnings for a user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Warning>> getUserWarnings(@PathVariable Long userId) {
        List<Warning> warnings = warningService.getUserWarnings(userId);
        return ResponseEntity.ok(warnings);
    }

    // Get warnings sent by an admin
    @GetMapping("/admin/{adminId}")
    public ResponseEntity<List<Warning>> getAdminWarnings(@PathVariable Long adminId) {
        List<Warning> warnings = warningService.getAdminWarnings(adminId);
        return ResponseEntity.ok(warnings);
    }

    // Get warning count for a user
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getWarningCount(@RequestParam Long userId) {
        long count = warningService.getWarningCount(userId);
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    // Delete a warning
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteWarning(@PathVariable Long id) {
        boolean deleted = warningService.deleteWarning(id);
        if (deleted) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Warning deleted"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Warning not found"));
        }
    }
}