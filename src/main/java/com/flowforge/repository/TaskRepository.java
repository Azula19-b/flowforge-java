package com.flowforge.repository;

import com.flowforge.entity.Task;
import com.flowforge.enums.TaskPriority;
import com.flowforge.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    long countByProjectId(Long projectId);
    long countByProjectIdAndStatus(Long projectId, TaskStatus status);
    long countByAssigneeId(Long assigneeId);
    long countByAssigneeIdAndStatus(Long assigneeId, TaskStatus status);
    long countByAssigneeIdAndPriorityIn(Long assigneeId, Collection<TaskPriority> priorities);
    long countByProjectIdAndDueDateBeforeAndStatusNot(Long projectId, Instant now, TaskStatus status);
    List<Task> findByProjectId(Long projectId);
    List<Task> findByAssigneeId(Long assigneeId);

    @Query("select t from Task t where t.assignee.id = :userId " +
            "and t.dueDate < :now and t.status <> com.flowforge.enums.TaskStatus.DONE")
    Page<Task> findOverdueForAssignee(@Param("userId") Long userId,
                                      @Param("now") Instant now,
                                      Pageable pageable);
}
