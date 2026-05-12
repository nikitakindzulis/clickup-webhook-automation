package com.example.webhook_clickup.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ClickUpApiClientTest {

    private static final String BASE_URL = "https://api.clickup.com/api/v2";
    private static final String TASK_ID = "task-123";
    private static final String TOKEN = "test-token";
    private static final String WORKSPACE_ID = "workspace-123";

    private MockRestServiceServer server;
    private ClickUpApiClient clickUpApiClient;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();

        server = MockRestServiceServer.createServer(restTemplate);
        clickUpApiClient = new ClickUpApiClient(restTemplate);

        ReflectionTestUtils.setField(clickUpApiClient, "token", TOKEN);
        ReflectionTestUtils.setField(clickUpApiClient, "workspaceId", WORKSPACE_ID);
    }

    @Test
    void shouldSendCorrectRequestWhenUpdatingTaskStatus() {
        server.expect(once(), requestTo(BASE_URL + "/task/" + TASK_ID))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header("Authorization", TOKEN))
                .andExpect(content().json("""
                        {
                          "status": "check task"
                        }
                        """))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        clickUpApiClient.updateTaskStatus(TASK_ID, "check task");

        server.verify();
    }

    @Test
    void shouldReturnTaskStatusFromResponse() {
        server.expect(once(), requestTo(BASE_URL + "/task/" + TASK_ID))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", TOKEN))
                .andRespond(withSuccess("""
                        {
                          "status": {
                            "status": "in progress"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        String status = clickUpApiClient.getTaskStatus(TASK_ID);

        assertEquals("in progress", status);
        server.verify();
    }

    @Test
    void shouldReturnSpacesFromResponse() {
        server.expect(once(), requestTo(BASE_URL + "/team/" + WORKSPACE_ID + "/space"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", TOKEN))
                .andRespond(withSuccess("""
                        {
                          "spaces": [
                            {
                              "id": "space-1",
                              "name": "Development"
                            },
                            {
                              "id": "space-2",
                              "name": "Support"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        var spaces = clickUpApiClient.getSpaces();

        assertEquals(2, spaces.size());

        assertEquals("space-1", spaces.get(0).get("id"));
        assertEquals("Development", spaces.get(0).get("name"));

        assertEquals("space-2", spaces.get(1).get("id"));
        assertEquals("Support", spaces.get(1).get("name"));

        server.verify();
    }

    @Test
    void shouldRetryFiveTimesWhenClickUpReturnsGatewayTimeout() {
        server.expect(times(5), requestTo(BASE_URL + "/task/" + TASK_ID))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", TOKEN))
                .andRespond(withStatus(HttpStatus.GATEWAY_TIMEOUT)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("timeout"));

        assertThrows(RuntimeException.class, () -> clickUpApiClient.getTaskStatus(TASK_ID));

        server.verify();
    }

    @Test
    void shouldFailWithoutRetryWhenClickUpReturnsUnauthorized() {
        server.expect(once(), requestTo(BASE_URL + "/task/" + TASK_ID))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", TOKEN))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("unauthorized"));

        assertThrows(RuntimeException.class, () -> clickUpApiClient.getTaskStatus(TASK_ID));

        server.verify();
    }

    @Test
    void shouldFailWithoutRetryWhenClickUpReturnsForbidden() {
        server.expect(once(), requestTo(BASE_URL + "/task/" + TASK_ID))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", TOKEN))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("forbidden"));

        assertThrows(RuntimeException.class, () -> clickUpApiClient.getTaskStatus(TASK_ID));

        server.verify();
    }
}