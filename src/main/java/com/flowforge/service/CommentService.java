package com.flowforge.service;

import com.flowforge.dto.comment.CommentResponse;
import com.flowforge.entity.Comment;
import com.flowforge.entity.Task;
import com.flowforge.entity.User;
import com.flowforge.enums.ActivityAction;
import com.flowforge.enums.Role;
import com.flowforge.exception.ResourceNotFoundException;
import com.flowforge.exception.UnauthorizedOperationException;
import com.flowforge.repository.CommentRepository;
import com.flowforge.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final ProjectAccessService projectAccessService;
    private final ActivityLogService activityLogService;

    @Transactional
    public CommentResponse create(Long taskId, String content) {
        User user = authenticatedUserService.getCurrentUser();
        Task task = requireTask(taskId);
        requireTaskAccess(task, user);

        Comment comment = new Comment();
        comment.setTask(task);
        comment.setAuthor(user);
        comment.setContent(content.trim());
        Comment saved = commentRepository.save(comment);
        activityLogService.record(ActivityAction.COMMENT_ADDED, user, task.getProject(), task,
                "Added a comment to task '" + task.getTitle() + "'");
        return CommentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(Long taskId) {
        User user = authenticatedUserService.getCurrentUser();
        Task task = requireTask(taskId);
        requireTaskAccess(task, user);
        return commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(CommentResponse::from)
                .toList();
    }

    @Transactional
    public CommentResponse update(Long commentId, String content) {
        User user = authenticatedUserService.getCurrentUser();
        Comment comment = requireComment(commentId);
        if (!comment.getAuthor().getId().equals(user.getId())) {
            throw new UnauthorizedOperationException("Only the comment author can edit this comment");
        }
        comment.setContent(content.trim());
        Comment saved = commentRepository.save(comment);
        activityLogService.record(ActivityAction.COMMENT_UPDATED, user,
                comment.getTask().getProject(), comment.getTask(),
                "Updated a comment on task '" + comment.getTask().getTitle() + "'");
        return CommentResponse.from(saved);
    }

    @Transactional
    public void delete(Long commentId) {
        User user = authenticatedUserService.getCurrentUser();
        Comment comment = requireComment(commentId);
        Task task = comment.getTask();
        boolean permitted = comment.getAuthor().getId().equals(user.getId())
                || user.getRole() == Role.ADMIN
                || task.getProject().getOwner().getId().equals(user.getId());
        if (!permitted) {
            throw new UnauthorizedOperationException("You cannot delete this comment");
        }
        commentRepository.delete(comment);
        activityLogService.record(ActivityAction.COMMENT_DELETED, user, task.getProject(), task,
                "Deleted a comment from task '" + task.getTitle() + "'");
    }

    private Task requireTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id " + taskId + " was not found"));
    }

    private Comment requireComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Comment with id " + commentId + " was not found"));
    }

    private void requireTaskAccess(Task task, User user) {
        if (!projectAccessService.canAccess(task.getProject(), user)) {
            throw new UnauthorizedOperationException("You do not have access to this task");
        }
    }
}
