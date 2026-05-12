package com.example.webhook_clickup.service;

import com.example.webhook_clickup.dto.WebhookPayload;
import com.example.webhook_clickup.service.handler.CommentEventHandler;
import com.example.webhook_clickup.service.handler.MoveToBacklogHandler;
import com.example.webhook_clickup.service.handler.RespTeamsChangeHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    private static final String WEBHOOK_ID = "webhook-id";
    private static final String TASK_ID = "task-123";
    private static final String USERNAME = "regular_user";

    private static final String EVENT_TASK_UPDATED = "taskUpdated";
    private static final String EVENT_TASK_COMMENT_POSTED = "taskCommentPosted";

    private static final String RESP_TEAMS_FIELD_ID = "resp-teams-field-id";
    private static final String MOVE_TO_BACKLOG_FIELD_ID = "move-to-backlog-field-id";

    @Mock
    private CommentEventHandler commentEventHandler;

    @Mock
    private RespTeamsChangeHandler respTeamsChangeHandler;

    @Mock
    private MoveToBacklogHandler moveToBacklogHandler;

    @InjectMocks
    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(webhookService, "respTeamsFieldId", RESP_TEAMS_FIELD_ID);
        ReflectionTestUtils.setField(webhookService, "moveToBacklogAsFieldId", MOVE_TO_BACKLOG_FIELD_ID);
    }

    @Test
    void shouldIgnoreNullPayload() {
        webhookService.processWebhook(null);

        verifyNoHandlersWereCalled();
    }

    @Test
    void shouldIgnorePayloadWithoutEvent() {
        WebhookPayload payload = payload(null, TASK_ID, List.of());

        webhookService.processWebhook(payload);

        verifyNoHandlersWereCalled();
    }

    @Test
    void shouldIgnorePayloadWithoutTaskId() {
        WebhookPayload payload = payload(EVENT_TASK_UPDATED, null, List.of());

        webhookService.processWebhook(payload);

        verifyNoHandlersWereCalled();
    }

    @Test
    void shouldSendCommentPostedEventToCommentHandler() {
        WebhookPayload payload = payload(
                EVENT_TASK_COMMENT_POSTED,
                TASK_ID,
                List.of(commentItem(USERNAME))
        );

        webhookService.processWebhook(payload);

        verify(commentEventHandler).handle(TASK_ID, USERNAME);
        verifyNoInteractions(respTeamsChangeHandler);
        verifyNoInteractions(moveToBacklogHandler);
    }

    @Test
    void shouldSendTaskUpdatedCommentToCommentHandler() {
        Map<String, Object> firstItem = Map.of(
                "field", "comment",
                "type", 2,
                "user", Map.of("username", USERNAME)
        );

        WebhookPayload payload = payload(
                EVENT_TASK_UPDATED,
                TASK_ID,
                List.of(firstItem)
        );

        webhookService.processWebhook(payload);

        verify(commentEventHandler).handle(TASK_ID, USERNAME);
        verifyNoInteractions(respTeamsChangeHandler);
        verifyNoInteractions(moveToBacklogHandler);
    }

    @Test
    void shouldSendRespTeamsCustomFieldChangeToRespTeamsHandler() {
        Map<String, Object> customField = customField(RESP_TEAMS_FIELD_ID);
        Map<String, Object> firstItem = customFieldItem(customField);

        WebhookPayload payload = payload(
                EVENT_TASK_UPDATED,
                TASK_ID,
                List.of(firstItem)
        );

        webhookService.processWebhook(payload);

        verify(respTeamsChangeHandler).handle(firstItem, TASK_ID, customField);
        verifyNoInteractions(commentEventHandler);
        verifyNoInteractions(moveToBacklogHandler);
    }

    @Test
    void shouldSendMoveToBacklogCustomFieldChangeToMoveToBacklogHandler() {
        Map<String, Object> customField = customField(MOVE_TO_BACKLOG_FIELD_ID);
        Map<String, Object> firstItem = customFieldItem(customField);

        WebhookPayload payload = payload(
                EVENT_TASK_UPDATED,
                TASK_ID,
                List.of(firstItem)
        );

        webhookService.processWebhook(payload);

        verify(moveToBacklogHandler).handle(firstItem, TASK_ID, customField);
        verifyNoInteractions(commentEventHandler);
        verifyNoInteractions(respTeamsChangeHandler);
    }

    @Test
    void shouldIgnoreTaskUpdatedEventWhenFieldIsNotCustomField() {
        WebhookPayload payload = payload(
                EVENT_TASK_UPDATED,
                TASK_ID,
                List.of(Map.of("field", "status"))
        );

        webhookService.processWebhook(payload);

        verifyNoHandlersWereCalled();
    }

    private WebhookPayload payload(
            String event,
            String taskId,
            List<Map<String, Object>> historyItems
    ) {
        return new WebhookPayload(
                WEBHOOK_ID,
                event,
                taskId,
                historyItems
        );
    }

    private Map<String, Object> commentItem(String username) {
        return Map.of(
                "user", Map.of("username", username)
        );
    }

    private Map<String, Object> customField(String fieldId) {
        return Map.of("id", fieldId);
    }

    private Map<String, Object> customFieldItem(Map<String, Object> customField) {
        return Map.of(
                "field", "custom_field",
                "custom_field", customField
        );
    }

    private void verifyNoHandlersWereCalled() {
        verifyNoInteractions(commentEventHandler);
        verifyNoInteractions(respTeamsChangeHandler);
        verifyNoInteractions(moveToBacklogHandler);
    }
}