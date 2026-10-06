package com.flowforge.repository;

import com.flowforge.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    Page<ActivityLog> findByProjectIdOrderByTimestampDesc(Long projectId, Pageable pageable);

    @Modifying
    @Query("update ActivityLog a set a.task = null where a.task.id = :taskId")
    void detachTask(@Param("taskId") Long taskId);
}
