package com.flowforge.dto.project;

import com.flowforge.dto.user.UserResponse;
import com.flowforge.entity.Project;
import com.flowforge.entity.User;
import com.flowforge.enums.ProjectStatus;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        UserResponse owner,
        List<UserResponse> members,
        ProjectStatus status,
        double completionPercentage,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProjectResponse from(Project project, double completionPercentage) {
        List<UserResponse> members = project.getMembers().stream()
                .sorted(Comparator.comparing(User::getId))
                .map(UserResponse::from)
                .toList();
        return new ProjectResponse(project.getId(), project.getName(), project.getDescription(),
                UserResponse.from(project.getOwner()), members, project.getStatus(),
                completionPercentage, project.getCreatedAt(), project.getUpdatedAt());
    }

}
