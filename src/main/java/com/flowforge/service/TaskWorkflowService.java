package com.flowforge.service;

import com.flowforge.enums.TaskStatus;
import com.flowforge.exception.InvalidTaskTransitionException;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
public class TaskWorkflowService {

    private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED_TRANSITIONS = transitions();

    public void validate(TaskStatus current, TaskStatus requested) {
        if (current == requested) {
            return;
        }
        if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(requested)) {
            throw new InvalidTaskTransitionException(
                    "Task status cannot change from " + current + " to " + requested);
        }
    }

    private static Map<TaskStatus, Set<TaskStatus>> transitions() {
        Map<TaskStatus, Set<TaskStatus>> transitions = new EnumMap<>(TaskStatus.class);
        transitions.put(TaskStatus.TODO, EnumSet.of(TaskStatus.IN_PROGRESS, TaskStatus.BLOCKED));
        transitions.put(TaskStatus.IN_PROGRESS,
                EnumSet.of(TaskStatus.TODO, TaskStatus.REVIEW, TaskStatus.BLOCKED));
        transitions.put(TaskStatus.REVIEW,
                EnumSet.of(TaskStatus.IN_PROGRESS, TaskStatus.DONE, TaskStatus.BLOCKED));
        transitions.put(TaskStatus.BLOCKED,
                EnumSet.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS));
        transitions.put(TaskStatus.DONE, EnumSet.of(TaskStatus.IN_PROGRESS));
        return Map.copyOf(transitions);
    }
}
