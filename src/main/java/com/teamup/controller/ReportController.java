package com.teamup.controller;

import com.teamup.dto.*;
import com.teamup.service.DashboardService;
import com.teamup.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class ReportController {

    private final ReportService reportService;
    private final DashboardService dashboardService;

    // ── Teacher-only: Generate full report data (JSON) ─────────

    @GetMapping("/group/{groupId}")
    public ResponseEntity<ApiResponse<GroupReportResponse>> getReportData(
            @PathVariable Long groupId,
            @RequestParam(required = false) Long teacherId) {
        GroupReportResponse report = dashboardService.buildGroupReport(groupId, teacherId);
        return ResponseEntity.ok(ApiResponse.ok(report));
    }

    // ── Teacher-only: Export as Excel (.xlsx) ──────────────────

    @GetMapping(value = "/group/{groupId}/excel", produces =
            MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> exportExcel(
            @PathVariable Long groupId,
            @RequestParam(required = false) Long teacherId) {

        log.info("Excel export requested: groupId={}, teacherId={}", groupId, teacherId);

        byte[] excelData = reportService.generateExcelReport(groupId, teacherId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment",
                "TeamUp_GroupReport_" + groupId + ".xlsx");
        headers.setContentLength(excelData.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }

    // ── Teacher-only: Export as PDF ────────────────────────────

    @GetMapping(value = "/group/{groupId}/pdf", produces =
            MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf(
            @PathVariable Long groupId,
            @RequestParam(required = false) Long teacherId) {

        log.info("PDF export requested: groupId={}, teacherId={}", groupId, teacherId);

        byte[] pdfData = reportService.generatePdfReport(groupId, teacherId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
                "TeamUp_GroupReport_" + groupId + ".pdf");
        headers.setContentLength(pdfData.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfData);
    }
}
