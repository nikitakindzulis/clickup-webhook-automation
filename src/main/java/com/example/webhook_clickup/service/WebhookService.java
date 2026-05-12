package com.example.webhook_clickup.service;

import com.example.webhook_clickup.dto.WebhookPayload;
import com.example.webhook_clickup.service.handler.CommentEventHandler;
import com.example.webhook_clickup.service.handler.MoveToBacklogHandler;
import com.example.webhook_clickup.service.handler.RespTeamsChangeHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class WebhookService {

    private final CommentEventHandler commentEventHandler;
    private final RespTeamsChangeHandler respTeamsChangeHandler;
    private final MoveToBacklogHandler moveToBacklogHandler;

    @Value("${clickup.fields.resp-teams-id}")
    private String respTeamsFieldId;

    @Value("${clickup.fields.move-to-backlog-as-id}")
    private String moveToBacklogAsFieldId;

    private static final String EVENT_TASK_UPDATED = "taskUpdated";
    private static final String EVENT_TASK_COMMENT_POSTED = "taskCommentPosted";

    private static final String FIELD_COMMENT = "comment";
    private static final String FIELD_CUSTOM_FIELD = "custom_field";

    public WebhookService(
            CommentEventHandler commentEventHandler,
            RespTeamsChangeHandler respTeamsChangeHandler,
            MoveToBacklogHandler moveToBacklogHandler
    ) {
        this.commentEventHandler = commentEventHandler;
        this.respTeamsChangeHandler = respTeamsChangeHandler;
        this.moveToBacklogHandler = moveToBacklogHandler;
    }

    public void processWebhook(WebhookPayload payload) {
        if (payload == null) {
            return;
        }

        String event = payload.event();
        String taskId = payload.taskId();

        if (event == null || taskId == null) {
            return;
        }

        List<Map<String, Object>> historyItems = payload.historyItems();

        Map<String, Object> firstItem = null;

        if (historyItems != null && !historyItems.isEmpty()) {
            firstItem = historyItems.get(0);
        }

        String field = null;
        Integer type = null;
        String username = null;

        if (firstItem != null) {
            field = asString(firstItem.get("field"));
            type = asInteger(firstItem.get("type"));
            username = extractUsername(firstItem);
        }

        boolean isCommentCreated =
                EVENT_TASK_COMMENT_POSTED.equals(event)
                        || (EVENT_TASK_UPDATED.equals(event)
                        && firstItem != null
                        && FIELD_COMMENT.equals(field)
                        && Integer.valueOf(2).equals(type));

        if (isCommentCreated) {
            commentEventHandler.handle(taskId, username);
        }

        if (EVENT_TASK_UPDATED.equals(event) && firstItem != null) {
            handleTaskUpdated(firstItem, taskId);
        }
    }

    private void handleTaskUpdated(Map<String, Object> firstItem, String taskId) {
        String field = asString(firstItem.get("field"));

        if (!FIELD_CUSTOM_FIELD.equals(field)) {
            return;
        }

        Map<String, Object> customField = asMap(firstItem.get("custom_field"));
        String customFieldId = customField == null ? null : asString(customField.get("id"));

        if (respTeamsFieldId.equals(customFieldId)) {
            respTeamsChangeHandler.handle(firstItem, taskId, customField);
            return;
        }

        if (moveToBacklogAsFieldId.equals(customFieldId)) {
            moveToBacklogHandler.handle(firstItem, taskId, customField);
        }
    }

    private String extractUsername(Map<String, Object> item) {
        Map<String, Object> userMap = asMap(item.get("user"));

        if (userMap == null) {
            return null;
        }

        return asString(userMap.get("username"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> rawMap) {
            return (Map<String, Object>) rawMap;
        }

        return null;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Integer integerValue) {
            return integerValue;
        }

        if (value instanceof Number numberValue) {
            return numberValue.intValue();
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}