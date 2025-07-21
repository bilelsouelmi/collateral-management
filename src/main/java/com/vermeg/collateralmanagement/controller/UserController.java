package com.vermeg.collateralmanagement.controller;

import com.vermeg.collateralmanagement.entity.User;
import com.vermeg.collateralmanagement.security.UserPrincipal;
import com.vermeg.collateralmanagement.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "*")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    /**
     * Get current user profile
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} requesting their profile", currentUser.getUsername());

        User user = userService.findById(currentUser.getId());
        return ResponseEntity.ok(user);
    }

    /**
     * Get all users (Admin only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<List<User>> getAllUsers() {
        log.info("Admin requesting all users list");

        List<User> users = userService.findAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Get user by ID (Admin and Risk Officers)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        log.info("Requesting user with ID: {}", id);

        try {
            User user = userService.findById(id);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("User not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update user status (Admin only)
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> updateUserStatus(@PathVariable Long id,
                                              @RequestParam Boolean isActive,
                                              @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("Admin {} updating status for user ID: {} to {}",
                currentUser.getUsername(), id, isActive);

        try {
            User updatedUser = userService.updateUserStatus(id, isActive);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            log.error("Failed to update user status: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to update user status: " + e.getMessage());
        }
    }

    /**
     * Delete user (Admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id,
                                        @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("Admin {} requesting to delete user ID: {}", currentUser.getUsername(), id);

        if (id.equals(currentUser.getId())) {
            return ResponseEntity.badRequest().body("Cannot delete your own account");
        }

        try {
            userService.deleteUser(id);
            return ResponseEntity.ok("User deleted successfully");
        } catch (Exception e) {
            log.error("Failed to delete user: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to delete user: " + e.getMessage());
        }
    }

    /**
     * Get dashboard statistics (All authenticated users)
     */
    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<?> getDashboardStats(@AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} requesting dashboard statistics", currentUser.getUsername());

        try {
            var stats = userService.getDashboardStats(currentUser);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Failed to get dashboard stats: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to get dashboard stats");
        }
    }
}