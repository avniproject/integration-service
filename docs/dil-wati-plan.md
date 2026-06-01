# DIL WATI WhatsApp Integration — Plan & Implementation Reference

**Branch:** `dil-wati-dev`  
**First org:** DIL (Pump Operator programme)  
**Status:** Implementation complete; pending deployment

---

## 1. What We Built

A WhatsApp messaging integration using Wati. Every day, the integration service:
1. Runs SQL queries (stored in Avni server DB) to find pump operators who need a message today
2. Creates a queued record for each message (`wati_message_request` table)
3. Sends each WhatsApp template message via the Wati API
4. Tracks delivery, handles failures, and retries

**Three flows for DIL:**

| Flow | Purpose | Runs |
|------|---------|------|
| `weekly_survey` | Reminder to fill in the weekly activity form (Pump Operators with incentives) | Daily 5am IST |
| `weekly_survey_rnd` | Same reminder for R&D group (no incentive amount) | Daily 5am IST |
| `biweekly_payment` | Payment summary — approved/rejected submissions + amount | 13th and 28th of each month |

---

## 2. Architecture — End to End

```
5am daily cron → AvniWatiMainJob.execute()
  │
  ├── WatiFlowWorker.processAllFlows()
  │     For each flow in config:
  │       1. Run custom SQL query on Avni server (POST /executeQuery)
  │          → returns rows: [phone, locale, entity_id, param1, param2, ...]
  │       2. Load cooldown set for this flow (one DB query, not per-row)
  │       3. For each row:
  │          a. Skip if flow disabled
  │          b. Warn if phone format looks wrong
  │          c. Resolve template name (locale-specific or default)
  │          d. Skip if entity is in cooldown
  │          e. Create wati_message_request row (status=Pending)
  │
  └── WatiMessageSendService.sendPending()
        For each Pending row (batch capped at 500):
          1. Transition Pending → Sending (prevents double-send)
          2. POST /api/v1/sendTemplateMessages to Wati
          3. Success → Sent (store wati_message_id)
          4. Bad phone / blocked → PermanentFailure + write ErrorRecord
          5. Wati 5xx / timeout → Failed (set next_retry_time)

Next run → AvniWatiErrorJob.execute()
  ├── WatiMessageSendService.retryFailed()
  │     Picks up Failed rows where next_retry_time has passed
  │     If attempt_count >= max_retries → PermanentFailure + write ErrorRecord
  │
  └── WatiMessageSendService.recoverStuck()
        Resets Sending rows stuck > 1 hour back to Pending
```

---

## 3. Data Model

### `wati_message_request` table

One row per message. Status lifecycle:

```
Pending → Sending → Sent                        ← happy path
Pending → Sending → Failed → Sending → Sent     ← retry succeeded
Pending → Sending → Failed → PermanentFailure   ← max retries exceeded
Pending → Sending → PermanentFailure            ← bad phone / blocked
```

Key columns:

| Column | Type | Notes |
|--------|------|-------|
| `flow_name` | VARCHAR | e.g. `weekly_survey` |
| `entity_id` | VARCHAR | Avni entity UUID — used for cooldown check |
| `entity_type` | VARCHAR | `encounter` for phase 1; column is stream-agnostic for future |
| `phone_number` | VARCHAR | |
| `template_name` | VARCHAR | Resolved at creation (locale-specific or default) |
| `parameters` | JSONB | `[{"name":"name","value":"Ramu"},{"name":"amount","value":"300"}]` |
| `locale` | VARCHAR | `te_IN`, `od_IN`, or null |
| `status` | VARCHAR | `Pending / Sending / Sent / Failed / PermanentFailure` |
| `attempt_count` | INTEGER | Pre-incremented in `markSending`; `>= max_retries` → PermanentFailure |
| `next_retry_time` | TIMESTAMP | When to retry if Failed |
| `wati_message_id` | VARCHAR | ID from Wati on successful send |
| `wati_status` | VARCHAR | Reserved for future webhook phase |
| `error_message` | VARCHAR | Set on failure |

---

## 4. Configuration Reference

### Global keys

| Key | Example |
|-----|---------|
| `avni_api_url` | `https://app.avniproject.org` |
| `avni_user` | `int@dil.org` |
| `avni_password` | `***` |
| `avni_auth_enabled` | `true` |
| `wati_api_url` | `https://live-mt-server.wati.io/12345` |
| `wati_api_key` | `eyJ...` |
| `int_env` | `prod` |
| `main.scheduled.job.cron` | `30 23 * * *` (5am IST when server is UTC) |
| `error.scheduled.job.cron` | `0 1 * * *` (6:30am IST) |

### Per-flow keys

| Key | Notes |
|-----|-------|
| `flow.<name>.enabled` | `true` / `false` — disables without deleting config |
| `flow.<name>.custom_query` | Name of the SQL query in Avni server |
| `flow.<name>.template_name` | Default (English) Wati template name |
| `flow.<name>.template_name.te_IN` | Telugu template — exact name from Wati dashboard |
| `flow.<name>.template_name.od_IN` | Odia template — exact name from Wati dashboard |
| `flow.<name>.template_params` | Comma-separated param names matching template variables |
| `flow.<name>.cooldown_days` | Min days between messages for same entity |
| `flow.<name>.max_retries` | Max send attempts (total, not retries after first) |
| `flow.<name>.retry_interval_hours` | Hours between retry attempts |
| `flow.<name>.failure_report_channel` | `error_record` (only v1 value; axis exists for future channels) |

> **Locale format:** Config keys use `te_IN` and `od_IN` (BCP 47 format). This matches the `locale` field in Avni user settings (`u.settings->>'locale'`). Earlier versions incorrectly used `te` and `or`.

### How locale and template name resolve

1. Query returns `locale` in col 1 (e.g. `od_IN`)
2. If locale is null/empty → use `flow.<name>.template_name` (default)
3. If locale is set → try `flow.<name>.template_name.od_IN` first; if not found, fall back to default

---

## 5. Custom Query Contract

Every flow's SQL query must return columns in this exact order:

| Position | Value |
|----------|-------|
| col 0 | phone number |
| col 1 | locale (`te_IN`, `od_IN`, or null) |
| col 2 | entity UUID (used for cooldown + deduplication) |
| col 3+ | template param values, in the same order as `flow.<name>.template_params` |

Computed values (`total_submissions`, `approved_count`, `payment_amount`) are calculated inside the SQL query — no Java processing per flow.

> **Important:** Use `u.settings->>'locale'` (not `u.settings->>'language'`) to read the locale from Avni user settings.

### DIL queries (stored in Avni prod DB)

| Query name | Flow | Returns |
|------------|------|---------|
| `dil_weekly_survey_scheduled_today` | `weekly_survey` | phone, locale, entity_id, name, amount |
| `dil_weekly_survey_rnd_scheduled_today` | `weekly_survey_rnd` | phone, locale, entity_id, name |
| `dil_biweekly_payment_summary` | `biweekly_payment` | phone, locale, entity_id, name, total_submissions, approved_count, payment_amount, rejected_count |

The `amount` and `payment_amount` columns are computed dynamically from the `Payment Rate` encounter — see [Section 7](#7-avni-setup--payment-rate-chart).

---

## 6. Wati API Integration

```
POST {wati_api_url}/api/v1/sendTemplateMessages
Authorization: Bearer {wati_api_key}
Content-Type: application/json

{
  "template_name": "weekly_survey_reminder_v2_te",
  "broadcast_name": "weekly_survey_reminder_v2_te_2026-05-31",
  "receivers": [{
    "whatsappNumber": "919876543210",
    "customParams": [
      {"name": "name",   "value": "Ramu"},
      {"name": "amount", "value": "300"}
    ]
  }]
}
```

**Response handling:**

| Response | Action |
|----------|--------|
| HTTP 2xx + `result: true` + `isValidWhatsAppNumber: true` | Mark `Sent`, store `wati_message_id` |
| HTTP 2xx + `isValidWhatsAppNumber: false` | Mark `PermanentFailure` + write ErrorRecord |
| HTTP 2xx + `result: false` | Mark `PermanentFailure` + write ErrorRecord |
| HTTP 4xx | Mark `PermanentFailure` + write ErrorRecord |
| HTTP 5xx / timeout | Mark `Failed`, schedule retry |

> **Delivery status (webhooks):** Wati does not expose a polling endpoint for delivery status. Webhooks are deferred to phase 2. `Sent` is the terminal success state for phase 1. The `wati_status` column is reserved.

---

## 7. Avni Setup — Payment Rate Chart

Payment rates (per approved submission, by state) are stored as Avni encounters so the support team can update them without any code or SQL changes.

### Avni objects created

| Object | Name |
|--------|------|
| LocationType | Account-Details |
| Location | PanIndia |
| SubjectType | PaymentRateChart (Individual) |
| EncounterType | Payment Rate |

### Payment Rate form fields

| Field | Concept UUID | Notes |
|-------|-------------|-------|
| Effective Date | `94d61f77-3da7-4533-b34b-055fc009e1c2` | Latest encounter by this date = current rates |
| Odisha Rate | `a808a7a3-83ff-4404-a319-c0cd9eff4ecd` | INR per approved submission — od_IN users |
| Andhra Pradesh Rate | `94483301-2899-4ba6-85f9-8233041015d5` | INR per approved submission — te_IN users |

### How rates are used

The `payment_rates` CTE in `dil_biweekly_payment_summary` and the `rate_lookup` CTE in `dil_weekly_survey_scheduled_today` both read from the latest non-voided `Payment Rate` encounter, filtered by `organisation_id = :org_id`, ordered by Effective Date descending. If no encounter exists, the queries fall back to 500.

**No integration service code changes needed.** Rates are fetched inside the SQL query at runtime — not in Java.

### To update rates (support team process)

1. Open Avni → find the `PaymentRateChart` subject at PanIndia
2. Add a new `Payment Rate` encounter
3. Fill in: Effective Date, Odisha Rate, Andhra Pradesh Rate
4. Done — next job run picks up the new rates automatically

---

## 8. Lead Review — Issues & Resolutions (G1–G6)

### G1 — SQL does too much
**Issue:** Event, Rule, Actor, Template selection all inside one SQL query. Changing any one requires editing server-side SQL.  
**Resolution:** Kept the SQL approach as-is for simplicity. The framework is generic — adding a new org/flow only needs new SQL + config rows, no Java changes. Added `flow.<name>.enabled` flag so flows can be disabled from config without touching SQL.

### G2 — No place to report permanent failures
**Issue:** PermanentFailure sits silently in DB.  
**Resolution:** When a message hits `PermanentFailure`, a row is written to the shared `error_record` table. Config key `flow.<name>.failure_report_channel = error_record` declares this axis (extensible to email/Slack later without schema change).

### G3 — No delivery status check
**Issue:** No way to confirm Wati actually delivered the message.  
**Resolution:** Deferred. Wati does not provide a polling endpoint — only webhooks. Webhooks require a public HTTP endpoint on the integration service (out of scope phase 1). `Sent` = Wati accepted the message. `wati_status` column reserved for phase 2 webhook integration.

### G4 — Stream scope not documented
**Issue:** Plan only handles Encounters but doesn't say so.  
**Resolution:** Phase 1 covers general encounters (ET) only. Subjects (ST) and Programme streams (PR) are out of scope for phase 1. The `entity_type` column in `wati_message_request` is already stream-agnostic — adding a new stream in future requires only a new SQL query + config rows.

### G5 — Template parameters never populated
**Issue:** `parameters` JSONB always empty; templates with `{{variables}}` would send blanks.  
**Resolution:** Added `flow.<name>.template_params = name,amount` config key. Query returns param values as extra columns starting col 3. Worker zips config names + query values: `[{"name":"name","value":"Ramu"},{"name":"amount","value":"300"}]`.

### G6 — Concurrent jobs could double-send
**Issue:** Two cron instances picking up the same Pending rows would send duplicate messages.  
**Resolution:** Added `Sending` state. Worker transitions `Pending → Sending` before calling Wati API. Concurrent job sees `Sending` and skips. Crash recovery: any row stuck in `Sending` > 1 hour is reset to `Pending` by the error job.

---

## 9. Code Review Follow-ups (B1–B6, C1–C6)

These were addressed in a separate pass post-implementation.

### Confirmed bugs fixed (already in `47f3e585`)
- **F1:** `isInCooldown` checked `Failed` status — `Failed` rows should not block new messages
- **F4–F9:** Various response parsing, retry logic, and state transition bugs

### Robustness fixes (this branch)

| Item | Change |
|------|--------|
| B1 | N+1 cooldown: load full cooldown set per flow (one query), not one query per row |
| B2 | Bound pending/failed fetch to 500 rows per run (`PageRequest`) |
| B3 | Deleted dead code: `WatiUserMessageErrorService`, `WatiSendMsgErrorType`, `WatiEntityType` |
| B4 | Clarified `max_retries` semantic: `3` = 3 total attempts (not 3 retries after first) |
| B6 | `getConfigsByPrefix` duplicate key: previously threw exception; now logs warning and continues |
| C1 | `IntegrationSystem` refetch per row → cached per job run (keyed by ID) |
| C2 | `WatiHttpClient`: replaced private `new ObjectMapper()` with shared `ObjectJsonMapper` |
| C3 | New migration `V2_4_9__AddWatiPendingIndex.sql`: composite index `(integration_system_id, status, next_retry_time)` |
| C4 | `WatiMessageRequestServiceTest`: added `@ExtendWith(MockitoExtension.class)`, added `markSending` test |
| C6 | `WatiMessageRequest`: added `@AssociationOverride` and `@AttributeOverride` for `nullable=false` on `integration_system_id` and `is_voided` |

---

## 10. Locale Fix

**Problem:** Odia users (`od_IN` locale in Avni) were receiving English messages.

**Root cause (two bugs):**
1. Custom queries used `u.settings->>'language'` — the correct key is `u.settings->>'locale'`
2. Config keys used `te` and `or` — Avni stores locales as `te_IN` and `od_IN`

**Fix applied (manually on prod DB):**
- Custom queries on Avni prod DB: `settings->>'language'` → `settings->>'locale'`
- Integration service config keys: `te` → `te_IN`, `or` → `od_IN`

No code changes needed — both fixes are DB-only and take effect on the next job run.

---

## 11. Setup SQL

See `docs/dil-wati-staging-setup.sql` for full setup SQL.

- **Section A:** Run on integration-service DB — creates integration system + all config rows
- **Section B:** Run on Avni prod DB — inserts the three custom queries (fresh install)
- **Section C:** Verification queries
- **Section D:** UPDATE statements for existing prod DB — updates `dil_weekly_survey_scheduled_today` and `dil_biweekly_payment_summary` to use dynamic payment rates from `Payment Rate` encounter

> Always use `(SELECT id FROM organisation WHERE name = '...')` — never hardcode numeric org IDs.

---

## 12. Verification Steps

1. `./gradlew build` — must pass cleanly
2. On startup: Flyway applies `V2_4_9__AddWatiPendingIndex.sql` automatically
3. Trigger `AvniWatiMainJob` manually → check `wati_message_request` rows appear with `Sent` status
4. Check Wati dashboard — messages should show as sent
5. Set one row to `Failed`, trigger error job → `attempt_count` should increment; after `max_retries` attempts → `PermanentFailure` + `error_record` row
6. Set two rows to `Pending` with same `entity_id` + `flow_name`, trigger job → only one message sent (cooldown check)
7. Set one row to `Sending`, trigger error job after 1 hour → row resets to `Pending`
8. Verify locale: Odia user should receive Odia template, Telugu user should receive Telugu template
9. Verify payment rates: create a new `Payment Rate` encounter in Avni → next run shows updated amounts
