package com.flowforge.service;

import com.flowforge.dto.activity.ActivityResponse;
import com.flowforge.entity.ActivityLog;
import com.flowforge.entity.Project;
import com.flowforge.entity.Task;
import com.flowforge.entity.User;
import com.flowforge.enums.ActivityAction;
import com.flowforge.exception.ValidationException;
import com.flowforge.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final ProjectAccessService projectAccessService;

    public void record(ActivityAction action, User user, Project project, Task task, String description) {
        ActivityLog activity = new ActivityLog();
        activity.setActionType(action);
        activity.setUser(user);
        activity.setProject(project);
        activity.setTask(task);
        activity.setDescription(description);
        activityLogRepository.save(activity);
    }

    @Transactional(readOnly = true)
    public Page<ActivityResponse> list(Long projectId, Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            throw new ValidationException("Page size cannot exceed 100");
        }
        User user = authenticatedUserService.getCurrentUser();
        projectAccessService.requireAccessible(projectId, user);
        return activityLogRepository.findByProjectIdOrderByTimestampDesc(projectId, pageable)
                .map(ActivityResponse::from);
    }
}
