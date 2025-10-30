package com.vermeg.collateralmanagement.controller;

import com.vermeg.collateralmanagement.dto.request.PasswordChangeRequest;
import com.vermeg.collateralmanagement.dto.request.ProfileUpdateRequest;
import com.vermeg.collateralmanagement.dto.request.UserPreferencesRequest;
import com.vermeg.collateralmanagement.dto.response.ApiResponse;
import com.vermeg.collateralmanagement.dto.response.ProfileResponse;
import com.vermeg.collateralmanagement.dto.response.UserPreferencesResponse;
import com.vermeg.collateralmanagement.entity.User;
import com.vermeg.collateralmanagement.security.UserPrincipal;
import com.vermeg.collateralmanagement.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    // ==================== PROFILE ENDPOINTS ====================

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
     * Get current user profile (detailed)
     */
    @GetMapping("/profile")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ProfileResponse>> getUserProfile(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} requesting their detailed profile", currentUser.getUsername());

        try {
            ProfileResponse profile = userService.getUserProfile(currentUser.getId());

            ApiResponse<ProfileResponse> response = new ApiResponse<>();
            response.setSuccess(true);
            response.setMessage("Profile retrieved successfully");
            response.setData(profile);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get user profile: {}", e.getMessage());

            ApiResponse<ProfileResponse> response = new ApiResponse<>();
            response.setSuccess(false);
            response.setMessage("Failed to retrieve profile");
            response.setData(null);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Update current user profile
     */
    @PutMapping("/profile")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateUserProfile(
            @Valid @RequestBody ProfileUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} updating their profile", currentUser.getUsername());

        try {
            ProfileResponse updatedProfile = userService.updateUserProfile(currentUser.getId(), request);

            ApiResponse<ProfileResponse> response = new ApiResponse<>();
            response.setSuccess(true);
            response.setMessage("Profile updated successfully");
            response.setData(updatedProfile);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Failed to update profile: {}", e.getMessage());

            ApiResponse<ProfileResponse> response = new ApiResponse<>();
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setData(null);

            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Unexpected error updating profile: {}", e.getMessage());

            ApiResponse<ProfileResponse> response = new ApiResponse<>();
            response.setSuccess(false);
            response.setMessage("Failed to update profile");
            response.setData(null);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Change user password
     */
    @PutMapping("/profile/password")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody PasswordChangeRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} attempting to change password", currentUser.getUsername());

        try {
            userService.changePassword(currentUser.getId(), request);

            ApiResponse<Void> response = new ApiResponse<>();
            response.setSuccess(true);
            response.setMessage("Password changed successfully");
            response.setData(null);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Failed to change password: {}", e.getMessage());

            ApiResponse<Void> response = new ApiResponse<>();
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setData(null);

            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Unexpected error changing password: {}", e.getMessage());

            ApiResponse<Void> response = new ApiResponse<>();
            response.setSuccess(false);
            response.setMessage("Failed to change password");
            response.setData(null);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ==================== PREFERENCES/SETTINGS ENDPOINTS ====================

    /**
     * Get user preferences
     */
    @GetMapping("/preferences")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<UserPreferencesResponse>> getUserPreferences(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} requesting their preferences", currentUser.getUsername());

        try {
            UserPreferencesResponse preferences = userService.getUserPreferences(currentUser.getId());

            ApiResponse<UserPreferencesResponse> response = new ApiResponse<>();
            response.setSuccess(true);
            response.setMessage("Preferences retrieved successfully");
            response.setData(preferences);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get user preferences: {}", e.getMessage());

            ApiResponse<UserPreferencesResponse> response = new ApiResponse<>();
            response.setSuccess(false);
            response.setMessage("Failed to retrieve preferences");
            response.setData(null);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Update user preferences
     */
    @PutMapping("/preferences")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<UserPreferencesResponse>> updateUserPreferences(
            @Valid @RequestBody UserPreferencesRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} updating their preferences", currentUser.getUsername());

        try {
            UserPreferencesResponse updatedPreferences = userService.updateUserPreferences(
                    currentUser.getId(), request);

            ApiResponse<UserPreferencesResponse> response = new ApiResponse<>();
            response.setSuccess(true);
            response.setMessage("Preferences updated successfully");
            response.setData(updatedPreferences);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to update preferences: {}", e.getMessage());

            ApiResponse<UserPreferencesResponse> response = new ApiResponse<>();
            response.setSuccess(false);
            response.setMessage("Failed to update preferences");
            response.setData(null);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ==================== ADMIN ENDPOINTS ====================

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