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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoveToBacklogHandlerTest {

    private static final String TASK_ID = "task-123";
    private static final String OPTION_ID = "option-id";

    private static final String TRIAGE_MAIN_FIELD_ID = "triage-main-field-id";
    private static final String TRIAGE_LIST_NAME = "TRIAGE 👋🏻";
    private static final String BACKLOG_LIST_NAME = "Backlog";

    @Mock
    private ClickUpApiClient clickUpApiClient;

    private MoveToBacklogHandler moveToBacklogHandler;

    @BeforeEach
    void setUp() {
        moveToBacklogHandler = new MoveToBacklogHandler(clickUpApiClient);

        ReflectionTestUtils.setField(moveToBacklogHandler, "triageMainFieldId", TRIAGE_MAIN_FIELD_ID);
        ReflectionTestUtils.setField(moveToBacklogHandler, "triageListName", TRIAGE_LIST_NAME);
        ReflectionTestUtils.setField(moveToBacklogHandler, "backlogListName", BACKLOG_LIST_NAME);
    }

    @Test
    void shouldMarkTaskAsTriageMainWhenHomeListIsTriage() {
        when(clickUpApiClient.getTaskSpaceAndList(TASK_ID))
                .thenReturn(taskInfo("triage-list-id", TRIAGE_LIST_NAME, "space-id"));

        moveToBacklogHandler.handle(
                firstItemWithAfterValue(OPTION_ID),
                TASK_ID,
                customFieldWithOption(OPTION_ID, "Some Team")
        );

        verify(clickUpApiClient).updateTaskCustomField(TASK_ID, TRIAGE_MAIN_FIELD_ID, true);
        verify(clickUpApiClient, never()).addTaskToList(anyString(), anyString());
        verify(clickUpApiClient, never()).removeTaskFromList(anyString(), anyString());
    }

    @Test
    void shouldMoveTaskFromTriageLocationToBacklogWhenHomeListIsNotTriage() {
        when(clickUpApiClient.getTaskSpaceAndList(TASK_ID))
                .thenReturn(taskInfo("current-list-id", "Some Other List", "space-id"));

        when(clickUpApiClient.getTaskTriageLocationId(TASK_ID))
                .thenReturn("triage-location-id");

        when(clickUpApiClient.getListSpace("triage-location-id"))
                .thenReturn("triage-space-id");

        when(clickUpApiClient.findListIdByName("triage-space-id", BACKLOG_LIST_NAME))
                .thenReturn("backlog-list-id");

        moveToBacklogHandler.handle(
                firstItemWithAfterValue(OPTION_ID),
                TASK_ID,
                customFieldWithOption(OPTION_ID, "Some Team")
        );

        verify(clickUpApiClient).addTaskToList("backlog-list-id", TASK_ID);
        verify(clickUpApiClient).removeTaskFromList("triage-location-id", TASK_ID);
        verify(clickUpApiClient, never()).updateTaskCustomField(anyString(), anyString(), any());
    }

    @Test
    void shouldDoNothingWhenAfterValueIsMissing() {
        moveToBacklogHandler.handle(
                Map.of(),
                TASK_ID,
                customFieldWithOption(OPTION_ID, "Some Team")
        );

        verify(clickUpApiClient, never()).getTaskSpaceAndList(anyString());
        verifyNoTaskMoveOrCustomFieldUpdate();
    }

    @Test
    void shouldDoNothingWhenTaskInfoIsMissing() {
        when(clickUpApiClient.getTaskSpaceAndList(TASK_ID))
                .thenReturn(null);

        moveToBacklogHandler.handle(
                firstItemWithAfterValue(OPTION_ID),
                TASK_ID,
                customFieldWithOption(OPTION_ID, "Some Team")
        );

        verify(clickUpApiClient).getTaskSpaceAndList(TASK_ID);
        verifyNoTaskMoveOrCustomFieldUpdate();
    }

    private Map<String, Object> firstItemWithAfterValue(String afterValue) {
        return Map.of("after", afterValue);
    }

    private Map<String, String> taskInfo(String listId, String listName, String spaceId) {
        return Map.of(
                "listId", listId,
                "listName", listName,
                "spaceId", spaceId
        );
    }

    private Map<String, Object> customFieldWithOption(String optionId, String optionName) {
        return Map.of(
                "type_config", Map.of(
                        "options", List.of(
                                Map.of("id", optionId, "name", optionName)
                        )
                )
        );
    }

    private void verifyNoTaskMoveOrCustomFieldUpdate() {
        verify(clickUpApiClient, never()).updateTaskCustomField(anyString(), anyString(), any());
        verify(clickUpApiClient, never()).addTaskToList(anyString(), anyString());
        verify(clickUpApiClient, never()).removeTaskFromList(anyString(), anyString());
    }
}