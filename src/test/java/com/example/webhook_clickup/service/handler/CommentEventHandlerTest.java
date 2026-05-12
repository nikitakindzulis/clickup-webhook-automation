package com.example.webhook_clickup.service.handler;

import com.example.webhook_clickup.client.ClickUpApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentEventHandlerTest {

    private static final String TASK_ID = "task-123";
    private static final String IGNORED_USER = "ignored_user";
    private static final String REGULAR_USER = "regular_user";
    private static final String CHECK_TASK_STATUS = "check task";

    @Mock
    private ClickUpApiClient clickUpApiClient;

    private CommentEventHandler commentEventHandler;

    @BeforeEach
    void setUp() {
        commentEventHandler = new CommentEventHandler(clickUpApiClient);

        ReflectionTestUtils.setField(
                commentEventHandler,
                "usernamesProperty",
                IGNORED_USER + ", bot_user"
        );
    }

    @Test
    void shouldIgnoreConfiguredUsername() {
        commentEventHandler.handle(TASK_ID, IGNORED_USER);

        verifyNoInteractions(clickUpApiClient);
    }

    @Test
    void shouldMoveTaskToCheckTaskForRegularUserWhenStatusCanBeChanged() {
        when(clickUpApiClient.getTaskStatus(TASK_ID)).thenReturn("in progress");

        commentEventHandler.handle(TASK_ID, REGULAR_USER);

        verify(clickUpApiClient).getTaskStatus(TASK_ID);
        verify(clickUpApiClient).updateTaskStatus(TASK_ID, CHECK_TASK_STATUS);
    }

    @Test
    void shouldNotChangeStatusWhenTaskIsOpen() {
        checkThatStatusIsNotChanged("Open");
    }

    @Test
    void shouldNotChangeStatusWhenTaskIsToDo() {
        checkThatStatusIsNotChanged("to do");
    }

    @Test
    void shouldNotChangeStatusWhenTaskIsAlreadyCheckTask() {
        checkThatStatusIsNotChanged(CHECK_TASK_STATUS);
    }

    @Test
    void shouldNotChangeStatusWhenCurrentStatusIsNull() {
        checkThatStatusIsNotChanged(null);
    }

    private void checkThatStatusIsNotChanged(String currentStatus) {
        when(clickUpApiClient.getTaskStatus(TASK_ID)).thenReturn(currentStatus);

        commentEventHandler.handle(TASK_ID, REGULAR_USER);

        verify(clickUpApiClient).getTaskStatus(TASK_ID);
        verify(clickUpApiClient, never()).updateTaskStatus(anyString(), anyString());
    }
}