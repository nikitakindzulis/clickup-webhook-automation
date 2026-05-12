package com.example.webhook_clickup.controller;

import com.example.webhook_clickup.dto.WebhookPayload;
import com.example.webhook_clickup.service.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
public class ClickUpWebhookController {

    private static final Logger log = LoggerFactory.getLogger(ClickUpWebhookController.class);

    private final WebhookService webhookService;

    @Value("${clickup.webhook-id}")
    private String expectedWebhookId;

    public ClickUpWebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestBody(required = false) WebhookPayload payload) {

        if (payload == null) {
            log.warn("Payload is null");
            return ResponseEntity.badRequest().body("Payload is missing");
        }

        if (payload.webhookId() == null || payload.taskId() == null) {
            log.warn("Invalid payload: webhook_id or task_id is missing");
            return ResponseEntity.badRequest().body("Invalid payload");
        }

        String receivedWebhookId = payload.webhookId();

        if (!receivedWebhookId.equals(expectedWebhookId)) {
            log.warn("Forbidden: invalid webhook_id received");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("forbidden");
        }

        log.info("Webhook accepted, processing started");

        webhookService.processWebhook(payload);

        return ResponseEntity.ok("ok");
    }
}