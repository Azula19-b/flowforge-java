package com.flowforge.dto.project;

import com.flowforge.enums.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 2000) String description,
        ProjectStatus status
) {
}
