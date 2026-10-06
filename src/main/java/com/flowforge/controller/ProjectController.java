package com.flowforge.controller;

import com.flowforge.dto.project.CompletionResponse;
import com.flowforge.dto.project.CreateProjectRequest;
import com.flowforge.dto.project.ProjectResponse;
import com.flowforge.dto.project.UpdateProjectRequest;
import com.flowforge.dto.user.UserResponse;
import com.flowforge.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(request));
    }

    @GetMapping
    public List<ProjectResponse> list() {
        return projectService.listAccessible();
    }

    @GetMapping("/{projectId}")
    public ProjectResponse get(@PathVariable Long projectId) {
        return projectService.get(projectId);
    }

    @PutMapping("/{projectId}")
    public ProjectResponse update(@PathVariable Long projectId,
                                  @Valid @RequestBody UpdateProjectRequest request) {
        return projectService.update(projectId, request);
    }

    @PatchMapping("/{projectId}/archive")
    public ProjectResponse archive(@PathVariable Long projectId) {
        return projectService.archive(projectId);
    }

    @PostMapping("/{projectId}/members/{userId}")
    public List<UserResponse> addMember(@PathVariable Long projectId, @PathVariable Long userId) {
        return projectService.addMember(projectId, userId);
    }

    @DeleteMapping("/{projectId}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long projectId, @PathVariable Long userId) {
        projectService.removeMember(projectId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{projectId}/members")
    public List<UserResponse> members(@PathVariable Long projectId) {
        return projectService.members(projectId);
    }

    @GetMapping("/{projectId}/completion")
    public CompletionResponse completion(@PathVariable Long projectId) {
        return projectService.completion(projectId);
    }
}
