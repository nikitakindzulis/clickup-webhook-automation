package com.example.webhook_clickup.service.handler;

import com.example.webhook_clickup.client.ClickUpApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RespTeamsChangeHandlerTest {

    private static final String TASK_ID = "task-123";
    private static final String TRIAGE_LIST_NAME = "TRIAGE 👋🏻";

    private static final String OLD_OPTION_ID = "old-option-id";
    private static final String NEW_OPTION_ID = "new-option-id";

    private static final String OLD_SPACE_ID = "old-space-id";
    private static final String NEW_SPACE_ID = "new-space-id";

    private static final String OLD_SPACE_NAME = "Old Space";
    private static final String NEW_SPACE_NAME = "New Space";

    private static final String OLD_TRIAGE_LIST_ID = "old-triage-list-id";
    private static final String NEW_TRIAGE_LIST_ID = "new-triage-list-id";

    @Mock
    private ClickUpApiClient clickUpApiClient;

    private RespTeamsChangeHandler respTeamsChangeHandler;

    @BeforeEach
    void setUp() {
        respTeamsChangeHandler = new RespTeamsChangeHandler(clickUpApiClient);
        ReflectionTestUtils.setField(respTeamsChangeHandler, "triageListName", TRIAGE_LIST_NAME);
    }

    @Test
    void shouldMoveTaskFromOldTriageListToNewTriageList() {
        when(clickUpApiClient.getSpaces()).thenReturn(List.of(
                space(OLD_SPACE_ID, OLD_SPACE_NAME),
                space(NEW_SPACE_ID, NEW_SPACE_NAME)
        ));

        when(clickUpApiClient.findListIdByName(NEW_SPACE_ID, TRIAGE_LIST_NAME))
                .thenReturn(NEW_TRIAGE_LIST_ID);

        when(clickUpApiClient.findListIdByName(OLD_SPACE_ID, TRIAGE_LIST_NAME))
                .thenReturn(OLD_TRIAGE_LIST_ID);

        respTeamsChangeHandler.handle(
                firstItemWithOldAndNewValue(OLD_OPTION_ID, NEW_OPTION_ID),
                TASK_ID,
                customFieldWithOptions(
                        option(OLD_OPTION_ID, OLD_SPACE_NAME),
                        option(NEW_OPTION_ID, NEW_SPACE_NAME)
                )
        );

        verify(clickUpApiClient).addTaskToList(NEW_TRIAGE_LIST_ID, TASK_ID);
        verify(clickUpApiClient).removeTaskFromList(OLD_TRIAGE_LIST_ID, TASK_ID);
    }

    @Test
    void shouldOnlyAddTaskToNewTriageListWhenOldValueIsMissing() {
        when(clickUpApiClient.getSpaces()).thenReturn(List.of(
                space(NEW_SPACE_ID, NEW_SPACE_NAME)
        ));

        when(clickUpApiClient.findListIdByName(NEW_SPACE_ID, TRIAGE_LIST_NAME))
                .thenReturn(NEW_TRIAGE_LIST_ID);

        respTeamsChangeHandler.handle(
                firstItemWithOnlyNewValue(NEW_OPTION_ID),
                TASK_ID,
                customFieldWithOptions(
                        option(NEW_OPTION_ID, NEW_SPACE_NAME)
                )
        );

        verify(clickUpApiClient).addTaskToList(NEW_TRIAGE_LIST_ID, TASK_ID);
        verify(clickUpApiClient, never()).removeTaskFromList(anyString(), anyString());
    }

    @Test
    void shouldDoNothingWhenNewSpaceIsNotFound() {
        respTeamsChangeHandler.handle(
                firstItemWithOnlyNewValue(NEW_OPTION_ID),
                TASK_ID,
                customFieldWithOptions(
                        option(NEW_OPTION_ID, "Unknown Space")
                )
        );

        when(clickUpApiClient.getSpaces()).thenReturn(List.of(
                space("space-id", "Other Space")
        ));

        respTeamsChangeHandler.handle(
                firstItemWithOnlyNewValue(NEW_OPTION_ID),
                TASK_ID,
                customFieldWithOptions(
                        option(NEW_OPTION_ID, "Unknown Space")
                )
        );

        verify(clickUpApiClient, never()).addTaskToList(anyString(), anyString());
        verify(clickUpApiClient, never()).removeTaskFromList(anyString(), anyString());
    }

    private Map<String, Object> firstItemWithOldAndNewValue(String oldValue, String newValue) {
        return Map.of(
                "before", oldValue,
                "after", newValue
        );
    }

    private Map<String, Object> firstItemWithOnlyNewValue(String newValue) {
        return Map.of("after", newValue);
    }

    private Map<String, Object> customFieldWithOptions(Map<String, Object>... options) {
        return Map.of(
                "type_config", Map.of(
                        "options", List.of(options)
                )
        );
    }

    private Map<String, Object> option(String id, String name) {
        return Map.of(
                "id", id,
                "name", name
        );
    }

    private Map<String, String> space(String id, String name) {
        return Map.of(
                "id", id,
                "name", name
        );
    }
}