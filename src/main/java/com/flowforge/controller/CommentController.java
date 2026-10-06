package com.flowforge.controller;

import com.flowforge.dto.comment.CommentResponse;
import com.flowforge.dto.comment.CreateCommentRequest;
import com.flowforge.dto.comment.UpdateCommentRequest;
import com.flowforge.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/tasks/{taskId}/comments")
    public ResponseEntity<CommentResponse> create(@PathVariable Long taskId,
                                                   @Valid @RequestBody CreateCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.create(taskId, request.content()));
    }

    @GetMapping("/tasks/{taskId}/comments")
    public List<CommentResponse> list(@PathVariable Long taskId) {
        return commentService.list(taskId);
    }

    @PutMapping("/comments/{commentId}")
    public CommentResponse update(@PathVariable Long commentId,
                                  @Valid @RequestBody UpdateCommentRequest request) {
        return commentService.update(commentId, request.content());
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable Long commentId) {
        commentService.delete(commentId);
        return ResponseEntity.noContent().build();
    }
}
