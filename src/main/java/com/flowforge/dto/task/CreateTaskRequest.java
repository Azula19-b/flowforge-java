package com.flowforge.dto.task;

import com.flowforge.enums.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateTaskRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 4000) String description,
        Long assigneeId,
        TaskPriority priority,
        Instant dueDate
) {
}
