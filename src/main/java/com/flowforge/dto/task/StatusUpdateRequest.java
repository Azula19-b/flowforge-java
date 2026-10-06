package com.flowforge.dto.task;

import com.flowforge.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull TaskStatus status) {
}
