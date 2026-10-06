package com.flowforge.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCommentRequest(
        @NotBlank(message = "Comment content is required")
        @Size(max = 3000, message = "Comment content cannot exceed 3000 characters")
        String content
) {
}
