package com.vermeg.collateralmanagement.service;

import com.vermeg.collateralmanagement.dto.request.LoginRequest;
import com.vermeg.collateralmanagement.dto.request.RegisterRequest;
import com.vermeg.collateralmanagement.dto.response.LoginResponse;
import com.vermeg.collateralmanagement.entity.Role;
import com.vermeg.collateralmanagement.entity.User;
import com.vermeg.collateralmanagement.enums.RoleType;
import com.vermeg.collateralmanagement.repository.RoleRepository;
import com.vermeg.collateralmanagement.repository.UserRepository;
import com.vermeg.collateralmanagement.security.JwtTokenProvider;
import com.vermeg.collateralmanagement.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    public LoginResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.generateToken(authentication);

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        Set<String> roles = userPrincipal.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toSet());

        return new LoginResponse(
                jwt,
                userPrincipal.getId(),
                userPrincipal.getUsername(),
                userPrincipal.getEmail(),
                roles
        );
    }

    public ResponseEntity<?> registerUser(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            return ResponseEntity.badRequest()
                    .body("Error: Username is already taken!");
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity.badRequest()
                    .body("Error: Email is already in use!");
        }

        // Set default role - using MANAGER as default (you can change this)
        Role userRole = roleRepository.findByType(RoleType.MANAGER)
                .orElseThrow(() -> new RuntimeException("Error: Default role is not found."));

        // Create new user using builder pattern
        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .role(userRole)
                .isActive(true)
                .build();

        userRepository.save(user);

        log.info("User registered successfully: {}", registerRequest.getUsername());
        return ResponseEntity.ok("User registered successfully!");
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
    }
}