package com.flowforge.service;

import com.flowforge.dto.analytics.DashboardResponse;
import com.flowforge.entity.Project;
import com.flowforge.entity.Task;
import com.flowforge.entity.User;
import com.flowforge.enums.ProjectStatus;
import com.flowforge.enums.Role;
import com.flowforge.enums.TaskPriority;
import com.flowforge.enums.TaskStatus;
import com.flowforge.repository.ProjectRepository;
import com.flowforge.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private AuthenticatedUserService authenticatedUserService;
    @Mock private ProjectAccessService projectAccessService;

    private AnalyticsService service;
    private User member;

    @BeforeEach
    void setUp() {
        member = new User();
        member.setId(9L);
        member.setRole(Role.MEMBER);
        Clock clock = Clock.fixed(Instant.parse("2026-06-10T12:00:00Z"), ZoneOffset.UTC);
        service = new AnalyticsService(projectRepository, taskRepository,
                authenticatedUserService, projectAccessService, clock);
    }

    @Test
    void dashboardCalculatesCompletionOverdueAndWeightedWorkload() {
        Project active = project(ProjectStatus.ACTIVE);
        Project archived = project(ProjectStatus.ARCHIVED);
        when(authenticatedUserService.getCurrentUser()).thenReturn(member);
        when(projectRepository.findAccessibleByUserId(9L)).thenReturn(List.of(active, archived));
        when(taskRepository.findByAssigneeId(9L)).thenReturn(List.of(
                task(TaskStatus.TODO, TaskPriority.CRITICAL, "2026-06-09T12:00:00Z"),
                task(TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, "2026-06-20T12:00:00Z"),
                task(TaskStatus.DONE, TaskPriority.HIGH, "2026-06-01T12:00:00Z")));

        DashboardResponse dashboard = service.dashboard();

        assertThat(dashboard.activeProjects()).isEqualTo(1);
        assertThat(dashboard.assignedTasks()).isEqualTo(3);
        assertThat(dashboard.completedTasks()).isEqualTo(1);
        assertThat(dashboard.overdueTasks()).isEqualTo(1);
        assertThat(dashboard.completionPercentage()).isEqualTo(33.33);
        assertThat(dashboard.workloadScore()).isEqualTo(12);
    }

    @Test
    void emptyDashboardUsesZeroInsteadOfDividingByZero() {
        when(authenticatedUserService.getCurrentUser()).thenReturn(member);
        when(projectRepository.findAccessibleByUserId(9L)).thenReturn(List.of());
        when(taskRepository.findByAssigneeId(9L)).thenReturn(List.of());

        DashboardResponse dashboard = service.dashboard();

        assertThat(dashboard.completionPercentage()).isZero();
        assertThat(dashboard.workloadScore()).isZero();
    }

    private Project project(ProjectStatus status) {
        Project project = new Project();
        project.setStatus(status);
        return project;
    }

    private Task task(TaskStatus status, TaskPriority priority, String dueDate) {
        Task task = new Task();
        task.setStatus(status);
        task.setPriority(priority);
        task.setDueDate(Instant.parse(dueDate));
        return task;
    }
}
