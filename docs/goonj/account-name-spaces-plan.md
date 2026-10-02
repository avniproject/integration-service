# Goonj Account Name extra spaces — plan

**Decision: we fix only user sync settings. We do not touch subject values.**

| | |
|---|---|
| Status | Ready to run on prerelease |
| Script | [`fix_goonj_account_name_spaces.sql`](../../fix_goonj_account_name_spaces.sql) |
| Concept | `Account  name` (two spaces), uuid `2978117c-a297-4171-99c6-23c3522ca0f8` |
| Scope | User `sync_settings` only |
| Out of scope | Subject/observation values, the concept name, the reverse-direction mismatches |

---

## 1. What the problem actually is

Sync matches a user's `syncConcept` values against the subject's
`sync_concept_1_value` / `sync_concept_2_value` by **exact string comparison**. So extra
spaces only break something when the two sides **disagree**.

| Subject value | User setting | State |
|---|---|---|
| `HDFC Bank ` | `HDFC Bank ` | Fine — matches. **Leave alone.** |
| `Axis Bank` | `Axis Bank  ` | **Broken. This is what we fix.** |
| `ICICI Bank ` | `ICICI Bank` | Broken, but subject is SF-owned — fix would be to *add* the space. Out of scope. |
| `Orphan Bank  ` | *(none)* | Nobody syncs it. Cosmetic. |

The original ticket framing — "remove extra spaces from account names" — was the wrong
instinct. A padded user value matching a padded subject is *correct* and must not be touched.

## 2. Why we are not fixing subject values

91 of the 95 padded subject values belong to entities written **from Salesforce on every
sync**. Verified in code on 2nd Oct 2026:

| Check | Evidence |
|---|---|
| Direction is inbound | `worker/goonj/` holds `DemandWorker`, `DispatchWorker`, `InventoryWorker`. Activity and Distribution are outbound, in `worker/avni/`. |
| AccountName isn't filtered out | [Dispatch.java:40-41](../../goonj/src/main/java/org/avni_integration_service/goonj/domain/Dispatch.java#L40-L41) — `Ignored_Fields` lists `ACCOUNT_ID` and `ACCOUNT_CODE` but **not** `ACCOUNT_NAME`. [Demand.java:22-23](../../goonj/src/main/java/org/avni_integration_service/goonj/domain/Demand.java#L22-L23) — every non-core field becomes an obs. [Inventory.java:28-31](../../goonj/src/main/java/org/avni_integration_service/goonj/domain/Inventory.java#L28-L31) — only `SourceOfMaterial` is ignored. |
| Written verbatim | [BaseGoonjService.java](../../goonj/src/main/java/org/avni_integration_service/goonj/service/BaseGoonjService.java#L38-L41), `dataTypeHint == null` branch: `addObservation(mapping.getAvniValue(), goonjEntity.getValue(obsField))`. No trim anywhere in that path. |
| Rewritten on every update | `avniSubjectRepository.create(...)` at [DemandEventWorker.java:52](../../goonj/src/main/java/org/avni_integration_service/goonj/worker/goonj/DemandEventWorker.java#L52), [DispatchEventWorker.java:59](../../goonj/src/main/java/org/avni_integration_service/goonj/worker/goonj/DispatchEventWorker.java#L59), [InventoryEventWorker.java:68](../../goonj/src/main/java/org/avni_integration_service/goonj/worker/goonj/InventoryEventWorker.java#L68) |

So any Avni-side edit to those values survives only until Salesforce next touches the
record — and by then user settings would hold the clean value, so the subject would
**silently stop reaching the user**. The cleanup would cause the very bug it was meant to fix.

Salesforce owns those values. Avni owns user sync settings. We make the settings match
reality rather than the other way round.

## 3. Observations from the investigation

- **95 subject records** carry padded account names — 92 on 3rd Sep, 95 on 21st Sep. The set
  is still growing, so this is not a fixed backlog.
- **Ownership split**: Inventory Item 43, Demand 26, Dispatch 22 — all Salesforce-written.
  Activity 3, Distribution 1 — Avni-authored.
- **All five subject types share one concept**, `Account  name`.
- **Mapping metadata** (integration DB, 21st Sep):

  | mapping_group | salesforce_field | avni_concept | data_type_hint |
  |---|---|---|---|
  | Demand | `AccountName` | `Account  name` | *(null)* |
  | Dispatch | `AccountName` | `Account  name` | *(null)* |
  | Inventory Item | `FromWhichAccount` | `Account  name` | *(null)* |

  `data_type_hint` null means the value is free text written verbatim — no coded-answer
  lookup, no trim.
- **Inventory Item reads a different SF field** (`FromWhichAccount`). If Goonj ever cleans at
  source, that's a second field on a second object — easy to miss.
- **Sync matches on `sync_concept_N_value`**, not the observations JSON. The queries use those
  columns: more correct, and far cheaper since they're indexed.

### ⚠️ Never rename the concept

`Account  name` has two spaces, and that string is hardcoded in
[ActivityConstants.java:13](../../goonj/src/main/java/org/avni_integration_service/goonj/domain/ActivityConstants.java#L13)
and [DistributionConstants.java:15](../../goonj/src/main/java/org/avni_integration_service/goonj/domain/DistributionConstants.java#L15),
looked up by name via `subject.getObservation()`. Tidying it is a natural thing to do during a
whitespace cleanup — and it would make that lookup return null, silently stopping
Activity/Distribution from sending the account to Salesforce.

## 4. The fix

`fix_goonj_account_name_spaces.sql`, in four sections:

| Section | What | Writes? |
|---|---|---|
| 1 | Code evidence for the decision | Reference only |
| 2 | **FETCH** — what would change and why | Read-only |
| 3 | **UPDATE** — apply it | **Yes** |
| 4 | **VERIFY** — confirm the result | Read-only |

Section 2 classifies every candidate:

| Verdict | Meaning | Fixed by Section 3? |
|---|---|---|
| `SAFE TO TRIM` | Every subject holds the clean spelling. Trimming gains those subjects, loses nothing. | Yes |
| `PARTIAL` | Subjects hold both spellings. Trimming gains the clean ones; padded ones stay unmatched. Strict improvement, not a complete fix. | Yes |
| `DO NOT TRIM` | The padded value **already matches** subjects exactly. Trimming would **lose** them. | No — deliberately skipped |

That third verdict is the important safety check: trimming a value that currently works
would actively break sync for that user.

### Safety properties of the UPDATE

- Value-by-value mapping, **not** a blanket trim — correctly-padded values are left alone.
- Skips anything matching subjects exactly (`DO NOT TRIM`).
- Preserves every other key in each sync setting (`subjectTypeUUID`, `syncConcept1`, …).
- `jsonb_agg(DISTINCT ...)` so trimming into an existing value collapses rather than duplicating.
- Users with null or non-array `sync_settings` are skipped, not errored.
- Backs up every touched row inside the same transaction, with a documented rollback.

Tested end to end against fixtures covering all three verdicts, a duplicate-producing trim,
a user needing no change, null sync settings, and the reverse-direction case. Result:
`UPDATE 2` of 5 users, no entries lost, correctly-padded values untouched.

## 5. Runbook

1. **Prerelease** — run Section 2 and save the output; run Section 3 in the transaction and
   check the UPDATE count against that output before `COMMIT`; run Section 4 (4b must return
   0 rows); then log in as an affected user on the Avni client, force a full sync, and confirm
   the expected subjects appear.
2. **Production** — same order, outside field hours. Changing `sync_settings` forces a full
   re-sync on that user's device.
3. **Afterwards** — re-run Section 2 after a Goonj sync cycle. New rows mean Salesforce is
   still emitting padded names and new users are being configured against them; at that point
   raise the ingestion trim or SF-side cleanup as a separate change.

## 6. Open items

- **The reverse direction** (Section 4d): user value clean, subject padded, so the user isn't
  receiving those subjects. The subject side is SF-owned, so the fix is to *add* the space.
  Needs a per-case look — raise with Goonj if the counts are material.
- **Durable prevention**, if padded names keep arriving. Either Goonj cleans `AccountName`
  *and* `FromWhichAccount` at source, or we add a trim at ingestion in
  `populateObservations()`. The second ships without Goonj and makes any cleanup permanent.
  Not in scope here.
