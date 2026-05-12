package com.example.webhook_clickup.service.handler;

import com.example.webhook_clickup.client.ClickUpApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class RespTeamsChangeHandler {

    private final ClickUpApiClient clickUpApiClient;

    @Value("${clickup.lists.triage-name}")
    private String triageListName;

    public RespTeamsChangeHandler(ClickUpApiClient clickUpApiClient) {
        this.clickUpApiClient = clickUpApiClient;
    }

    public void handle(Map<String, Object> firstItem, String taskId, Map<String, Object> customField) {
        String oldValue = asStringOrNone(firstItem.get("before"));
        String newValue = asStringOrNone(firstItem.get("after"));

        List<Map<String, Object>> options = extractOptions(customField);

        String oldFieldName = findOptionNameById(options, oldValue);
        String newFieldName = findOptionNameById(options, newValue);

        String oldSpaceId = findSpaceIdByName(oldFieldName);
        String newSpaceId = findSpaceIdByName(newFieldName);

        if (newSpaceId != null) {
            String newTriageListId = clickUpApiClient.findListIdByName(newSpaceId, triageListName);

            if (newTriageListId != null) {
                clickUpApiClient.addTaskToList(newTriageListId, taskId);
            }
        }

        if (oldSpaceId != null) {
            String oldTriageListId = clickUpApiClient.findListIdByName(oldSpaceId, triageListName);

            if (oldTriageListId != null) {
                clickUpApiClient.removeTaskFromList(oldTriageListId, taskId);
            }
        }
    }

    private String findSpaceIdByName(String name) {
        if (name == null || "None".equals(name)) {
            return null;
        }

        List<Map<String, String>> spaces = clickUpApiClient.getSpaces();

        for (Map<String, String> space : spaces) {
            if (name.equals(space.get("name"))) {
                return space.get("id");
            }
        }

        return null;
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