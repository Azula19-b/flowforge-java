package com.flowforge.repository;

import com.flowforge.entity.Project;
import com.flowforge.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("select distinct p from Project p left join p.members m " +
            "where p.owner.id = :userId or m.id = :userId")
    List<Project> findAccessibleByUserId(@Param("userId") Long userId);

    @Query("select count(distinct p) from Project p left join p.members m " +
            "where (p.owner.id = :userId or m.id = :userId) and p.status = :status")
    long countAccessibleByUserIdAndStatus(@Param("userId") Long userId,
                                          @Param("status") ProjectStatus status);
}
