package com.flowforge.dto.activity;

import com.flowforge.entity.ActivityLog;
import com.flowforge.enums.ActivityAction;

import java.time.Instant;

public record ActivityResponse(
        Long id,
        ActivityAction action,
        Long userId,
        String userName,
        Long projectId,
        Long taskId,
        String description,
        Instant timestamp
) {
    public static ActivityResponse from(ActivityLog activity) {
        return new ActivityResponse(
                activity.getId(),
                activity.getActionType(),
                activity.getUser().getId(),
                activity.getUser().getFirstName() + " " + activity.getUser().getLastName(),
                activity.getProject().getId(),
                activity.getTask() == null ? null : activity.getTask().getId(),
                activity.getDescription(),
                activity.getTimestamp());
    }
}
