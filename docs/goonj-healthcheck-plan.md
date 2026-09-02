# Fix Goonj health-check flapping and false negatives

## Context / Problem

Support reported frequent, confusing Freshdesk tickets from the Goonj health checks — repeated
"down" then "up" pairs, often with nothing fixed in between. The work here is to reduce spurious
ticket creation and resolve the false negatives behind it.

Three Healthchecks.io checks exist for Goonj:

| Check | What it reports | Who acts on it |
|---|---|---|
| `goonj` | Liveness — did the job run at all | Whoever owns the deployment |
| `goonj-integration` | Count of `Internal` follow-up-step errors | **Us** — integration service / Avni config |
| `goonj-salesforce` | Count of `External` follow-up-step errors | **Goonj** — Salesforce data / config |

The counts are always **per follow-up step, never an overall total**. Each check reads exactly one
bucket, which is what separates "we can fix this" from "Goonj has to fix this". `Terminal` and
`Process` records drive neither check.

The evidence that this is a counting problem, not an outage: in June 2026 `goonj-integration`
logged 9 downtimes / 5 days 8 hours / 82.22% uptime, while `goonj` was **green the whole month**.
The job never crashed. Every ticket support received came from the two error checks.

Goonj is also the only one of seven integrations using Healthchecks.io for error counts at all —
Power, Lahi, Bahmni, Amrit, RWB and Wati use plain liveness only.

**Goal:** make the two error checks report *current state* — "are any records still failing?" — so
a real problem produces one down ticket, stays red while unfixed, and produces one up ticket when
genuinely resolved.

**Decision taken:** code fix only. No Healthchecks.io dashboard changes in this scope.

---

## Root Cause

The two error checks don't report whether anything is broken. Each run they ask *"were any error
rows written since the last bookmark?"*, then advance the bookmark. Consecutive runs therefore read
different, non-overlapping slices of time: a record that is still unsynced falls behind the new
window start, and the next run reports "all clear". That is the flapping.

All four defects sit in `AvniGoonjErrorRecordsWorker.evaluateNewErrors()`
(`goonj/src/main/java/org/avni_integration_service/goonj/worker/AvniGoonjErrorRecordsWorker.java:117-130`):

1. **Watermark blindness.** The main job only fetches records newer than the entity's
   `read_upto_date_time`. Once it passes a failing record nothing re-logs it, so the count is 0 →
   green while the row is still open in `error_record`.
2. **The error job eats the window silently.** `AvniGoonjFullErrorJob.java:52-58` calls
   `evaluateNewErrors()` and discards the result — advancing the bookmark without ever pinging.
3. **Cross-system bookmark race.** It uses `errorRecordLogRepository.findTopByOrderByLoggedAtDesc()`
   (`ErrorRecordLogRepository.java:34`) — global, with no integration-system filter. All seven
   integrations share `error_record_log` and run in one app, so a Wati or RWB error can drag Goonj's
   bookmark past Goonj's own errors. It also reads the bookmark via the `@Deprecated` unscoped
   `findByEntityType(...)` instead of the system-scoped `find(...)`.
4. **Silent crash on empty table.** With `error_record_log` empty, that same call returns null →
   NPE → swallowed by `catch (Exception ignore)` in `AvniGoonjMainJob.execute()` → no ping at all.

Replacing the time window with a backlog count removes the bookmark entirely, so all four disappear
together rather than needing four separate patches.

---

## Proposed Solution

`error_record` is already a self-maintaining answer to "what is currently broken":

- **On failure** — `AvniGoonjErrorService.saveGoonjError` (`:71-92`) creates the row plus an
  `error_record_log` entry. A repeat either bumps `logged_at` on the last log (identical error type
  *and* message) or appends a new log row.
- **On success, however much later** — `successfullyProcessedGoonjEntity` (`:103-107`) **deletes**
  the record. There is no status column and no "completed" state. `errorRecordLogs` is
  `CascadeType.ALL` with `orphanRemoval = true` (`ErrorRecord.java:30`), so the logs go with it.

A row therefore exists precisely while something is unsynced. Count those rows instead of counting
log writes in a time window, bucketed by follow-up step:

- scoped to this integration system
- **not** filtered on `processingDisabled` (see Correctness Considerations)
- **first** failure older than a grace period (default 60 min), so a blip the error job fixes on its
  own never raises a ticket

Ping `fail` when the relevant bucket is non-empty, `success` when it is empty — identical to today's
ping logic, only the number changes meaning.

---

## Expected Behaviour

| | Before | After |
|---|---|---|
| Question asked | New error logs since bookmark | Records still failing right now |
| One record broken for 3 h | down, up, down, up… | one down, red for 3 h, one up |
| Broken record past the watermark | green (false negative) | red |
| Error job runs in between | can silently swallow the signal | no effect |
| Another integration logs an error | can skip Goonj's errors | no effect |
| `error_record_log` empty | no ping at all | success ping |
| Transient error fixed by retry | down + up tickets | no ticket (inside grace) |
| Terminal records | invisible | still green, but counted in the job log |

Expect the checks to sit red for longer stretches than today — that is the point. Red now means
"records are unsynced", which was previously invisible.

### ⚠️ On deploy, `goonj-integration` goes red and stays red

Prod data (below) shows **~1,119 open records with `Internal` error types**, and 4,602 of the 4,696
are older than a day. The grace period does not help — it only covers 45 records under an hour old.

So this change does not produce "quiet green with occasional real alerts". It produces **one down
ticket and a permanently red check** until that backlog is triaged. That is factually correct —
those records are unsynced and today's logic hides it — but it is a different outcome from what
support asked for, and it must be a deliberate choice.

#### 🔓 Open decision — for Himesh

How to sequence this is unresolved and deliberately left open. Four options:

| Option | Effect | Cost |
|---|---|---|
| **Triage first, then deploy** | Clear or reclassify the ~1,119 Internal records, then ship. Check starts green; every later red is a real new event. | Slowest; needs someone to work the backlog |
| **Deploy now, accept permanent red** | Flapping stops immediately, which was the original ask. Check stays red until cleanup. | Check tells nobody anything meanwhile |
| **Deploy with a baseline** | Record today's Internal count and alert only on growth. Starts green with no cleanup. | Adds state back into the logic; can mask the 1,119 indefinitely |
| **Reclassify, then deploy** | Move known-stuck types to Terminal so they stop driving the check. Fast, DB-only. | Hides the 350 stuck 502s rather than fixing them |

The trade-off is the same in each case: how much does a trustworthy signal matter now versus
stopping the ticket noise now. The code change is identical under all four — only the deploy order
and any DB tidy-up differ.

**Two things that make "triage first" cheaper than it looks:**

- The cleanup is **~1,196 records, not 4,696** — the 4,646 Terminal records drive neither check.
- **The two checks can go green independently.** `goonj-salesforce` needs only 77 records cleared
  (all `FieldCustomValidationException`, all Goonj's side). That one could be green quickly, leaving
  `goonj-integration` and its 1,119 as the slower, separate piece of work.

**Related finding, whichever is chosen:** 350 open `BadGateway` records. 502s are transient and
should clear on retry. That they have not means the retry path is not reaching them — worth
investigating on its own merits, independent of this change.

**Risk and rollback.** The change touches only the health-check counting path: no writes to
`error_record`, no schema change, no migration, and the sync logic itself is untouched. Removing the
`IntegratingEntityStatus` write leaves the `GoonjErrorRecordLog` row frozen at its current value,
which is harmless and is what makes a rollback clean — reverting the deploy restores the old
behaviour with no data repair. Worst case if the count is wrong: the checks misreport, which is the
situation today.

---

## Important Correctness Considerations

**Do not filter on `processing_disabled`.** Confirmed by the platform team: `AvniGoonjFullErrorJob`
calls the error worker with `allErrors = true`, and that branch
(`AvniGoonjErrorRecordsWorker.java:77-78`) uses a query omitting the `ProcessingDisabledFalse`
clause its sibling branches carry. This is **deliberate** — it is how Avni recovers records when
Goonj sends no signal back through the APIs, and it must not be changed. The consequence here: a
record with `processing_disabled = true` is still retried on every error run, so excluding it would
report green while failures continue. The count must mirror what the `allErrors = true` path
actually retries.

**Terminal needs no explicit filter.** `pingGoonjInternalHealthCheckStatus` is only ever called for
`Internal` and `External` (`AvniGoonjMainJob.java:96-97`), so bucketing by the last log's follow-up
step already keeps Terminal out of the ping decision. Counting Terminal into its own bucket and
logging it gives visibility of the standing backlog without driving a single ticket. **Decision
taken:** exclude Terminal from pings, but log the count.

**Grace must key off the *first* log, not the latest.** A recurring identical error bumps
`logged_at` on the existing row rather than adding one (`AvniGoonjErrorService.java:73-77` →
`ErrorRecord.updateLoggedAtForLastErrorRecordLog`), so the latest `logged_at` on a
permanently-broken record is always minutes old — grace keyed off it would suppress every error
forever. Use `min(loggedAt)` across the record's logs: "first failed over an hour ago and is still
failing." `errorRecordLogs` is `FetchType.EAGER` (`ErrorRecord.java:30`), so this is in-memory with
no extra query.

Prod shows multiple log rows per record with distinct dates, which looks at first like it
contradicts the bump behaviour. It doesn't: the bump applies only when error type **and exact
message** match (`AvniGoonjErrorService.java:74`), and the stack-trace line number in the message
moves with each Avni server upgrade (189 → 185 → 205 → 223), so an upgrade produces a new log row.
`min(loggedAt)` is the correct basis either way.

**Deduplicate the result — but for a different reason than the existing code.** The retry loop calls
`.distinct()` at `AvniGoonjErrorRecordsWorker.java:83-86` because its `ErrorRecordLogsErrorTypeNotIn`
predicate joins the log table. The new finder has no such predicate, so that reason does not carry
over. It still needs deduplicating: `errorRecordLogs` is an EAGER `@OneToMany`
(`ErrorRecord.java:30`), which Hibernate may satisfy with a join returning one root row per log.

Note that `ErrorRecord` overrides neither `equals` nor `hashCode`, and neither does `BaseEntity` or
`BaseIntegrationSpecificEntity`. `.distinct()` therefore falls back to reference identity — which
does work here, because a single persistence context returns the same instance for a given row, but
it is incidental rather than guaranteed. Prefer counting `distinct` ids explicitly (e.g. map to
`getId()` before deduplicating) so correctness does not rest on Hibernate's instance caching.

---

## Code Changes

### 1. `integration-data/.../repository/ErrorRecordRepository.java`

Add one derived finder alongside the existing ones. Covers both sync directions, matching the scope
of the current count, which filters on `integration_system_id` only:

```java
List<ErrorRecord> findAllByIntegrationSystemId(int integrationSystemId);
```

`IntegrationSystemId` is already an established derived path in this interface (lines 25-28), so no
`@Query` is needed. **No `ProcessingDisabledFalse` clause**, per the note above.

Do not reuse `getProcessableErrorRecords()` (`:41-43`): it resolves the system from
`IntegrationContext`, which the Goonj flow never sets (Goonj uses `GoonjContextProvider`), and it
filters on `ProcessingDisabledFalse`.

### 2. `goonj/.../worker/AvniGoonjErrorRecordsWorker.java`

Replace `evaluateNewErrors()` with `countOpenErrorsByFollowUpStep()`, keeping the return type
`Map<ErrorTypeFollowUpStep, Long>` so the call site needs no reshaping. The rename matters: the old
name describes a time window that no longer exists.

- drop the `IntegratingEntityStatus` read/write and the `findTopByOrderByLoggedAtDesc()` call
- load via the new finder, deduplicating by id (see Correctness Considerations)
- filter out records whose earliest `loggedAt` is within the grace period
- group by `getLastErrorRecordLog().getErrorType().getFollowUpStep()`, seeding every enum value to
  `0L` so `Internal`/`External` are always present and the call site's `.get()` never NPEs. **No
  Terminal filter** — grouping isolates them and the call site never pings on that bucket
- log all four bucket counts at INFO, Terminal included. Terminal drives no ticket but is the
  standing backlog figure, so it must be visible and trendable in the job log; `Internal` and
  `External` are what a ticket should reconcile against

`integrationEntityStatusRepository` and `errorRecordLogRepository` become unused here; remove the
fields if nothing else in the class needs them.

### 3. `goonj/.../job/AvniGoonjMainJob.java`

- `performAdditionalHealthChecks()` (`:93-101`) calls `countOpenErrorsByFollowUpStep()`; the ping
  logic at `:104-106` is unchanged in shape.
- Replace the empty `catch (Exception ignore) {}` at `:86-88` with a logged catch. It is currently
  redundant (the inner method catches `Throwable`), but it is exactly the construct that hid root
  cause #4.

### 4. `goonj/.../job/AvniGoonjFullErrorJob.java`

Remove the `errorRecordsWorker.evaluateNewErrors()` call in the `finally` block (`:52-58`). It
existed only to advance the bookmark; with no bookmark it has no purpose, and leaving it would
reintroduce root cause #2.

Deliberately **not** adding a ping here. The main job runs every 15 min and picks up the cleared
backlog on its next pass; pinging from two jobs adds a concurrency surface for no real gain in
recovery time.

### 5. `goonj/.../config/GoonjConfig.java`

Add the grace period following the existing `getStringConfigValue` pattern (e.g.
`getDeleteAndRecreateDispatchReceipt()` at `:70-72`). The default means **no DB migration needed**:

```java
public int getHealthCheckErrorGraceMinutes() {
    return Integer.parseInt(getStringConfigValue("healthcheck_error_grace_minutes", "60"));
}
```

Tune per environment later by inserting an `integration_system_config` row.

### 6. Leave alone

The `integrating_entity_status` row with `entity_type = 'GoonjErrorRecordLog'` (seeded by
`V2_3_9__Goonj_add_error_log_marker.sql`) becomes unused. Leave the row and the migration in place —
deleting it adds migration risk for no benefit. `GoonjConstants.GoonjErrorRecordLog` becomes dead;
removing the constant is optional tidy-up.

---

## Existing 4,696 Error Records — Separate Issue

This is the platform team's ticket ("establish why `/api/subject` rejects those records"), and it is
**not** fixed by this code change. Total open `error_record` rows for Goonj: **4,696**.

### Distribution across all 4,696 (confirmed)

| Step | Drives | Fixed by | Types | Records |
|---|---|---|---|---|
| `1` Terminal | nothing | human decision | AddressNotFound 2951, DispatchLineItemsDeletion 1456, UpdateDispatchReceipt 113, InvalidAddress 80, EntityIsDeleted 46 | **4,646** |
| `2` Internal | `goonj-integration` | **us** | Unclassified 746, BadGateway 350, AnswerMappingNotFound 23 | **1,119** |
| `3` External | `goonj-salesforce` | **Goonj** | FieldCustomValidation 77 | **77** |

**The cleanup is far smaller than 4,696.** The 4,646 Terminal records drive neither check, so only
**~1,196 records** matter — and they split cleanly between two teams. The External types confirm the
split: `FieldCustomValidationException`, `BadValueForRestrictedPicklist`,
`MustNotHave2SimilarElements`, `TooManyRows` are all Apex/Salesforce-side failures.

These total 5,842 against 4,696 actual records, because the query groups on *every* log — 1,146
records changed error type over their lifetime and are counted more than once. The code buckets on
each record's **last** log only, so the true `Internal` figure is somewhat below 1,119. Get the
exact number with the last-log query in Testing & Verification step 4a; it is what decides red
versus green.

**Age:** 45 records under 1 h old, 74 between 1 h and 1 day, **4,602 over a day**. The grace period
therefore does nothing for the backlog — by design; it exists to suppress new blips.

**Still active:** 159 records last failed in Sept 2026, 207 in Aug, 105 in Jun, with an 831 spike in
May 2026. This backlog is being added to continuously.

### Why the oldest records fail

The following is a 30-row sample taken `ORDER BY er.id LIMIT 30` — the *oldest* records, not a
representative draw. It explains the 2,951 `AddressNotFoundError` rows, the single largest group.
Within that sample the rows are uniform:

```
AddressNotFoundError, follow_up_step=1, Demand, a1CC5000000dAfxMAE, 2024-06-18
  500 : "java.lang.IllegalArgumentException: Address 'Madhya Pradesh, Indore' not found
      at SubjectApiController.updateSubjectDetails(SubjectApiController.java:205)
```

The chain: `GoonjEntity.getAddressMap` (`GoonjEntity.java:18-25`) builds `{State, District}` →
`Subject.setAddressMap` (`Subject.java:105`) stores it under the `Address map` key →
`AvniSubjectRepository.subjectApiVersion` (`:126-128`) selects the address-map API version →
`/api/subject` cannot resolve the State/District pair against Avni's location hierarchy.

**The failing addresses are ordinary districts spread across India** — Indore, Mumbai City,
Kandhamal, Alwar, Delhi West/South, East Godavari, Kasaragod, Barmer, Mandla, Jammu, Parbhani,
Moradabad. Not typos, not new districts. Goonj's Salesforce operates nationally while Goonj's Avni
location hierarchy covers only where Avni is actually deployed, so these will never resolve.

**Resolution is data/config, not code** — either add the locations to Avni, or accept these Demands
as out of scope and close them out. This confirms the platform team's position that the retry loop
is not the defect.

Two properties of this set matter for the health-check fix:

1. **`AddressNotFoundError` is `follow_up_step = 1` (Terminal) in prod**, not `2` (Internal) as the
   sample in `usefulQueries.sql:54-56` suggests. Terminal records are already excluded from the
   retry loop at `AvniGoonjErrorRecordsWorker.java:85`, so records of this type are **not** being
   retried and will **not** turn either check red. Correct behaviour — Terminal means "human action
   needed", which belongs on the weekly prod-health card (`avni-product-ops#268`), not a liveness
   check — but it is a deliberate choice, recorded above.
2. **The sampled records are dormant.** Their latest `logged_at` is **2024-06-18**, over two years
   old, clustered on a handful of dates (2023-11-14, 2024-02-28, 2024-05-23, 2024-06-11, 2024-06-18)
   that look like manual job runs rather than a 15-minute cycle. Worth raising with the platform
   team: their ticket describes these records as retrying continuously, and the timestamps do not
   support it for the ones sampled. This does **not** reopen the retry-loop question — that reading
   is superseded and untouched here — but it means at least the oldest slice of the backlog is not
   what generates today's Freshdesk churn.

### The Internal backlog — what must be triaged before deploy

Only the 1,119 `Internal` records affect the checks. They break down as:

- **`UnclassifiedError` — 746.** No `comparison_operator`/`comparison_value`, so this is the
  `bypassErrors` fallback: errors that matched no pattern. The largest actionable group and entirely
  opaque until someone reads their `error_msg`. Start here — some are probably known types that just
  lack a matching rule.
- **`BadGateway` — 350.** Transient 502s from Salesforce that *should* clear on retry. That 350 are
  still open means the retry path is not reaching them. Worth investigating on its own merits,
  independent of this change.
- **`AnswerMappingNotFoundForCodedConcept` — 23.** Missing `mapping_metadata` answer rows; a known,
  fixable data gap.

**Nothing in the code change is specific to any error type** — it counts all open records bucketed
by follow-up step. The breakdown matters for sequencing the deploy, not for writing the fix.

---

## Out of Scope

- **Dashboard changes.** Notification routing (support tickets vs the integration team) and the
  `goonj` schedule mismatch — cron `0/30 * * * *` with 1 h grace against actual 15-min pings, so a
  genuine outage takes ~90 min to alert. Both config-only, doable any time.
- **Resolving the 4,696 records.** Data/config decision for the Goonj team, per the section above.
- **Triaging the 1,119 Internal records.** Data work, not part of this code change. *Whether* it
  happens before or after deploy is the open decision in Expected Behaviour.
- **No history of resolved errors.** Because success *deletes* the row rather than closing it,
  "how many failed last week and how many recovered" is unanswerable from this schema. Worth raising
  against the prod health dashboard card (`avni-product-ops#269`), which needs data this design
  throws away.
- **`Loading and Truck Images` discrepancy** spotted while reading `DispatchService.java:53`: it
  reads the Salesforce field as `ImagesLink` while `Dispatch.java:38` declares
  `LoadingAndTruckImagesLink` and ignores it. If Salesforce sends the latter, the concept is written
  empty on every sync. Unrelated; worth checking against a real payload.

---

## Testing & Verification

1. `./gradlew :goonj:build :integration-data:build` — compile both modules.

2. **Unit-test the counting logic.** No test exists for `AvniGoonjErrorRecordsWorker`; the goonj
   tests that do exist are `@Disabled` live-API smoke tests. Add a focused test with a mocked
   `ErrorRecordRepository` covering:

   | Case | Expected |
   |---|---|
   | Empty backlog | all buckets `0` |
   | Open Internal record older than grace | `Internal = 1` |
   | Same record inside grace | `0` |
   | Latest log bumped inside grace, first log older | still counted — guards `min(loggedAt)` |
   | `processing_disabled = true` | **counted** — guards the platform team's constraint |
   | Terminal record | lands in `Terminal`; `Internal`/`External` stay `0` |
   | Duplicate-joined record | counted once |

3. **Confirm the false-negative fix locally.** Point at a DB copy with a known open `error_record`
   row whose `logged_at` is older than the entity watermark. Old code reports 0; new code reports 1.

4. **Reconcile against the triage query** — the logged bucket counts must match:

```sql
SELECT et.follow_up_step, et.name, count(DISTINCT er.id) AS records
FROM error_record er
JOIN error_record_log erl ON er.id = erl.error_record_id
JOIN error_type et        ON erl.error_type_id = et.id
WHERE er.integration_system_id = (SELECT id FROM integration_system WHERE name = 'Goonj')
GROUP BY 1, 2
ORDER BY records DESC;
```

   (`follow_up_step`: `0`=Process, `1`=Terminal, `2`=Internal, `3`=External.) Only buckets `2` and
   `3` drive pings; bucket `1` must leave both checks green whatever its size.

   Note the query groups on *every* log while the code buckets on each record's *last* log, so they
   agree exactly only for records whose error type never changed — reconcile `Internal`/`External`
   on that basis rather than expecting a naive total match.

4a. **The exact red/green number.** The query above over-counts, because it groups on every log. The
    code buckets on each record's *last* log. This is what the new method will actually compute —
    run it before and after:

```sql
WITH last_log AS (
  SELECT DISTINCT ON (er.id) er.id, et.follow_up_step
  FROM error_record er
  JOIN error_record_log erl ON erl.error_record_id = er.id
  JOIN error_type et        ON et.id = erl.error_type_id
  WHERE er.integration_system_id = (SELECT id FROM integration_system WHERE name = 'Goonj')
  ORDER BY er.id, erl.logged_at DESC
)
SELECT follow_up_step, count(*) AS records FROM last_log GROUP BY 1 ORDER BY 1;
```

   `follow_up_step = 2` is what turns `goonj-integration` red; `3` turns `goonj-salesforce` red. If
   either is non-zero at deploy time, that check will be red from the first run.

5. **Verify in staging without alerting anyone.** `HealthCheckService.java:32` skips the HTTP call
   entirely when `HEALTHCHECK_PING_KEY` is unset or `dummy`, so a staging run exercises the counting
   path and logs the intended status via `HealthCheckService.java:30` with no ping leaving the box.

6. **After deploy, watch `goonj-integration` for ~24 h.** Success looks like fewer state
   transitions, each remaining one traceable to a real change in the query above.

---

## Production Findings / Additional Observations

**Prod's `error_type` rows have drifted from the repo — trust prod, not `usefulQueries.sql`.** Goonj
error types are seeded by no Flyway migration; they live in
`goonj/src/main/resources/database/usefulQueries.sql` and are applied by hand. Two confirmed
divergences:

| | Repo sample | Prod |
|---|---|---|
| `AddressNotFoundError` follow-up step | `2` (Internal) | `1` (Terminal) |
| `AddressNotFoundError` pattern | `500.*Address ''.*'' not found.*` | `.*Address .* not found.*` |

The prod pattern is already the widened one and matches **both** Avni server wordings — the older
`Address 'State, District' not found` and the newer `Address corresponding to 'Address map' not
found`. No change needed there. *(This supersedes an earlier note in this plan claiming the pattern
failed to match the newer wording; that was based on the repo's narrower version.)*

**Prod has error types the code enum does not.** `MissingTotalNoReceivers` and
`SchoolAanganwadiLearningCenterName` exist in `error_type` but not in `GoonjErrorType`. Harmless —
classification is DB-driven and matches by pattern — but the enum is stale, and anything reading it
as the authoritative list will be wrong.

**Nine error types have a null `comparison_operator`** (`DemandDeletionFailure`,
`DispatchDeletionFailure`, the `*AttributesMismatch` family, `UnclassifiedError`, and others).
`ErrorClassifier.evaluate` returns false for these, so they are never pattern-matched — they are
assigned only when code passes the name explicitly, or, for `UnclassifiedError`, as the
`bypassErrors` fallback.

**Skipping also deletes the error record.** In `GeneralEncounterWorker.java:116-128` the same
delete runs when an encounter is incomplete, voided, or its subject is voided. So a row disappearing
does not always mean "synced successfully" — sometimes it means "no longer applicable."

**`UnclassifiedError` is unmatchable by design.** Its `comparison_operator` and `comparison_value`
are both null, so `ErrorClassifier.evaluate` always returns false for it; it is reachable only as
the `bypassErrors` fallback via `getFallbackRecord`.
