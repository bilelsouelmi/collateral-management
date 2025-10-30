package com.vermeg.collateralmanagement.controller;

import com.vermeg.collateralmanagement.entity.User;
import com.vermeg.collateralmanagement.repository.RoleRepository;
import com.vermeg.collateralmanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/roles")
    public ResponseEntity<?> getAllRoles() {
        return ResponseEntity.ok(roleRepository.findAll());
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("totalUsers", userRepository.count());
        status.put("totalRoles", roleRepository.count());
        status.put("activeUsers", userRepository.countActiveUsers());
        status.put("message", "Collateral Management System is running!");

        return ResponseEntity.ok(status);
    }

    @GetMapping("/admin")
    public ResponseEntity<?> getAdminUser() {
        return userRepository.findByUsername("admin")
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }
}