package com.flowforge.dto.analytics;

import com.flowforge.enums.TaskPriority;
import com.flowforge.enums.TaskStatus;

import java.util.Map;

public record ProjectAnalyticsResponse(
        Long projectId,
        long totalTasks,
        long completedTasks,
        long overdueTasks,
        double completionPercentage,
        long workloadScore,
        Map<TaskStatus, Long> tasksByStatus,
        Map<TaskPriority, Long> tasksByPriority
) {
}
