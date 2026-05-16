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
```

The application validates the incoming `webhook_id`, reads the event type, and runs the required business logic.

If an external user comments on a task, the task status is changed to `check task`.

If the `Resp. Teams` custom field changes, the task is moved to the TRIAGE list of the new responsible team.

If the `Move to Backlog as?` custom field changes, the task is moved to Backlog or marked as a main TRIAGE task.

## Configuration

Create a local `application.properties` file using `application.properties.example`.

Example configuration:

```properties
clickup.api.token=YOUR_CLICKUP_API_TOKEN
clickup.webhook-id=YOUR_WEBHOOK_ID
clickup.workspace-id=YOUR_WORKSPACE_ID

clickup.usernames=internal.user1,internal.user2

clickup.fields.resp-teams-id=RESP_TEAMS_FIELD_ID
clickup.fields.move-to-backlog-as-id=MOVE_TO_BACKLOG_AS_FIELD_ID
clickup.fields.triage-main-id=TRIAGE_MAIN_FIELD_ID

clickup.lists.triage-name=TRIAGE
clickup.lists.backlog-name=Backlog
```

Do not commit real API tokens, webhook IDs, or private configuration values.

## How to Run

Clone the repository:

```bash
git clone https://github.com/your-username/your-repository-name.git
cd your-repository-name
```

Run the application:

```bash
./gradlew bootRun
```

On Windows:

```bash
gradlew.bat bootRun
```

The application starts on:

```text
http://localhost:8080
```

For local webhook testing, expose the application with ngrok:

```bash
ngrok http 8080
```

Use the generated URL as the ClickUp webhook URL:

```text
https://your-ngrok-url.ngrok-free.app/webhook
```

## Testing

Run tests:

```bash
./gradlew test
```

On Windows:

```bash
gradlew.bat test
```
