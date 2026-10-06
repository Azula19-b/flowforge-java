package com.flowforge.controller;

import com.flowforge.dto.task.AssignTaskRequest;
import com.flowforge.dto.task.DueDateUpdateRequest;
import com.flowforge.dto.task.PriorityUpdateRequest;
import com.flowforge.dto.task.StatusUpdateRequest;
import com.flowforge.dto.task.TaskResponse;
import com.flowforge.dto.task.UpdateTaskRequest;
import com.flowforge.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping("/{taskId}")
    public TaskResponse get(@PathVariable Long taskId) {
        return taskService.get(taskId);
    }

    @PutMapping("/{taskId}")
    public TaskResponse update(@PathVariable Long taskId,
                               @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.update(taskId, request);
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> delete(@PathVariable Long taskId) {
        taskService.delete(taskId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{taskId}/assignee")
    public TaskResponse assign(@PathVariable Long taskId,
                               @Valid @RequestBody AssignTaskRequest request) {
        return taskService.assign(taskId, request.userId());
    }

    @PatchMapping("/{taskId}/status")
    public TaskResponse changeStatus(@PathVariable Long taskId,
                                     @Valid @RequestBody StatusUpdateRequest request) {
        return taskService.changeStatus(taskId, request.status());
    }

    @PatchMapping("/{taskId}/priority")
    public TaskResponse changePriority(@PathVariable Long taskId,
                                       @Valid @RequestBody PriorityUpdateRequest request) {
        return taskService.changePriority(taskId, request.priority());
    }

    @PatchMapping("/{taskId}/due-date")
    public TaskResponse changeDueDate(@PathVariable Long taskId,
                                      @Valid @RequestBody DueDateUpdateRequest request) {
        return taskService.changeDueDate(taskId, request.dueDate());
    }

    @GetMapping("/overdue")
    public Page<TaskResponse> overdue(
            @PageableDefault(size = 20, sort = "dueDate") Pageable pageable) {
        return taskService.overdue(pageable);
    }

    @GetMapping("/search")
    public Page<TaskResponse> search(
            @RequestParam("q") String query,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return taskService.search(query, pageable);
    }
}
