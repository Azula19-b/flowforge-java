package com.flowforge.dto.task;

import com.flowforge.dto.user.UserResponse;
import com.flowforge.entity.Task;
import com.flowforge.enums.TaskPriority;
import com.flowforge.enums.TaskStatus;

import java.time.Clock;
import java.time.Instant;

public record TaskResponse(
        Long id,
        String title,
        String description,
        Long projectId,
        UserResponse assignee,
        UserResponse creator,
        TaskStatus status,
        TaskPriority priority,
        Instant dueDate,
        boolean overdue,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt
) {
    public static TaskResponse from(Task task, Clock clock) {
        boolean overdue = task.getDueDate() != null
                && task.getDueDate().isBefore(clock.instant())
                && task.getStatus() != TaskStatus.DONE;
        return new TaskResponse(task.getId(), task.getTitle(), task.getDescription(),
                task.getProject().getId(),
                task.getAssignee() == null ? null : UserResponse.from(task.getAssignee()),
                UserResponse.from(task.getCreator()), task.getStatus(), task.getPriority(),
                task.getDueDate(), overdue, task.getCreatedAt(), task.getUpdatedAt(),
                task.getCompletedAt());
    }
}
