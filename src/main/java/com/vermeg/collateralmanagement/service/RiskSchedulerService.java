package com.vermeg.collateralmanagement.service;

import com.vermeg.collateralmanagement.entity.Portfolio;
import com.vermeg.collateralmanagement.repository.PortfolioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class RiskSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(RiskSchedulerService.class);

    @Autowired
    private RiskMetricService riskMetricService;

    @Autowired
    private PortfolioRepository portfolioRepository;

    /**
     * Log when service is initialized
     */
    @PostConstruct
    public void init() {
        log.info("========================================");
        log.info("✅ RiskSchedulerService INITIALIZED");
        log.info("⏰ Scheduler will run every 2 minutes");
        log.info("========================================");
    }

    /**
     * Recalcule les risques toutes les 2 MINUTES (pour tester)
     * Vous changerez en "0 0 * * * *" pour toutes les heures après le test
     */
    @Scheduled(cron = "0 0 * * * *")  // Toutes les heures
    @Transactional
    public void recalculateAllRisks() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        log.info("⏰ ========================================");
        log.info("⏰ RECALCUL AUTOMATIQUE DES RISQUES");
        log.info("⏰ Heure: {}", timestamp);
        log.info("⏰ ========================================");

        try {
            List<Portfolio> allPortfolios = portfolioRepository.findAll();

            if (allPortfolios.isEmpty()) {
                log.info("ℹ️  Aucun portfolio trouvé");
                return;
            }

            log.info("📊 {} portfolios à traiter", allPortfolios.size());

            int success = 0;
            int failed = 0;

            for (Portfolio portfolio : allPortfolios) {
                try {
                    log.info("🔄 Portfolio: {} (ID: {})", portfolio.getName(), portfolio.getId());

                    riskMetricService.calculatePortfolioRisk(
                            portfolio.getId(),
                            portfolio.getUser().getId()
                    );

                    success++;
                    log.info("✅ Succès pour: {}", portfolio.getName());

                } catch (Exception e) {
                    failed++;
                    log.error("❌ Échec pour Portfolio ID {}: {}", portfolio.getId(), e.getMessage());
                }
            }

            log.info("⏰ ========================================");
            log.info("⏰ RÉSULTAT: {} réussis | {} échoués", success, failed);
            log.info("⏰ ========================================");

        } catch (Exception e) {
            log.error("❌ ERREUR FATALE: {}", e.getMessage());
        }
    }

    /**
     * Méthode pour déclencher manuellement le recalcul (via API)
     */
    @Transactional
    public void recalculateAllRisksManually() {
        log.info("🔧 RECALCUL MANUEL DÉCLENCHÉ");
        recalculateAllRisks();
    }
}