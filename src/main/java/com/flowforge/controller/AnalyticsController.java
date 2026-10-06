package com.flowforge.controller;

import com.flowforge.dto.activity.ActivityResponse;
import com.flowforge.dto.analytics.DashboardResponse;
import com.flowforge.dto.analytics.ProjectAnalyticsResponse;
import com.flowforge.service.ActivityLogService;
import com.flowforge.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final ActivityLogService activityLogService;

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return analyticsService.dashboard();
    }

    @GetMapping("/projects/{projectId}/analytics")
    public ProjectAnalyticsResponse projectAnalytics(@PathVariable Long projectId) {
        return analyticsService.projectAnalytics(projectId);
    }

    @GetMapping("/projects/{projectId}/activity")
    public Page<ActivityResponse> activity(@PathVariable Long projectId,
                                           @PageableDefault(size = 20) Pageable pageable) {
        return activityLogService.list(projectId, pageable);
    }
}
