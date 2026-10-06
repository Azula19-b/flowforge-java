package com.flowforge.dto.analytics;

public record DashboardResponse(
        long activeProjects,
        long assignedTasks,
        long completedTasks,
        long overdueTasks,
        long urgentTasks,
        double completionPercentage,
        long workloadScore
) {
}
