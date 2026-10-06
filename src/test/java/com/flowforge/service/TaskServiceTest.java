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
import com.flowforge.exception.UnauthorizedOperationException;
import com.flowforge.exception.ValidationException;
import com.flowforge.repository.ActivityLogRepository;
import com.flowforge.repository.CommentRepository;
import com.flowforge.repository.TaskRepository;
import com.flowforge.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T12:00:00Z");

    @Mock private TaskRepository taskRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthenticatedUserService authenticatedUserService;
    @Mock private ProjectAccessService projectAccessService;
    @Mock private TaskWorkflowService workflowService;
    @Mock private CommentRepository commentRepository;
    @Mock private ActivityLogRepository activityLogRepository;
    @Mock private ActivityLogService activityLogService;

    private TaskService service;

    @BeforeEach
    void setUp() {
        service = new TaskService(taskRepository, userRepository, authenticatedUserService,
                projectAccessService, workflowService, commentRepository, activityLogRepository,
                activityLogService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createRejectsPastDueDate() {
        User owner = user(1L, Role.MANAGER);
        Project project = project(owner);
        when(authenticatedUserService.getCurrentUser()).thenReturn(owner);
        when(projectAccessService.requireManageable(10L, owner)).thenReturn(project);

        CreateTaskRequest request = new CreateTaskRequest("Late task", null, null,
                TaskPriority.HIGH, NOW.minusSeconds(60));

        assertThatThrownBy(() -> service.create(10L, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("future");
        verify(taskRepository, never()).save(any());
    }

    @Test
    void unrelatedMemberCannotEditTask() {
        User owner = user(1L, Role.MANAGER);
        User actor = user(2L, Role.MEMBER);
        Project project = project(owner);
        project.getMembers().add(actor);
        Task task = task(20L, project, owner, null);
        when(authenticatedUserService.getCurrentUser()).thenReturn(actor);
        when(taskRepository.findById(20L)).thenReturn(Optional.of(task));
        when(projectAccessService.canAccess(project, actor)).thenReturn(true);

        assertThatThrownBy(() -> service.update(20L, new UpdateTaskRequest("Changed", null)))
                .isInstanceOf(UnauthorizedOperationException.class);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void unsupportedSortFieldIsRejected() {
        User actor = user(2L, Role.MEMBER);
        when(authenticatedUserService.getCurrentUser()).thenReturn(actor);

        assertThatThrownBy(() -> service.listProjectTasks(10L, null, null, null,
                null, null, PageRequest.of(0, 20, Sort.by("title"))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("sorted");
        verify(taskRepository, never()).findAll(any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void assigneeCanCompleteReviewedTask() {
        User owner = user(1L, Role.MANAGER);
        User assignee = user(2L, Role.MEMBER);
        Project project = project(owner);
        project.getMembers().add(assignee);
        Task task = task(20L, project, owner, assignee);
        task.setStatus(TaskStatus.REVIEW);
        when(authenticatedUserService.getCurrentUser()).thenReturn(assignee);
        when(taskRepository.findById(20L)).thenReturn(Optional.of(task));
        when(projectAccessService.canAccess(project, assignee)).thenReturn(true);
        when(taskRepository.save(task)).thenReturn(task);

        TaskResponse response = service.changeStatus(20L, TaskStatus.DONE);

        assertThat(response.status()).isEqualTo(TaskStatus.DONE);
        assertThat(response.completedAt()).isEqualTo(NOW);
        verify(workflowService).validate(TaskStatus.REVIEW, TaskStatus.DONE);
        verify(activityLogService).record(any(), any(), any(), any(), any());
    }

    private User user(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("user" + id + "@example.com");
        user.setRole(role);
        user.setCreatedAt(NOW);
        return user;
    }

    private Project project(User owner) {
        Project project = new Project();
        project.setId(10L);
        project.setName("Project");
        project.setOwner(owner);
        project.setStatus(ProjectStatus.ACTIVE);
        project.getMembers().add(owner);
        return project;
    }

    private Task task(Long id, Project project, User creator, User assignee) {
        Task task = new Task();
        task.setId(id);
        task.setTitle("Task");
        task.setProject(project);
        task.setCreator(creator);
        task.setAssignee(assignee);
        task.setPriority(TaskPriority.MEDIUM);
        task.setCreatedAt(NOW.minusSeconds(3600));
        task.setUpdatedAt(NOW.minusSeconds(3600));
        return task;
    }
}
