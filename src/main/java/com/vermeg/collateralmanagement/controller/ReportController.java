// ReportController.java - Clean version without Swagger annotations
package com.vermeg.collateralmanagement.controller;

import com.vermeg.collateralmanagement.dto.request.ReportRequest;
import com.vermeg.collateralmanagement.dto.request.BulkReportRequest;
import com.vermeg.collateralmanagement.dto.request.ReportSearchRequest;
import com.vermeg.collateralmanagement.dto.response.ApiResponse;
import com.vermeg.collateralmanagement.dto.response.ReportResponse;
import com.vermeg.collateralmanagement.dto.response.BulkReportResponse;
import com.vermeg.collateralmanagement.dto.response.ReportStatistics;
import com.vermeg.collateralmanagement.dto.response.ReportSystemHealth;
import com.vermeg.collateralmanagement.enums.ReportType;
import com.vermeg.collateralmanagement.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

    // ==================== REPORT GENERATION ENDPOINTS ====================

    /**
     * Generate Report from Request DTO
     */
    @PostMapping
    @PreAuthorize("hasRole('RISK_OFFICER') or hasRole('ADMINISTRATOR') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ReportResponse>> generateReport(
            @Valid @RequestBody ReportRequest request,
            Authentication authentication) {

        try {
            String username = authentication.getName();
            ReportResponse response = reportService.generateReport(request, username);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Report generation initiated successfully", response));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid report request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid request: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error generating report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to generate report: " + e.getMessage()));
        }
    }

    /**
     * Generate Portfolio Summary Report
     */
    @PostMapping("/portfolio/{portfolioId}/summary")
    @PreAuthorize("hasRole('RISK_OFFICER') or hasRole('ADMINISTRATOR') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ReportResponse>> generatePortfolioSummaryReport(
            @PathVariable @NotNull @Min(1) Long portfolioId,
            Authentication authentication) {

        try {
            String username = authentication.getName();
            ReportResponse response = reportService.generatePortfolioSummaryReport(portfolioId, username);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Portfolio summary report generated successfully", response));
        } catch (RuntimeException e) {
            log.error("Error generating portfolio summary report: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Portfolio not found or access denied: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error generating portfolio summary report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to generate portfolio summary report: " + e.getMessage()));
        }
    }

    /**
     * Generate Risk Analysis Report
     */
    @PostMapping("/portfolio/{portfolioId}/risk-analysis")
    @PreAuthorize("hasRole('RISK_OFFICER') or hasRole('ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<ReportResponse>> generateRiskAnalysisReport(
            @PathVariable @NotNull @Min(1) Long portfolioId,
            Authentication authentication) {

        try {
            String username = authentication.getName();
            ReportResponse response = reportService.generateRiskAnalysisReport(portfolioId, username);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Risk analysis report generated successfully", response));
        } catch (RuntimeException e) {
            log.error("Error generating risk analysis report: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Portfolio not found or access denied: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error generating risk analysis report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to generate risk analysis report: " + e.getMessage()));
        }
    }

    /**
     * Generate Margin Report
     */
    @PostMapping("/portfolio/{portfolioId}/margin")
    @PreAuthorize("hasRole('RISK_OFFICER') or hasRole('ADMINISTRATOR') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ReportResponse>> generateMarginReport(
            @PathVariable @NotNull @Min(1) Long portfolioId,
            Authentication authentication) {

        try {
            String username = authentication.getName();
            ReportResponse response = reportService.generateMarginReport(portfolioId, username);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Margin report generated successfully", response));
        } catch (RuntimeException e) {
            log.error("Error generating margin report: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Portfolio not found or access denied: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error generating margin report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to generate margin report: " + e.getMessage()));
        }
    }

    /**
     * Generate Compliance Report
     */
    @PostMapping("/compliance")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<ApiResponse<ReportResponse>> generateComplianceReport(Authentication authentication) {

        try {
            String username = authentication.getName();
            ReportResponse response = reportService.generateComplianceReport(username);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Compliance report generated successfully", response));
        } catch (Exception e) {
            log.error("Error generating compliance report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to generate compliance report: " + e.getMessage()));
        }
    }

    // ==================== BULK OPERATIONS ====================

    /**
     * Bulk Generate Reports
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<BulkReportResponse>> generateBulkReports(
            @Valid @RequestBody BulkReportRequest request,
            Authentication authentication) {

        try {
            String username = authentication.getName();
            BulkReportResponse response = reportService.generateBulkReports(request, username);

            HttpStatus status = response.getFailedReports() > 0 ? HttpStatus.MULTI_STATUS : HttpStatus.CREATED;
            return ResponseEntity.status(status)
                    .body(ApiResponse.success("Bulk report generation completed", response));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid bulk report request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid request: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error generating bulk reports: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to generate bulk reports: " + e.getMessage()));
        }
    }

    // ==================== SEARCH AND RETRIEVAL ====================

    /**
     * Advanced Search Reports
     */
    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> searchReports(
            @Valid @RequestBody ReportSearchRequest searchRequest,
            Authentication authentication) {

        try {
            String username = authentication.getName();

            // Validate search request
            if (!searchRequest.isValidDateRange()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid date range in search criteria"));
            }

            List<ReportResponse> reports = reportService.searchReports(searchRequest, username);

            String message = String.format("Found %d reports matching criteria", reports.size());
            return ResponseEntity.ok(ApiResponse.success(message, reports));
        } catch (Exception e) {
            log.error("Error searching reports: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to search reports: " + e.getMessage()));
        }
    }

    /**
     * Get User Reports with Pagination
     */
    @GetMapping("/my-reports")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Page<ReportResponse>>> getMyReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {

        try {
            String username = authentication.getName();

            // Create proper sort object
            Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
            Sort sort = Sort.by(direction, sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<ReportResponse> reports = reportService.getUserReports(username, pageable);

            // Create metadata map
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("totalElements", reports.getTotalElements());
            metadata.put("totalPages", reports.getTotalPages());
            metadata.put("currentPage", reports.getNumber());
            metadata.put("pageSize", reports.getSize());

            return ResponseEntity.ok(ApiResponse.withMetadata(
                    "Reports retrieved successfully",
                    reports,
                    metadata
            ));
        } catch (Exception e) {
            log.error("Error retrieving user reports: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve reports: " + e.getMessage()));
        }
    }

    /**
     * Get Report by ID
     */
    @GetMapping("/{reportId}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ReportResponse>> getReportById(
            @PathVariable @NotNull @Min(1) Long reportId,
            Authentication authentication) {

        try {
            String username = authentication.getName();
            ReportResponse report = reportService.getReportById(reportId, username);

            return ResponseEntity.ok(ApiResponse.success("Report retrieved successfully", report));
        } catch (SecurityException e) {
            log.warn("Unauthorized access attempt to report {} by user {}: {}", reportId, authentication.getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Not authorized to access this report"));
        } catch (RuntimeException e) {
            log.error("Report not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Report not found: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error retrieving report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve report: " + e.getMessage()));
        }
    }

    /**
     * Get Reports by Type
     */
    @GetMapping("/type/{type}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getReportsByType(
            @PathVariable @NotNull ReportType type) {

        try {
            List<ReportResponse> reports = reportService.getReportsByType(type);

            String message = String.format("Found %d reports of type %s", reports.size(), type.getDisplayName());
            return ResponseEntity.ok(ApiResponse.success(message, reports));
        } catch (Exception e) {
            log.error("Error retrieving reports by type: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve reports by type: " + e.getMessage()));
        }
    }

    // ==================== STATISTICS AND MONITORING ====================

    /**
     * Get Report Statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<ApiResponse<ReportStatistics>> getReportStatistics() {

        try {
            ReportStatistics stats = reportService.getReportStatistics();

            return ResponseEntity.ok(ApiResponse.success("Report statistics retrieved successfully", stats));
        } catch (Exception e) {
            log.error("Error retrieving report statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve report statistics: " + e.getMessage()));
        }
    }

    /**
     * Get System Health
     */
    @GetMapping("/system-health")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<ReportSystemHealth>> getReportSystemHealth() {

        try {
            ReportSystemHealth health = reportService.getSystemHealth();

            // Determine appropriate HTTP status based on system health
            HttpStatus status = HttpStatus.OK;
            if ("CRITICAL".equals(health.getStatus())) {
                status = HttpStatus.SERVICE_UNAVAILABLE;
            } else if ("WARNING".equals(health.getStatus())) {
                status = HttpStatus.ACCEPTED;
            }

            return ResponseEntity.status(status)
                    .body(ApiResponse.success("System health retrieved successfully", health));
        } catch (Exception e) {
            log.error("Error retrieving report system health: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve system health: " + e.getMessage()));
        }
    }

    // ==================== FILE OPERATIONS ====================

    /**
     * Download Report File
     */
    @GetMapping("/{reportId}/download")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<byte[]> downloadReport(
            @PathVariable @NotNull @Min(1) Long reportId,
            Authentication authentication) {

        try {
            String username = authentication.getName();
            byte[] reportContent = reportService.downloadReport(reportId, username);

            // Get report details for filename
            ReportResponse report = reportService.getReportById(reportId, username);
            String filename = report.getFileName() != null ? report.getFileName() : "report_" + reportId + ".pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(reportContent.length);

            log.info("Report {} downloaded by user {}", reportId, username);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(reportContent);

        } catch (SecurityException e) {
            log.warn("Unauthorized download attempt for report {} by user {}: {}", reportId, authentication.getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalStateException e) {
            log.warn("Report not ready for download: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (RuntimeException e) {
            log.error("Report not found for download: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error downloading report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== MANAGEMENT OPERATIONS ====================

    /**
     * Delete Report
     */
    @DeleteMapping("/{reportId}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<ApiResponse<Void>> deleteReport(
            @PathVariable @NotNull @Min(1) Long reportId,
            Authentication authentication) {

        try {
            String username = authentication.getName();
            reportService.deleteReport(reportId, username);

            return ResponseEntity.ok(ApiResponse.success("Report deleted successfully"));
        } catch (SecurityException e) {
            log.error("Unauthorized deletion attempt by user {}: {}", authentication.getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Not authorized to delete this report"));
        } catch (RuntimeException e) {
            log.error("Report not found for deletion: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Report not found: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete report: " + e.getMessage()));
        }
    }

    // ==================== UTILITY ENDPOINTS ====================

    /**
     * Get Available Report Types
     */
    @GetMapping("/types")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<ReportTypeInfo>>> getAvailableReportTypes() {

        try {
            List<ReportTypeInfo> reportTypes = Arrays.stream(ReportType.values())
                    .map(type -> new ReportTypeInfo(
                            type,
                            type.getDisplayName(),
                            type.isRegulatoryRequired(),
                            type.isRiskRelated()
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success("Report types retrieved successfully", reportTypes));
        } catch (Exception e) {
            log.error("Error retrieving report types: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve report types: " + e.getMessage()));
        }
    }

    /**
     * Get Report Status
     */
    @GetMapping("/{reportId}/status")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ReportStatusInfo>> getReportStatus(
            @PathVariable @NotNull @Min(1) Long reportId,
            Authentication authentication) {

        try {
            String username = authentication.getName();
            ReportResponse report = reportService.getReportById(reportId, username);

            ReportStatusInfo statusInfo = new ReportStatusInfo(
                    report.getId(),
                    report.getStatus(),
                    report.isGenerated(),
                    report.getProgressPercentage(),
                    report.getGeneratedAt(),
                    report.isDownloadable()
            );

            return ResponseEntity.ok(ApiResponse.success("Report status retrieved successfully", statusInfo));
        } catch (SecurityException e) {
            log.warn("Unauthorized status check for report {} by user {}: {}", reportId, authentication.getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Not authorized to check this report status"));
        } catch (RuntimeException e) {
            log.error("Report not found for status check: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Report not found: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting report status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get report status: " + e.getMessage()));
        }
    }

    // ==================== HELPER CLASSES ====================

    /**
     * Helper class for report type information
     */
    public static class ReportTypeInfo {
        public final ReportType type;
        public final String displayName;
        public final boolean isRegulatoryRequired;
        public final boolean isRiskRelated;

        public ReportTypeInfo(ReportType type, String displayName, boolean isRegulatoryRequired, boolean isRiskRelated) {
            this.type = type;
            this.displayName = displayName;
            this.isRegulatoryRequired = isRegulatoryRequired;
            this.isRiskRelated = isRiskRelated;
        }
    }

    /**
     * Helper class for report status information
     */
    public static class ReportStatusInfo {
        public final Long reportId;
        public final String status;
        public final boolean isGenerated;
        public final Integer progressPercentage;
        public final LocalDateTime generatedAt;
        public final boolean isDownloadable;

        public ReportStatusInfo(Long reportId, String status, boolean isGenerated,
                                Integer progressPercentage, LocalDateTime generatedAt, boolean isDownloadable) {
            this.reportId = reportId;
            this.status = status;
            this.isGenerated = isGenerated;
            this.progressPercentage = progressPercentage;
            this.generatedAt = generatedAt;
            this.isDownloadable = isDownloadable;
        }
    }
}