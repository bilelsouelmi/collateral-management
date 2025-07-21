package com.vermeg.collateralmanagement.service;

import com.vermeg.collateralmanagement.entity.Role;
import com.vermeg.collateralmanagement.entity.User;
import com.vermeg.collateralmanagement.enums.RoleType;
import com.vermeg.collateralmanagement.repository.RoleRepository;
import com.vermeg.collateralmanagement.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class DataInitializationService implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        initializeRoles();
        initializeDefaultAdmin();
        log.info("✅ Data initialization completed successfully!");
    }

    private void initializeRoles() {
        createRoleIfNotExists(RoleType.ADMINISTRATOR, "Administrator", "Full system access and user management");
        createRoleIfNotExists(RoleType.RISK_OFFICER, "Risk Officer", "Portfolio and risk management operations");
        createRoleIfNotExists(RoleType.MANAGER, "Manager", "Read-only access to reports and dashboards");
    }

    private void createRoleIfNotExists(RoleType roleType, String name, String description) {
        if (!roleRepository.existsByName(name)) {
            Role role = Role.builder()
                    .name(name)
                    .description(description)
                    .type(roleType)
                    .isActive(true)
                    .build();

            roleRepository.save(role);
            log.info("🔧 Created role: {}", name);
        }
    }

    private void initializeDefaultAdmin() {
        if (!userRepository.existsByUsername("admin")) {
            Role adminRole = roleRepository.findByType(RoleType.ADMINISTRATOR)
                    .orElseThrow(() -> new RuntimeException("Administrator role not found"));

            User admin = User.builder()
                    .username("admin")
                    .email("admin@vermeg.com")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .firstName("System")
                    .lastName("Administrator")
                    .phoneNumber("+216123456789")
                    .isActive(true)
                    .role(adminRole)
                    .build();

            userRepository.save(admin);
            log.info("👤 Created default admin user: admin / admin123");
        }
    }
}