package com.example.webhook_clickup.service.handler;

import com.example.webhook_clickup.client.ClickUpApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class CommentEventHandler {

    private final ClickUpApiClient clickUpApiClient;

    @Value("${clickup.usernames:}")
    private String usernamesProperty;

    private static final String STATUS_CHECK_TASK = "check task";

    public CommentEventHandler(ClickUpApiClient clickUpApiClient) {
        this.clickUpApiClient = clickUpApiClient;
    }

    public void handle(String taskId, String username) {
        List<String> ignoredUsernames = parseUsernames();

        if (username != null && ignoredUsernames.contains(username)) {
            return;
        }

        String currentStatus = clickUpApiClient.getTaskStatus(taskId);

        if (currentStatus == null) {
            return;
        }

        if ("Open".equals(currentStatus)
                || "to do".equals(currentStatus)
                || STATUS_CHECK_TASK.equals(currentStatus)) {
            return;
        }

        clickUpApiClient.updateTaskStatus(taskId, STATUS_CHECK_TASK);
    }

    private List<String> parseUsernames() {
        if (usernamesProperty == null || usernamesProperty.isBlank()) {
            return List.of();
        }

        return Arrays.stream(usernamesProperty.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}