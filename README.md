# ClickUp Webhook Automation

A Spring Boot application that receives ClickUp webhook events and automates task workflow changes based on comments and custom field updates.

The project demonstrates backend integration with an external REST API, webhook processing, configurable business rules, error handling, retry logic, and automated tests.

## Features

- Receives ClickUp webhook events through a REST endpoint
- Validates incoming webhook requests by `webhook_id`
- Handles task comment events
- Updates task status based on comment author and current task status
- Handles changes in the `Resp. Teams` custom field
- Moves tasks between TRIAGE lists when the responsible team changes
- Handles changes in the `Move to Backlog as?` custom field
- Moves tasks from TRIAGE to Backlog or marks them as main TRIAGE tasks
- Uses ClickUp REST API for task, list, space, and custom field operations
- Includes retry logic for temporary ClickUp API failures
- Includes unit and controller tests

## Tech Stack

- Java
- Spring Boot
- Spring Web
- REST API
- ClickUp API
- JUnit 5
- Mockito
- MockMvc
- MockRestServiceServer
- Gradle

## How It Works

ClickUp sends webhook events to the application endpoint:

```http
POST /webhook
The controller validates the incoming payload and checks that the received webhook_id matches the configured expected webhook id.

After validation, the payload is passed to the service layer, where the event is analyzed and delegated to the correct handler.