package com.vermeg.collateralmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableTransactionManagement
@EnableScheduling
public class CollateralManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(CollateralManagementApplication.class, args);

        System.out.println("========================================");
        System.out.println("🚀 Collateral Management System Started!");
        System.out.println("========================================");
        System.out.println("📊 Swagger UI: http://localhost:8080/api/swagger-ui.html");
        System.out.println("🔧 API Docs: http://localhost:8080/api/api-docs");
        System.out.println("❤️  Health Check: http://localhost:8080/api/actuator/health");
        System.out.println("========================================");
    }
}