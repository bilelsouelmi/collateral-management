package com.vermeg.collateralmanagement.service;

import com.vermeg.collateralmanagement.entity.User;
import com.vermeg.collateralmanagement.repository.UserRepository;
import com.vermeg.collateralmanagement.repository.RoleRepository;
import com.vermeg.collateralmanagement.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private RoleRepository roleRepository;

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

        // Basic stats available to all users
        stats.put("currentUser", currentUser.getUsername());
        stats.put("userRole", currentUser.getAuthorities().iterator().next().getAuthority());
        stats.put("lastLogin", LocalDateTime.now()); // Will be updated with actual last login later

        // Role-specific statistics
        String role = currentUser.getAuthorities().iterator().next().getAuthority();

        switch (role) {
            case "ROLE_ADMINISTRATOR":
                // Admin gets full system statistics
                stats.put("totalUsers", userRepository.count());
                stats.put("activeUsers", userRepository.countActiveUsers());
                stats.put("totalRoles", roleRepository.count());
                stats.put("systemStatus", "Operational");
                break;

            case "ROLE_RISK_OFFICER":
                // Risk officers get risk-related statistics
                stats.put("activeUsers", userRepository.countActiveUsers());
                stats.put("riskAssessments", 0); // Will be implemented with collateral entities
                stats.put("portfolioCount", 0); // Will be implemented with portfolio entities
                break;

            case "ROLE_MANAGER":
                // Managers get summary view
                stats.put("managedUsers", userRepository.countActiveUsers());
                stats.put("reportsGenerated", 0); // Will be implemented later
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
}