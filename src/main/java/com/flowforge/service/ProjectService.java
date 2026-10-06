package com.flowforge.service;

import com.flowforge.dto.project.CompletionResponse;
import com.flowforge.dto.project.CreateProjectRequest;
import com.flowforge.dto.project.ProjectResponse;
import com.flowforge.dto.project.UpdateProjectRequest;
import com.flowforge.dto.user.UserResponse;
import com.flowforge.entity.Project;
import com.flowforge.entity.User;
import com.flowforge.enums.ProjectStatus;
import com.flowforge.enums.Role;
import com.flowforge.enums.TaskStatus;
import com.flowforge.exception.DuplicateResourceException;
import com.flowforge.exception.ResourceNotFoundException;
import com.flowforge.exception.UnauthorizedOperationException;
import com.flowforge.repository.ProjectRepository;
import com.flowforge.repository.TaskRepository;
import com.flowforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final ProjectAccessService projectAccessService;

    @Transactional
    public ProjectResponse create(CreateProjectRequest request) {
        User user = authenticatedUserService.getCurrentUser();
        requireManagerRole(user);

        Project project = new Project();
        project.setName(request.name().trim());
        project.setDescription(trimToNull(request.description()));
        project.setOwner(user);
        project.getMembers().add(user);
        project.setStatus(request.status() == null ? ProjectStatus.PLANNING : request.status());
        return response(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listAccessible() {
        User user = authenticatedUserService.getCurrentUser();
        List<Project> projects = user.getRole() == Role.ADMIN
                ? projectRepository.findAll()
                : projectRepository.findAccessibleByUserId(user.getId());
        return projects.stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(Long projectId) {
        User user = authenticatedUserService.getCurrentUser();
        return response(projectAccessService.requireAccessible(projectId, user));
    }

    @Transactional
    public ProjectResponse update(Long projectId, UpdateProjectRequest request) {
        User user = authenticatedUserService.getCurrentUser();
        Project project = projectAccessService.requireManageable(projectId, user);

        if (request.name() != null) {
            project.setName(request.name().trim());
        }
        if (request.description() != null) {
            project.setDescription(trimToNull(request.description()));
        }
        if (request.status() != null) {
            project.setStatus(request.status());
        }
        return response(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse archive(Long projectId) {
        User user = authenticatedUserService.getCurrentUser();
        Project project = projectAccessService.requireManageable(projectId, user);
        project.setStatus(ProjectStatus.ARCHIVED);
        return response(projectRepository.save(project));
    }

    @Transactional
    public List<UserResponse> addMember(Long projectId, Long userId) {
        User actor = authenticatedUserService.getCurrentUser();
        Project project = projectAccessService.requireManageable(projectId, actor);
        User member = findUser(userId);
        if (projectAccessService.isMember(project, member)) {
            throw new DuplicateResourceException("User is already a member of this project");
        }
        project.getMembers().add(member);
        projectRepository.save(project);
        return memberResponses(project);
    }

    @Transactional
    public void removeMember(Long projectId, Long userId) {
        User actor = authenticatedUserService.getCurrentUser();
        Project project = projectAccessService.requireManageable(projectId, actor);
        if (project.getOwner().getId().equals(userId)) {
            throw new UnauthorizedOperationException("The project owner cannot be removed");
        }
        boolean removed = project.getMembers().removeIf(member -> member.getId().equals(userId));
        if (!removed) {
            throw new ResourceNotFoundException("User is not a member of this project");
        }
        projectRepository.save(project);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> members(Long projectId) {
        User actor = authenticatedUserService.getCurrentUser();
        return memberResponses(projectAccessService.requireAccessible(projectId, actor));
    }

    @Transactional(readOnly = true)
    public CompletionResponse completion(Long projectId) {
        User actor = authenticatedUserService.getCurrentUser();
        Project project = projectAccessService.requireAccessible(projectId, actor);
        long total = taskRepository.countByProjectId(projectId);
        long completed = taskRepository.countByProjectIdAndStatus(projectId, TaskStatus.DONE);
        return new CompletionResponse(project.getId(), total, completed, percentage(total, completed));
    }

    private ProjectResponse response(Project project) {
        long total = taskRepository.countByProjectId(project.getId());
        long completed = taskRepository.countByProjectIdAndStatus(project.getId(), TaskStatus.DONE);
        return ProjectResponse.from(project, percentage(total, completed));
    }

    private List<UserResponse> memberResponses(Project project) {
        return project.getMembers().stream()
                .sorted((first, second) -> first.getId().compareTo(second.getId()))
                .map(UserResponse::from)
                .toList();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with id " + userId + " was not found"));
    }

    private void requireManagerRole(User user) {
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.MANAGER) {
            throw new UnauthorizedOperationException("Only managers and admins can create projects");
        }
    }

    private double percentage(long total, long completed) {
        return total == 0 ? 0.0 : Math.round(completed * 10000.0 / total) / 100.0;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
