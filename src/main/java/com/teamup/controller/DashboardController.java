package com.teamup.controller;

import com.teamup.dto.*;
import com.teamup.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;

    // ── Real-time Dashboard — contribution % per member ─────────
    // This endpoint is the primary source for the frontend pie chart.

    @GetMapping("/group/{groupId}")
    public ResponseEntity<ApiResponse<DashboardResponse>> getGroupDashboard(
            @PathVariable Long groupId) {
        DashboardResponse dashboard = dashboardService.getGroupDashboard(groupId);
        return ResponseEntity.ok(ApiResponse.ok(dashboard));
    }
}
