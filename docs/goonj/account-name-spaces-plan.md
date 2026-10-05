# Goonj Account Name extra spaces

**Decision: fix user sync settings only. Do not touch subject values.**

Script: [`fix_goonj_account_name_spaces.sql`](../../fix_goonj_account_name_spaces.sql)
Concept uuid: `2978117c-a297-4171-99c6-23c3522ca0f8`

## Background

Card: [Goonj-Data-Tech/work#42](https://github.com/Goonj-Data-Tech/work/issues/42)

The card from Maha asked us to remove extra spaces from Goonj account names — 92 subject
records with padded values, and 302 padded values across user sync settings. The obvious
reading was: strip the spaces everywhere, on subjects via `/bulkSubjectMigration` and on user
settings via SQL.

Maha's review comments pushed back on that, asking whether padded names coming from
Salesforce (Demand, Dispatch) were actually correct and shouldn't be touched.

Digging into it confirmed that concern, and changed the approach:

- **91 of 95 padded subject values come from Salesforce** and are rewritten on every sync, so
  cleaning them in Avni doesn't hold.
- Worse, cleaning them would have *caused* breakage: the padded value returns on the next SF
  update while user settings keep the clean value, silently cutting subjects off from users.
- Spaces only matter when the two sides **disagree**. Plenty of padded values match padded
  subjects and work fine today — a blanket trim would have broken those.

So the work is no longer "remove extra spaces". It is **make user sync settings match what
the subjects actually hold, so sync works.** That may mean removing a space, and in some
cases leaving one in place.

## The problem

Sync matches a user's `syncConcept` values against the subject's `sync_concept_N_value` by
**exact string comparison**.

| Subject | User setting | State |
|---|---|---|
| `HDFC Bank ` | `HDFC Bank ` | matches — leave alone |
| `Axis Bank` | `Axis Bank  ` | **broken — we fix this** |
| `Orphan Bank  ` | *(none)* | cosmetic |

## Why subject values aren't fixed

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

Tested end-to-end on fixtures covering all three verdicts, duplicate-producing trims and null
settings: `UPDATE 2` of 5 users, no entries lost.

## Running it

1. **Prerelease** — Section 1, save output → Section 2 in the transaction, check UPDATE count
   before `COMMIT` → Section 3 (3b must be 0 rows) → log in as an affected user, force sync.
2. **Production** — same order, outside field hours (sync_settings changes force a full re-sync).
3. **After a sync cycle** — re-run Section 1. New rows mean SF is still emitting padded names.

Don't blanket-trim user settings — it breaks the pairs that currently work.
