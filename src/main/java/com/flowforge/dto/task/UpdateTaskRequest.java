package com.flowforge.dto.task;

import jakarta.validation.constraints.Size;

public record UpdateTaskRequest(
        @Size(min = 1, max = 200) String title,
        @Size(max = 4000) String description
) {
}
