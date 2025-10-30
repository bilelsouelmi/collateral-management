package com.vermeg.collateralmanagement.service;

import com.vermeg.collateralmanagement.dto.request.PasswordChangeRequest;
import com.vermeg.collateralmanagement.dto.request.ProfileUpdateRequest;
import com.vermeg.collateralmanagement.dto.request.UserPreferencesRequest;
import com.vermeg.collateralmanagement.dto.response.ProfileResponse;
import com.vermeg.collateralmanagement.dto.response.UserPreferencesResponse;
import com.vermeg.collateralmanagement.entity.User;
import com.vermeg.collateralmanagement.entity.UserPreferences;
import com.vermeg.collateralmanagement.repository.UserRepository;
import com.vermeg.collateralmanagement.repository.UserPreferencesRepository;
import com.vermeg.collateralmanagement.repository.RoleRepository;
import com.vermeg.collateralmanagement.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPreferencesRepository userPreferencesRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ==================== EXISTING METHODS ====================

    /**
     * Find user by ID
     */
    public User findById(Long id) {
        log.debug("Finding user by ID: {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
    }

    /**
     * Find user by username
     */
    public User findByUsername(String username) {
        log.debug("Finding user by username: {}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
    }

    /**
     * Find user by email
     */
    public User findByEmail(String email) {
        log.debug("Finding user by email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    /**
     * Get all users
     */
    public List<User> findAllUsers() {
        log.debug("Retrieving all users");
        return userRepository.findAll();
    }

    /**
     * Get all active users
     */
    public List<User> findActiveUsers() {
        log.debug("Retrieving all active users");
        return userRepository.findByIsActiveTrue();
    }

    /**
     * Update user status (active/inactive)
     */
    public User updateUserStatus(Long userId, Boolean isActive) {
        log.info("Updating user status - ID: {}, Active: {}", userId, isActive);

        User user = findById(userId);
        user.setIsActive(isActive);

        User updatedUser = userRepository.save(user);
        log.info("User status updated successfully for ID: {}", userId);

        return updatedUser;
    }

    /**
     * Update user last login time
     */
    public void updateLastLogin(Long userId) {
        log.debug("Updating last login for user ID: {}", userId);

        User user = findById(userId);
        user.updateLastLogin();
        userRepository.save(user);
    }

    /**
     * Delete user (soft delete by setting inactive)
     */
    public void deleteUser(Long userId) {
        log.info("Soft deleting user with ID: {}", userId);

        User user = findById(userId);
        user.setIsActive(false);
        userRepository.save(user);

        log.info("User soft deleted successfully: {}", userId);
    }

    /**
     * Hard delete user (permanent deletion)
     */
    public void hardDeleteUser(Long userId) {
        log.warn("Hard deleting user with ID: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found with ID: " + userId);
        }

        userRepository.deleteById(userId);
        log.warn("User permanently deleted: {}", userId);
    }

    /**
     * Get dashboard statistics based on user role
     */
    public Map<String, Object> getDashboardStats(UserPrincipal currentUser) {
        log.debug("Generating dashboard stats for user: {}", currentUser.getUsername());

        Map<String, Object> stats = new HashMap<>();

        stats.put("currentUser", currentUser.getUsername());
        stats.put("userRole", currentUser.getAuthorities().iterator().next().getAuthority());
        stats.put("lastLogin", LocalDateTime.now());

        String role = currentUser.getAuthorities().iterator().next().getAuthority();

        switch (role) {
            case "ROLE_ADMINISTRATOR":
                stats.put("totalUsers", userRepository.count());
                stats.put("activeUsers", userRepository.countActiveUsers());
                stats.put("totalRoles", roleRepository.count());
                stats.put("systemStatus", "Operational");
                break;

            case "ROLE_RISK_OFFICER":
                stats.put("activeUsers", userRepository.countActiveUsers());
                stats.put("riskAssessments", 0);
                stats.put("portfolioCount", 0);
                break;

            case "ROLE_MANAGER":
                stats.put("managedUsers", userRepository.countActiveUsers());
                stats.put("reportsGenerated", 0);
                break;

            default:
                stats.put("accessLevel", "Limited");
        }

        stats.put("timestamp", LocalDateTime.now());

        log.debug("Dashboard stats generated for user: {}", currentUser.getUsername());
        return stats;
    }

    /**
     * Check if user exists by username
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Check if user exists by email
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Get user count
     */
    public long getUserCount() {
        return userRepository.count();
    }

    /**
     * Get active user count
     */
    public long getActiveUserCount() {
        return userRepository.countActiveUsers();
    }

    // ==================== NEW PROFILE METHODS ====================

    /**
     * Get user profile
     */
    public ProfileResponse getUserProfile(Long userId) {
        log.debug("Getting profile for user ID: {}", userId);

        User user = findById(userId);

        return ProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole() != null ? user.getRole().getName() : "N/A")
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .lastModifiedAt(user.getLastModifiedAt())
                .build();
    }

    /**
     * Update user profile
     */
    public ProfileResponse updateUserProfile(Long userId, ProfileUpdateRequest request) {
        log.info("Updating profile for user ID: {}", userId);

        User user = findById(userId);

        // Check if email is being changed and if it's already in use
        if (!user.getEmail().equals(request.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email is already in use by another user");
            }
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());

        User updatedUser = userRepository.save(user);
        log.info("Profile updated successfully for user ID: {}", userId);

        return getUserProfile(updatedUser.getId());
    }

    /**
     * Change user password
     */
    public void changePassword(Long userId, PasswordChangeRequest request) {
        log.info("Attempting password change for user ID: {}", userId);

        // Validate new password matches confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("New password and confirmation do not match");
        }

        User user = findById(userId);

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Check if new password is same as current
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new RuntimeException("New password must be different from current password");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user ID: {}", userId);
    }

    // ==================== NEW PREFERENCES METHODS ====================

    /**
     * Get user preferences
     */
    public UserPreferencesResponse getUserPreferences(Long userId) {
        log.debug("Getting preferences for user ID: {}", userId);

        UserPreferences preferences = userPreferencesRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));

        return mapToPreferencesResponse(preferences);
    }

    /**
     * Update user preferences
     */
    public UserPreferencesResponse updateUserPreferences(Long userId, UserPreferencesRequest request) {
        log.info("Updating preferences for user ID: {}", userId);

        User user = findById(userId);

        UserPreferences preferences = userPreferencesRepository.findByUserId(userId)
                .orElse(UserPreferences.builder().user(user).build());

        // Update notification preferences
        if (request.getEmailNotifications() != null) {
            preferences.setEmailNotifications(request.getEmailNotifications());
        }
        if (request.getAlertNotifications() != null) {
            preferences.setAlertNotifications(request.getAlertNotifications());
        }
        if (request.getMarginCallNotifications() != null) {
            preferences.setMarginCallNotifications(request.getMarginCallNotifications());
        }
        if (request.getReportNotifications() != null) {
            preferences.setReportNotifications(request.getReportNotifications());
        }

        // Update display preferences
        if (request.getTheme() != null) {
            preferences.setTheme(request.getTheme());
        }
        if (request.getLanguage() != null) {
            preferences.setLanguage(request.getLanguage());
        }
        if (request.getDateFormat() != null) {
            preferences.setDateFormat(request.getDateFormat());
        }
        if (request.getCurrency() != null) {
            preferences.setCurrency(request.getCurrency());
        }

        // Update dashboard preferences
        if (request.getDefaultView() != null) {
            preferences.setDefaultView(request.getDefaultView());
        }
        if (request.getShowWelcomeMessage() != null) {
            preferences.setShowWelcomeMessage(request.getShowWelcomeMessage());
        }

        // Update alert thresholds
        if (request.getRiskAlertThreshold() != null) {
            preferences.setRiskAlertThreshold(request.getRiskAlertThreshold());
        }
        if (request.getMarginCallThreshold() != null) {
            preferences.setMarginCallThreshold(request.getMarginCallThreshold());
        }

        UserPreferences savedPreferences = userPreferencesRepository.save(preferences);
        log.info("Preferences updated successfully for user ID: {}", userId);

        return mapToPreferencesResponse(savedPreferences);
    }

    /**
     * Create default preferences for new user
     */
    private UserPreferences createDefaultPreferences(Long userId) {
        log.debug("Creating default preferences for user ID: {}", userId);

        User user = findById(userId);
        UserPreferences preferences = UserPreferences.builder()
                .user(user)
                .build();

        return userPreferencesRepository.save(preferences);
    }

    /**
     * Map UserPreferences entity to response DTO
     */
    private UserPreferencesResponse mapToPreferencesResponse(UserPreferences preferences) {
        return UserPreferencesResponse.builder()
                .id(preferences.getId())
                .userId(preferences.getUser().getId())
                .emailNotifications(preferences.getEmailNotifications())
                .alertNotifications(preferences.getAlertNotifications())
                .marginCallNotifications(preferences.getMarginCallNotifications())
                .reportNotifications(preferences.getReportNotifications())
                .theme(preferences.getTheme())
                .language(preferences.getLanguage())
                .dateFormat(preferences.getDateFormat())
                .currency(preferences.getCurrency())
                .defaultView(preferences.getDefaultView())
                .showWelcomeMessage(preferences.getShowWelcomeMessage())
                .riskAlertThreshold(preferences.getRiskAlertThreshold())
                .marginCallThreshold(preferences.getMarginCallThreshold())
                .createdAt(preferences.getCreatedAt())
                .lastModifiedAt(preferences.getLastModifiedAt())
                .build();
    }
}