package com.flowforge.service;

import com.flowforge.dto.task.CreateTaskRequest;
import com.flowforge.dto.task.TaskResponse;
import com.flowforge.dto.task.UpdateTaskRequest;
import com.flowforge.entity.Project;
import com.flowforge.entity.Task;
import com.flowforge.entity.User;
import com.flowforge.enums.ProjectStatus;
import com.flowforge.enums.Role;
import com.flowforge.enums.TaskPriority;
import com.flowforge.enums.TaskStatus;
import com.flowforge.exception.ResourceNotFoundException;
import com.flowforge.exception.UnauthorizedOperationException;
import com.flowforge.exception.ValidationException;
import com.flowforge.repository.TaskRepository;
import com.flowforge.repository.TaskSpecifications;
import com.flowforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TaskService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("dueDate", "priority", "createdAt");

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final ProjectAccessService projectAccessService;
    private final TaskWorkflowService workflowService;
    private final Clock clock;

    @Transactional
    public TaskResponse create(Long projectId, CreateTaskRequest request) {
        User actor = authenticatedUserService.getCurrentUser();
        Project project = projectAccessService.requireManageable(projectId, actor);
        ensureProjectActive(project);
        validateDueDate(request.dueDate());

        Task task = new Task();
        task.setTitle(request.title().trim());
        task.setDescription(trimToNull(request.description()));
        task.setProject(project);
        task.setCreator(actor);
        task.setPriority(request.priority() == null ? TaskPriority.MEDIUM : request.priority());
        task.setDueDate(request.dueDate());
        if (request.assigneeId() != null) {
            task.setAssignee(requireProjectMember(project, request.assigneeId()));
        }
        return response(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long taskId) {
        User actor = authenticatedUserService.getCurrentUser();
        Task task = findTask(taskId);
        requireAccess(task, actor);
        return response(task);
    }

    @Transactional
    public TaskResponse update(Long taskId, UpdateTaskRequest request) {
        User actor = authenticatedUserService.getCurrentUser();
        Task task = findTask(taskId);
        requireTaskEditor(task, actor);
        if (request.title() != null) {
            task.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            task.setDescription(trimToNull(request.description()));
        }
        return response(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long taskId) {
        User actor = authenticatedUserService.getCurrentUser();
        Task task = findTask(taskId);
        projectAccessService.requireManageable(task.getProject().getId(), actor);
        taskRepository.delete(task);
    }

    @Transactional
    public TaskResponse assign(Long taskId, Long assigneeId) {
        User actor = authenticatedUserService.getCurrentUser();
        Task task = findTask(taskId);
        projectAccessService.requireManageable(task.getProject().getId(), actor);
        task.setAssignee(requireProjectMember(task.getProject(), assigneeId));
        return response(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse changeStatus(Long taskId, TaskStatus status) {
        User actor = authenticatedUserService.getCurrentUser();
        Task task = findTask(taskId);
        requireTaskEditor(task, actor);
        workflowService.validate(task.getStatus(), status);
        task.setStatus(status);
        task.setCompletedAt(status == TaskStatus.DONE ? clock.instant() : null);
        return response(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse changePriority(Long taskId, TaskPriority priority) {
        User actor = authenticatedUserService.getCurrentUser();
        Task task = findTask(taskId);
        requireTaskManagerOrCreator(task, actor);
        task.setPriority(priority);
        return response(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse changeDueDate(Long taskId, Instant dueDate) {
        User actor = authenticatedUserService.getCurrentUser();
        Task task = findTask(taskId);
        requireTaskManagerOrCreator(task, actor);
        validateDueDate(dueDate);
        task.setDueDate(dueDate);
        return response(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> listProjectTasks(Long projectId, TaskStatus status,
                                               TaskPriority priority, Long assigneeId,
                                               Boolean overdue, String search,
                                               Pageable pageable) {
        User actor = authenticatedUserService.getCurrentUser();
        projectAccessService.requireAccessible(projectId, actor);
        validatePageable(pageable);
        Specification<Task> specification = Specification
                .where(TaskSpecifications.inProject(projectId))
                .and(TaskSpecifications.hasStatus(status))
                .and(TaskSpecifications.hasPriority(priority))
                .and(TaskSpecifications.assignedTo(assigneeId))
                .and(TaskSpecifications.overdue(overdue, clock.instant()))
                .and(TaskSpecifications.textContains(search));
        return taskRepository.findAll(specification, pageable).map(this::response);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> overdue(Pageable pageable) {
        return searchAccessible(null, true, pageable);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> search(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            throw new ValidationException("Search text cannot be blank");
        }
        return searchAccessible(query, null, pageable);
    }

    private Page<TaskResponse> searchAccessible(String query, Boolean overdue, Pageable pageable) {
        validatePageable(pageable);
        User actor = authenticatedUserService.getCurrentUser();
        Specification<Task> specification = Specification
                .where(TaskSpecifications.accessibleTo(actor.getId(), actor.getRole() == Role.ADMIN))
                .and(TaskSpecifications.textContains(query))
                .and(TaskSpecifications.overdue(overdue, clock.instant()));
        return taskRepository.findAll(specification, pageable).map(this::response);
    }

    private void requireAccess(Task task, User actor) {
        if (!projectAccessService.canAccess(task.getProject(), actor)) {
            throw new UnauthorizedOperationException("You do not have access to this task");
        }
    }

    private void requireTaskEditor(Task task, User actor) {
        requireAccess(task, actor);
        boolean elevated = actor.getRole() == Role.ADMIN
                || task.getProject().getOwner().getId().equals(actor.getId());
        boolean involved = task.getCreator().getId().equals(actor.getId())
                || (task.getAssignee() != null && task.getAssignee().getId().equals(actor.getId()));
        if (!elevated && !involved) {
            throw new UnauthorizedOperationException("Only project managers or task participants may update this task");
        }
    }

    private void requireTaskManagerOrCreator(Task task, User actor) {
        requireAccess(task, actor);
        boolean permitted = actor.getRole() == Role.ADMIN
                || task.getProject().getOwner().getId().equals(actor.getId())
                || task.getCreator().getId().equals(actor.getId());
        if (!permitted) {
            throw new UnauthorizedOperationException("You cannot change task planning details");
        }
    }

    private User requireProjectMember(Project project, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with id " + userId + " was not found"));
        if (!projectAccessService.isMember(project, user)) {
            throw new ValidationException("Tasks can only be assigned to project members");
        }
        return user;
    }

    private Task findTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Task with id " + taskId + " was not found"));
    }

    private void validateDueDate(Instant dueDate) {
        if (dueDate != null && dueDate.isBefore(clock.instant())) {
            throw new ValidationException("Due date must be in the future");
        }
    }

    private void validatePageable(Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            throw new ValidationException("Page size cannot exceed 100");
        }
        pageable.getSort().forEach(order -> {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new ValidationException(
                        "Tasks can only be sorted by dueDate, priority, or createdAt");
            }
        });
    }

    private void ensureProjectActive(Project project) {
        if (project.getStatus() == ProjectStatus.ARCHIVED
                || project.getStatus() == ProjectStatus.COMPLETED) {
            throw new ValidationException("Tasks cannot be added to a completed or archived project");
        }
    }

    private TaskResponse response(Task task) {
        return TaskResponse.from(task, clock);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
