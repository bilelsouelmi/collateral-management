package com.vermeg.collateralmanagement.controller;

import com.vermeg.collateralmanagement.dto.request.LoginRequest;
import com.vermeg.collateralmanagement.dto.request.RegisterRequest;
import com.vermeg.collateralmanagement.dto.response.LoginResponse;
import com.vermeg.collateralmanagement.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    /**
     * Test endpoint to verify API is working
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Auth API is working! 🚀");
    }

    /**
     * Register a new user
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Registration attempt for username: {}", registerRequest.getUsername());

        try {
            ResponseEntity<?> response = authService.registerUser(registerRequest);
            log.info("Registration successful for username: {}", registerRequest.getUsername());
            return response;
        } catch (Exception e) {
            log.error("Registration failed for username: {}", registerRequest.getUsername(), e);
            return ResponseEntity.badRequest()
                    .body("Registration failed: " + e.getMessage());
        }
    }

    /**
     * Authenticate user and return JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login attempt for user: {}", loginRequest.getUsernameOrEmail());

        try {
            LoginResponse response = authService.authenticateUser(loginRequest);
            log.info("Login successful for user: {}", loginRequest.getUsernameOrEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Login failed for user: {}", loginRequest.getUsernameOrEmail(), e);
            return ResponseEntity.badRequest()
                    .body("Login failed: " + e.getMessage());
        }
    }

    /**
     * Logout user
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("User logged out successfully!");
    }
}