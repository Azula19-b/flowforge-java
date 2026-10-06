package com.flowforge.config;

import com.flowforge.entity.ActivityLog;
import com.flowforge.entity.Comment;
import com.flowforge.entity.Project;
import com.flowforge.entity.Task;
import com.flowforge.entity.User;
import com.flowforge.enums.ActivityAction;
import com.flowforge.enums.ProjectStatus;
import com.flowforge.enums.Role;
import com.flowforge.enums.TaskPriority;
import com.flowforge.enums.TaskStatus;
import com.flowforge.repository.ActivityLogRepository;
import com.flowforge.repository.CommentRepository;
import com.flowforge.repository.ProjectRepository;
import com.flowforge.repository.TaskRepository;
import com.flowforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.temporal.ChronoUnit;

@Configuration
@Profile("dev")
@RequiredArgsConstructor
public class DemoDataConfig {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;
    private final ActivityLogRepository activityLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Bean
    ApplicationRunner demoDataInitializer() {
        return arguments -> {
            if (userRepository.count() > 0) {
                return;
            }

            saveUser("Avery", "Admin", "admin@flowforge.dev", Role.ADMIN);
            User manager = saveUser("Morgan", "Manager", "manager@flowforge.dev", Role.MANAGER);
            User member = saveUser("Taylor", "Member", "member@flowforge.dev", Role.MEMBER);

            Project project = new Project();
            project.setName("FlowForge Launch");
            project.setDescription("Demo project created by the development profile.");
            project.setOwner(manager);
            project.setStatus(ProjectStatus.ACTIVE);
            project.getMembers().add(manager);
            project.getMembers().add(member);
            project = projectRepository.save(project);

            Task apiTask = new Task();
            apiTask.setTitle("Review public API documentation");
            apiTask.setDescription("Verify examples and endpoint descriptions before release.");
            apiTask.setProject(project);
            apiTask.setCreator(manager);
            apiTask.setAssignee(member);
            apiTask.setStatus(TaskStatus.IN_PROGRESS);
            apiTask.setPriority(TaskPriority.HIGH);
            apiTask.setDueDate(clock.instant().plus(5, ChronoUnit.DAYS));
            apiTask = taskRepository.save(apiTask);

            Task releaseTask = new Task();
            releaseTask.setTitle("Prepare release checklist");
            releaseTask.setProject(project);
            releaseTask.setCreator(manager);
            releaseTask.setAssignee(manager);
            releaseTask.setStatus(TaskStatus.TODO);
            releaseTask.setPriority(TaskPriority.CRITICAL);
            releaseTask.setDueDate(clock.instant().plus(2, ChronoUnit.DAYS));
            taskRepository.save(releaseTask);

            Comment comment = new Comment();
            comment.setTask(apiTask);
            comment.setAuthor(member);
            comment.setContent("The authentication examples are ready for review.");
            commentRepository.save(comment);

            ActivityLog activity = new ActivityLog();
            activity.setActionType(ActivityAction.PROJECT_CREATED);
            activity.setUser(manager);
            activity.setProject(project);
            activity.setDescription("Created the demo launch project");
            activityLogRepository.save(activity);

        };
    }

    private User saveUser(String firstName, String lastName, String email, Role role) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("FlowForge123!"));
        user.setRole(role);
        return userRepository.save(user);
    }
}
