package com.flowforge.dto.project;

public record CompletionResponse(
        Long projectId,
        long totalTasks,
        long completedTasks,
        double completionPercentage
) {
}
