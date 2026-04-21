# Wati Integration — Planning Sheet

**Status:** Draft — pending review before execution  
**Reference module:** Goonj (`goonj/`)  
**Branch:** `dil-wati-dev`  
**Current code state:** `wati/` module — all classes and packages use `Wati*` naming

---

## 1. Context & Goals

Build a **Wati WhatsApp messaging integration** in the integration service. The integration will:
- Track flows of Avni **general encounters** using the Avni external REST API (`GET /api/encounters`)
- Evaluate each encounter for actionability per configurable rules
- Create a durable message-request queue with retry and status-tracking capabilities
- Send WhatsApp template messages **per contact** (not broadcast) via Wati API directly
- Support multi-org through `integration_system_config` (same pattern as Goonj/RWB)

**Scope:** General encounters only. Subjects, programme enrolments, and programme encounters are out of scope.

DIL is the first org using this framework.

---

## 2. Reference Architecture (Goonj Patterns to Follow)

| Goonj Pattern | Wati Equivalent |
|--------------|-----------------|
| `GoonjConfig` reads from `integration_system_config` | `WatiConfig` (already exists, extend it) |
| `GoonjContextProvider` ThreadLocal | `WatiContextProvider` (already exists) |
| `ErrorRecord` / `AvniGoonjErrorService` | Reuse `ErrorRecord`; create `WatiErrorService` |
| `AvniGoonjMainJob` → workers → services → repositories | `AvniWatiMainJob` → `WatiFlowWorker` → `WatiMessageService` → repositories |
| `IntegrationJobScheduler.scheduleGoonj()` | `scheduleWati()` (restore from cleanup) |

**Key reusable repository (already exists in `avni/` module):**
- `AvniQueryRepository` — `POST /executeQuery` (same as RWB)

---

## 3. Framework Design

### 4.1 Module Structure

```
wati/
├── config/
│   ├── WatiConfig.java                  # extend: add flow config accessors
│   ├── WatiContextProvider.java         # keep as-is
│   ├── WatiAvniSessionFactory.java      # keep as-is
│   ├── WatiFlowConfig.java              # NEW: per-flow config wrapper
│   └── WatiSendMsgErrorType.java        # keep (already cleaned up)
├── domain/
│   ├── WatiMessageRequest.java          # NEW: JPA entity for message queue
│   └── WatiMessageStatus.java           # NEW: enum (Pending/Sent/Delivered/Failed/PermanentFailure)
├── repository/
│   ├── WatiMessageRequestRepository.java # NEW: JPA repository
│   └── WatiHttpClient.java              # KEEP (from cleanup work) — direct Wati API
├── service/
│   ├── WatiMessageRequestService.java    # NEW: create/update message requests, cooldown check
│   ├── WatiMessageSendService.java       # NEW: sends Pending requests via WatiHttpClient
│   └── WatiUserMessageErrorService.java  # keep
├── worker/
│   ├── WatiFlowWorker.java              # NEW: iterates configured flows, fetches entities
│   └── WatiUsersMessageWorker.java      # KEEP (now delegates to WatiMessageRequestService)
├── job/
│   └── AvniWatiMainJob.java             # KEEP, extend to orchestrate new workers
└── dto/
    └── WatiUserRequestDTO.java          # keep
```

**DB migration** (new file in `integration-data/src/main/resources/db/migration/`):
- `V2_x__CreateWatiMessageRequestTable.sql`

---

### 4.2 Data Model — New Table: `wati_message_request`

```sql
CREATE TABLE wati_message_request (
    id                      BIGSERIAL PRIMARY KEY,
    uuid                    UUID NOT NULL DEFAULT uuid_generate_v4(),
    integration_system_id   BIGINT NOT NULL REFERENCES integration_system(id),
    flow_name               VARCHAR(255) NOT NULL,      -- which flow config triggered this
    entity_id               VARCHAR(500) NOT NULL,      -- Avni entity UUID
    entity_type             VARCHAR(100) NOT NULL,      -- subject / encounter / program_enrolment / program_encounter
    phone_number            VARCHAR(20)  NOT NULL,
    template_name           VARCHAR(255) NOT NULL,
    parameters              JSONB,                      -- template parameter values
    locale                  VARCHAR(20),
    status                  VARCHAR(50)  NOT NULL DEFAULT 'Pending',
    attempt_count           INTEGER NOT NULL DEFAULT 0,
    last_attempt_time       TIMESTAMP,
    next_retry_time         TIMESTAMP,
    wati_message_id         VARCHAR(500),               -- Wati's ID (for status check)
    wati_status             VARCHAR(100),               -- delivery status from Wati
    error_message           VARCHAR(2000),
    created_date_time       TIMESTAMP NOT NULL DEFAULT NOW(),
    is_voided               BOOLEAN NOT NULL DEFAULT FALSE,
    version                 INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_wati_msg_req_status        ON wati_message_request(status, integration_system_id);
CREATE INDEX idx_wati_msg_req_entity        ON wati_message_request(entity_id, flow_name);
CREATE INDEX idx_wati_msg_req_retry         ON wati_message_request(next_retry_time) WHERE status = 'Failed';
```

**Status lifecycle:**
```
Pending → Sent → Delivered  (terminal — success)
Pending → Failed → Pending  (retry, attempt_count < max_retries)
Pending → Failed → PermanentFailure  (terminal — max retries exceeded)
Sent → Failed               (Wati confirmed non-delivery)
```

**Cooldown check** — before creating a new request, query:
```sql
SELECT 1 FROM wati_message_request
WHERE entity_id = :entityId AND flow_name = :flowName
  AND status IN ('Pending','Sent','Delivered')
  AND created_date_time > NOW() - INTERVAL ':cooldownDays days'
LIMIT 1;
```
If row found → skip (already in cooldown window).

---

### 4.3 Configuration

**Global keys (per `integration_system_config` row, applies to the whole org):**

| Key | Description | Example |
|-----|-------------|---------|
| `avni_api_url` | Avni server base URL | `https://app.avniproject.org` |
| `avni_user` | Avni integration user | `int@dil.org` |
| `avni_password` | Avni password (secret) | `***` |
| `avni_auth_enabled` | Enable Avni auth | `true` |
| `wati_api_url` | Wati API base URL | `https://live-mt-server.wati.io/12345` |
| `wati_api_key` | Wati Bearer token (secret) | `eyJ...` |
| `int_env` | Environment tag | `prod` |
| `main.scheduled.job.cron` | Main job schedule | `0 6 * * *` |
| `error.scheduled.job.cron` | Retry job schedule | `0 7 * * *` |

**Per-flow keys (prefix `flow.<name>.`):**

| Key | Description | Example |
|-----|-------------|---------|
| `flow.<name>.enabled` | Enable/disable flow | `true` |
| `flow.<name>.custom_query` | Query name in avni-server `custom_query` table | `dil_weekly_survey_scheduled_today` |
| `flow.<name>.template_name` | Default Wati template name | `weekly_survey_reminder` |
| `flow.<name>.template_name.<locale>` | Locale-specific template override | `weekly_survey_reminder_te` |
| `flow.<name>.cooldown_days` | Min days between messages | `7` |
| `flow.<name>.max_retries` | Max send attempts | `3` |
| `flow.<name>.retry_interval_hours` | Hours between retries | `24` |

**Trigger: `on_scheduled_date`** — all flows use this trigger type. Runs a named custom SQL query via `AvniQueryRepository.invokeCustomQuery()`. The query returns `phone_number`, `locale`, and `entity_id` directly — no concept config needed, the query encapsulates all field extraction logic.

Cron set to desired send time. For 5am IST: `30 23 * * *` (UTC) or `0 5 * * *` (IST) — confirm server timezone.

**Locale-specific template resolution** (already implemented in `WatiConfig.getTemplateName()`, same logic applies):
1. locale comes from query result (col 1)
2. Try `flow.<name>.template_name.<locale>` (e.g., `flow.weekly_survey.template_name.te`)
3. Fall back to `flow.<name>.template_name` (the default)

Flow names are discovered dynamically from config keys using prefix `flow.` (same pattern as RWB's `flow.` prefix in `RwbConfig.getQueryToFlowIdMap()`).

---

### 4.4 Client Layer

**`WatiHttpClient`** (already built, update to fix response parsing):
```
POST {wati_api_url}/api/v1/sendTemplateMessage?whatsappNumber={phone}
Authorization: Bearer {wati_api_key}
Body: { template_name, broadcast_name, parameters: [{name:"1", value:"..."}] }

Response parsing (current code only checks HTTP status — needs fix):
  HTTP 2xx + body.result == true  → Sent  (extract body.messageId → wati_message_id)
  HTTP 2xx + body.result == false → PermanentFailure  (invalid phone / unapproved template)
  HTTP 4xx                        → PermanentFailure  (bad request, auth failure)
  HTTP 5xx / network timeout      → RuntimeError → retry
```

---

### 4.5 Persistence Layer

**New — `WatiMessageRequest.java`** — JPA entity mapping to the `wati_message_request` table. One row = one message to send (or already sent/failed).

**New — `WatiMessageRequestRepository.java`** — Spring Data JPA interface (3–5 lines, Spring generates all SQL):
```java
public interface WatiMessageRequestRepository extends JpaRepository<WatiMessageRequest, Long> {
    // Used by send worker: fetch all Pending requests due for sending
    List<WatiMessageRequest> findByStatusAndNextRetryTimeLessThanEqual(
            WatiMessageStatus status, LocalDateTime now);

    // Used by cooldown check: is there already a recent request for this entity+flow?
    boolean existsByEntityIdAndFlowNameAndStatusInAndCreatedDateTimeAfter(
            String entityId, String flowName,
            List<WatiMessageStatus> statuses, LocalDateTime cutoff);
}
```
Spring generates the SQL at startup from the method names — no manual query writing needed.

**Reused from `integration-data` module:**
- `ErrorRecord` + `ErrorTypeRepository` — for send/retry error tracking

---

### 4.6 Worker Layer

**`WatiFlowWorker`** (new):
```
Flow discovery — scan config keys for pattern flow.<name>.custom_query:
  flowQueryMap = integrationSystemConfigCollection.getConfigsByPrefix("flow.")
      .filter(key -> key.endsWith(".custom_query"))
      .collect(flowName → queryName)
  // e.g. { "weekly_survey" → "dil_weekly_survey_scheduled_today" }
  // This avoids picking up flow.<name>.enabled, .cooldown_days etc. as flow names

For each (flowName, queryName) in flowQueryMap:
  1. WatiFlowConfig flowConfig = WatiConfig.getFlowConfig(flowName)
  2. AvniQueryRepository.invokeCustomQuery(flowConfig.customQueryName)
     → POST /executeQuery  (query filters scheduled_date = today, returns rows with phone + locale)
  3. For each row:
     a. phone = row[0], locale = row[1], entityId = row[2]  (query defines column order)
     b. Check cooldown (prevents double-send if job restarts same day)
     c. If not in cooldown → WatiMessageRequestService.createRequest(row, flowConfig)
     (no IntegratingEntityStatus needed — query is stateless)
```

**`WatiMessageSendService`** (new):
```
sendPending():
  Fetch all WatiMessageRequest where status=Pending AND next_retry_time <= NOW()
  For each request:
    1. WatiHttpClient.sendTemplateMessage(phone, templateName, params)
    2. result = SendMessageResponse

    If result.isSuccess():
      → status=Sent, wati_message_id=result.messageId, last_attempt_time=NOW()

    If result.isPermanentFailure():  (HTTP 4xx, or HTTP 200 + result:false)
      → status=PermanentFailure, error_message=result.error
      → WatiErrorService.recordError(request, PermanentFailure)
      (no retry — invalid number or unapproved template won't succeed regardless)

    If result.isTransientFailure():  (HTTP 5xx, network timeout)
      → attempt_count++
      → if attempt_count < max_retries:
           status=Failed, next_retry_time=NOW() + retry_interval_hours
        else:
           status=PermanentFailure, WatiErrorService.recordError(request, MaxRetriesExceeded)
```

**Failure reasons and classification:**
| Reason | Category | Retry |
|--------|----------|-------|
| Phone not on WhatsApp / wrong format | Permanent | No |
| Template not approved by Meta | Permanent | No |
| Wrong parameter count for template | Permanent | No |
| User blocked business number | Permanent | No |
| Wati HTTP 5xx / internal error | Transient | Yes |
| Network timeout / connection refused | Transient | Yes |
| Wati rate limit (429) | Transient | Yes, after delay |

**Webhooks:** Wati can POST delivery status updates (Delivered, Read, Failed-after-delivery) to a configured URL. Not implemented in this phase — `wati_status` column is reserved for future webhook receiver. `Sent` is the terminal success state for now.

---

### 4.7 Job Layer

**`AvniWatiMainJob`** (updated):
```
execute(WatiConfig config):
  1. WatiContextProvider.set(config)
  2. AvniHttpClient.setAvniSession(WatiAvniSessionFactory.createSession())
  3. WatiFlowWorker.processAllFlows()         ← NEW: fetch entities, create message requests
  4. WatiMessageSendWorker.sendPending()      ← NEW: send all Pending requests
  5. healthCheckService.success("wati")
  finally: clear session + context
```

**`AvniWatiErrorJob`** (new or reuse pattern from RWB):
```
execute(WatiConfig config):
  Re-run WatiMessageSendWorker for Failed requests whose next_retry_time has passed
```

**`IntegrationJobScheduler`** — add `scheduleWati()` back (after cleanup):
```java
List<IntegrationSystem> watiSystems = integrationSystemRepository.findAllBySystemType(wati);
for each system → schedule AvniWatiMainJob + AvniWatiErrorJob
```

---

## 4. DIL Org Setup SQL

This file is maintained at `wati/src/main/resources/dil_org_setup.sql`. Run against the integration service DB (`avni_int`).

Replace `<DIL_ORG_NAME>` with the actual DIL org `db_user` value before running.

```sql
-- ============================================================
-- DIL ORG SETUP
-- ============================================================
-- Run against: avni_int (integration service database)
-- Replace <DIL_ORG_NAME> with the actual DIL org db_user
-- ============================================================

-- 1. Create integration system
INSERT INTO public.integration_system (id, name, system_type, uuid, is_voided)
VALUES (DEFAULT, '<DIL_ORG_NAME>', 'wati', uuid_generate_v4(), false);

-- ============================================================
-- Global config
-- ============================================================
INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'avni_api_url', 'https://app.avniproject.org', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'avni_user', '<AVNI_USER>', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'avni_password', '<AVNI_PASSWORD>', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'avni_auth_enabled', 'true', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'wati_api_url', '<WATI_API_URL>', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'wati_api_key', '<WATI_API_KEY>', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

-- 5am IST daily: use '30 23 * * *' if server is UTC, '0 5 * * *' if server is IST
-- Verify server timezone: run 'timedatectl' on the server before setting
INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'main.scheduled.job.cron', '30 23 * * *', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'error.scheduled.job.cron', '<ERROR_CRON_EXPRESSION>', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

-- Must match avni.int.env property (e.g. 'prod', 'staging')
INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'int_env', '<INT_ENV>', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

-- ============================================================
-- Flow 1: weekly_survey
-- Trigger: on_scheduled_date — runs daily at 5am IST (cron: 30 23 * * * UTC / 0 5 * * * IST)
-- Sends when encounter "Self-report Survey" is scheduled for today
-- Templates: weekly_survey_reminder (en), weekly_survey_reminder_te (te), weekly_survey_reminder_or (or)
-- ============================================================
INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey.enabled', 'true', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

-- Name of the custom_query registered in avni-server DB
INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey.custom_query', 'dil_weekly_survey_scheduled_today', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey.template_name', 'weekly_survey_reminder', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey.template_name.te', 'weekly_survey_reminder_te', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey.template_name.or', 'weekly_survey_reminder_or', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey.cooldown_days', '6', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey.max_retries', '3', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey.retry_interval_hours', '24', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

-- ============================================================
-- Flow 2: biweekly_payment
-- Trigger: on_scheduled_date (custom query, schedule TBD)
-- Condition: send when ALL of the user's work encounters are approved or rejected
-- Schedule: TBD — depends on when approvals complete (e.g. end of day or next morning)
-- Cooldown 10 days: shorter than the 14-day cycle so next cycle can still trigger,
--   but long enough to block re-sending within the same cycle if job runs daily
-- Templates: bi-weekly_payment_summary_chlorine_refill (en),
--            bi-weekly_payment_summary_chlorine_refill_te (te),
--            bi-weekly_payment_summary_chlorine_refill_or (or)
-- TODO: confirm approval model in Avni (entity_approval_status table structure for DIL)
-- TODO: replace <CUSTOM_QUERY_NAME>, <CRON_EXPRESSION> before running
-- ============================================================
INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.biweekly_payment.enabled', 'true', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

-- Name of the custom_query registered in avni-server DB
INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.biweekly_payment.custom_query', '<CUSTOM_QUERY_NAME>', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.biweekly_payment.template_name', 'bi-weekly_payment_summary_chlorine_refill', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.biweekly_payment.template_name.te', 'bi-weekly_payment_summary_chlorine_refill_te', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.biweekly_payment.template_name.or', 'bi-weekly_payment_summary_chlorine_refill_or', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.biweekly_payment.cooldown_days', '10', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.biweekly_payment.max_retries', '3', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.biweekly_payment.retry_interval_hours', '24', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

```

### Locale Template Resolution Flow

```
WatiFlowWorker runs custom query → row[0]=phone, row[1]=locale ("te"), row[2]=entityId
  → WatiConfig.getTemplateName("weekly_survey", "te")
      → checks: flow.weekly_survey.template_name.te  → "weekly_survey_reminder_te"  ✓ use this
      → if "or": flow.weekly_survey.template_name.or → "weekly_survey_reminder_or"
      → if absent or "en": flow.weekly_survey.template_name → "weekly_survey_reminder" (fallback)
  → stores resolved template_name in WatiMessageRequest.templateName
WatiMessageSendService reads WatiMessageRequest.templateName → sends to Wati API
```

Wati templates are pre-registered in Wati dashboard per language. The integration only selects which template name to use — Wati handles the actual language rendering.

### Custom Query for `weekly_survey` (insert into avni-server DB)

```sql
-- Run against the DIL org's Avni DB
-- Replace :org_id with the DIL organisation id
INSERT INTO public.custom_query (uuid, name, query, organisation_id, is_voided, version,
                                 created_by_id, last_modified_by_id, created_date_time, last_modified_date_time)
VALUES (
    uuid_generate_v4(),
    'dil_weekly_survey_scheduled_today',
    'SELECT
         obs_phone.value_as_string   AS phone_number,
         u.settings->>''language''   AS locale,
         e.uuid                      AS entity_id
     FROM encounter e
              JOIN encounter_type et   ON et.id = e.encounter_type_id AND et.is_voided = false
              JOIN individual i        ON i.id = e.individual_id      AND i.is_voided = false
              JOIN users u             ON u.id = i.created_by_id      AND u.is_voided = false
              JOIN observation obs_phone
                   ON obs_phone.encounter_id = e.id
                  AND obs_phone.is_voided = false
                  AND obs_phone.concept_id = (
                      SELECT id FROM concept
                      WHERE name = :phone_concept AND is_voided = false
                      LIMIT 1
                  )
     WHERE et.name          = :encounter_type
       AND e.organisation_id = :org_id
       AND e.is_voided        = false
       AND e.encounter_date_time IS NULL
       AND DATE(e.earliest_visit_date_time) = CURRENT_DATE',
    :org_id, false, 0, 1, 1, now(), now()
);
```

Column order in query result must be: `phone_number` (col 0), `locale` (col 1), `entity_id` (col 2) — the worker reads by position.

---

## 5. Files to Create / Modify

### Create
| File | Module | Purpose |
|------|--------|---------|
| `WatiFlowConfig.java` | `wati` | Per-flow config wrapper (reads flow.* keys) |
| `WatiMessageRequest.java` | `wati` | JPA entity for message queue |
| `WatiMessageStatus.java` | `wati` | Enum: Pending/Sent/Delivered/Failed/PermanentFailure |
| `WatiSendResult.java` | `wati` | Result of a Wati API send call — replaces WatiSendMsgErrorType |
| `WatiMessageRequestRepository.java` | `wati` | Spring Data JPA repo |
| `WatiMessageRequestService.java` | `wati` | Create requests, cooldown check |
| `WatiMessageSendService.java` | `wati` | Send pending, update status, handle retries |
| `WatiFlowWorker.java` | `wati` | Iterate flows, fetch entities, create requests |
| `V2_x__CreateWatiMessageRequestTable.sql` | `integration-data` | DB migration |

### Modify
| File | Change |
|------|--------|
| `WatiConfig.java` | Add flow discovery via `flow.*.custom_query` key pattern (not raw `flow.` prefix — that picks up all sub-keys); add `getWatiApiUrl/Key` (already done) and other flow getters |
| `AvniWatiMainJob.java` | Orchestrate WatiFlowWorker + WatiMessageSendService |
| `IntegrationJobScheduler.java` | Restore `scheduleWati()` (after cleanup) |
| `IntegrationSystem.java` | Restore `wati` enum value (after cleanup) |
| `settings.gradle` | Restore `include 'wati'` (after cleanup) |
| `integrator/build.gradle` | Restore `project(':wati')` (after cleanup) |
### Keep As-Is (already correct)
| File | Status |
|------|--------|
| `WatiContextProvider.java` | No change needed |
| `WatiAvniSessionFactory.java` | No change needed |
| `WatiUserMessageErrorService.java` | No change needed |

### Fix During Implementation
| File | Fix needed |
|------|-----------|
| `WatiHttpClient.java` | Parse response body `result` field (not just HTTP status); extract `messageId`; distinguish permanent vs transient failures |
| `WatiSendMsgErrorType.java` | Add `PermanentFailure` variant distinct from `RuntimeError` (for HTTP 200 + result:false case) |

---

## 6. Implementation Order

1. **Cleanup** — revert dil→wati rename, restore scheduler and enum (keep functional changes)
2. **DB migration** — create `wati_message_request` table
3. **Domain + Repository** — `WatiMessageRequest`, `WatiMessageStatus`, `WatiMessageRequestRepository`
4. **Config** — `WatiFlowConfig`, extend `WatiConfig` with flow config accessors
5. **WatiMessageRequestService** — create request, cooldown check
6. **WatiFlowWorker** — iterate flows, call Avni custom query API, create requests
7. **WatiMessageSendService** — send pending requests, retry logic
8. **Update `AvniWatiMainJob`** — orchestrate all workers
9. **WatiErrorJob** — retry failed messages
10. **DB seed (DIL)** — run `wati/src/main/resources/dil_org_setup.sql` after substituting all placeholders

---

## 7. Verification

1. `./gradlew build` passes cleanly after cleanup
2. Insert DIL `integration_system` + `integration_system_config` rows

4. Trigger `AvniWatiMainJob` manually
5. Verify `wati_message_request` rows created for actionable entities
6. Verify `wati_message_request.status` transitions: Pending → Sent
7. Verify Wati delivers message (check Wati dashboard)
8. Verify retry: set a request to Failed, run error job, confirm attempt_count increments
9. Verify cooldown: confirm duplicate requests are not created within cooldown window
