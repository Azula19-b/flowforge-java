package com.flowforge.service;

import com.flowforge.dto.project.CreateProjectRequest;
import com.flowforge.dto.project.ProjectResponse;
import com.flowforge.entity.Project;
import com.flowforge.entity.User;
import com.flowforge.enums.ProjectStatus;
import com.flowforge.enums.Role;
import com.flowforge.exception.UnauthorizedOperationException;
import com.flowforge.repository.ProjectRepository;
import com.flowforge.repository.TaskRepository;
import com.flowforge.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private AuthenticatedUserService authenticatedUserService;
    @Mock private ProjectAccessService projectAccessService;
    @Mock private ActivityLogService activityLogService;

    private ProjectService service;

    @BeforeEach
    void setUp() {
        service = new ProjectService(projectRepository, userRepository, taskRepository,
                authenticatedUserService, projectAccessService, activityLogService);
    }

    @Test
    void managerCanCreateProjectAndIsAddedAsMember() {
        User manager = user(3L, Role.MANAGER);
        when(authenticatedUserService.getCurrentUser()).thenReturn(manager);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.setId(11L);
            project.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
            project.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
            return project;
        });

        ProjectResponse response = service.create(
                new CreateProjectRequest(" Launch ", " First release ", null));

        assertThat(response.name()).isEqualTo("Launch");
        assertThat(response.status()).isEqualTo(ProjectStatus.PLANNING);
        assertThat(response.members()).hasSize(1);
        verify(activityLogService).record(any(), any(), any(), any(), any());
    }

    @Test
    void ordinaryMemberCannotCreateProject() {
        when(authenticatedUserService.getCurrentUser()).thenReturn(user(4L, Role.MEMBER));

        assertThatThrownBy(() -> service.create(
                new CreateProjectRequest("Launch", null, ProjectStatus.ACTIVE)))
                .isInstanceOf(UnauthorizedOperationException.class);

        verify(projectRepository, never()).save(any());
    }

    private User user(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("user" + id + "@example.com");
        user.setRole(role);
        return user;
    }
}
