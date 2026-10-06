package com.flowforge.dto.comment;

import com.flowforge.entity.Comment;
import com.flowforge.dto.user.UserResponse;

import java.time.Instant;

public record CommentResponse(
        Long id,
        String content,
        UserResponse author,
        Long taskId,
        Instant createdAt,
        Instant updatedAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(comment.getId(), comment.getContent(),
                UserResponse.from(comment.getAuthor()), comment.getTask().getId(),
                comment.getCreatedAt(), comment.getUpdatedAt());
    }
}
