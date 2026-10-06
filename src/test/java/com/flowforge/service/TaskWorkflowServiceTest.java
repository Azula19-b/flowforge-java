package com.flowforge.service;

import com.flowforge.enums.TaskStatus;
import com.flowforge.exception.InvalidTaskTransitionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskWorkflowServiceTest {

    private final TaskWorkflowService workflow = new TaskWorkflowService();

    @Test
    void allowsNormalProgressionThroughReviewToDone() {
        assertThatCode(() -> workflow.validate(TaskStatus.TODO, TaskStatus.IN_PROGRESS))
                .doesNotThrowAnyException();
        assertThatCode(() -> workflow.validate(TaskStatus.IN_PROGRESS, TaskStatus.REVIEW))
                .doesNotThrowAnyException();
        assertThatCode(() -> workflow.validate(TaskStatus.REVIEW, TaskStatus.DONE))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsSkippingDirectlyFromTodoToDone() {
        assertThatThrownBy(() -> workflow.validate(TaskStatus.TODO, TaskStatus.DONE))
                .isInstanceOf(InvalidTaskTransitionException.class)
                .hasMessageContaining("TODO")
                .hasMessageContaining("DONE");
    }

    @Test
    void allowsReopeningCompletedWork() {
        assertThatCode(() -> workflow.validate(TaskStatus.DONE, TaskStatus.IN_PROGRESS))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsIdempotentStatusUpdates() {
        assertThatCode(() -> workflow.validate(TaskStatus.BLOCKED, TaskStatus.BLOCKED))
                .doesNotThrowAnyException();
    }
}
