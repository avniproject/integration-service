# Goonj Account Name extra spaces

**Decision: fix user sync settings only. Do not touch subject values.**

Script: [`fix_goonj_account_name_spaces.sql`](../../fix_goonj_account_name_spaces.sql)
Concept: `Account  name` (two spaces), uuid `2978117c-a297-4171-99c6-23c3522ca0f8`

## The problem

Sync matches a user's `syncConcept` values against the subject's `sync_concept_N_value` by
**exact string comparison**. Spaces only break things when the two sides disagree.

| Subject | User setting | State |
|---|---|---|
| `HDFC Bank ` | `HDFC Bank ` | matches — leave alone |
| `Axis Bank` | `Axis Bank  ` | **broken — we fix this** |
| `ICICI Bank ` | `ICICI Bank` | broken, but subject is SF-owned — fix is to *add* a space. Out of scope. |
| `Orphan Bank  ` | *(none)* | cosmetic |

"Strip all spaces" is wrong: a padded user value matching a padded subject is correct.

## Why subject values aren't fixed

91 of 95 padded subject values come from Salesforce and are rewritten on every sync. Clean
them in Avni and the next SF update reverts them — while user settings hold the clean value,
silently cutting the subject off from the user.

| Check | Evidence |
|---|---|
| Inbound from SF | `worker/goonj/`: Demand, Dispatch, Inventory workers |
| Not filtered out | [Dispatch.java:40-41](../../goonj/src/main/java/org/avni_integration_service/goonj/domain/Dispatch.java#L40-L41) ignores `ACCOUNT_ID`/`ACCOUNT_CODE`, not `ACCOUNT_NAME`; [Demand.java:22-23](../../goonj/src/main/java/org/avni_integration_service/goonj/domain/Demand.java#L22-L23) and [Inventory.java:28-31](../../goonj/src/main/java/org/avni_integration_service/goonj/domain/Inventory.java#L28-L31) pass through non-core fields |
| Written verbatim | [BaseGoonjService.java:38-41](../../goonj/src/main/java/org/avni_integration_service/goonj/service/BaseGoonjService.java#L38-L41) — no trim |
| Rewritten every update | `create(...)` at [DemandEventWorker:52](../../goonj/src/main/java/org/avni_integration_service/goonj/worker/goonj/DemandEventWorker.java#L52), [DispatchEventWorker:59](../../goonj/src/main/java/org/avni_integration_service/goonj/worker/goonj/DispatchEventWorker.java#L59), [InventoryEventWorker:68](../../goonj/src/main/java/org/avni_integration_service/goonj/worker/goonj/InventoryEventWorker.java#L68) |

Ownership: Inventory Item 43, Demand 26, Dispatch 22 (Salesforce) · Activity 3, Distribution 1 (Avni).

## The fix

Section 1 fetches candidates and classifies them:

| Verdict | Meaning | Updated? |
|---|---|---|
| `SAFE TO TRIM` | all subjects clean — gains them, loses nothing | yes |
| `PARTIAL` | subjects hold both spellings — gains the clean ones | yes |
| `DO NOT TRIM` | padded value **already matches** subjects — trimming would lose them | **no** |

Section 2 applies it: value-by-value mapping, not a blanket trim. Preserves other keys,
dedupes, skips null settings, backs up inside the transaction.

Tested end-to-end on fixtures covering all three verdicts, duplicate-producing trims, null
settings and the reverse case: `UPDATE 2` of 5 users, no entries lost.

## Running it

1. **Prerelease** — Section 1, save output → Section 2 in the transaction, check UPDATE count
   before `COMMIT` → Section 3 (3b must be 0 rows) → log in as an affected user, force sync.
2. **Production** — same order, outside field hours (sync_settings changes force a full re-sync).
3. **After a sync cycle** — re-run Section 1. New rows mean SF is still emitting padded names.

## Don'ts

- **Never rename the `Account  name` concept.** Two spaces, hardcoded in
  [ActivityConstants:13](../../goonj/src/main/java/org/avni_integration_service/goonj/domain/ActivityConstants.java#L13)
  and [DistributionConstants:15](../../goonj/src/main/java/org/avni_integration_service/goonj/domain/DistributionConstants.java#L15).
  Renaming silently breaks Activity/Distribution sync to Salesforce.
- Don't blanket-trim user settings — it breaks the pairs that currently work.

## Open

- **Reverse direction** (Section 3c): user clean, subject padded. Fix is to *add* the space.
  Needs per-case review; raise with Goonj if counts are material.
- **Prevention**: Goonj cleans `AccountName` *and* `FromWhichAccount` at source, or we trim at
  ingestion in `populateObservations()`. The latter ships without Goonj. Not in scope.
