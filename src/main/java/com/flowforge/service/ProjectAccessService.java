package com.flowforge.service;

import com.flowforge.entity.Project;
import com.flowforge.entity.User;
import com.flowforge.enums.Role;
import com.flowforge.exception.ResourceNotFoundException;
import com.flowforge.exception.UnauthorizedOperationException;
import com.flowforge.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectAccessService {

    private final ProjectRepository projectRepository;

    public Project requireAccessible(Long projectId, User user) {
        Project project = find(projectId);
        if (!canAccess(project, user)) {
            throw new UnauthorizedOperationException("You do not have access to this project");
        }
        return project;
    }

    public Project requireManageable(Long projectId, User user) {
        Project project = find(projectId);
        if (user.getRole() != Role.ADMIN && !project.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedOperationException("Only the project owner or an admin can manage this project");
        }
        return project;
    }

    public boolean canAccess(Project project, User user) {
        if (user.getRole() == Role.ADMIN || project.getOwner().getId().equals(user.getId())) {
            return true;
        }
        return project.getMembers().stream().anyMatch(member -> member.getId().equals(user.getId()));
    }

    public boolean isMember(Project project, User user) {
        return project.getOwner().getId().equals(user.getId())
                || project.getMembers().stream().anyMatch(member -> member.getId().equals(user.getId()));
    }

    private Project find(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project with id " + projectId + " was not found"));
    }
}
