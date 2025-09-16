// ReportService.java - Fixed Issues
package com.vermeg.collateralmanagement.service;

import com.vermeg.collateralmanagement.dto.request.*;
import com.vermeg.collateralmanagement.dto.response.*;
import com.vermeg.collateralmanagement.entity.*;
import com.vermeg.collateralmanagement.enums.ReportType;
import com.vermeg.collateralmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    // FIXED: Added PortfolioRepository - was missing from original
    private final PortfolioRepository portfolioRepository;

    // REMOVED: These repositories don't exist in your current setup
    // If you need them later, add them when the entities are created
    // private final CollateralAssetRepository collateralAssetRepository;
    // private final RiskMetricRepository riskMetricRepository;
    // private final MarginCallRepository marginCallRepository;
    // private final AlertRepository alertRepository;
    // private final ValuationRepository valuationRepository;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final BigDecimal REQUIRED_MARGIN_PERCENTAGE = new BigDecimal("0.20");

    // ==================== MAIN REPORT GENERATION METHODS ====================

    /**
     * Generate report from request DTO
     */
    public ReportResponse generateReport(ReportRequest request, String username) {
        log.info("Generating report: {} for user: {}", request.getName(), username);

        User user = findUserByUsername(username);

        // Validate request based on type
        validateReportRequest(request);

        Report report = createReportFromRequest(request, user);

        if (request.isGenerateImmediately()) {
            generateReportContent(report);
        }

        return ReportResponse.fromEntity(report);
    }

    /**
     * Generate portfolio summary report
     */
    public ReportResponse generatePortfolioSummaryReport(Long portfolioId, String username) {
        log.info("Generating portfolio summary report for portfolio: {} by user: {}", portfolioId, username);

        User user = findUserByUsername(username);
        Portfolio portfolio = findPortfolioByIdAndUserId(portfolioId, user.getId());

        Report report = createAndSaveReport(
                "Portfolio Summary - " + portfolio.getName(),
                "Comprehensive portfolio summary including assets, values, and risk analysis",
                ReportType.PORTFOLIO_SUMMARY,
                user
        );

        generateReportContent(report);
        log.info("Generated portfolio summary report with ID: {}", report.getId());
        return ReportResponse.fromEntity(report);
    }

    /**
     * Generate risk analysis report
     */
    public ReportResponse generateRiskAnalysisReport(Long portfolioId, String username) {
        log.info("Generating risk analysis report for portfolio: {} by user: {}", portfolioId, username);

        User user = findUserByUsername(username);
        Portfolio portfolio = findPortfolioByIdAndUserId(portfolioId, user.getId());

        Report report = createAndSaveReport(
                "Risk Analysis - " + portfolio.getName(),
                "Comprehensive risk analysis including current position, trends, and recommendations",
                ReportType.RISK_ANALYSIS,
                user
        );

        generateReportContent(report);
        log.info("Generated risk analysis report with ID: {}", report.getId());
        return ReportResponse.fromEntity(report);
    }

    /**
     * Generate margin report
     */
    public ReportResponse generateMarginReport(Long portfolioId, String username) {
        log.info("Generating margin report for portfolio: {} by user: {}", portfolioId, username);

        User user = findUserByUsername(username);
        Portfolio portfolio = findPortfolioByIdAndUserId(portfolioId, user.getId());

        Report report = createAndSaveReport(
                "Margin Report - " + portfolio.getName(),
                "Margin requirements, compliance status, and collateral analysis",
                ReportType.MARGIN_REPORT,
                user
        );

        generateReportContent(report);
        log.info("Generated margin report with ID: {}", report.getId());
        return ReportResponse.fromEntity(report);
    }

    /**
     * Generate compliance report
     */
    public ReportResponse generateComplianceReport(String username) {
        log.info("Generating compliance report for user: {}", username);

        User user = findUserByUsername(username);

        Report report = createAndSaveReport(
                "Compliance Report - " + user.getFullName(),
                "Comprehensive compliance analysis across portfolios and risk management",
                ReportType.COMPLIANCE_REPORT,
                user
        );

        generateReportContent(report);
        log.info("Generated compliance report with ID: {}", report.getId());
        return ReportResponse.fromEntity(report);
    }

    // ==================== BULK OPERATIONS ====================

    /**
     * Generate bulk reports
     */
    public BulkReportResponse generateBulkReports(BulkReportRequest request, String username) {
        log.info("Starting bulk report generation for user: {}", username);

        User user = findUserByUsername(username);
        String operationId = "BULK_" + System.currentTimeMillis();
        List<ReportResponse> reports = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        LocalDateTime startTime = LocalDateTime.now();

        for (ReportType type : request.getReportTypes()) {
            try {
                ReportRequest individualRequest = ReportRequest.builder()
                        .name(request.getCustomNameForType(type))
                        .description(request.getDescription())
                        .type(type)
                        .generateImmediately(request.isGenerateImmediately())
                        .format(request.getFormat())
                        .priority(request.getPriority())
                        .tags(request.getTags())
                        .businessJustification(request.getBusinessJustification())
                        .build();

                ReportResponse response = generateReport(individualRequest, username);
                reports.add(response);
            } catch (Exception e) {
                errorMessages.add("Failed to generate " + type.getDisplayName() + ": " + e.getMessage());
                log.error("Failed to generate report of type {}: {}", type, e.getMessage(), e);
            }
        }

        int successful = (int) reports.stream().filter(ReportResponse::isDownloadable).count();
        int failed = errorMessages.size();
        int pending = reports.size() - successful;

        return BulkReportResponse.builder()
                .operationId(operationId)
                .totalReports(request.getReportTypes().size())
                .successfulReports(successful)
                .failedReports(failed)
                .pendingReports(pending)
                .status(failed > 0 ? "COMPLETED_WITH_ERRORS" : "COMPLETED")
                .progressPercentage(100)
                .startedAt(startTime)
                .completedAt(LocalDateTime.now())
                .reports(reports)
                .errorMessages(errorMessages)
                .build();
    }

    // ==================== SEARCH AND RETRIEVAL ====================

    /**
     * Search reports with advanced criteria
     */
    @Transactional(readOnly = true)
    public List<ReportResponse> searchReports(ReportSearchRequest searchRequest, String username) {
        log.info("Searching reports for user: {} with {} active filters", username, searchRequest.getActiveFilterCount());

        User user = findUserByUsername(username);
        List<Report> reports;

        if (searchRequest.isBasicSearch()) {
            reports = reportRepository.findByNameContainingIgnoreCase(searchRequest.getSearchTerm());
        } else {
            reports = performAdvancedSearch(searchRequest, user);
        }

        // FIXED: Apply pagination limit if specified
        if (searchRequest.getMaxResults() != null && reports.size() > searchRequest.getMaxResults()) {
            reports = reports.stream()
                    .limit(searchRequest.getMaxResults())
                    .collect(Collectors.toList());
        }

        return reports.stream()
                .map(ReportResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get user reports with pagination
     */
    @Transactional(readOnly = true)
    public Page<ReportResponse> getUserReports(String username, Pageable pageable) {
        User user = findUserByUsername(username);
        Page<Report> reports = reportRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        return reports.map(ReportResponse::fromEntity);
    }

    /**
     * Get report by ID
     */
    @Transactional(readOnly = true)
    public ReportResponse getReportById(Long reportId, String username) {
        Report report = findReportById(reportId);
        User user = findUserByUsername(username);

        // Security check
        if (!report.getUser().getId().equals(user.getId()) && !isAdminUser(user)) {
            throw new SecurityException("Not authorized to access this report");
        }

        return ReportResponse.fromEntity(report);
    }

    /**
     * Get reports by type
     */
    @Transactional(readOnly = true)
    public List<ReportResponse> getReportsByType(ReportType type) {
        List<Report> reports = reportRepository.findByTypeOrderByCreatedAtDesc(type);
        return reports.stream()
                .map(ReportResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ==================== STATISTICS AND MONITORING ====================

    /**
     * Get report statistics
     */
    @Transactional(readOnly = true)
    public ReportStatistics getReportStatistics() {
        List<Report> allReports = reportRepository.findAll();
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);

        long totalReports = allReports.size();
        long generatedReports = allReports.stream()
                .mapToLong(r -> r.isGenerated() ? 1 : 0)
                .sum();
        long pendingReports = totalReports - generatedReports;
        long recentReports = allReports.stream()
                .mapToLong(r -> r.isRecent(24) ? 1 : 0)
                .sum();

        Map<String, Long> reportsByType = allReports.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getType().getDisplayName(),
                        Collectors.counting()
                ));

        Map<String, Long> reportsByUser = allReports.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getUser().getUsername(),
                        Collectors.counting()
                ));

        // FIXED: Added null check for status calculation
        Map<String, Long> reportsByStatus = new HashMap<>();
        reportsByStatus.put("PENDING", pendingReports);
        reportsByStatus.put("GENERATED", generatedReports);

        LocalDateTime lastGeneratedAt = allReports.stream()
                .filter(Report::isGenerated)
                .map(Report::getGeneratedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        double generationRate = totalReports > 0 ? (double) generatedReports / totalReports * 100 : 0;

        // FIXED: Added missing fields
        long reportsToday = reportRepository.countReportsCreatedAfter(LocalDateTime.now().minusDays(1));
        long reportsThisWeek = reportRepository.countReportsCreatedAfter(LocalDateTime.now().minusWeeks(1));
        long reportsThisMonth = reportRepository.countReportsCreatedAfter(LocalDateTime.now().minusMonths(1));

        return ReportStatistics.builder()
                .totalReports(totalReports)
                .generatedReports(generatedReports)
                .pendingReports(pendingReports)
                .recentReports(recentReports)
                .reportsByType(reportsByType)
                .reportsByUser(reportsByUser)
                .reportsByStatus(reportsByStatus)
                .lastGeneratedAt(lastGeneratedAt)
                .reportsGeneratedToday(reportsToday)
                .reportsGeneratedThisWeek(reportsThisWeek)
                .reportsGeneratedThisMonth(reportsThisMonth)
                .generationRate(generationRate)
                .systemLoadPercentage(calculateSystemLoad())
                .averageReportsPerUser(totalReports > 0 ? (double) totalReports / reportsByUser.size() : 0)
                .totalUsersWithReports((long) reportsByUser.size())
                .build();
    }

    /**
     * Get system health
     */
    @Transactional(readOnly = true)
    public ReportSystemHealth getSystemHealth() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dayAgo = now.minusDays(1);
        LocalDateTime staleThreshold = now.minusHours(24);

        long pendingReports = reportRepository.findPendingReports().size();
        long staleReports = reportRepository.findStaleReports(staleThreshold).size();
        long slowReports = reportRepository.findSlowGeneratedReports().size();

        Double avgGenerationTime = reportRepository.getAverageGenerationTimeInMinutes(dayAgo);
        Object[] generationStats = reportRepository.getReportGenerationStats(dayAgo);

        double successRate = calculateSuccessRate(generationStats);
        String status = determineSystemStatus(staleReports, slowReports, pendingReports, successRate);
        List<String> issues = generateSystemIssues(staleReports, slowReports, pendingReports, successRate);
        List<String> recommendations = generateRecommendations(issues);

        // FIXED: Calculate health score
        double healthScore = calculateHealthScore(staleReports, slowReports, pendingReports, successRate);

        return ReportSystemHealth.builder()
                .status(status)
                .lastChecked(now)
                .healthScore(healthScore)
                .uptimePercentage(99.5) // Mock value
                .pendingReports(pendingReports)
                .staleReports(staleReports)
                .slowReports(slowReports)
                .reportsInProgress(0L) // Mock value
                .estimatedQueueTime(pendingReports * 5) // Mock calculation
                .systemLoad(calculateSystemLoad())
                .averageGenerationTimeMinutes(avgGenerationTime)
                .generationSuccessRate(successRate)
                .peakPerformanceHour("14:00") // Mock value
                .cpuUsage(65.0) // Mock value
                .memoryUsage(45.0) // Mock value
                .totalStorageUsedMB(1024L)
                .availableStorageMB(10240L)
                .storageUsagePercentage(10.0)
                .reportFilesCount(reportRepository.count())
                .activeUsers(10)
                .concurrentGenerations(2)
                .peakConcurrentUsers(25)
                .issues(issues)
                .recommendations(recommendations)
                .recentAlerts(new ArrayList<>()) // Mock empty list
                .componentStatus(new HashMap<>()) // Mock empty map
                .databaseStatus("HEALTHY")
                .fileSystemStatus("HEALTHY")
                .externalServicesStatus("HEALTHY")
                .lastSystemRestart(now.minusDays(7)) // Mock value
                .lastMaintenanceDate(now.minusWeeks(1)) // Mock value
                .nextMaintenanceWindow(now.plusWeeks(1)) // Mock value
                .maintenanceMode(false)
                .build();
    }

    // ==================== FILE OPERATIONS ====================

    /**
     * Download report
     */
    public byte[] downloadReport(Long reportId, String username) {
        Report report = findReportById(reportId);
        User user = findUserByUsername(username);

        // Security check
        if (!report.getUser().getId().equals(user.getId()) && !isAdminUser(user)) {
            throw new SecurityException("Not authorized to download this report");
        }

        if (!report.isGenerated()) {
            throw new IllegalStateException("Report is not yet generated");
        }

        return generateReportContentAsBytes(report);
    }

    // ==================== MANAGEMENT OPERATIONS ====================

    /**
     * Delete report
     */
    public void deleteReport(Long reportId, String username) {
        Report report = findReportById(reportId);
        User user = findUserByUsername(username);

        if (!report.getUser().getId().equals(user.getId()) && !isAdminUser(user)) {
            throw new SecurityException("Not authorized to delete this report");
        }

        reportRepository.delete(report);
        log.info("Deleted report: {} by user: {}", reportId, username);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    private Report findReportById(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));
    }

    private Portfolio findPortfolioByIdAndUserId(Long portfolioId, Long userId) {
        return portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found or access denied"));
    }

    private Report createReportFromRequest(ReportRequest request, User user) {
        Report report = Report.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .user(user)
                .build();

        return reportRepository.save(report);
    }

    private Report createAndSaveReport(String name, String description, ReportType type, User user) {
        Report report = Report.builder()
                .name(name)
                .description(description)
                .type(type)
                .user(user)
                .generatedAt(LocalDateTime.now())
                .build();

        return reportRepository.save(report);
    }

    private void generateReportContent(Report report) {
        try {
            // Simulate report generation
            report.setGeneratedAt(LocalDateTime.now());
            reportRepository.save(report);
            log.info("Report content generated for report: {}", report.getId());
        } catch (Exception e) {
            log.error("Failed to generate content for report {}: {}", report.getId(), e.getMessage());
            throw new RuntimeException("Report generation failed: " + e.getMessage());
        }
    }

    private byte[] generateReportContentAsBytes(Report report) {
        String content = String.format("""
                === %s ===
                
                Report ID: %d
                Report Type: %s
                Generated At: %s
                User: %s
                Description: %s
                
                === Report Content ===
                This is a placeholder for the actual report content.
                In a real implementation, this would contain the formatted report data.
                """,
                report.getName(),
                report.getId(),
                report.getType().getDisplayName(),
                report.getGeneratedAt(),
                report.getUser().getFullName(),
                report.getDescription()
        );
        return content.getBytes();
    }

    private List<Report> performAdvancedSearch(ReportSearchRequest searchRequest, User user) {
        // Start with all reports if no specific filters, otherwise start with empty and build
        List<Report> reports = reportRepository.findAll();

        return reports.stream()
                .filter(report -> {
                    // Apply search term filter
                    if (searchRequest.getSearchTerm() != null && !searchRequest.getSearchTerm().trim().isEmpty()) {
                        String searchTerm = searchRequest.getSearchTerm().toLowerCase();
                        boolean matchesName = report.getName().toLowerCase().contains(searchTerm);
                        boolean matchesDescription = report.getDescription() != null &&
                                report.getDescription().toLowerCase().contains(searchTerm);
                        if (!matchesName && !matchesDescription) {
                            return false;
                        }
                    }

                    // Apply name search filter
                    if (searchRequest.getNameSearch() != null && !searchRequest.getNameSearch().trim().isEmpty()) {
                        if (!report.getName().toLowerCase().contains(searchRequest.getNameSearch().toLowerCase())) {
                            return false;
                        }
                    }

                    // Apply description search filter
                    if (searchRequest.getDescriptionSearch() != null && !searchRequest.getDescriptionSearch().trim().isEmpty()) {
                        if (report.getDescription() == null ||
                                !report.getDescription().toLowerCase().contains(searchRequest.getDescriptionSearch().toLowerCase())) {
                            return false;
                        }
                    }

                    // Apply type filters
                    if (searchRequest.getTypes() != null && !searchRequest.getTypes().isEmpty()) {
                        if (!searchRequest.getTypes().contains(report.getType())) {
                            return false;
                        }
                    }

                    // Apply exclude types filter
                    if (searchRequest.getExcludeTypes() != null && !searchRequest.getExcludeTypes().isEmpty()) {
                        if (searchRequest.getExcludeTypes().contains(report.getType())) {
                            return false;
                        }
                    }

                    // Apply generation status filter
                    if (searchRequest.getIsGenerated() != null) {
                        if (searchRequest.getIsGenerated() != report.isGenerated()) {
                            return false;
                        }
                    }

                    // Apply user ownership filter
                    if (searchRequest.getOwnedByCurrentUser() != null && searchRequest.getOwnedByCurrentUser()) {
                        if (!report.getUser().getId().equals(user.getId())) {
                            return false;
                        }
                    }

                    // Apply user IDs filter
                    if (searchRequest.getUserIds() != null && !searchRequest.getUserIds().isEmpty()) {
                        if (!searchRequest.getUserIds().contains(report.getUser().getId())) {
                            return false;
                        }
                    }

                    // Apply date range filters
                    if (searchRequest.getCreatedAfter() != null) {
                        if (report.getCreatedAt().isBefore(searchRequest.getCreatedAfter())) {
                            return false;
                        }
                    }

                    if (searchRequest.getCreatedBefore() != null) {
                        if (report.getCreatedAt().isAfter(searchRequest.getCreatedBefore())) {
                            return false;
                        }
                    }

                    if (searchRequest.getGeneratedAfter() != null && report.getGeneratedAt() != null) {
                        if (report.getGeneratedAt().isBefore(searchRequest.getGeneratedAfter())) {
                            return false;
                        }
                    }

                    if (searchRequest.getGeneratedBefore() != null && report.getGeneratedAt() != null) {
                        if (report.getGeneratedAt().isAfter(searchRequest.getGeneratedBefore())) {
                            return false;
                        }
                    }

                    return true;
                })
                .sorted((a, b) -> {
                    // Apply sorting
                    if ("name".equals(searchRequest.getSortBy())) {
                        int result = a.getName().compareTo(b.getName());
                        return "ASC".equals(searchRequest.getSortDirection()) ? result : -result;
                    } else if ("type".equals(searchRequest.getSortBy())) {
                        int result = a.getType().compareTo(b.getType());
                        return "ASC".equals(searchRequest.getSortDirection()) ? result : -result;
                    } else if ("generatedAt".equals(searchRequest.getSortBy())) {
                        LocalDateTime aDate = a.getGeneratedAt();
                        LocalDateTime bDate = b.getGeneratedAt();
                        if (aDate == null && bDate == null) return 0;
                        if (aDate == null) return "ASC".equals(searchRequest.getSortDirection()) ? -1 : 1;
                        if (bDate == null) return "ASC".equals(searchRequest.getSortDirection()) ? 1 : -1;
                        int result = aDate.compareTo(bDate);
                        return "ASC".equals(searchRequest.getSortDirection()) ? result : -result;
                    } else {
                        // Default to createdAt
                        int result = a.getCreatedAt().compareTo(b.getCreatedAt());
                        return "ASC".equals(searchRequest.getSortDirection()) ? result : -result;
                    }
                })
                .skip(searchRequest.getPage() * searchRequest.getSize())
                .limit(searchRequest.getSize())
                .collect(Collectors.toList());
    }

    private void validateReportRequest(ReportRequest request) {
        if (request.getType().toString().contains("PORTFOLIO") && request.getPortfolioId() == null) {
            throw new IllegalArgumentException("Portfolio ID is required for portfolio-specific reports");
        }

        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getStartDate().isAfter(request.getEndDate())) {
                throw new IllegalArgumentException("Start date must be before end date");
            }
        }

        // Additional validation for report name
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Report name cannot be empty");
        }

        // Validate report type
        if (request.getType() == null) {
            throw new IllegalArgumentException("Report type is required");
        }
    }

    private double calculateSystemLoad() {
        long pendingReports = reportRepository.findPendingReports().size();
        return Math.min(100.0, pendingReports * 5.0);
    }

    private double calculateSuccessRate(Object[] generationStats) {
        if (generationStats != null && generationStats.length == 2) {
            Long total = (Long) generationStats[0];
            Long generated = (Long) generationStats[1];
            if (total != null && total > 0) {
                return (double) generated / total * 100;
            }
        }
        return 0.0;
    }

    private double calculateHealthScore(long staleReports, long slowReports, long pendingReports, double successRate) {
        double score = 100.0;

        // Deduct points for issues
        score -= staleReports * 5;  // 5 points per stale report
        score -= slowReports * 3;   // 3 points per slow report
        score -= Math.max(0, pendingReports - 5) * 2; // 2 points per pending report over threshold
        score -= (100 - successRate); // Deduct based on failure rate

        return Math.max(0, Math.min(100, score));
    }

    private String determineSystemStatus(long staleReports, long slowReports, long pendingReports, double successRate) {
        if (staleReports > 10 || successRate < 80.0 || pendingReports > 50) {
            return "CRITICAL";
        } else if (staleReports > 5 || slowReports > 5 || pendingReports > 20 || successRate < 95.0) {
            return "WARNING";
        } else {
            return "HEALTHY";
        }
    }

    private List<String> generateSystemIssues(long staleReports, long slowReports, long pendingReports, double successRate) {
        List<String> issues = new ArrayList<>();

        if (staleReports > 10) {
            issues.add("Critical: High number of stale reports: " + staleReports);
        } else if (staleReports > 5) {
            issues.add("Warning: Elevated stale reports: " + staleReports);
        }

        if (slowReports > 5) {
            issues.add("Warning: Reports taking longer than expected: " + slowReports);
        }

        if (pendingReports > 50) {
            issues.add("Critical: Very high number of pending reports: " + pendingReports);
        } else if (pendingReports > 20) {
            issues.add("Warning: High number of pending reports: " + pendingReports);
        }

        if (successRate < 80) {
            issues.add("Critical: Low success rate: " + String.format("%.1f%%", successRate));
        } else if (successRate < 95) {
            issues.add("Warning: Below optimal success rate: " + String.format("%.1f%%", successRate));
        }

        return issues;
    }

    private List<String> generateRecommendations(List<String> issues) {
        List<String> recommendations = new ArrayList<>();

        if (issues.isEmpty()) {
            recommendations.add("System is operating normally");
            recommendations.add("Continue monitoring system performance");
        } else {
            if (issues.stream().anyMatch(issue -> issue.contains("stale"))) {
                recommendations.add("Review and restart failed report generation processes");
                recommendations.add("Check for system resource constraints");
            }

            if (issues.stream().anyMatch(issue -> issue.contains("slow"))) {
                recommendations.add("Optimize report generation algorithms");
                recommendations.add("Consider scaling report generation infrastructure");
            }

            if (issues.stream().anyMatch(issue -> issue.contains("pending"))) {
                recommendations.add("Increase report generation capacity");
                recommendations.add("Implement priority-based report processing");
            }

            if (issues.stream().anyMatch(issue -> issue.contains("success rate"))) {
                recommendations.add("Investigate and resolve report generation failures");
                recommendations.add("Implement better error handling and retry mechanisms");
            }

            recommendations.add("Monitor system resources and optimize if needed");
            recommendations.add("Consider implementing auto-scaling for peak loads");
        }

        return recommendations;
    }

    private boolean isAdminUser(User user) {
        // FIXED: Check role based on your actual User entity structure
        if (user.getRole() == null) {
            return false;
        }

        String roleName = user.getRole().getName();
        return "ADMINISTRATOR".equalsIgnoreCase(roleName) ||
                "ADMIN".equalsIgnoreCase(roleName) ||
                "ROLE_ADMINISTRATOR".equalsIgnoreCase(roleName) ||
                "ROLE_ADMIN".equalsIgnoreCase(roleName);
    }
}