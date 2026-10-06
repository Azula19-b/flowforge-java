package com.flowforge.dto.task;

import jakarta.validation.constraints.NotNull;

public record AssignTaskRequest(@NotNull Long userId) {
}
