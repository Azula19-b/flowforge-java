package com.flowforge.service;

import com.flowforge.dto.analytics.DashboardResponse;
import com.flowforge.dto.analytics.ProjectAnalyticsResponse;
import com.flowforge.entity.Project;
import com.flowforge.entity.Task;
import com.flowforge.entity.User;
import com.flowforge.enums.ProjectStatus;
import com.flowforge.enums.Role;
import com.flowforge.enums.TaskPriority;
import com.flowforge.enums.TaskStatus;
import com.flowforge.repository.ProjectRepository;
import com.flowforge.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final ProjectAccessService projectAccessService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        User user = authenticatedUserService.getCurrentUser();
        List<Project> projects = user.getRole() == Role.ADMIN
                ? projectRepository.findAll()
                : projectRepository.findAccessibleByUserId(user.getId());
        long activeProjects = projects.stream()
                .filter(project -> project.getStatus() != ProjectStatus.ARCHIVED
                        && project.getStatus() != ProjectStatus.COMPLETED)
                .count();

        List<Task> assigned = taskRepository.findByAssigneeId(user.getId());
        long completed = assigned.stream().filter(task -> task.getStatus() == TaskStatus.DONE).count();
        long overdue = assigned.stream().filter(this::isOverdue).count();
        long urgent = assigned.stream().filter(task -> task.getStatus() != TaskStatus.DONE)
                .filter(task -> task.getPriority() == TaskPriority.HIGH
                        || task.getPriority() == TaskPriority.CRITICAL)
                .count();
        return new DashboardResponse(activeProjects, assigned.size(), completed, overdue, urgent,
                percentage(assigned.size(), completed), workloadScore(assigned));
    }

    @Transactional(readOnly = true)
    public ProjectAnalyticsResponse projectAnalytics(Long projectId) {
        User user = authenticatedUserService.getCurrentUser();
        projectAccessService.requireAccessible(projectId, user);
        List<Task> tasks = taskRepository.findByProjectId(projectId);
        long completed = tasks.stream().filter(task -> task.getStatus() == TaskStatus.DONE).count();
        long overdue = tasks.stream().filter(this::isOverdue).count();

        Map<TaskStatus, Long> byStatus = new EnumMap<>(TaskStatus.class);
        Map<TaskPriority, Long> byPriority = new EnumMap<>(TaskPriority.class);
        for (TaskStatus status : TaskStatus.values()) {
            byStatus.put(status, tasks.stream().filter(task -> task.getStatus() == status).count());
        }
        for (TaskPriority priority : TaskPriority.values()) {
            byPriority.put(priority, tasks.stream().filter(task -> task.getPriority() == priority).count());
        }
        return new ProjectAnalyticsResponse(projectId, tasks.size(), completed, overdue,
                percentage(tasks.size(), completed), workloadScore(tasks), byStatus, byPriority);
    }

    private long workloadScore(List<Task> tasks) {
        return tasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.DONE)
                .mapToLong(task -> (long) task.getPriority().getWeight() * (isOverdue(task) ? 2 : 1))
                .sum();
    }

    private boolean isOverdue(Task task) {
        return task.getStatus() != TaskStatus.DONE
                && task.getDueDate() != null
                && task.getDueDate().isBefore(clock.instant());
    }

    private double percentage(long total, long completed) {
        return total == 0 ? 0.0 : Math.round(completed * 10000.0 / total) / 100.0;
    }
}
