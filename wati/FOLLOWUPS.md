# Wati module — follow-up tasks

Follow-ups from the code review of the Wati WhatsApp integration. The confirmed bugs (F1, F4–F9)
were fixed in commit `47f3e585`. The items below were deferred. F2 (`biweekly_payment` attribution)
and F3 (`weekly_survey` recipient) were reviewed and intentionally left unchanged — `last_modified_by_id`
attribution is intended, and each catchment has exactly one Pump Operator, so there is no fan-out.

Each item lists the location and a concrete fix. Checkboxes are unchecked.

---

## A. Verify before go-live (no code change — confirm against the real systems)

- [ ] **A1 — Confirm the Wati response field names (validates F4/F5).**
  `WatiHttpClient.firstReceiver` / `receiverMessageId` parse `isValidWhatsAppNumber`, `localMessageId`,
  and `errors` from the bulk `/api/v1/sendTemplateMessages` `receivers[]` array, per the Wati docs
  (https://docs.wati.io/reference/post_api-v1-sendtemplatemessages). The parsing is **defensive**: if
  the field names are wrong, it silently falls back to the old behavior (invalid numbers marked `Sent`,
  message id = broadcast name) — i.e. a wrong guess fails quiet.
  *Action:* in staging, send to a valid and a known-invalid number, log the raw `response.getBody()`,
  confirm the actual keys, and adjust the three string literals in `WatiHttpClient` if they differ.

- [ ] **A2 — Confirm the permanent-failure path end-to-end (validates F1) + settle provisioning.**
  On a DB seeded the way prod is seeded, force a permanent failure (Wati `result:false`) and confirm a
  row appears in `error_record` with type `WatiMessagePermanentFailure`.
  *Open architectural question:* a wati `integration_system` is provisioned per-org via
  `docs/dil-wati-staging-setup.sql` (it carries secrets), yet prod was described as "migrations only".
  `V2_4_8` backfills any wati system that exists when it runs; systems created later rely on setup
  Step 6. Decide the canonical provisioning path so the error type is guaranteed for every wati org.

---

## B. Robustness / efficiency (worth doing; low risk)

- [ ] **B1 — N+1 cooldown query.**
  `worker/WatiFlowWorker.processRow` calls `isInCooldown` once per row → one `existsBy…` query per row
  (`service/WatiMessageRequestService.isInCooldown`). For N candidate rows that is N DB round-trips.
  *Fix:* one query per flow returning the set of entityIds messaged within the cooldown window; load
  into a `HashSet` and test membership in memory.

- [ ] **B2 — Unbounded pending/failed fetch.**
  `service/WatiMessageRequestService.getPendingRequests` / `getFailedRequestsDueForRetry`
  (repo `WatiMessageRequestRepository`) load the entire matching set with no `LIMIT`/pagination. Under a
  Wati outage the whole backlog is materialized in one in-memory list and processed serially.
  *Fix:* page (`Pageable`) or process in bounded Top-N batches per run.

- [ ] **B3 — Remove dead code.**
  `service/WatiUserMessageErrorService`, `config/WatiSendMsgErrorType`, and
  `config/WatiEntityType.UserMessage` have zero references (the live path uses `WatiErrorService`).
  *Fix:* delete them. The four error types `V2_4_5` tried to seed are consumed only by this dead service;
  `V2_4_5` is already inert, so leave the migration as-is.

- [ ] **B4 — `max_retries` off-by-one.**
  `service/WatiMessageRequestService.markFailed` uses `attemptCount >= maxRetries` with `attemptCount`
  pre-incremented in `markSending`, giving `maxRetries` *total* attempts (= `maxRetries - 1` retries).
  *Fix:* decide the intended semantic vs the config name; if it means "retries after the first send",
  use `attemptCount > maxRetries`.

- [ ] **B5 — Error-job recovery gap.**
  `job/AvniWatiErrorJob` calls `recoverStuck()` (Sending→Pending) then `retryFailed()`, which queries
  `Failed` only — recovered rows are re-sent only by the main job (`AvniWatiMainJob.sendPending`). Fine
  while both jobs are scheduled (they are, per setup cron).
  *Fix (if the error job ever runs standalone):* also drain `Pending` from the error job, or recover
  stuck rows into `Failed`.

- [ ] **B6 — `getConfigsByPrefix` duplicate-key crash.** ⚠️ touches shared/live code
  `integration-data/.../config/IntegrationSystemConfigCollection.getConfigsByPrefix` uses 2-arg
  `Collectors.toMap`, which throws `IllegalStateException` on duplicate stripped keys. This class is
  **also used by live RWB**, so treat with care.
  *Fix:* add a merge function (keep-last + warn).

---

## C. Nice-to-have (lowest priority)

- [ ] **C1 — Per-row `IntegrationSystem` refetch.** `service/WatiMessageRequestService.createRequest`
  calls `integrationSystemRepository.findEntity(...)` per row though the system is constant for the job.
  *Fix:* fetch once per job and reuse the context entity.

- [ ] **C2 — `WatiHttpClient` builds its own `RestTemplate`/`ObjectMapper`** instead of shared beans
  (timeouts/interceptors) and `util.ObjectJsonMapper`. *Fix:* inject a configured `RestTemplate` bean;
  use `ObjectJsonMapper` (incl. in `parseParameters`).

- [ ] **C3 — Index for the Pending query.** `V2_4_6` `idx_wati_msg_req_retry` is partial
  (`WHERE status = 'Failed'`), so the Pending query's `next_retry_time` range isn't index-covered. Low
  impact (Pending rows are due immediately). *Fix (optional):* add
  `(integration_system_id, status, next_retry_time)`.

- [ ] **C4 — Test gap.** `test/.../service/WatiMessageRequestServiceTest` constructs entities directly
  and never exercises `markSending`, so it bakes in the B4 off-by-one; also `openMocks` has no teardown
  and no `@ExtendWith(MockitoExtension.class)`. *Fix:* exercise the real send path; add teardown / use
  the extension.

- [ ] **C5 — `V2_4_7` locale migration** is unscoped (no `integration_system_id`), one-way, and dead on
  the normal install path (setup SQL already writes the `_IN` forms). *Fix:* make the locale→template
  lookup tolerant of locale formats so no rename migration is ever needed.

- [ ] **C6 — `V2_4_6`** lacks `IF NOT EXISTS` on the table + indexes; two benign JPA nullability-metadata
  mismatches (`integration_system_id`, `is_voided`) where the DB enforces `NOT NULL` but the entity
  mapping doesn't declare it.

- [ ] **C7 — Altitude / dedup.** `job/AvniWatiMainJob`, `job/AvniWatiErrorJob`, and
  `config/WatiContextProvider` duplicate the Bugsnag + healthcheck + session/ThreadLocal boilerplate
  already repeated across rwb/goonj/lahi. *Fix (larger refactor):* a shared `AbstractAvniJob` base to
  remove it repo-wide.
