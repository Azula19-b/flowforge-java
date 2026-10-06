package com.flowforge.dto.task;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record DueDateUpdateRequest(@NotNull Instant dueDate) {
}
