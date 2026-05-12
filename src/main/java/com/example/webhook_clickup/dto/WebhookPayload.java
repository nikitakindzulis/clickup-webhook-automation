package com.example.webhook_clickup.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record WebhookPayload(

        @JsonProperty("webhook_id")
        String webhookId,

        String event,

        @JsonProperty("task_id")
        String taskId,

        @JsonProperty("history_items")
        List<Map<String, Object>> historyItems

) {
}