package com.example.webhook_clickup.controller;

import com.example.webhook_clickup.dto.WebhookPayload;
import com.example.webhook_clickup.service.WebhookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClickUpWebhookController.class)
@TestPropertySource(properties = {
        "clickup.webhook-id=test-webhook-id"
})
class ClickUpWebhookControllerTest {

    private static final String WEBHOOK_URL = "/webhook";
    private static final String VALID_WEBHOOK_ID = "test-webhook-id";
    private static final String INVALID_WEBHOOK_ID = "wrong-webhook-id";
    private static final String TASK_ID = "task-123";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WebhookService webhookService;

    @Test
    void shouldReturnBadRequestWhenPayloadIsEmpty() throws Exception {
        mockMvc.perform(postJson("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(webhookService);
    }

    @Test
    void shouldReturnForbiddenWhenWebhookIdIsInvalid() throws Exception {
        mockMvc.perform(postJson(validPayloadWithWebhookId(INVALID_WEBHOOK_ID)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(webhookService);
    }

    @Test
    void shouldReturnOkAndCallServiceWhenWebhookIsValid() throws Exception {
        mockMvc.perform(postJson(validPayloadWithWebhookId(VALID_WEBHOOK_ID)))
                .andExpect(status().isOk());

        verify(webhookService, times(1)).processWebhook(any(WebhookPayload.class));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder postJson(String json) {
        return post(WEBHOOK_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);
    }

    private String validPayloadWithWebhookId(String webhookId) {
        return """
                {
                  "webhook_id": "%s",
                  "event": "taskUpdated",
                  "task_id": "%s",
                  "history_items": []
                }
                """.formatted(webhookId, TASK_ID);
    }
}