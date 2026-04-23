# Wati Integration — Plan (v2, post lead review)

**Status:** Updated — all G1-G6 review comments addressed  
**Branch:** `dil-wati-dev`  
**First org:** DIL

---

## What Are We Building?

A WhatsApp messaging integration using Wati. Every day, the integration service:
1. Runs SQL queries (stored in Avni server) to find beneficiaries who need a message today
2. Creates a record for each message to be sent (in a new DB table)
3. Sends each WhatsApp template message via the Wati API
4. Tracks delivery, handles failures, and retries if something goes wrong

Two flows for DIL:
- **weekly_survey** — sends a reminder to fill in the weekly activity form
- **biweekly_payment** — sends a summary of approved/rejected submissions and payment info

---

## Lead Review — What Was Wrong and What We Fixed

### G1 — SQL does too much (Events, Rules, Actors all in one query)
**Review feedback:** The custom SQL query decides everything — who to message, when to message, what the rule is. Changing any of these requires editing SQL in Avni server, not just config.

**What we decided (post-discussion):** Keep the SQL approach as-is. The framework is generic — adding a new flow for any org only needs new SQL + new config rows. No Java changes needed. The only small fix: add a check for `flow.<name>.enabled` so a flow can be turned off from config without deleting SQL.

---

### G2 — No place to report permanent failures
**Review feedback:** When a message permanently fails (e.g. wrong phone number, blocked user), it just sits in the DB as `PermanentFailure`. Nobody is notified.

**Fix:** When a message hits `PermanentFailure`, we write a row to the `error_record` table (the same table Goonj and RWB already use for failure tracking). We add a config key `flow.<name>.failure_report_channel = error_record` that declares this. If in future someone wants email or Slack alerts for failures, they just change this config value — no DB or code change needed.

---

### G3 — No delivery status check
**Review feedback:** We never check whether Wati actually delivered the message after sending.

**Decision: Deferred.** We verified Wati's API documentation — Wati does **not** provide a polling endpoint to check message delivery status. The only mechanism Wati supports is webhooks (Wati calls us when delivery status changes). Implementing webhooks needs a public HTTP endpoint on the integration service, which is out of scope for phase 1. The `wati_status` column in the DB is reserved for a future webhook phase. For phase 1, `Sent` (Wati accepted the message) is the terminal success state.

> **Verified:** `PUT /api/ext/v3/conversations/{target}/status` is the only relevant Wati API found — it manages conversation inbox state (open/solved/pending), not message delivery. Source: docs.wati.io

---

### G4 — Stream scope not documented
**Review feedback:** The design brief covers Subjects (ST), Encounters (ET), and Programme streams (PR). The plan only handles Encounters but doesn't say so.

**Fix:** Explicitly stating: **phase-1 covers ET (general encounters) only.** ST and PR are out of scope for phase 1. No code or schema change is needed to support them in future — the `entity_type` column in `wati_message_request` already stores the entity type, so any stream can be added by writing a new SQL query + config rows.

---

### G5 — Template parameters never populated
**Review feedback:** The `parameters` column in `wati_message_request` is always empty. Templates that use variables (like `{{name}}`, `{{amount}}`) would send blank values.

**Fix:** DIL's templates use named variables:
- Weekly survey: `{{name}}`, `{{amount}}`
- Biweekly payment: `{{name}}`, `{{total_submissions}}`, `{{approved_count}}`, `{{payment_amount}}`, `{{rejected_count}}`

Two changes:
1. Add config key `flow.<name>.template_params = name,amount` listing the param names in order
2. Query returns param values as extra columns starting from column 3, in the same order

The worker zips config names + query values:
`[{"name": "name", "value": "Ramu"}, {"name": "amount", "value": "50"}]`

This is stored in `parameters` JSONB and passed to Wati when sending. Values that need calculation (like `total_submissions`, `approved_count`) are computed inside the SQL query itself — no Java logic needed per flow.

Also fix: `WatiHttpClient.buildParameters()` currently uses positional numbers (`"1"`, `"2"`) — update to use the actual variable names from config.

---

### G6 — Two jobs could send the same message twice
**Review feedback:** If two cron instances run at the same time (or if the server restarts mid-job), both will pick up the same `Pending` rows and send duplicate messages to the same person.

**Fix:** Add a `Sending` state. Before calling the Wati API, the worker transitions the row from `Pending → Sending` inside a DB transaction. Any concurrent job sees `Sending` and skips that row — only one job actually sends.

After the API call:
- Success → `Sent`
- Permanent failure → `PermanentFailure`
- Transient failure → `Failed` (retried later)

Safety net: if the server crashes after transitioning to `Sending` but before getting the API response, the row stays stuck in `Sending`. The error job has a recovery step: any row stuck in `Sending` for more than 1 hour gets reset back to `Pending` and retried.

---

### Fabricated References Fixed
The original plan claimed to follow patterns from the existing codebase that don't actually exist:
- `IntegrationSystemConfigCollection.getConfigsByPrefix()` — this method does not exist. Fix: use a `flow.names` config key listing flow names (e.g. `weekly_survey,biweekly_payment`). `WatiConfig.getFlowNames()` reads this key, splits on comma, returns the list.
- `wati` enum value in `IntegrationSystem.SystemType` — never existed, must be **added fresh**
- `scheduleWati()` in `IntegrationJobScheduler` — never existed, must be **added fresh** following the `scheduleGoonj()` pattern

---

### Lesser Issues Fixed
- **Locale null handling:** If locale is null or empty, skip locale lookup and use the default template directly
- **`Delivered` state dropped:** Unreachable in phase 1 without webhooks. Dropped from enum. `Sent` is the terminal success state.
- **Retry state machine:** `Failed` rows are retried by the error job — they do not go back to `Pending`, they stay `Failed` until the error job picks them up
- **`flow.<name>.enabled` check:** `WatiFlowWorker` skips a flow if this config key is `false`
- **Phone format warning:** Log a warning if the phone number doesn't start with a digit or `+`

---

## How It Works — End to End

```
5am daily cron fires → AvniWatiMainJob.execute()
  │
  ├── WatiFlowWorker.processAllFlows()
  │     For each flow in flow.names config:
  │       1. Run custom SQL query on Avni server (POST /executeQuery)
  │          → returns rows: [phone, locale, entity_id, param1, param2, ...]
  │       2. For each row:
  │          a. Skip if flow is disabled
  │          b. Warn if phone format looks wrong
  │          c. Resolve template name (locale-specific or default)
  │          d. Check cooldown — skip if this entity was messaged recently
  │          e. Create a wati_message_request row with status=Pending
  │
  └── WatiMessageSendService.sendPending()
        For each Pending row:
          1. Transition Pending → Sending (prevents double-send)
          2. Call Wati API: POST /api/v1/sendTemplateMessage
          3. If success → Sending → Sent, store wati_message_id
          4. If permanent failure (bad phone, blocked) → PermanentFailure + write ErrorRecord
          5. If transient failure (Wati 5xx, timeout) → Failed, set next_retry_time

Next morning → AvniWatiErrorJob.execute()
  ├── WatiMessageSendService.retryFailed()
  │     Picks up Failed rows where next_retry_time has passed, retries sending
  │     If attempt_count >= max_retries → PermanentFailure + write ErrorRecord
  │
  └── WatiMessageSendService.recoverStuck()
        Resets any Sending rows stuck > 1 hour back to Pending
```

---

## Data Model — New Table `wati_message_request`

One row per message. Created when the flow worker finds a beneficiary to message. Updated as the message moves through its lifecycle.

```sql
CREATE TABLE wati_message_request (
    id                    BIGSERIAL PRIMARY KEY,
    uuid                  UUID NOT NULL DEFAULT uuid_generate_v4(),
    integration_system_id BIGINT NOT NULL REFERENCES integration_system(id),
    flow_name             VARCHAR(255) NOT NULL,   -- e.g. "weekly_survey"
    entity_id             VARCHAR(500) NOT NULL,   -- Avni entity UUID (encounter, subject, etc.)
    entity_type           VARCHAR(100) NOT NULL,   -- "encounter" for phase 1; stream-agnostic for future
    phone_number          VARCHAR(20)  NOT NULL,
    template_name         VARCHAR(255) NOT NULL,   -- resolved at creation time (locale-specific or default)
    parameters            JSONB,                   -- [{"name":"name","value":"Ramu"},{"name":"amount","value":"50"}]
    locale                VARCHAR(20),             -- "te", "or", or null for default
    status                VARCHAR(50)  NOT NULL DEFAULT 'Pending',
    attempt_count         INTEGER NOT NULL DEFAULT 0,
    last_attempt_time     TIMESTAMP,
    next_retry_time       TIMESTAMP,               -- when to retry if Failed
    wati_message_id       VARCHAR(500),            -- ID returned by Wati on successful send
    wati_status           VARCHAR(100),            -- reserved for webhook phase (phase 2)
    error_message         VARCHAR(2000),
    created_date_time     TIMESTAMP NOT NULL DEFAULT NOW(),
    is_voided             BOOLEAN NOT NULL DEFAULT FALSE,
    version               INTEGER NOT NULL DEFAULT 0
);
```

**Status lifecycle:**
```
Pending → Sending → Sent                  ← happy path
Pending → Sending → Failed → (retry) → Sending → Sent
Pending → Sending → Failed → PermanentFailure  ← max retries exceeded
Pending → Sending → PermanentFailure           ← bad phone / blocked / HTTP 4xx
```

**Cooldown check** — before creating a new Pending row, check if a recent row already exists for this entity + flow:
- Active statuses checked: `Pending`, `Sending`, `Sent`
- If a row exists within the last `cooldown_days` days → skip, do not create duplicate

---

## Configuration

### Global keys (one-time per org)

| Key | What it does | Example |
|-----|-------------|---------|
| `avni_api_url` | Avni server base URL | `https://app.avniproject.org` |
| `avni_user` | Avni integration username | `int@dil.org` |
| `avni_password` | Avni password | `***` |
| `avni_auth_enabled` | Enable Avni auth | `true` |
| `wati_api_url` | Wati API base URL | `https://live-mt-server.wati.io/12345` |
| `wati_api_key` | Wati Bearer token | `eyJ...` |
| `int_env` | Environment tag | `prod` |
| `flow.names` | Comma-separated list of active flows | `weekly_survey,biweekly_payment` |
| `main.scheduled.job.cron` | When to run the main job | `30 23 * * *` (5am IST if server is UTC) |
| `error.scheduled.job.cron` | When to run the retry job | `0 1 * * *` (6:30am IST if server is UTC) |

### Per-flow keys (repeat for each flow)

| Key | What it does | Example |
|-----|-------------|---------|
| `flow.<name>.enabled` | Turn flow on/off without deleting config | `true` |
| `flow.<name>.custom_query` | Name of the SQL query in Avni server | `dil_weekly_survey_scheduled_today` |
| `flow.<name>.template_name` | Default (English) Wati template name | `<exact name from Wati dashboard>` |
| `flow.<name>.template_name.te` | Telugu template name — can be completely different from English | `<exact name from Wati dashboard>` |
| `flow.<name>.template_name.or` | Odia template name — can be completely different from English | `<exact name from Wati dashboard>` |

> **Note:** Template names are the exact names as registered in the Wati dashboard. Each locale's template name is stored as a separate config row and can be completely different — there is no naming convention required. Values to be filled in once Meta/Wati approves the templates. **TODO: Replace placeholders with actual approved template names before running setup SQL.**
| `flow.<name>.template_params` | Named params in order, matching template variables | `name,amount` |
| `flow.<name>.cooldown_days` | Min days between messages for same entity | `6` |
| `flow.<name>.max_retries` | Max send attempts before giving up | `3` |
| `flow.<name>.retry_interval_hours` | Hours to wait between retries | `24` |
| `flow.<name>.failure_report_channel` | Where to log permanent failures | `error_record` |

### How locale and template name are resolved

1. Query returns `locale` in col 1 (e.g. `"te"`)
2. If locale is null/empty → use `flow.<name>.template_name` (default)
3. If locale is set → try `flow.<name>.template_name.te` first; if not found, fall back to default

---

## Custom Query Contract

Every flow's SQL query must return columns in this exact order:

| Column | Value |
|--------|-------|
| col 0 | phone number |
| col 1 | locale (`"te"`, `"or"`, or null) |
| col 2 | entity UUID |
| col 3 | value for first template param (if any) |
| col 4 | value for second template param (if any) |
| col N | ... |

The param names come from `flow.<name>.template_params` config. The worker zips names + values automatically.

Aggregated values (like `total_submissions`, `approved_count`) are computed inside the SQL query with `COUNT`, `SUM`, `GROUP BY` — no Java processing needed.

---

## Wati API — How We Send Messages

```
POST {wati_api_url}/api/v1/sendTemplateMessage?whatsappNumber={phone}
Authorization: Bearer {wati_api_key}
Body:
{
  "template_name": "weekly_survey_reminder_te",
  "broadcast_name": "weekly_survey_reminder_te",
  "parameters": [
    {"name": "name", "value": "Ramu"},
    {"name": "amount", "value": "50"}
  ]
}
```

**Response handling:**
| Response | What it means | Action |
|----------|--------------|--------|
| HTTP 2xx + `result: true` | Message sent | Mark `Sent`, store `wati_message_id` |
| HTTP 2xx + `result: false` | Permanent problem (bad phone, blocked) | Mark `PermanentFailure`, write ErrorRecord |
| HTTP 4xx | Bad request / auth failure | Mark `PermanentFailure`, write ErrorRecord |
| HTTP 5xx / timeout | Wati server issue | Mark `Failed`, schedule retry |

---

## Files — What Needs to Change

### New files to create

| File | What it does |
|------|-------------|
| `WatiFlowConfig.java` | Holds config for one flow (query name, cooldown days, retries etc.) — ✅ already created, minor updates needed |
| `WatiMessageRequest.java` | JPA entity — one row = one message — ✅ already created |
| `WatiMessageStatus.java` | Enum: `Pending / Sending / Sent / Failed / PermanentFailure` — ✅ created, needs `Sending` added, `Delivered` removed |
| `WatiMessageRequestRepository.java` | DB queries via Spring Data JPA — ✅ already created |
| `WatiMessageRequestService.java` | Create requests, check cooldown, update status — ✅ already created |
| `WatiMessageSendService.java` | Send pending messages, handle responses, retry logic — ✅ created, needs `Pending→Sending` transition (G6) |
| `WatiFlowWorker.java` | Loop over flows, run queries, create requests — ✅ created, needs template params (G5) + enabled check (G1) |
| `WatiErrorService.java` | Writes `ErrorRecord` when a message permanently fails (G2) — **new** |
| `AvniWatiErrorJob.java` | Retry job — retries Failed messages, recovers stuck Sending rows — **new** |
| `V2_4_6__CreateWatiMessageRequestTable.sql` | DB migration to create the table — ✅ already created |

### Files to modify

| File | What changes |
|------|-------------|
| `WatiConfig.java` | Add `getFlowNames()` (reads `flow.names` key); finalize `getFlowConfig()` |
| `WatiMessageSendService.java` | Add `Pending→Sending` transition (G6); add `recoverStuck()` method |
| `WatiFlowWorker.java` | Read template params from cols 3..N (G5); check `enabled` flag (G1) |
| `WatiHttpClient.java` | Fix `buildParameters()` to use named keys instead of `"1"`, `"2"` (G5) |
| `AvniWatiMainJob.java` | ✅ already updated — orchestrates FlowWorker + SendService |
| `IntegrationSystem.java` | Add `wati` value to `SystemType` enum (fresh add) |
| `IntegrationJobScheduler.java` | Add `scheduleWati()` method (fresh add, following `scheduleGoonj()` pattern) |

### Files to keep as-is

`WatiContextProvider.java`, `WatiAvniSessionFactory.java`

---

## DIL Org Setup SQL

Run against the integration service DB (`avni_int`). Replace all `<PLACEHOLDERS>` before running.

```sql
-- ============================================================
-- Step 1: Create integration system entry for DIL
-- ============================================================
INSERT INTO public.integration_system (name, system_type, uuid, is_voided)
VALUES ('<DIL_ORG_NAME>', 'wati', uuid_generate_v4(), false);

-- ============================================================
-- Step 2: Global config (Avni + Wati credentials, schedule)
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

-- 5am IST: use '30 23 * * *' if server is UTC, '0 5 * * *' if server is IST
-- Check server timezone first: run 'timedatectl' on server
INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'main.scheduled.job.cron', '30 23 * * *', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

-- 6:30am IST (30 min after main job)
INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'error.scheduled.job.cron', '0 1 * * *', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'int_env', '<INT_ENV>', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

-- List of active flows
INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.names', 'weekly_survey,biweekly_payment', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

-- ============================================================
-- Step 3: Flow 1 — weekly_survey
-- Sends daily reminder to fill in the weekly activity form
-- Template: "Good morning {{name}}! ... INR {{amount}} will be processed..."
-- Runs at 5am IST daily; sends when encounter is scheduled for today
-- ============================================================
INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey.enabled', 'true', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey.custom_query', 'dil_weekly_survey_scheduled_today', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

-- Template names: use exact names from Wati dashboard (each locale can have a completely different name)
INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey.template_name', '<WEEKLY_SURVEY_TEMPLATE_EN>', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey.template_name.te', '<WEEKLY_SURVEY_TEMPLATE_TE>', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey.template_name.or', '<WEEKLY_SURVEY_TEMPLATE_OR>', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

-- Template variables: {{name}}, {{amount}}
INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey.template_params', 'name,amount', uuid_generate_v4()
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

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey.failure_report_channel', 'error_record', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

-- ============================================================
-- Step 4: Flow 2 — biweekly_payment
-- Sends 2-week summary of approved/rejected submissions + payment amount
-- Template: "Hello {{name}}, total: {{total_submissions}}, approved: {{approved_count}}..."
-- Schedule: TBD — run after approvals are complete
-- TODO: confirm custom query name and cron expression before running
-- ============================================================
INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.biweekly_payment.enabled', 'true', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.biweekly_payment.custom_query', '<CUSTOM_QUERY_NAME>', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

-- Template names: use exact names from Wati dashboard (each locale can have a completely different name)
INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.biweekly_payment.template_name', '<BIWEEKLY_PAYMENT_TEMPLATE_EN>', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.biweekly_payment.template_name.te', '<BIWEEKLY_PAYMENT_TEMPLATE_TE>', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.biweekly_payment.template_name.or', '<BIWEEKLY_PAYMENT_TEMPLATE_OR>', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

-- Template variables: {{name}}, {{total_submissions}}, {{approved_count}}, {{payment_amount}}, {{rejected_count}}
INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.biweekly_payment.template_params', 'name,total_submissions,approved_count,payment_amount,rejected_count', uuid_generate_v4()
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

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.biweekly_payment.failure_report_channel', 'error_record', uuid_generate_v4()
FROM integration_system WHERE name = '<DIL_ORG_NAME>';

-- ============================================================
-- Step 5: Error type for permanent message failures (G2)
-- Required for WatiErrorService to write error_record rows
-- ============================================================
INSERT INTO error_type (name, follow_up_step, uuid, is_voided, integration_system_id)
SELECT 'WatiMessagePermanentFailure', 'Terminal', uuid_generate_v4(), false, id
FROM integration_system WHERE name = '<DIL_ORG_NAME>';
```

---

## Custom Query — weekly_survey (insert into Avni server DB)

Returns: phone (col 0), locale (col 1), entity_id (col 2), name (col 3), amount (col 4)

```sql
INSERT INTO public.custom_query (uuid, name, query, organisation_id, is_voided, version,
                                 created_by_id, last_modified_by_id, created_date_time, last_modified_date_time)
VALUES (
    uuid_generate_v4(),
    'dil_weekly_survey_scheduled_today',
    'SELECT
         obs_phone.value_as_string        AS phone_number,
         u.settings->>''language''        AS locale,
         e.uuid                           AS entity_id,
         i.first_name                     AS name,
         ''50''                           AS amount
     FROM encounter e
              JOIN encounter_type et   ON et.id = e.encounter_type_id AND et.is_voided = false
              JOIN individual i        ON i.id = e.individual_id      AND i.is_voided = false
              JOIN users u             ON u.id = i.created_by_id      AND u.is_voided = false
              JOIN observation obs_phone
                   ON obs_phone.encounter_id = e.id
                  AND obs_phone.is_voided = false
                  AND obs_phone.concept_id = (
                      SELECT id FROM concept
                      WHERE name = ''Mobile Number'' AND is_voided = false
                      LIMIT 1
                  )
     WHERE et.name           = ''Self-report Survey''
       AND e.organisation_id = :orgId
       AND e.is_voided        = false
       AND e.encounter_date_time IS NULL
       AND DATE(e.earliest_visit_date_time) = CURRENT_DATE',
    :orgId, false, 0, 1, 1, now(), now()
);
```

---

## Implementation Order

Steps already done (code written, needs review adjustments):

1. ✅ DB migration — `V2_4_6__CreateWatiMessageRequestTable.sql`
2. ✅ `WatiMessageRequest.java` — JPA entity
3. ✅ `WatiMessageStatus.java` — enum (needs `Sending` added, `Delivered` removed)
4. ✅ `WatiMessageRequestRepository.java` — Spring Data JPA repo
5. ✅ `WatiFlowConfig.java` — per-flow config wrapper
6. ✅ `WatiMessageRequestService.java` — create/update requests, cooldown check
7. ✅ `WatiFlowWorker.java` — needs G5 (template params) + G1 (enabled check) updates
8. ✅ `WatiMessageSendService.java` — needs G6 (`Pending→Sending` transition) update
9. ✅ `AvniWatiMainJob.java` — orchestrates flow worker + send service

Still to do:

10. `WatiMessageStatus.java` — add `Sending`, remove `Delivered`
11. `WatiFlowWorker.java` — read cols 3..N as template params; check `enabled` flag
12. `WatiMessageSendService.java` — add `Pending→Sending` transition; add `recoverStuck()`
13. `WatiHttpClient.java` — fix `buildParameters()` to use named keys
14. `WatiErrorService.java` — new class, writes `ErrorRecord` on `PermanentFailure`
15. `AvniWatiErrorJob.java` — new class, retry job
16. `IntegrationSystem.java` — add `wati` enum value
17. `IntegrationJobScheduler.java` — add `scheduleWati()` method
18. `WatiConfig.java` — finalize `getFlowNames()` using `flow.names` key

---

## How to Verify After Implementation

1. Run `./gradlew build` — must pass cleanly
2. Run DB migration — `wati_message_request` table must be created
3. Insert DIL config rows (run setup SQL above with real values)
4. Trigger `AvniWatiMainJob` manually
5. Check `wati_message_request` — rows should appear with status `Sent`
6. Check Wati dashboard — messages should show as sent
7. Manually set one row to `Failed`, trigger error job — `attempt_count` should increment
8. Manually set two rows to `Pending` with same `entity_id` + `flow_name`, trigger job — only one message should send (cooldown check)
9. Manually set one row to `Sending`, trigger error job — row should reset to `Pending` after 1 hour check

### Add Unit and Integration tests to verify the wati module

