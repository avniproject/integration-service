# Demand registration from Avni — implementation walkthrough

Pseudo-code showing **where** each change goes and **what** it does, in the order a Demand flows
through the system. Read alongside `goonj-demand-avni-registration-tech.md`.

Labels: **[EXISTS]** unchanged, shown for orientation · **[CHANGE]** edit to an existing file ·
**[NEW]** new file or row · **TBD-n** = depends on an open question, listed at the end.

Names like `AvniDemand` are proposals, not decided.

---

## 0. Background: how the existing push entities identify records

Activity, Distribution and Dispatch Receipt already push Avni → Salesforce. In all three:

- The "source ID" is **not a separate field** — it is simply the Avni record's own UUID, which every
  Avni record has from the moment it is saved. The converter copies it into the payload:
  `distributionDTO.setSource_id(subject.getUuid())`, `activityDTO.setSourceId(subject.getUuid())`,
  `drsDTO.setSourceId(encounter.getUuid())`.
- Salesforce's endpoints are already upserts keyed on it (`DistributionService/upsertDistribution`,
  `ActivityService/upsertActivity`, …).
- **Nothing ever comes back.** No code writes to those Avni records afterwards, they never get an
  External ID, and they are only ever created in Avni.

Demand is the **first entity that goes both ways** — created in Avni *or* SF, pushed up, and then
updated by the pull. That is why three problems appear here that never existed for the others:

| Problem | Why Distribution never had it |
|---|---|
| Pull must find the Avni-created record (by UUID, not External ID) | Nothing is ever pulled back |
| Push and pull keep re-triggering each other (echo) | Nothing is ever pulled back |
| The push watcher also sees SF-originated Demands | All Distributions originate in Avni |

---

## 1. Database config — the on/off switches

### 1a. `integration_system_config` [EXISTS, edited at go-live]

```sql
-- main.scheduled.job.cron : how often the main job runs (already set)
-- int_tasks               : which workers run. Add AvniDemand ONLY on go-live day.
UPDATE integration_system_config
SET    value = value || ',AvniDemand'
WHERE  key = 'int_tasks'
  AND  integration_system_id = (SELECT id FROM integration_system WHERE name = 'Goonj');
-- then RESTART the service — config is read once at startup
```

### 1b. New Flyway migration `V2_4_xx__GoonjAvniDemandPush.sql` [NEW]

```sql
-- The push gets its OWN progress marker, separate from the pull's existing 'Demand' row
INSERT INTO integrating_entity_status (entity_type, read_upto_date_time, integration_system_id, uuid)
VALUES ('Avni Demand', now(),                       -- start from go-live, not from 1900
        (SELECT id FROM integration_system WHERE name = 'Goonj'), uuid_generate_v4());

-- Error type for push failures — must NOT be Terminal ('1'), or failures are never retried
INSERT INTO error_type (name, integration_system_id, follow_up_step, ...)
VALUES ('AvniDemandAttributesMismatch',
        (SELECT id FROM integration_system WHERE name = 'Goonj'), '2' /* Internal */, ...);
```

---

## 2. Registering the new worker

### 2a. `goonj/.../GoonjEntityType.java` [CHANGE]

```java
public enum GoonjEntityType {
    Demand("Demand"),            // existing — the PULL (SF → Avni)
    AvniDemand("Avni Demand"),   // NEW — the PUSH (Avni → SF); matches the row in 1b
    ...
}
```

Separate value because both the watermark row **and** error-record routing are keyed on this name.
Reusing `Demand` would share the pull's watermark, and failed pushes would be retried by the pull's
`DemandEventWorker`, which would ask SF for a Demand using an Avni UUID.

### 2b. `goonj/.../job/IntegrationTask.java` [CHANGE]

```java
None, AvniDispatchReceipt, AvniActivity, AvniDistribution,
AvniDemand,                     // NEW — the name used in int_tasks
...
isAvniSourceTask: Arrays.asList(AvniDispatchReceipt, AvniActivity, AvniDistribution, AvniDemand)
```

### 2c. `goonj/.../job/AvniGoonjMainJob.java` [CHANGE]

```java
@Autowired private AvniDemandWorker avniDemandWorker;          // NEW

public void execute(GoonjConfig goonjConfig) {
    processDemandAndDispatch(tasks);   // PULL — brings down codes + assigned status
    processAvniDemand(tasks);          // NEW PUSH — order relative to the pull: see §3c / TBD-11
    processActivity(tasks);
    ...
}

private void processAvniDemand(List<IntegrationTask> tasks) {  // NEW, same shape as the others
    try {
        if (hasTask(tasks, IntegrationTask.AvniDemand)) avniDemandWorker.performAllProcesses();
    } catch (Throwable e) { logger.error("Failed processAvniDemand", e); bugsnag.notify(e); }
}
```

---

## 3. The watcher

### 3a. How "has U1 changed?" is answered — [EXISTS] in `SubjectWorker.processSubjects()`

There is no change-detection logic of our own. Avni answers it:

```
watermark = integrating_entity_status['Avni Demand'].read_upto_date_time   e.g. 10:00:00

GET /api/subjects?subjectType=Demand&lastModifiedDateTime=10:00:00&size=10
    → Avni returns ONLY Demands whose last-modified time is after 10:00:00
      (any save in the webapp, or any write by the pull, bumps that time)

for each subject returned:
    filter / push / record error
    watermark = subject.lastModifiedDateTime        ← saved after every record
```

So "U1 changed" simply means "U1 came back in this response". Each subject in the response carries
its observations and an `audit` block, which is all the gate in 3b needs:

```json
{
  "ID": "U1",
  "External ID": null,
  "Registration date": "2026-11-02",
  "observations": { "Demand Status": null, "Type of Initiative": ["CFW"], "...": "..." },
  "audit": { "Created by": "Priya (Data POC)", "Last modified by": "Priya (Data POC)",
             "Last modified at": "2026-11-02T10:04:11Z" }
}
```

### 3b. How "not locked / should this be pushed?" is answered — `worker/avni/AvniDemandWorker.java` [NEW]

```java
@Component
public class AvniDemandWorker extends SubjectWorker {

    AvniDemandWorker(...) {
        super(..., GoonjErrorType.AvniDemandAttributesMismatch,
              GoonjEntityType.AvniDemand,   // watermark + error key "Avni Demand"
              "Demand",                      // Avni subject type to poll   (needs 3d)
              ...);
    }

    @Override
    protected boolean shouldSkip(Subject s) {
        // 1. Only Demands that STARTED in Avni. SF-originated Demands also live in Avni (the pull
        //    created them), so the watcher sees them whenever the pull updates them.
        //    They were created by the integration (API) user; Avni-originated ones by a real
        //    Data POC user — the two are always different users, so "Created by" tells them apart.
        if (isIntegrationUser(s.getCreatedBy())) return true;

        // 2. LOCK LAYER 2 — never push once assigned. "Demand Status" is written by the pull.
        String status = (String) s.getObservation("Demand Status");
        if (LOCKED_STATUSES.contains(status)) return true;                // TBD-6 (exact strings)

        // 3. ECHO GUARD — the pull just wrote this record; don't bounce it back.   (see 3c)
        if (isIntegrationUser(s.getLastModifiedBy())) return true;        // TBD-11
        return false;
    }

    @Override
    protected void createSubject(Subject s) {
        syncSubjectToGoonj(s, demandRepository, "DemandId");   // EXISTS helper → createEvent()
    }
}
```

The existing filter still runs first: `!subject.isCompleted()` (no registration date) or voided →
skip.

Caveat on `isIntegrationUser`: the audit block holds the user's **display name**, not the
username in `GoonjConfig.getAvniImplUser()`, so the comparison value must be configured or looked up.

### 3c. Known weakness of the echo guard in 3b — needs a decision (TBD-11)

"Last modified by" only shows the **most recent** writer. If a Data POC edits U1 and the pull then
also writes U1 before the push has run, the record shows the integration user, the push skips, and
**the Data POC's edit never reaches SF** (until they edit again).

| Option | How it works | Trade-off |
|---|---|---|
| A. Run push **before** pull in each cycle | Data POC edits get pushed before the pull can overwrite "last modified by" | Shrinks the race to seconds but doesn't remove it; push may not yet see a fresh "assigned" status — SF's rejection (layer 3) covers that |
| B. No echo guard; SF treats a no-change upsert as a no-op and **doesn't bump LastModifiedDate** | Loop ends after one harmless bounce | Depends on SF; one extra call per pull update |
| C. Store a hash of the last-pushed payload per Demand in our DB; push only if it differs | Exact — pushes real changes only, regardless of who touched the record last | New table + a little code (~0.5 day) |

Recommendation: **C**, or **B** if SF confirms that behaviour.

### 3d. `worker/avni/SubjectWorker.java` [CHANGE — small, shared by all push workers]

```java
// TODAY: subjectType = entityType.getDbName() — used for BOTH the Avni fetch and the watermark.
// CHANGE: overloaded constructor taking the Avni subject type separately.
this.watermarkKey    = entityType.getDbName();   // "Avni Demand"
this.avniSubjectType = avniSubjectType;          // "Demand"
// old constructor unchanged: avniSubjectType = entityType.getDbName()

private boolean shouldFilterSubject(Subject s) {
    return !s.isCompleted() || s.getVoided() || shouldSkip(s);   // NEW hook
}
protected boolean shouldSkip(Subject s) { return false; }         // NEW, default no-op
```

---

## 4. Converting and calling Salesforce

### 4a. `domain/DemandConstants.java` [NEW]

```java
String RESOURCE_UPSERT_DEMAND = "DemandService/upsertDemand";   // TBD-1
String ACCOUNT_NAME           = "Account name";   // Distribution uses "Account  name" (2 spaces) — check
String TYPE_OF_INITIATIVE     = "Type of Initiative";
String DISPATCH_ADDRESS       = "Dispatch Address";   // subject question → holds a subject UUID
String LINE_ITEMS             = "Demand Line Items";  // repeatable question group
String DEMAND_STATUS          = "Demand Status";
...                                                    // concept names from the shared form info
```

### 4b. `dto/DemandRequestDTO.java`, `DemandDTO.java`, `DemandLineDTO.java` [NEW]

```java
// {"Demands": [ {...} ]} — wrapper shape copied from DistributionRequestDTO; real shape TBD-2
class DemandDTO {
    String  source_id;            // Avni UUID — the upsert key SF stores
    String  nameOfAccount;
    String  typeOfInitiative;     // multi-select in the sheet; SF needs ONE code segment — TBD-13
    String  typeOfMaterial, otherMaterial;
    String  dateOfEngagement, tillWhenRequired;     // IST yyyy-MM-dd via DateTimeUtil
    String  isUrgent, isLocalDemand, disasterType, targetCommunity, remarks, pocName;
    Integer numberOfPeople;
    String  dispatchAddressId;    // SF ID of the Dispatch Address, not its Avni UUID
    List<DemandLineDTO> demandLines;                // TBD-4
}
class DemandLineDTO {
    String  sourceId;             // stable per-line key — see "Line items on edit" below
    String  recordType, kitType, kitSubType, nonKitMaterial;
    Integer quantity;
}
```

**Line items on edit — follows the existing convention.** Every push sends the Demand's *full*
current list of lines, and every line carries its own stable `sourceId` so SF can upsert line by
line. This is exactly how the existing push entities already do it:

| Entity | Line `sourceId` today |
|---|---|
| Distribution | `subject UUID + connector + inventory External ID` (`DistributionRepository.getSourceId`) |
| Dispatch Receipt | the SF Dispatch Status Line Item ID the line came from |
| **Demand (proposed)** | `Demand UUID + connector + kit type + kit sub-type` (non-kit lines: `+ record type + material`) |

```java
String lineSourceId(Subject demand, Map<String, Object> line) {
    return demand.getUuid() + LINE_CONNECTOR + line.get(KIT_TYPE) + LINE_CONNECTOR + line.get(KIT_SUB_TYPE);
}
```

For that key to be stable, the same kit type + sub-type must not appear twice in one Demand — enforce
it with a form validation ("add to the quantity of the existing line instead"). Quantity changes then
update the same SF line; changing a line's kit type turns it into a new line.

Only one thing isn't covered by the existing convention: what SF does with a line that was in the
previous push but is **missing** from this one (the Data POC deleted it). See TBD-4.

### 4c. `repository/DemandRepository.java` → `createEvent(Subject)` [CHANGE]

```java
@Override
public HashMap<String, Object>[] createEvent(Subject subject) {
    // TODAY: throw new UnsupportedOperationException();
    return super.createSingleEntity(RESOURCE_UPSERT_DEMAND,          // EXISTS: POST + error wrapping
                                    new HttpEntity<>(toRequest(subject)));
}

private DemandDTO toDemandDTO(Subject s) {
    d.setSource_id(s.getUuid());
    ...
    // Dispatch Address observation = Avni UUID of a Dispatch Address subject.
    // Send that subject's External ID (= SF Dispatch Address ID) instead.
    Subject addr = avniSubjectRepository.getSubject((String) s.getObservation(DISPATCH_ADDRESS));
    if (addr == null || addr.getExternalId() == null)
        throw new RuntimeException("Dispatch Address not linked to Salesforce");
    d.setDispatchAddressId(addr.getExternalId());
    d.setDemandLines(toLines(s.getObservation(LINE_ITEMS)));
    // unmapped picklist value → throw a clear "no mapping found"
}
```

When this throws or SF errors, `SubjectWorker.handleError()` [EXISTS] writes an `error_record`
against `AvniDemand`; the watermark still moves on, so recovery relies on the error job.

### 4d. `worker/AvniGoonjErrorRecordsWorker.getErrorRecordWorker()` [CHANGE]

```java
if (type.equals(GoonjEntityType.AvniDemand.name())) return avniDemandWorker;   // NEW
if (type.equals(GoonjEntityType.Demand.name()))     return demandEventWorker;  // EXISTS (pull)
```

Also confirm how the error job picks up Avni→SF retries (`AvniGoonjFullErrorJob` shows
`SyncDirection.GoonjToAvni`) — check how Distribution retries run today and follow the same path.

---

## 5. The pull side — linking the code back without a duplicate

### 5a. `worker/goonj/DemandEventWorker.processDemand()` [CHANGE]

```java
Subject subject = demand.subjectWithoutObservations();   // External ID = SF DemandId (EXISTS)
demandService.populateObservations(subject, demand);     // EXISTS

String avniSourceId = (String) sfDemand.get("AvniSourceId");   // field name TBD-3

if (avniSourceId == null) {
    avniSubjectRepository.create(subject);    // EXISTS — SF-originated, unchanged
    return;
}
// Avni-originated. POST /api/subject looks up by External ID only → would miss → DUPLICATE.
// PATCH by UUID, sending only the fields the pull owns, so line items etc. aren't wiped.
avniSubjectRepository.patch(avniSourceId, onlyPullOwnedFields(subject));
//   External ID (= SF DemandId), Demand Code, Demand Status,
//   Demand Name (free text in the form; SF's value overrides it)            — final list TBD-15
```

### 5b. `avni/.../AvniHttpClient.java` + `AvniSubjectRepository.java` [CHANGE]

```java
// AvniHttpClient — next to put()
public <T, U> ResponseEntity<U> patch(String url, T body, Class<U> type) { ... HttpMethod.PATCH ... }
// check the RestTemplate request factory supports PATCH (the default JDK one does not)

// AvniSubjectRepository
public Subject patch(String uuid, Map<String, Object> fields) {
    return avniHttpClient.patch("/api/subject/" + uuid, fields, Subject.class).getBody();
}
```

Confirm production avni-server has `PATCH /api/subject/{id}` and that POST still matches by External
ID only (verified on a local checkout dated Apr 2026) — TBD-16.

---

## 6. Avni app configuration (Avni admin UI / bundle, not this repo)

```
Subject type "Dispatch Address"  [NEW]    location = District; External ID = SF Dispatch Address ID
Subject type "Demand"            [EXISTS] reused — the pull already writes into it
Demand registration form         [NEW/CHANGE]
  ├─ header fields (Account, Initiative, Material, dates, ...)            picklists from the shared form info
  ├─ Dispatch Address → Subject question, filtered by Demand location / Account   TBD-19
  ├─ Demand Line Items → repeatable group, qty > 0, sub-type mandatory if the kit has one
  └─ Demand Code, Demand Status → read-only, filled only by the pull

Edit form rule (lock layer 1)                                               TBD-18
  ({params}) => {
      const status = params.entity.getObservationReadableValue('Demand Status');
      return { editable: { value: !LOCKED_STATUSES.includes(status) } };
  }

Data POC user group → Register/Edit on Demand; others view-only. Starts OFF.   TBD-8
```

---

## 7. One Demand, cycle by cycle

```
T0  Data POC saves      Avni: Demand U1 (External ID null, Status null, created by Data POC)
T1  cron → MainJob
      pull: nothing from SF
      push: U1 returned by GET /api/subjects (modified after watermark)
            created by Data POC ✓, status not locked ✓, last modified by Data POC ✓
            → POST DemandService/upsertDemand {source_id: U1, ...}
            SF creates D-99 with code 2026/ACC-…/383, stores source_id U1
T2  cron
      pull: D-99 arrives with AvniSourceId=U1 → PATCH /api/subject/U1 {External ID: D-99, Code, Status}
      push: U1 returned again (the PATCH bumped it) → echo guard skips it (see 3c)
T3  Data POC edits quantity
T4  push: U1 returned, last modified by Data POC → upsert again → SF updates D-99 (no duplicate)
T5  MMT assigns in SF → pull PATCHes Status = DA → form locked (layer 1), push skips (layer 2),
    SF rejects any late upsert (layer 3)
```

---

## 8. Open items

### Resolved

| # | Was | Resolution |
|---|---|---|
| TBD-12 | How to tell Avni-originated from SF-originated Demands | "Created by": the integration (API) user and real Data POC users are always different users |
| TBD-1, TBD-2 | Endpoint path, auth, payload format | Salesforce will provide these as their endpoint spec — no question to ask, just wait for the spec and check it against the list below |
| TBD-5, 14, 17, 20 | Picklists, line items vs material type, concept names, Demand location | Covered by the form information Goonj has shared |
| TBD-21 | Demand Name at save time | Free text on the form; SF's value overrides it on the pull |

### Check that Salesforce's endpoint spec covers these

Not open questions — items to tick off when the spec arrives, and to ask about only if missing.

| # | Item | Why we need it |
|---|---|---|
| TBD-3 | `getDemands` returns the stored Avni source ID, and under what field name | Pull linking (5a) — without it, duplicates |
| TBD-4 | A line present in the previous push but **missing** from this one (Data POC deleted it) — does SF delete it? Line-by-line upsert by `sourceId` already follows the Distribution convention | Deleted lines lingering in SF |
| TBD-6 | Live data shows `DemandStatus` = `Approved` / `Closed` only (§10 F6). How does **assignment** appear: a new `DemandStatus` value (exact string?) or a separate field? | Lock layers 1 and 2 |
| TBD-7 | A distinct response for "rejected — already assigned" | Classify as stop, not retry |
| TBD-9 | Does a no-change upsert leave LastModifiedDate untouched? | Only if echo option B is chosen |
| TBD-13 | ~~Initiative segment of the Demand Code~~ **Resolved from live data (§10 F2/F3):** the segment is the Demand's **State** code, and the running number is per state | — |

### Goonj programme team

| # | Question | Blocks |
|---|---|---|
| TBD-8 | Where the Account and "Reference of Team (Synced POC)" lists come from in Avni, and how a user is tied to their account | "Create only for own account" |
| TBD-10 | ~~Dispatch Address: SF API or bulk upload?~~ **Decided: SF API pull (option 1)** — see §9 for the endpoint checklist (E1–E9) | — |
| TBD-22 | Duplicate **warning before submission** is in the sheet; with the check moved to SF the Data POC never sees it — accepted? | Scope sign-off |
| TBD-23 | Written sign-off on dropping "Verified by Data POC" (the sheet says sync happens after verification) | Scope sign-off |
| TBD-24 | Who can void a Demand, and when? What happens if SF deletes an Avni-created Demand before its code syncs back? | Void/delete handling |
| TBD-25 | Can SF-originated Demands be edited in Avni? If yes, those edits are never pushed (the watcher skips them) | Form edit rule scope |

### Avni product team

| # | Question | Blocks |
|---|---|---|
| TBD-16 | In production avni-server: does `POST /api/subject` still match only by External ID, does POST/PUT still replace all observations, and is `PATCH /api/subject/{id}` available? | Pull design (5a/5b) |
| TBD-18 | Does webapp **data entry** run the edit form rule? (The local Apr 2026 checkout only shows it in the form designer.) | Lock layer 1 on webapp |
| TBD-19 | Can a Subject-type question be filtered by the Demand's location and/or Account in the webapp? (Goonj: filter by location, Account, or both?) | Dispatch Address field |

### Internal (our decisions)

| # | Question | Blocks |
|---|---|---|
| TBD-11 | Echo guard approach — A, B or C in §3c (recommend C) | Push gate; may add ~0.5 day |
| TBD-15 | Exact list of fields the pull owns and PATCHes (Code, Status, Name, External ID, anything else?) | Pull (5a) |
| TBD-26 | Line `sourceId` composition for Demand lines, and the form rule preventing the same kit type + sub-type twice | Line-item upsert (4b) |

---


## 9. Dispatch Address — option 1 (SF → Avni sync), decided

### 9.1 What it is, and why it comes first

Each Goonj Account (office) has addresses that material is dispatched to. They are mastered in
Salesforce. A Demand created in Avni must name one, and the Demand push must send **Salesforce's ID**
for it — so every Dispatch Address must already be in Avni, carrying its SF ID as External ID,
before a Data POC can create a Demand against it.

**Decision:** Dispatch Addresses are pulled from Salesforce by this service on the normal cron, the
same way Inventory items are pulled today. Avni holds a read-only copy; nobody creates or edits a
Dispatch Address in Avni.

### 9.2 Who owns what

| Piece | Owner |
|---|---|
| Address data — create, edit, retire | Goonj, in Salesforce |
| New read endpoint `getDispatchAddresses` | Goonj's SF team (spec comes from them; our needs are in 9.4) |
| `Dispatch Address` subject type, Account filter on the Demand form | Us — Avni config |
| Pull worker, mappings, migration, error handling | Us — this service |
| Sending the chosen address's SF ID in the Demand push | Us — §4c |

### 9.3 The template: the existing Inventory pull

Option 1 is a near-copy of a pull that already runs in production. Verified behaviour:

```
AvniGoonjMainJob.execute()
  └─ if int_tasks has GoonjInventory → inventoryWorker.performAllProcesses()     [BaseGoonjWorker]
       1. processDeletions()  → one SF call → "deletedImplementationInventory": [ids]
                               → DELETE /api/subject/{SF id}  (Avni voids it; 404 ignored, other errors abort)
       2. process()           → ONE SF call per cycle (no paging loop — SF decides the batch size)
            GET ImplementationInventoryService/getImplementationInventories?dateTimestamp=<watermark>
            for each record:
              Inventory.subjectWithoutObservations()   ← External ID, name, State+District, voided
              populateObservations(mapping group "Inventory Item")
                 · SF field with no mapping_metadata row → logged and SKIPPED (silently missing in Avni)
                 · coded value with no answer mapping    → MappingException → error record
              POST /api/subject   ← upsert by External ID (correct here: always SF-originated)
              success → clear error record;  failure → error record
              either way → watermark = record's "LastModifiedDate"
```

Key mechanics we inherit:

| Mechanic | Behaviour | Consequence for Dispatch Address |
|---|---|---|
| Watermark key | `entityType.name()` — the **enum name** (Inventory → row `Inventory`, not "Inventory Item") | New row must be named exactly `DispatchAddress` |
| Watermark value | Taken from each record's timestamp field | SF must return a last-modified timestamp and sort ascending by it |
| One call per cycle | Batch size is whatever SF returns | Initial load of N addresses takes N ÷ batch cycles — fine, but know the number |
| Error retry | Error job calls `processError(id)` → SF **single-record lookup** by ID | SF endpoint must support `?dispatchAddressId=X` |
| Address | `getAddressMap()` throws if State or District blank; Avni rejects names not in its hierarchy | See risk R1 |
| Adhoc re-sync | `AvniGoonjAdhocJob` can re-run a pull with state/account/date filters | Add a `GoonjDispatchAddress` case — our tool for re-syncs and the initial load |

### 9.4 What we need in Salesforce's endpoint spec

SF provides the spec; this is the checklist to hold it against. Everything here exists because the
inherited mechanics above depend on it.

| # | Need | Why |
|---|---|---|
| E1 | `GET DispatchAddressService/getDispatchAddresses?dateTimestamp=…` — same auth and date format as the other Goonj endpoints | Reuses `getAPIFilters()` unchanged |
| E2 | Optional `state` and `account` filters, as the other endpoints accept | Adhoc re-syncs |
| E3 | Single-record lookup, e.g. `?dispatchAddressId=X` | Error retry (`processError`) |
| E4 | Per record: `DispatchAddressId`, display name, **Account** (name, and ID if available), **State**, **District**, address line(s), pincode, active/voided flag, `LastModifiedDate` | Subject fields + mapping |
| E5 | State and District values from the **same picklist** SF already uses for Demand/Dispatch | Avoids `AddressNotFoundError` (R1) |
| E6 | Records sorted by `LastModifiedDate` ascending; the batch size stated | Watermark correctness; load planning |
| E7 | A deleted/retired list (`deletedDispatchAddresses: [ids]`) and/or an `IsVoided` flag | Retire addresses in Avni |
| E8 | One address → one Account, or shared across Accounts? | Shapes the Account field and the Demand form filter |
| E9 | Approximate total count today, and how often they change | Initial-load planning; confirms polling frequency is fine |

### 9.5 What we build (pseudo-code, by file)

```sql
-- Migration V2_4_xx__GoonjDispatchAddress.sql [NEW]
INSERT INTO integrating_entity_status (entity_type, read_upto_date_time, integration_system_id, uuid)
VALUES ('DispatchAddress',                     -- = enum NAME (see 9.3)
        '1900-01-01',                          -- from the beginning: initial load pulls everything
        (SELECT id FROM integration_system WHERE name = 'Goonj'), uuid_generate_v4());

INSERT INTO error_type ... 'DispatchAddressAttributesMismatch', follow_up_step '2' (Internal, retryable)

-- mapping_metadata, mapping group 'Dispatch Address', one row per SF field that becomes an
-- observation (Account, address line, pincode, ...). Missing rows = silently dropped fields.
```

```java
// GoonjEntityType  [CHANGE]  DispatchAddress("Dispatch Address")
// IntegrationTask  [CHANGE]  GoonjDispatchAddress; add to isSalesforceSourceTask
// GoonjMappingDbConstants [CHANGE]  MappingGroup_DispatchAddress = "Dispatch Address"
// GoonjErrorType   [CHANGE]  DispatchAddressAttributesMismatch

// domain/DispatchAddress.java  [NEW] — copy of Inventory.java
Subject subjectWithoutObservations() {
    subject.setSubjectType("Dispatch Address");
    subject.setExternalId(get("DispatchAddressId"));     // the link the Demand push sends
    subject.setFirstName(get("Name"));
    subject.setRegistrationDate(get("CreatedDate"));
    subject.setAddressMap(getAddressMap("State", "District", response));
    subject.setVoided(get("IsVoided"));
}
// Core_Fields = id, name, State, District, IsVoided, dates → everything else goes through mapping

// dto/DispatchAddressResponseDTO.java   [NEW] — copy of InventoryResponseDTO (two lists)
// repository/DispatchAddressRepository  [NEW] — copy of InventoryRepository (fetchEvents,
//                                                fetchDeletionEvents, getDispatchAddress(id))
// service/DispatchAddressService        [NEW] — populateObservations(..., MappingGroup_DispatchAddress)
// worker/goonj/DispatchAddressWorker      [NEW] — copy of InventoryWorker
// worker/goonj/DispatchAddressEventWorker [NEW] — copy of InventoryEventWorker
//     (minus the "quantity 0 → void" rule; override updateReadUptoDateTime to SF's timestamp field)

// AvniGoonjMainJob [CHANGE] — order matters:
processDispatchAddress(tasks);   // 1. addresses first
processDemandAndDispatch(tasks); // 2. Demand pull
processAvniDemand(tasks);        // 3. Demand push — can rely on addresses being current
...
// AvniGoonjErrorRecordsWorker [CHANGE]  DispatchAddress → dispatchAddressEventWorker
// AvniGoonjAdhocJob           [CHANGE]  case GoonjDispatchAddress -> ...
```

```
Avni config [NEW]
  Subject type "Dispatch Address"   registrable at District level
  Its registration form             Account, address line, pincode, ... (concepts = mapping targets)
  Privileges                        View only for everyone (incl. Data POCs); only the API user writes
  Demand form "Dispatch Address"    Subject-type question → Dispatch Address,
                                    filtered by the Demand's location and/or Account (TBD-19)
```

### 9.6 Risks under option 1

| # | Risk | What happens | Mitigation |
|---|---|---|---|
| R1 | SF's State/District text doesn't match Avni's location hierarchy | Avni rejects the subject (`AddressNotFoundError`, Terminal in prod); that address never appears in Avni, so no Demand can be raised against it | **Accepted known limitation** — same issue already exists for Demand/Dispatch/Inventory and can't be fixed from our side. Make it visible: list `DispatchAddress` errors after the initial load and share with Goonj. |
| R2 | A field in SF's response has no `mapping_metadata` row | It is silently dropped — no error | Mapping rows reviewed against the final spec; a test that asserts every expected concept is populated |
| R3 | An address is retired in SF while a Demand that uses it is still being edited | Avni voids the address; the Demand still holds its UUID; the next push sends a retired SF ID | Ask SF how the Demand upsert treats a retired address (reject → error record vs. accept). Our converter can't see voided subjects via the normal lookup, so decide whether to fail clearly |
| R4 | Initial load is large and SF's batch is small | Takes many cycles; Demand registration can't open until it completes | Get the count (E9); run the initial load with an adhoc task well before opening registration; verify count in Avni = count in SF |
| R5 | Deletion call fails with a non-404 error | Inventory's pattern rethrows → the whole Dispatch Address section is skipped that cycle (retried next cycle) | Acceptable; it self-heals |
| R6 | Account on the address doesn't match the Account value on the Demand form exactly (spelling, ID vs name) | The filtered picker shows nothing | Use the same Account source for both (ideally SF Account ID); covered by TBD-8 and E8 |
| R7 | Later, field teams use the mobile app | Dispatch Address subjects must be in their catchment to sync to devices | Out of scope now (webapp-only); note for the field-team pilot |

### 9.7 Rollout order (Dispatch Address is step one)

1. SF deploys `getDispatchAddresses` to staging, then production.
2. We deploy the subject type, form, mappings and the pull with `GoonjDispatchAddress` added to `int_tasks`.
3. Initial load runs (cron or adhoc). Reconcile: count in Avni = count in SF; error list for
   `DispatchAddress` is empty (fix every R1 mismatch).
4. Only then is the Demand form published and the Demand push switched on (§1a).

### 9.8 Testing

| # | Scenario | Expected |
|---|---|---|
| D1 | New address created in SF | Appears in Avni next cycle, External ID = SF ID, correct District and Account |
| D2 | Address edited in SF | Same Avni subject updated — no duplicate |
| D3 | Address retired/deleted in SF | Avni subject voided; no longer offered on the Demand form |
| D4 | State/District not in Avni's hierarchy | Error record created; visible in the error list; fixed + re-saved in SF → syncs |
| D5 | Initial load | Avni count = SF count; no errors outstanding |
| D6 | Demand form, Account A selected | Only Account A's addresses offered |
| D7 | Demand pushed | Payload carries the chosen address's SF ID, not its Avni UUID |
| D8 | SF returns an error/timeout for the address pull | Section skipped, watermark not advanced, next cycle catches up |
| D9 | Error retry | Error job re-fetches the single address by ID and syncs it |

### 9.9 Effort (already in the ~10-day estimate)

| Work | Days |
|---|---|
| Avni config — subject type, form, privileges, Demand-form filter | ~1 |
| Integration — domain, DTO, repository, service, two workers, migration + mappings, main/error/adhoc job wiring, tests | ~2 |
| Initial load + reconciliation on staging | inside UAT |

Holds if E1–E9 come back straightforward and R1 mismatches are few. A large number of location
mismatches is the one thing that would push it.

### 9.10 Minimum fields — what we actually need

The core of a Dispatch Address in Avni is small:

| Field | Needed for | Required? |
|---|---|---|
| SF Dispatch Address ID → External ID | Sent in the Demand push; links updates/deletions | Yes |
| Name | What the Data POC picks from | Yes |
| Account | Filtering the picker to the Demand's Account | Yes |
| Active / voided flag | Hiding retired addresses | Yes |
| A readable address line (street / city / pincode) | Telling apart two addresses with similar names | Recommended |
| State + District | Avni subject location; **filters the picker** — the Data POC selects the Demand's location first, then only addresses in that location are offered | Yes |

**Decided:** Dispatch Address subjects keep a District (no "allow empty location"). Reading of the
requirement sheet row *"Dispatch Address — Searchable Lookup or Location Concept — Synced address
master / AVNI Location hierarchy"*: the Demand's selected location drives which Dispatch Addresses
are offered. The location mismatch (R1) is therefore accepted as-is.

Still to confirm (TBD-19, widened):
- Filter by **location**, by **Account**, or **both**?
- The sheet also says *"Demand State — derived from Dispatch Address"* — the reverse direction. If
  the Demand's location is picked first, Demand State simply comes from that selection.
- Whether the webapp can filter a Subject-type question by the Demand's location and/or Account
  (Avni product team).

---

## 10. What the live `getDemands` response tells us (sample fetched 2026-10-16)

Each Demand comes back with exactly 12 fields:

```json
{ "DemandId": "a1CS30000054gKLMAY", "DemandName": "2025/ACC-30792/APD/249",
  "AccountId": "0012w00000d57nUAAQ", "AccountName": "Vyavsayaka & Sanghika Abhivruddhi Sanstha (ASDS)",
  "State": "Andhra Pradesh", "District": "Alluri Sitharama Raju",
  "DemandStatus": "Approved", "DispatchStatus": "Dispatched",
  "TypeOfDisaster": "Floods", "TargetCommunity": "Agricultural Related",
  "NumberOfPeople": 500.00, "LastUpdatedDateTime": "2025-07-21T10:02:07.000Z" }
```

### Findings

| # | Observation | What it means for us |
|---|---|---|
| F1 | **`DemandName` is the Demand Code.** There is no separate code field | The Data POC's free-text name gets replaced by the code on the first pull, which is expected. If Goonj wants the original free text kept, SF has to store it in another field |
| F2 | **The third segment is a state code, not an initiative.** `BHR`, `ASM`, `WEB`, `KER`… match `State`. One account (`ACC-30792`) has `TLG/414` for Telangana and `APD/249` for Andhra Pradesh | **Resolves TBD-13.** The code is built from Account plus the **Demand's State**, so the push must send the correct State. This supports picking the location first on the form |
| F3 | **The running number is per state, not per account.** BHR 2025 runs 891, 892, 943, 948, 955, 958… across many different accounts | The requirement sheet's "account-wise sequence" is wrong. This is SF's logic, not ours, but correct it in the docs so nobody builds or tests against the wrong rule |
| F4 | **There's no Avni source ID field** | **Confirms TBD-3.** SF must add one (e.g. `AvniSourceId`) to `getDemands`, or Avni-created Demands will be duplicated |
| F5 | **Only 12 fields come back.** There are no line items, initiative, material type, dates, urgency, remarks or Dispatch Address | **Confirms gap B.** A full overwrite (POST/PUT) on the pull would erase almost everything the Data POC entered, so the pull **must PATCH** only the fields it owns (§5a) |
| F6 | **`DemandStatus` values seen: `Approved`, `Closed`.** No DA/DV/DPV values appear | **TBD-6 is now sharper.** Ask SF whether assignment shows up as a new `DemandStatus` value, or as a separate field or object. The lock depends on reading it from this response |
| F7 | **`AccountId` (the SF ID) is present** next to the name | Key the Account on its **ID**, not its name, on the Demand form, in the Dispatch Address filter and in the push (reduces R6). Check whether the current mapping already brings `AccountId` into Avni |
| F8 | Multi-selects are `;`-joined (`"Tribal Community;Daily Wagers;Children"`); `"None"` is used as a value | The push sends them back the same way, as Distribution already does |
| F9 | Data quality: `State: Uttarakhand` with `District: Adilabad` (Adilabad is in Telangana); a test account with `State: null` | Real examples of the existing location-mismatch errors. Expected and accepted |
| F10 | **`NumberOfPeople` comes back as a decimal** (`317.00`, and some `0.00`) | The form validation says "whole number", but SF has zeros. Agree with Goonj whether 0 is allowed |
| F11 | **Records are not ordered by `LastUpdatedDateTime`.** They look ordered by `DemandId`. The watermark is set from each record in turn | Harmless if SF returns *all* changes in one response. **If SF caps the batch size**, records modified earlier than the last one returned could be skipped. Worth confirming for the existing pull, and ask for proper ordering in the Dispatch Address endpoint (E6) |

### Values seen (useful for picklist checks)

- `TargetCommunity`: Adolescents, Agricultural Related, Children, Daily Wagers, Devdasi community,
  Differently Abled, Leprosy affected, Mentally Challenged, Senior Citizens, Tribal Community,
  Unorganized sector, None
- `TypeOfDisaster`: Not Applicable, Floods
- `DispatchStatus`: Dispatched, Closed, null
