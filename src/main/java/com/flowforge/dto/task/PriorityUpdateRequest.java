package com.flowforge.dto.task;

import com.flowforge.enums.TaskPriority;
import jakarta.validation.constraints.NotNull;

public record PriorityUpdateRequest(@NotNull TaskPriority priority) {
}
