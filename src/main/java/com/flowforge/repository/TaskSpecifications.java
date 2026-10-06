package com.flowforge.repository;

import com.flowforge.entity.Project;
import com.flowforge.entity.Task;
import com.flowforge.entity.User;
import com.flowforge.enums.TaskPriority;
import com.flowforge.enums.TaskStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class TaskSpecifications {

    private TaskSpecifications() {
    }

    public static Specification<Task> inProject(Long projectId) {
        return (root, query, builder) -> builder.equal(root.get("project").get("id"), projectId);
    }

    public static Specification<Task> hasStatus(TaskStatus status) {
        return status == null ? null :
                (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    public static Specification<Task> hasPriority(TaskPriority priority) {
        return priority == null ? null :
                (root, query, builder) -> builder.equal(root.get("priority"), priority);
    }

    public static Specification<Task> assignedTo(Long assigneeId) {
        return assigneeId == null ? null :
                (root, query, builder) -> builder.equal(root.get("assignee").get("id"), assigneeId);
    }

    public static Specification<Task> overdue(Boolean overdue, Instant now) {
        if (overdue == null) {
            return null;
        }
        return (root, query, builder) -> {
            var isOverdue = builder.and(
                    builder.isNotNull(root.get("dueDate")),
                    builder.lessThan(root.get("dueDate"), now),
                    builder.notEqual(root.get("status"), TaskStatus.DONE));
            return overdue ? isOverdue : builder.not(isOverdue);
        };
    }

    public static Specification<Task> textContains(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String pattern = "%" + text.trim().toLowerCase() + "%";
        return (root, query, builder) -> builder.or(
                builder.like(builder.lower(root.get("title")), pattern),
                builder.like(builder.lower(root.get("description")), pattern));
    }

    public static Specification<Task> accessibleTo(Long userId, boolean admin) {
        if (admin) {
            return null;
        }
        return (root, query, builder) -> {
            query.distinct(true);
            Join<Task, Project> project = root.join("project");
            Join<Project, User> members = project.join("members", JoinType.LEFT);
            return builder.or(
                    builder.equal(project.get("owner").get("id"), userId),
                    builder.equal(members.get("id"), userId));
        };
    }
}
