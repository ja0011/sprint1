package com.example.socialapp.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.socialapp.model.User;
import com.example.socialapp.model.User.Role;
import com.example.socialapp.model.Warning;
import com.example.socialapp.repository.UserRepository;
import com.example.socialapp.repository.WarningRepository;

@Service
public class WarningService {

    private final WarningRepository warningRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public WarningService(WarningRepository warningRepository, UserRepository userRepository, NotificationService notificationService) {
        this.warningRepository = warningRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    // Send a warning from admin to user
    @Transactional
    public Warning sendWarning(Long adminId, Long userId, String message) {
        // Validate that sender is an admin
        Optional<User> adminOpt = userRepository.findById(adminId);
        if (adminOpt.isEmpty()) {
            throw new IllegalArgumentException("Admin user not found");
        }
        
        User admin = adminOpt.get();
        Role adminRole = admin.getRole();
        
        // DEBUG: Print the actual role
        System.out.println("DEBUG: Admin ID: " + adminId);
        System.out.println("DEBUG: Admin username: " + admin.getUsername());
        System.out.println("DEBUG: Admin role from DB: " + adminRole);
        System.out.println("DEBUG: Role is ADMIN: " + (adminRole == Role.ADMIN));
        
        if (adminRole != Role.ADMIN) {
            throw new IllegalArgumentException("Only admins can send warnings");
        }

        // Validate that recipient is not an admin
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        if (userOpt.get().getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Cannot send warnings to admins");
        }

        // Validate message is not empty
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Warning message cannot be empty");
        }

        // Create warning
        Warning warning = new Warning(userId, adminId, message);
        Warning savedWarning = warningRepository.save(warning);

        // Create notification
        notificationService.createWarningNotification(userId, adminId, message);

        return savedWarning;
    }

    // Get all warnings for a user
    public List<Warning> getUserWarnings(Long userId) {
        return warningRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // Get all warnings sent by an admin
    public List<Warning> getAdminWarnings(Long adminId) {
        return warningRepository.findByAdminIdOrderByCreatedAtDesc(adminId);
    }

    // Get warning count for a user
    public long getWarningCount(Long userId) {
        return warningRepository.countByUserId(userId);
    }

    // Get warning by ID
    public Optional<Warning> getWarningById(Long id) {
        return warningRepository.findById(id);
    }

    // Delete warning
    @Transactional
    public boolean deleteWarning(Long id) {
        if (warningRepository.existsById(id)) {
            warningRepository.deleteById(id);
            return true;
        }
        return false;
    }
}