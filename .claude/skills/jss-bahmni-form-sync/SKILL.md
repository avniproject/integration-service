---
name: jss-bahmni-form-sync
description: Set up, rebuild or deploy JSS Ganiyari Bahmni → Avni form sync — generate Avni forms from Bahmni dumps, write mapping migrations, upload to Avni, test per form, and deploy to prerelease or production. Use when adding/regenerating Bahmni forms for JSS, resetting JSS mappings, or doing the JSS prerelease/prod rollout. Encodes every mistake made in the 2026 trial build and its fix.
---

# JSS Bahmni → Avni Form Sync

Drives the whole process for the **final JSS form list**, for one target environment at a time (`prerelease` or `production`). Run the phases in order; each ends with a **gate** — do not move on until the gate passes.

Ask the user up front:
1. **Target environment and Avni org:** prerelease ("JSSCP Training") or production. If the integration has never run against that org, start with **Phase S**.
2. **Scope:** all forms on the final list, or named forms only.

Never make changes to an Avni server, a Bahmni server or an integration DB without the user saying go for that step. Code/file changes in the repo are fine once the user has asked for this process.

---

## Key facts

| Item | Value |
|---|---|
| Final form list (source of truth) | [Google Sheet](https://docs.google.com/spreadsheets/d/1ZRTp0mB2xPG5vlQMetj6Tp35jS9pQS2kBvV1I1tS7f0/edit?gid=2110362113#gid=2110362113) — also in `JSS Bahmni integration/docs/README.md` |
| Bahmni dumps | `JSS Bahmni integration/CSV dumps/<Source>/concepts.csv` + `concept_sets.csv` (prerelease Bahmni = JSS prod dump, so these match prod) |
| Bundle + migration builder | `scripts/build_jss_avni_bundle.py` (reads `JSS Bahmni integration/final_forms.csv`) |
| Output | `JSS Bahmni integration/avni_org_setup/jss-avni-bundle_<org>.zip` + report; `docs/02-final-form-status.md` |
| Migrations | `integration-data/src/main/resources/db/migration/` |
| Sync entry point | `PatientEncounterEventWorker` (Bahmni encounter feed → Avni General Encounter) |
| Test class | `bahmni/src/test/java/.../worker/bahmni/PatientEncounterEventWorkerExternalTest.java` |
| Bahmni Entity UUID concept | `a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d` |
| Avni Subject UUID person attribute type (Bahmni) | `ceddfccf-7913-4a63-b60e-d3114f867e4b` |
| Design + docs | `JSS Bahmni integration/docs/README.md` → `01-sync-design.md` (IDs, flows, jobs) |

Several forms on the list live **inside** a catalogue source rather than their own dump folder — e.g. Vitals, Birth Template, History and Examination, death template, Community Program Referral Form are in `All Observation Templates`; General Patient Template is in `All Disease Templates`. Find the right `--source` by exact name match on the `name` column of `concept_sets.csv`.

---

## Lessons from the trial build (read before every run)

| # | Mistake we made | What went wrong | Rule now |
|---|---|---|---|
| 1 | Coded fields generated as `SingleSelect` everywhere | Bahmni allows several answers for some questions (e.g. Diabetes → Complaint). Avni **cannot** change Single→Multi after upload; only fix is delete + re-upload | Detect multi-select **before** generating (Phase 1). Never upload a form until the multi-select audit is done |
| 2 | Mapped the catalogues "All Observation Templates" / "All Disease Templates" as encounter types | They are containers, not forms a doctor fills. Real forms inside them (Vitals, Birth Template…) got no encounter-type mapping, so their encounters can't sync | Map one encounter type **per actual form** on the final list. Never map a catalogue |
| 3 | Created the Avni encounter type before uploading the form, or linked it afterwards | `Cannot read properties of undefined (reading 'formUUID')` / `Duplicate form mapping` | Let the Metadata Zip create encounter types and form mappings together (`encounterTypes.json` + `formMappings.json`, Avni's own export format). Never create or link them by hand. If one ever has to be made by hand: upload the form first, then pick it while creating the encounter type |
| 4 | Put `"encounterType"` in form JSON | Broken/duplicate form mapping | Never include `encounterType` in form JSON |
| 5 | `avni_value` for answers had an `"Avni - "` prefix | Lookup failed → `ZZ` / null values | `avni_value` must equal the **exact** Avni concept/encounter-type name (`Bahmni - <Name>`). No other prefixes |
| 6 | Coded questions mapped without their answers | `Answer concept not found`, raw UUIDs in Avni | Every Coded question needs one mapping row per answer |
| 7 | Hardcoded `integration_system_id = 1` | IDs differ per environment; mappings never found | Always `(SELECT id FROM integration_system WHERE name = 'bahmni')`, including inside mapping_group/mapping_type subqueries |
| 8 | A "fix" migration re-inserted rows | Duplicates → `query did not return unique result`; three cleanup migrations followed | Every INSERT uses `WHERE NOT EXISTS (… AND is_voided = false)`. Never fix by inserting again — void/update instead |
| 9 | Many small fix/revert migrations (V2_4_23–26, 33–34) | Noisy history, hard to reason about | Generate the mapping SQL **from the final, audited bundle** in one pass; review before committing |
| 10 | JSS migrations numbered V2_4_5… on a long-lived branch | Same numbers used by main's Wati migrations → Flyway checksum mismatch, merge blocked | Before writing a migration, `git fetch` and take the next number after **origin/main's** highest. Never edit or renumber a migration that has run in any shared DB |
| 11 | Concepts generated as `Text` when dump lacked datatype | Coded values stored as UUIDs | Take datatypes from `concepts.csv`; flag any concept with no datatype for manual review |
| 12 | Forgot `Bahmni Entity UUID` / editable fields | No link back to Bahmni encounter; users could edit synced data | `Bahmni Entity UUID` is the first field of every form; all fields read-only |
| 13 | Commas in concept names | CSV columns shift | Always read/write CSV with the `csv` module, never split on commas |
| 14 | Voided fields included | Wrong field counts | Skip anything with `voided: true` (section and field level) |
| 15 | External tests advanced the sync cursor / left data | Scheduled job skipped records | Tests call workers with `updateSyncStatus = false`; external tests stay `@Disabled` in committed code |
| 16 | Work left in git stashes for months | Production code nearly lost | Commit at the end of every phase (WIP commits are fine) |
| 17 | Medication unit lists treated as forms | Pointless forms | Dose Quantity Units, Dosing Units, Drug Routes, Duration Units, Dosing Instructions are answer lists for prescriptions (synced as `Bahmni - Medication` text). Confirm with JSS before building forms for them |
| 18 | Endoscopy / Advanced / Normal Procedure had no questions in the dump | Empty forms | Check whether these are filled as forms or ordered as procedures in Bahmni before building |
| 19 | Bridge encounter type "Bahmni Patient Registration" set up by hand per org | Easy to miss; visit sync silently finds no patient | Create it from the setup bundle in Phase S, never by hand |
| 20 | Real credentials and an enabled cron committed as defaults in `bahmni-application.properties` | Passwords pushed to git; schedule would have switched on in every client's deployment | Shared properties keep main's dummy defaults. Local creds go in git-ignored `bahmni-secret.properties`; servers use env vars |
| 21 | Mapped only the list form's own concept set | The sync treats each **top-level** obs group as a form. Surgeries and Procedures, General Patient Template, CDST Forms are never top-level — doctors fill Operation Notes Template, ECG Report, RMRCT Form directly (~62,000 visits would have been skipped silently) | Run the top-level usage query (Phase 1) and list such sub-forms in `extra_top_level_sets` |
| 22 | Multi-answer obs mapped one by one | `OpenMRSEncounterMapper` overwrote earlier answers, so only the last answer reached Avni | Fixed (Oct 2026): repeated coded answers are collected into a list (`addCodedAnswer`). Keep it |
| 23 | Coded search-box questions (Coded Diagnosis, Chief Complaint, Surgical Procedure) have no answer list in the dump; "Other Diagnoses" treated as a form | No answers in Avni; 860 diagnoses built as a useless form | Builder answers them with every concept of the class (`ANSWERS_BY_CLASS`); "Other Diagnoses" has status `answers`. A Bahmni answer not in the dumps still fails the encounter — watch `error_record` |
| 24 | Clean-up of old trial rows run after the new migration | Old and new rows share Bahmni UUIDs → clean-up deletes 1,452 new mappings | Clean-up runs before V2_5_0 and refuses otherwise |
| 25 | Broad queries on Bahmni `obs` (33M rows) | Ran >10 minutes, timed out | Filter by the final forms' concept ids |
| 26 | Only top-level obs groups were treated as forms | List forms recorded *inside* an unlisted form (Obstetrics P/A inside "Obstetrics", Referral Form Summary inside "Referral Form") never synced | Fixed (Oct 2026): `OpenMRSFullEncounter.getMappedFormObservations` looks inside unmapped groups for mapped forms. A mapped group is still taken whole (Blood Pressure inside Vitals stays with Vitals) |
| 27 | Assumed every Bahmni ID starts with GAN | JSS has 5 clinic prefixes (GAN, SEM, SIV, BAM, EME); prod Avni ID Updates are mostly SEM… | Match the Bahmni ID exactly as typed (trim/uppercase only). No prefix logic, no zero-stripping — SEM014770 and SEM14770 are different patients |
| 29 | Answers taken only from the CSV exports | Search-box questions (Coded Diagnosis, Chief Complaint, Surgical Procedure) and some lists (Treating Doctor) use answers not in the exports → those visits fail | Builder also reads `CSV dumps/bahmni_db_answers.csv`: answers defined in Bahmni **plus every answer used in real visits, including retired ones** (Bahmni DB query on `concept_answer` + `obs.value_coded` for the bundle's coded questions). Refresh it before a production build |
| 30 | Paging Bahmni encounter search with `limit` | JSS Bahmni returns HTTP 500 with `limit`, and 50 full encounters per call overload the small server (timeouts, dropped connections) | List encounter UUIDs with `v=custom:(uuid)&startIndex=n` (follow the "next" link), then fetch each encounter on its own |
| 31 | Metadata loaded at run start, read later | `could not initialize proxy … no Session` when mapping group/type were read outside the loading session | `getForBahmniEncounterToAvniEntities` initialises the form mappings' group/type up front |
| 32 | Forms whose only answers are unmapped (e.g. ECG Report with just a photo) | Empty Avni visit with only "Bahmni Entity UUID" | Sync skips a form when nothing but the Entity UUID would be sent |
| 33 | Bahmni visit types could be started by hand in the Avni app ("Unplanned visit") | Field staff could create fake hospital visits | Every encounter type in the bundle (including "Bahmni Patient Registration") carries the eligibility rule `({params, imports}) => false` (`NOT_USER_FILLABLE_RULE` in the builder). Verified Oct 2026: the forms no longer appear under unplanned visits, and the integration still writes through the API |
| 34 | Photos/PDFs (Bahmni "Complex" obs, e.g. ECG Report) skipped | ECG visits arrive empty and are skipped | Not synced yet — needs file download from Bahmni (`/obs/{uuid}/value`) + upload to Avni storage. Prerelease Bahmni has no image files (only the DB was copied), so it can only be tested with files copied over or on production |
| 35 | Prescription text built from `scheduledDate` | Older Bahmni prescriptions (2014) have none → visit failed | Falls back to `dateActivated` |
| 36 | Bahmni Boolean questions mapped as plain text | Bahmni sends its True/False *concept*, so Avni showed a UUID | Avni question stays **Text** (a concept's type can never change once uploaded); mapping hint is Coded with answers True→"Bahmni - Yes", False→"Bahmni - No" (`BOOLEAN_ANSWERS`, from Bahmni global properties `concept.true` / `concept.false`) |
| 37 | Bahmni's own diagnosis bookkeeping synced | "Bahmni Initial Diagnosis", "Bahmni Diagnosis Revised", "Bahmni Diagnosis Status" showed raw UUIDs | Excluded in the builder (`EXCLUDED_QUESTIONS`) |
| 38 | Bahmni write timed out → whole run aborted | The write had actually been saved; other people were not processed | Link write re-reads the patient after a timeout before retrying; each person is processed on their own |
| 39 | Rebuilt bundle changed a question's type | Avni rejects type changes on upload | Before every upload, compare the bundle with the org: no type changes, no name taken by another concept (`/web/concepts?name=Bahmni`) |
| 28 | Forms judged by prerelease usage alone | Prerelease is an old copy; JSS's list reflects production | Keep every list form that has questions; only leave out forms that cannot be built, and record the reason in `final_forms.csv` (the builder writes `docs/02-final-form-status.md`) |

---

## Phase S — One-time setup for a new JSSCP Avni org

Run this **once** whenever the integration is pointed at a JSSCP Avni org for the first time (e.g. "JSSCP Training" on prerelease, then the JSSCP production org). Skip it if the org already has the "Bahmni Patient Registration" encounter type. Current test org: the org of `apiuser@jsscp_org` on prerelease.avniproject.org (temporary); "JSSCP Training" (`apiuser@jsscp_training`) once the prod dump is applied there.

| # | Step | How |
|---|---|---|
| S1 | **Integration user** in the org with rights to read individuals/encounters and create encounters | Avni admin (user does it; never use a personal account) |
| S2 | **Local credentials** for running external tests | Copy `bahmni/src/test/resources/bahmni-secret.properties.example` → `bahmni-secret.properties` (git-ignored) and fill in. Never put credentials in `bahmni-application.properties` (shared with every deployment). Servers use env vars `BAHMNI_AVNI_API_*` / `OPENMRS_*` |
| S3 | **Check the org** — find its "Individual" subject type UUID, confirm the "Individual ID Update" encounter type and its concepts exist (Census ID `88500ad8-…`, Bahmni ID `744414a4-…`, "Registered in Bhamni?" `b69098ce-fd2f-4c6e-a60b-9a6945476cd8` — note the "Bhamni" spelling; match by UUID, not name), and that there are no leftover `Bahmni - …` forms | Read-only SQL on the Avni DB (give the user the query) |
| S4 | **Bridge encounter type** "Bahmni Patient Registration" + "Bahmni Entity UUID" concept + forms + form mappings | `python3 scripts/build_jss_org_setup_bundle.py --subject-type-uuid <uuid from S3>` → upload the zip in Avni (Admin → Upload → "Metadata Zip"). Template lives in `JSS Bahmni integration/avni_org_setup/bundle/` |
| S5 | **Verify S4**: open any individual in Avni, start a "Bahmni Patient Registration" visit — the form must open (no `formUUID` error) with one read-only field "Bahmni Entity UUID". Cancel without saving | User checks in the Avni web app |
| S6 | **Bahmni side**: person attribute type "Avni Subject UUID" (`ceddfccf-7913-4a63-b60e-d3114f867e4b`, Text) exists | `GET /openmrs/ws/rest/v1/personattributetype/ceddfccf-…` — create in Bahmni Admin only if missing (once per Bahmni instance, not per Avni org) |
| S7 | **Integration DB constants** for the JSS deployment: `BahmniIdentifierPrefix`, `IntegrationBahmniIdentifierType`, `IntegrationAvniSubjectType` (= `Individual`), `AvniSubjectUuidBahmniAttributeTypeUuid` | Part of the fresh JSS migrations (Phase 3) — confirm present after deploy |

The integration looks the bridge items up **by name** ("Bahmni Patient Registration", "Bahmni Entity UUID"), so never rename them. If the upload fails with a duplicate-UUID error, regenerate the UUIDs of the form, form mappings and encounter type in the template (names stay) — the integration does not depend on those Avni-side UUIDs.

**Gate:** S5 passes and S3/S6/S7 confirmed. Record the org name, date and subject type UUID in `JSS Bahmni integration/docs/form-sync-tracker.md`.

---

## Phase 0 — Inventory (read only)

1. Load the final form list (ask the user to paste it if the sheet can't be read). Drop duplicates; ignore the bracketed numbers and the category column.
2. For each form, resolve its concept-set UUID and `--source` folder by exact name in the dumps.
3. Compare against what exists: generated bundles, encounter-type mappings and concept mappings in migrations, and (if the user provides access) the Avni server's forms/encounter types.
4. Output one table: form | source | UUID | #questions | Avni form? | encounter-type mapping? | concepts mapped x/y | action (build / rebuild / remove / decide).

**Gate:** user confirms the table, including decisions for lessons 17 and 18.

## Phase 1 — Multi-select audit

For each Coded question in scope, decide SingleSelect vs MultiSelect:
- **Bahmni config:** in the JSS Bahmni config (`openmrs/apps/clinical/app.json` → `conceptSetConfig`), a concept with `"multiSelect": true` is multi-select.
- **Bahmni data (confirms config):** coded questions with more than one answer in the same obs group:
  ```sql
  SELECT c.uuid, cn.name, COUNT(*) AS groups_with_repeats
  FROM (SELECT COALESCE(obs_group_id, encounter_id) AS grp, concept_id FROM obs
        WHERE voided = 0 AND value_coded IS NOT NULL
        GROUP BY COALESCE(obs_group_id, encounter_id), concept_id HAVING COUNT(*) > 1) x
  JOIN concept c ON c.concept_id = x.concept_id
  JOIN concept_name cn ON cn.concept_id = c.concept_id
       AND cn.concept_name_type = 'FULLY_SPECIFIED' AND cn.locale = 'en'
  GROUP BY c.uuid, cn.name;
  ```
- Result lives in `JSS Bahmni integration/CSV dumps/multiselect_concepts.csv` (uuid,name,source) — config entries plus data finds with ≥10 repeat groups. The builder reads it.
- **Also run the top-level usage query** (which sets doctors actually fill at the top of an encounter, with visit counts). A list form that is never top-level but whose sub-form is (e.g. Operation Notes Template inside Surgeries and Procedures) needs that sub-form in `extra_top_level_sets` in `final_forms.csv`, or those visits are silently skipped (lesson 21).
- Run queries on the Bahmni DB read-only via `ssh jss-bahmni-prerelease` → `docker compose exec -T openmrsdb mysql -uopenmrs_admin openmrs` (password from `~/bahmni-docker/.env`, `OPENMRS_DB_PASSWORD`). `obs` has ~33M rows: always restrict to the concept ids of the final forms or the query runs for many minutes.

**Gate:** known case `Bahmni - Diabetes, Complaint` comes out MultiSelect; user reviews the list.

## Phase 2 + 3 — Build the bundle and the mapping migration (one command)

`final_forms.csv` (in `JSS Bahmni integration/`) is the input: one row per list form with `status` = `build` / `decide` / `answers`, optional `extra_top_level_sets` (`;`-separated) and a note.

```bash
git fetch origin main   # version must be above origin/main's highest V* (lesson 10)
python3 scripts/build_jss_avni_bundle.py --subject-type-uuid <Individual subject type uuid> --migration-version <e.g. 2_5_0> --org-label <org>
```
Writes `JSS Bahmni integration/avni_org_setup/jss-avni-bundle_<org>.zip` (bridge setup + all forms + concepts + encounter types + form mappings — **one** Metadata Zip), `integration-data/.../migration/V<version>__JSSBahmniFormMappings.sql` and a report. It refuses to write anything if validation fails (Entity UUID first, read-only fields, no `encounterType` key, no duplicate names, no `Avni - `). Every encounter type gets the "not user-fillable" eligibility rule (lesson 33). (The old `scripts/generate_avni_form.py` was removed in Oct 2026 — it had no Entity UUID field, no multi-select and no answer lists.)

Then:
1. Read the report: forms built, multi-select fields, "not built" list, warnings (expected: only Complex/image fields and `Bahmni Diagnosis Status`).
2. Check the zip against the target org: no existing concept with the same name and a different UUID (`GET /web/concepts?name=Bahmni` with the integration user).
3. Test the migration on a throwaway copy of the local test DB (`CREATE DATABASE x TEMPLATE avni_int_test`): run it twice — second run must add nothing.

**Gate:** user reviews report + SQL; commit.

## Phase 4 — Environment setup (user-driven, one environment at a time)

Prerequisite: Phase S done for this org.

Give the user a checklist; tick items as they confirm:
- [ ] **Avni:** upload `jss-avni-bundle_<org>.zip` once (Admin → Upload → "Metadata Zip"). It creates the encounter types and form mappings itself — do not create encounter types by hand
- [ ] **Avni:** spot-check: start a "Bahmni - Diabetes Intake Template" visit on any person — form opens, first field "Bahmni Entity UUID", Complaint allows several answers; cancel without saving
- [ ] **Bahmni:** person attribute type `ceddfccf-…` exists ("Avni Subject UUID", Text) and is read-only in registration config
- [ ] **Integration DB:** service deployed with the new migration (Flyway applies on start); `constants` has `AvniSubjectUuidBahmniAttributeTypeUuid`
- [ ] Restart integration service after any DB config change (config is cached at startup)

For an integration DB that ran the **old trial migrations** (prerelease only): stop the service → `pg_dump` backup → `psql -f integration-data/src/main/resources/db/onetime/JSS_ResetOldTrialMappings_prerelease.sql` → deploy → start. The script must run **before** V2_5_0 (the old and new rows share Bahmni UUIDs); it refuses to run if V2_5_0 is already applied (lesson 24). Never run it on production.

For **production**: same checklist against prod Avni/Bahmni; migrations reach prod only via main, so they must already be merged and verified on prerelease.

**Gate:** user confirms each item.

## Phase 5 — Verify per form

For each form:
1. Find a real Bahmni encounter containing that form for a linked patient (Bahmni DB: `SELECT e.uuid, e.encounter_datetime FROM encounter e JOIN obs o ON o.encounter_id = e.encounter_id JOIN concept c ON c.concept_id = o.concept_id JOIN patient_identifier pi ON pi.patient_id = e.patient_id WHERE pi.identifier = '<Bahmni ID>' AND c.uuid = '<form concept set uuid>' AND e.voided = 0 ORDER BY e.encounter_datetime DESC LIMIT 5;`).
2. Sync it (external test with `updateSyncStatus = false`, or let the scheduled job run on the environment).
3. Check in Avni: encounter `Bahmni - <Form>` exists, every answered question shows, coded answers readable, multi-select shows all answers, `Bahmni Entity UUID` set.
4. Check `error_record` for that patient — must be empty.

Record results in `JSS Bahmni integration/docs/form-sync-tracker.md` (form | env | encounter used | result | notes).

**Gate:** every in-scope form passes on this environment → ready for UAT (prerelease) or sign-off (production).

---

## Adding a new form later (JSS asks for one more form)

Once V2_5_0 has run on any shared DB it must never change. A new form is an **add-on**: its own small zip and its own new migration.

| # | Step | Who |
|---|---|---|
| A1 | Get the exact Bahmni form name from JSS | User |
| A2 | Make sure the form is in the dumps (`concept_sets.csv`, exact `name` match). If not, export it from Bahmni into `JSS Bahmni integration/CSV dumps/<Form>/` (concepts.csv + concept_sets.csv) | Claude checks; user exports if missing |
| A3 | Run the usage query (Phase 1) for this form only: is it filled at the top level, inside another form, or are its sub-forms filled on their own? Fill `extra_top_level_sets` accordingly. Also re-check multi-select for its coded questions and add to `multiselect_concepts.csv` | Claude (read-only on Bahmni) |
| A4 | Add a row to `final_forms.csv` (`status = build`); the builder regenerates `docs/02-final-form-status.md` | Claude |
| A5 | `git fetch origin main`, pick the next version above every `V*` on main and this branch, then `python3 scripts/build_jss_avni_bundle.py --subject-type-uuid <uuid> --migration-version <next> --org-label <org> --forms "<Form>"` → `jss-avni-bundle_<org>_addon_V<next>.zip` + `V<next>__JSSBahmniFormMappings_Addon.sql` | Claude |
| A6 | Test the add-on migration on a throwaway DB copy (run twice; second run adds nothing) | Claude |
| A7 | Upload the add-on zip to each Avni org (prerelease first, production later); deploy the service so Flyway applies the add-on migration | User |
| A8 | Phase 5 verification for the new form only | Both |

The builder refuses to overwrite an existing migration file (`--overwrite` only for a file that has never run anywhere). The full build (no `--forms`) is only for a brand-new environment before V2_5_0 exists.

## When something fails

| Symptom | Likely cause | Lesson |
|---|---|---|
| `formUUID` undefined / `Duplicate form mapping` | Encounter type created/linked in wrong order | 3, 4 |
| `ZZ`, null or UUID shown as answer | Missing answer mapping or wrong `avni_value` | 5, 6 |
| `Answer concept not found` | Answer concept not in bundle or not mapped | 6 |
| `query did not return unique result` | Duplicate mapping rows | 8 |
| Mapping exists but never found | Hardcoded ID, or name mismatch with Avni | 5, 7 |
| Flyway `checksum mismatch` / service won't start | Migration edited after running, or version clash | 10 |
| Encounter skipped, no error | Form's concept set has no `EncounterType` mapping (often a form inside a catalogue) | 2 |
| Only one answer shows for a multi-answer question | Form uploaded as SingleSelect — delete form and re-upload | 1 |

When you hit a **new** problem and fix it, add a row to the lessons table above so the next run avoids it.
