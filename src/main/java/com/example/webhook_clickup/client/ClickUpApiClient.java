package com.example.webhook_clickup.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ClickUpApiClient {

    private static final Logger log = LoggerFactory.getLogger(ClickUpApiClient.class);

    private static final String BASE_URL = "https://api.clickup.com/api/v2/";
    private static final int MAX_ATTEMPTS = 5;
    private static final long RETRY_DELAY_MS = 1000L;

    private final RestTemplate restTemplate;

    @Value("${clickup.api.token}")
    private String token;

    @Value("${clickup.workspace-id}")
    private String workspaceId;

    public ClickUpApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.set("Content-Type", "application/json");
        return headers;
    }

    private Map<String, Object> makeRequest(String endpoint, HttpMethod method) {
        return makeRequest(endpoint, method, null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> makeRequest(String endpoint, HttpMethod method, Map<String, Object> body) {
        String url = BASE_URL + endpoint;
        HttpEntity<?> entity = new HttpEntity<>(body, buildHeaders());

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                ResponseEntity<Map> response = restTemplate.exchange(url, method, entity, Map.class);
                Map responseBody = response.getBody();
                return responseBody != null ? (Map<String, Object>) responseBody : new HashMap<>();

            } catch (HttpStatusCodeException e) {
                int statusCode = e.getStatusCode().value();

                log.error(
                        "ClickUp API returned HTTP {} on attempt {}/{} for {} {}. Response body: {}",
                        statusCode, attempt, MAX_ATTEMPTS, method, url, e.getResponseBodyAsString(), e
                );

                if (statusCode == 504 && attempt < MAX_ATTEMPTS) {
                    sleepBeforeRetry(attempt, method, url);
                    continue;
                }

                throw new RuntimeException(
                        "ClickUp API error: HTTP " + statusCode + " for " + method + " " + url, e
                );

            } catch (ResourceAccessException e) {
                log.error(
                        "Network error on attempt {}/{} for {} {}",
                        attempt, MAX_ATTEMPTS, method, url, e
                );

                if (attempt < MAX_ATTEMPTS) {
                    sleepBeforeRetry(attempt, method, url);
                    continue;
                }

                throw new RuntimeException(
                        "Network error while calling ClickUp: " + method + " " + url, e
                );

            } catch (Exception e) {
                log.error(
                        "Unexpected error on attempt {}/{} for {} {}",
                        attempt, MAX_ATTEMPTS, method, url, e
                );
                throw new RuntimeException(
                        "Unexpected error while calling ClickUp: " + method + " " + url, e
                );
            }
        }

        throw new RuntimeException(
                "ClickUp request failed after " + MAX_ATTEMPTS + " attempts: " + method + " " + url
        );
    }

    private void sleepBeforeRetry(int attempt, HttpMethod method, String url) {
        try {
            log.warn(
                    "Retrying ClickUp request, next attempt {}/{} for {} {}",
                    attempt + 1, MAX_ATTEMPTS, method, url
            );
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Retry interrupted for " + method + " " + url, e);
        }
    }

    public void updateTaskStatus(String taskId, String status) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status);

        makeRequest("task/" + taskId, HttpMethod.PUT, body);
    }

    public String getTaskStatus(String taskId) {
        Map<String, Object> body = makeRequest("task/" + taskId, HttpMethod.GET);

        Map<String, Object> status = asMap(body.get("status"));
        return status != null ? asString(status.get("status")) : null;
    }

    public Map<String, String> getTaskSpaceAndList(String taskId) {
        Map<String, Object> body = makeRequest("task/" + taskId, HttpMethod.GET);

        if (body.isEmpty()) {
            return null;
        }

        Map<String, Object> list = asMap(body.get("list"));
        Map<String, Object> space = asMap(body.get("space"));

        Map<String, String> result = new HashMap<>();
        result.put("listId", list != null ? asString(list.get("id")) : null);
        result.put("listName", list != null ? asString(list.get("name")) : null);
        result.put("spaceId", space != null ? asString(space.get("id")) : null);

        return result;
    }

    public void updateTaskCustomField(String taskId, String fieldId, Object value) {
        Map<String, Object> body = new HashMap<>();
        body.put("value", value);

        makeRequest("task/" + taskId + "/field/" + fieldId, HttpMethod.POST, body);
    }

    public void addTaskToList(String listId, String taskId) {
        makeRequest("list/" + listId + "/task/" + taskId, HttpMethod.POST);
    }

    public void removeTaskFromList(String listId, String taskId) {
        makeRequest("list/" + listId + "/task/" + taskId, HttpMethod.DELETE);
    }

    public List<Map<String, String>> getSpaces() {
        Map<String, Object> body = makeRequest("team/" + workspaceId + "/space", HttpMethod.GET);

        List<Map<String, String>> result = new ArrayList<>();
        Object spacesObj = body.get("spaces");

        if (spacesObj instanceof List<?> spaces) {
            for (Object spaceObj : spaces) {
                Map<String, Object> space = asMap(spaceObj);
                if (space != null) {
                    Map<String, String> item = new HashMap<>();
                    item.put("id", asString(space.get("id")));
                    item.put("name", asString(space.get("name")));
                    result.add(item);
                }
            }
        }

        return result;
    }

    public List<Map<String, String>> loadFolderlessListsWithSpace(String spaceId) {
        Map<String, Object> body = makeRequest("space/" + spaceId + "/list", HttpMethod.GET);

        List<Map<String, String>> result = new ArrayList<>();
        Object listsObj = body.get("lists");

        if (listsObj instanceof List<?> lists) {
            for (Object listObj : lists) {
                Map<String, Object> list = asMap(listObj);
                if (list != null) {
                    Map<String, String> item = new HashMap<>();
                    item.put("id", asString(list.get("id")));
                    item.put("name", asString(list.get("name")));
                    result.add(item);
                }
            }
        }

        return result;
    }

    public String getTaskTriageLocationId(String taskId) {
        Map<String, Object> body = makeRequest("task/" + taskId, HttpMethod.GET);

        if (body.isEmpty()) {
            return null;
        }

        Object locationsObj = body.get("locations");
        if (!(locationsObj instanceof List<?> locations)) {
            return null;
        }

        for (Object locationObj : locations) {
            Map<String, Object> location = asMap(locationObj);
            if (location == null) {
                continue;
            }

            String name = asString(location.get("name"));
            String id = asString(location.get("id"));

            if (name != null && name.contains("TRIAGE")) {
                return id;
            }
        }

        return null;
    }

    public String getListSpace(String listId) {
        Map<String, Object> body = makeRequest("list/" + listId, HttpMethod.GET);

        if (body.isEmpty()) {
            return null;
        }

        Map<String, Object> space = asMap(body.get("space"));
        return space != null ? asString(space.get("id")) : null;
    }

    public String findListIdByName(String spaceId, String targetListName) {
        List<Map<String, String>> lists = loadFolderlessListsWithSpace(spaceId);

        for (Map<String, String> list : lists) {
            if (targetListName.equals(list.get("name"))) {
                return list.get("id");
            }
        }

        return null;
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
}