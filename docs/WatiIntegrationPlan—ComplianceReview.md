# Wati Integration Plan — Compliance Review

**Reviewing:** `docs/wati-integration-plan.md` @ commit `3d9004c4` on `avniproject/integration-service`
**Against:** `/Users/himeshr/Downloads/DIL-Wati-design.txt` (platform-team design brief)
**Reference patterns:** `goonj/` and `rwb/` modules in `integration-service` (local checkout)
**Reviewer stance:** Integration design architect, platform-team side. No platform-surface changes — integration-service only.

---

## Context

The plan proposes a Wati WhatsApp framework with a durable `wati_message_request` queue, per-flow config under `flow.<name>.*`, and a single custom SQL query (via `/executeQuery`) per flow that returns phone+locale+entityId. DIL is the first consumer.

The design brief requires a generic, simple, Goonj-style integration that **segregates Events, Actions, Actors, Rules, and Templates** as orthogonal, configurable axes, and provides a **message-queuing system with save / retry / response / status-tracking**. It also expects stream tracking across **ST, ET, and PR** (subjects, encounters, programme enrolments/encounters).

---

## Compliance matrix

Legend: ✅ complies · ⚠️ partial / needs change · ❌ non-compliant / missing

| # | Design requirement | Status | Notes |
|---|---|---|---|
| 1 | Simple, best-practices, generic persistence | ✅ | `wati_message_request` + JPA + Spring Data is appropriate. |
| 2 | Reference Goonj (API reads + integration_system_config) | ⚠️ | See §"Fabricated references" below — plan claims to copy a `flow.` convention from RWB that does not exist. |
| 3 | **Segregate Events, Actions, Actors, Rules, Templates** | ❌ | All five are conflated inside one opaque SQL custom_query in avni-server. Event filter (encounter type + scheduled-today), Rule (WHERE clauses deciding actionable), Actor extraction (phone concept + user.settings.language), and Template selection keying all live in the query. Changing any one dimension requires editing server-side SQL — not config. |
| 4 | Per-contact not broadcast | ✅ | `POST /sendTemplateMessage?whatsappNumber=…` per row. |
| 5 | Configurable: **Actor config** | ❌ | No declarative "who receives" config. Phone concept + locale source are hard-coded inside the custom_query. |
| 5 | Configurable: **Trigger event** | ⚠️ | Plan names a single trigger type `on_scheduled_date` but the event itself is opaque SQL. Swapping triggers = DB migration on avni-server, not an `integration_system_config` change. |
| 5 | Configurable: **Gap between nudges (cooldown)** | ✅ | `flow.<name>.cooldown_days` + existsBy… repo method. |
| 5 | Configurable: **Failure retry config** | ✅ | `max_retries`, `retry_interval_hours` per flow. |
| 5 | Configurable: **Final-failure reporting** | ❌ | Only an in-table `PermanentFailure` state + an `ErrorRecord` row. No destination/channel (email, webhook, dashboard) configured. Design explicitly lists this as a separate axis. |
| 6 | Avni-client for **external-api.yaml** (GET /api/encounters etc.) | ❌ | Plan is `/executeQuery`-only. Design lists both external REST (encounters API) and executeQuery. Skipping the REST path leaves an entire read channel unused — and locks every flow into server-side SQL. |
| 6 | Avni-client for execute-query | ✅ | Uses existing `AvniQueryRepository.invokeCustomQuery()`. |
| 7 | **Message-queue: save / retry / response / status tracking** | ✅ | `wati_message_request` with status lifecycle covers save + retry + response capture. Caveat: this is **new infrastructure** — neither Goonj nor RWB has a durable outbound queue; they rely on `ErrorRecord` + `IntegratingEntityStatus`. The plan should state this explicitly rather than implying reuse. |
| 8 | Wati-client **sendRequest** | ⚠️ | `WatiHttpClient` exists; response parsing bug (only checks HTTP status, ignores body `result` field) is called out and scheduled to fix — OK. |
| 8 | Wati-client **msg status check** | ❌ | Deferred to "future phase." Design lists it as a required capability (lines 27–28). Without it, `Delivered` is unreachable and failures after `Sent` are invisible. |
| 9 | **Stream coverage: ST(1..N), ET(1..N), PR(1..N)** | ❌ | Plan explicitly scopes to general encounters only. Design brief covers Subjects, Encounters, and Programme streams uniformly. Needs architect sign-off as a deliberate phase-1 cut, or expansion. |
| 10 | "Plan-verify-execute" | ⚠️ | Plan document itself is the "plan" deliverable — good. "Verify" is §7 but step 3 is blank; only happy-path + one retry + one cooldown case. Needs fleshing out, including permanent-failure branch and concurrent-run case. |

---

## Fabricated / incorrect references to the existing codebase

These are not design-doc compliance issues, but they misrepresent the reference patterns the plan claims to follow. Flagging because they'll cause implementation drift.

| Claim in plan | Reality in repo |
|---|---|
| `integrationSystemConfigCollection.getConfigsByPrefix("flow.")` (§4.6 pseudocode) | Method does not exist. `IntegrationSystemConfigCollection` only exposes `getConfigValue(key)` — exact-match lookup. A prefix scan must be added, likely on the collection or repository side. |
| "same pattern as RWB's `flow.` prefix in `RwbConfig.getQueryToFlowIdMap()`" (§4.3) | RWB config keys are flat: `custom_query`, `mgs_template_id`, `since_no_of_days`, `within_no_of_days`. No `flow.<name>.*` convention exists in RWB. The Wati plan is **inventing** this convention, not reusing one. |
| "same as RWB" for `AvniQueryRepository` (§2) | Correct — RWB does use `AvniQueryRepository.invokeCustomQuery()` via `AvniRwbUserNudgeRepository`. ✅ |
| "Restore `wati` enum value" in `IntegrationSystem.SystemType` | Current enum is `{ Goonj, power, lahi, Amrit, bahmni, rwb }`. No `wati` to "restore" — it must be **added**. Wording matters: nothing was ever removed. |
| "Restore `scheduleWati()`" in `IntegrationJobScheduler` | Same — must be added fresh, following the `scheduleGoonj()` template (main + error cron). |

---

## Critical design-compliance gaps (must resolve before execution)

### G1 — Separation of concerns (design doc §9, lines 9–10)

The design brief asks for Events, Actions, Actors, Rules, and Templates to be distinct, swappable configuration axes. The plan collapses Events + Rules + Actor into a single `flow.<name>.custom_query`.

**Impact:** every change to "who gets messaged" or "under what rule" requires a server-side SQL migration rather than a config row. This directly contradicts design doc lines 11–17 ("Keep config capability such that it can be changed based on user needs").

**Minimum fix that keeps the plan simple:**
- Keep the custom_query as the *event source* (returns entity_id + scheduled-date).
- Add explicit config for:
  - `flow.<name>.actor.phone_concept_uuid` (or similar) — actor extraction happens in the worker, not the query.
  - `flow.<name>.actor.locale_source` — e.g. `user_settings` vs `subject_observation`.
  - `flow.<name>.rule` — a named rule identifier (even if the v1 implementation only supports "actionable-if-row-exists"). Design explicitly calls out "Categorise Actionable or Not" (lines 45, 62, 80) as a distinct step.
- Templates already segregated via `flow.<name>.template_name[.<locale>]` — ✅ keep.

(**Post discussion note**) Make it reusable with minor changes, and not configurable 

### G2 — Final-failure reporting channel (design line 16)

`PermanentFailure` is just a DB state today. Design asks for configured reporting. Minimum: an `ErrorRecord` entry is already written — make that the documented contract, and add a config key `flow.<name>.failure_report_channel` with at least `error_record` as the only v1 value, so the axis exists for future extension (email, slack, etc.) without schema change.

### G3 — Msg status check (design lines 27–28)

Webhooks are deferred — OK for now — but a **polling** status-check path against Wati's `getMessageStatus` endpoint is a much smaller lift than webhooks (no public endpoint, no DNS, no auth surface) and satisfies the design requirement. Recommend adding a `WatiStatusCheckWorker` that runs on the error cron for rows in `Sent` state older than N hours. Without this, the `wati_status` column is permanently NULL and delivery-failure blind spots stay open.

### G4 — Stream scope (design lines 34–39)

Design covers ST + ET + PR. Plan restricts to ET. If this is a phase-1 cut, say so explicitly in the plan's Scope section with a line like "Subjects and Programme streams are out of scope for phase 1; framework must admit them without schema change." The current `wati_message_request.entity_type` column already supports this — plan should note that the framework is stream-agnostic even if flows aren't.

### G5 — Custom-query parameter gap (functional, not compliance)

The query returns only phone + locale + entity_id; `wati_message_request.parameters` JSONB is never populated. Templates with >0 params will send empty strings. Either extend the query contract (`SELECT phone, locale, entity_id, param1, param2, …`) or add a separate param-builder. This is the single biggest functional hole and not addressed in the plan at all.

### G6 — Concurrency on send worker

Two cron fires / two instances will pick up the same `Pending` batch and double-send. Needs `FOR UPDATE SKIP LOCKED` or a `Pending → Sending` state transition. Not addressed.

---

## Lesser issues carried over from the earlier code-review pass

(Still valid, kept here so the author has the full list in one place.)

- Retry state machine contradicts itself: §4.2 diagram vs §4.5 repo method vs §4.6 pseudocode disagree on whether retries sit in `Pending` or `Failed`.
- Cooldown SQL snippet uses `INTERVAL ':cooldownDays days'` — JDBC won't bind that. The Spring Data method is fine; fix the illustrative SQL.
- Locale lookup with `locale=null` will try `flow.<name>.template_name.null` before falling back. Explicit null/`en` short-circuit needed.
- Phone normalization absent — Wati requires E.164-ish; raw `obs.value_as_string` will cause silent `PermanentFailure` bursts.
- `Delivered` in the enum is unreachable without webhooks/status-check — drop until reachable, or document `Sent` as the success terminal for phase 1.
- Index `(status, integration_system_id)` doesn't support the hot send query `WHERE status='Pending' AND next_retry_time <= NOW()`. Not urgent at DIL volume, flag for later.
- `flow.<name>.enabled` is seeded but the `WatiFlowWorker` pseudocode doesn't check it.
- Section numbering: "3. Framework Design" has subsections labeled 4.1–4.7; §7 verification step 3 is blank.

---

## Files referenced during review

- Plan under review: `docs/wati-integration-plan.md` @ commit 3d9004c4d48829fdd8538df0a09a714906a38ee6 (remote-only; fetched via gh API)
- Reference config loader: `integration-data/src/main/java/org/avni_integration_service/integration_data/domain/config/IntegrationSystemConfigCollection.java` — `getConfigValue(String)` is the only lookup method
- Reference scheduler: `integrator/src/main/java/org/avni_integration_service/scheduler/IntegrationJobScheduler.java` — `scheduleGoonj()` is the template
- Goonj reference module: `goonj/src/main/java/org/avni_integration_service/goonj/` — config + worker + service + job layout
- RWB reference module: `rwb/src/main/java/org/avni_integration_service/rwb/` — closer match (single-worker, nudge-style); note the absence of `flow.*` prefix config here
- Design brief: `/Users/himeshr/Downloads/DIL-Wati-design.txt`

---

## Recommendation

**Do not approve for execution as drafted.** Resolve G1 and G3 at minimum (they're design-doc line-item non-compliances), and correct the fabricated references (they'll mislead the implementer). G2, G4, G5, G6 should be addressed or explicitly deferred with architect sign-off recorded in the plan.

Once G1/G3 are resolved, the framework shape (durable queue + per-flow config + cron-driven workers) is sound and acceptably simple.
