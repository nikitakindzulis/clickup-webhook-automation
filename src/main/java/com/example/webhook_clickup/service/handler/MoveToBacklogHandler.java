package com.example.webhook_clickup.service.handler;

import com.example.webhook_clickup.client.ClickUpApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MoveToBacklogHandler {

    private final ClickUpApiClient clickUpApiClient;

    @Value("${clickup.fields.triage-main-id}")
    private String triageMainFieldId;

    @Value("${clickup.lists.triage-name}")
    private String triageListName;

    @Value("${clickup.lists.backlog-name}")
    private String backlogListName;

    public MoveToBacklogHandler(ClickUpApiClient clickUpApiClient) {
        this.clickUpApiClient = clickUpApiClient;
    }

    public void handle(Map<String, Object> firstItem, String taskId, Map<String, Object> customField) {
        String beforeValue = asStringOrNone(firstItem.get("before"));
        String afterValue = asStringOrNone(firstItem.get("after"));

        List<Map<String, Object>> options = extractOptions(customField);

        String beforeName = findOptionNameById(options, beforeValue);
        String afterName = findOptionNameById(options, afterValue);

        if ("None".equals(afterName)) {
            return;
        }

        Map<String, String> taskInfo = clickUpApiClient.getTaskSpaceAndList(taskId);

        if (taskInfo == null) {
            return;
        }

        String currentHomeListName = taskInfo.get("listName");

        if (triageListName.equals(currentHomeListName)) {
            clickUpApiClient.updateTaskCustomField(taskId, triageMainFieldId, true);
            return;
        }

        String triageLocationId = clickUpApiClient.getTaskTriageLocationId(taskId);

        if (triageLocationId == null) {
            return;
        }

        String triageSpaceId = clickUpApiClient.getListSpace(triageLocationId);

        if (triageSpaceId == null) {
            return;
        }

        String backlogListId = clickUpApiClient.findListIdByName(triageSpaceId, backlogListName);

        if (backlogListId == null) {
            return;
        }

        clickUpApiClient.addTaskToList(backlogListId, taskId);
        clickUpApiClient.removeTaskFromList(triageLocationId, taskId);
    }

    private String findOptionNameById(List<Map<String, Object>> options, String optionId) {
        if (optionId == null || "None".equals(optionId) || options == null) {
            return "None";
        }

        for (Map<String, Object> option : options) {
            String currentId = asString(option.get("id"));

            if (optionId.equals(currentId)) {
                return asString(option.get("name"));
            }
        }

        return "None";
    }

    private List<Map<String, Object>> extractOptions(Map<String, Object> customField) {
        if (customField == null) {
            return List.of();
        }

        Map<String, Object> typeConfig = asMap(customField.get("type_config"));

        if (typeConfig == null) {
            return List.of();
        }

        Object optionsObj = typeConfig.get("options");

        if (optionsObj instanceof List<?> rawList) {
            return rawList.stream()
                    .filter(item -> item instanceof Map<?, ?>)
                    .map(item -> (Map<String, Object>) item)
                    .toList();
        }

        return List.of();
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

    private String asStringOrNone(Object value) {
        return value == null ? "None" : String.valueOf(value);
    }
}