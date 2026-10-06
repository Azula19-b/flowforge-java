package com.flowforge.dto.project;

import com.flowforge.enums.ProjectStatus;
import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
        @Size(min = 1, max = 150) String name,
        @Size(max = 2000) String description,
        ProjectStatus status
) {
}
