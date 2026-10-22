# JSS Bahmni ↔ Avni — Sync Design

How JSS Ganiyari's hospital system (Bahmni) and the JSSCP field app (Avni) are linked, what moves between them, and which IDs tie records together.

---

## In one paragraph

Almost every person JSSCP works with is registered in **Avni** and has a **Census ID**. Some of them are also hospital patients in **Bahmni**. A field worker links the two by filling the **"Individual ID Update"** form in Avni with the person's **Bahmni ID**. From then on, the integration copies that person's hospital visits (the forms on JSS's final list, lab results and prescriptions) from Bahmni into Avni, and writes the person's Avni ID into Bahmni so the hospital can open their Avni record. People who are not linked are ignored.

---

## Ground rules

| # | Rule |
|---|---|
| 1 | Avni is the master list of people. The integration never creates people in Avni or patients in Bahmni |
| 2 | Linking is done by a person (the ID Update form), never by matching names or dates of birth |
| 3 | Only linked people sync. Hospital patients without an Avni record, and Avni people never seen in hospital, are skipped quietly |
| 4 | Bahmni → Avni only, apart from one value written back: the Avni ID on the Bahmni patient |

---

## The IDs

| ID | Lives in | Example | Used for |
|---|---|---|---|
| **Census ID** | Avni, "Individual ID Update" form (concept `88500ad8-a233-48e3-a0cc-962964103fb0`) | `SEM01233` | JSSCP's own ID for every person. Not used for matching |
| **Bahmni ID** | Avni, "Individual ID Update" form (concept `744414a4-a88f-41e1-b46d-d3b04409e7b3`), entered by the field worker | `SEM4072`, `GAN279731` | **The link.** Matched exactly against Bahmni's "Patient Identifier" |
| Registered in Bhamni? | Same form (concept `b69098ce-fd2f-4c6e-a60b-9a6945476cd8`, spelled "Bhamni"), Yes / Not yet | `Yes` | Only `Yes` + a Bahmni ID is linked |
| Bahmni patient UUID | Stored in Avni on the bridge visit "Bahmni Patient Registration" (field "Bahmni Entity UUID") | `0f4c…` | Finds the Avni person for every incoming Bahmni visit |
| Bahmni visit UUID | Stored on each synced Avni visit (field "Bahmni Entity UUID") | `9a1e…` | Updates the same Avni visit instead of creating a duplicate |
| Avni person UUID | Written to the Bahmni patient attribute "Avni Subject UUID" (`ceddfccf-7913-4a63-b60e-d3114f867e4b`) | `6ce0…` | "View on Avni" link from Bahmni |

### Bahmni ID format

JSS has five clinics, each with its own prefix, all stored as identifier type "Patient Identifier":

| Prefix | Patients (prerelease copy, Dec 2025) |
|---|---|
| GAN | 278,312 |
| SEM | 16,286 |
| SIV | 11,944 |
| BAM | 11,139 |
| EME | 9,837 |

- Match the ID **exactly as typed** (trim spaces, upper-case). No prefix is added or removed, and leading zeros are kept — `SEM014770` and `SEM14770` are two different patients.
- The Avni "Bahmni ID" field has the validation regex `^(GAN|SEM|SIV|BAM|EME)[0-9]+$` (covers 99.99% of Bahmni IDs).
- In a sample of 186 production entries, 155 of 178 distinct IDs existed in Bahmni; the rest were typos or unknown numbers. Expect "Bahmni patient not found" errors and a report for JSSCP to fix them.

---

## The three flows

```
Avni "Individual ID Update"  ──(1) link job──►  Bahmni patient (by Bahmni ID)
        │                                              │
        │  creates "Bahmni Patient Registration"       │  writes "Avni Subject UUID"
        ▼  visit holding the Bahmni patient UUID       ▼
Avni person ◄──(2) visit sync── Bahmni visits (final-list forms, lab results, prescriptions)
            ◄──(3) catch-up──── past Bahmni visits, once, right after linking
```

| Flow | What it does | Status |
|---|---|---|
| **1. Link** | Reads new/edited "Individual ID Update" visits. For `Yes` + Bahmni ID: finds the Bahmni patient, creates/updates the bridge visit, writes the Avni ID into Bahmni. Errors: patient not found, same Bahmni ID on two Avni people (neither linked), ID changed (link moves). Latest ID Update wins | **To build** |
| **2. Visit sync** | `PatientEncounterEventWorker` reads Bahmni's encounter feed. Each mapped form in the visit becomes an Avni visit "Bahmni - <Form>". Lab results → "Bahmni - All_Tests_and_Panels", prescriptions → "Bahmni - Medication" (one readable line per medicine). Unlinked patients should be skipped quietly (today they are recorded as "No subject" errors — **to change**) | Built |
| **3. Catch-up** | After a person is linked, fetch their past Bahmni visits and run them through flow 2 | **To build** |

### How a Bahmni visit becomes Avni visits

- Each form the doctor filled is a group of answers in the Bahmni visit. A group whose concept set is mapped becomes one Avni visit, taken whole.
- A group that is not mapped is searched inside for mapped forms — e.g. "Obstetrics, P/A" recorded inside "Obstetrics" still syncs.
- Forms that doctors fill in parts (Operation Notes Template, ECG Report, RMRCT Form) are mapped to their list form explicitly (`extra_top_level_sets` in `final_forms.csv`).
- A question with several answers (e.g. Diabetes → Complaint) is sent to Avni as a list; the Avni form field is multi-select.
- Every Bahmni answer must have a mapping; an unknown answer fails that visit and is recorded in `error_record`.

---

## Which jobs run for JSS

Set with `BAHMNI_INT_APP_TASKS` on the integration server.

| Job | JSS | Why |
|---|---|---|
| ID link (new) | ✅ On | Flow 1 |
| `BahmniEncounter`, `BahmniLabResult` | ✅ On | Flow 2 |
| `BahmniErrorRecords` | ✅ On | Retries failed visits and links |
| `AvniSubject`, `AvniEnrolment`, `AvniProgramEncounter`, `AvniGeneralEncounter` | ❌ Off | Old approach that pushed Avni data into Bahmni (dropped March 2026) |
| `BahmniPatient` | ❌ Off | Old matching by an Avni registration field; replaced by flow 1 |

---

## What each system needs

| Where | What | How it gets there |
|---|---|---|
| Avni org | "Bahmni Patient Registration" visit type + "Bahmni Entity UUID" concept; a "Bahmni - <Form>" visit type and form per final-list form; "Bahmni - Medication" | One Metadata Zip built by `scripts/build_jss_avni_bundle.py` |
| Avni org | "Individual ID Update" form with the validation rule above | Already in JSSCP; rule added in App Designer |
| Avni org | An integration user | Avni admin |
| Bahmni | Person attribute type "Avni Subject UUID" (`ceddfccf-…`) | Exists on prerelease; check on production |
| Integration DB | Form, question and answer mappings + constants | Migration `V2_5_0__JSSBahmniFormMappings.sql` (generated, never edited) |
| Integration server | Avni and Bahmni credentials | Environment variables (`BAHMNI_AVNI_API_*`, `OPENMRS_*`) — never in properties files |

---

## Environments

| | Avni | Bahmni | Notes |
|---|---|---|---|
| Prerelease | prerelease.avniproject.org, org **JSSCP_UAT_Org** (user `apiuser@jsscp_org`); later "JSSCP Training" | jss-bahmni-prerelease.avniproject.org (JSS data up to Dec 2025) | Old trial mappings must be removed from the integration DB first: `db/onetime/JSS_ResetOldTrialMappings_prerelease.sql` (refuses to run after V2_5_0) |
| Production | JSSCP production org | JSS production Bahmni | Migrations reach production only through `main` |

---

## Production setup (checklist for the JSS deployment meeting)

The integration service runs on Avni's server (`int-prod`) and talks to JSS's Bahmni only through the Bahmni (OpenMRS) web API. It never connects to the Bahmni database.

| Need | From | Notes |
|---|---|---|
| Network path from `int-prod` to production Bahmni | JSS IT | Production Bahmni sits in JSS's network (the team uses an OpenVPN profile "JSS_Prod"). Either allow HTTPS from `int-prod`'s IP, or a server-to-server VPN |
| Bahmni API user for the integration | JSS IT | Read patients/encounters, write the "Avni Subject UUID" person attribute. Credentials go into `int-prod` environment variables (`OPENMRS_*`), never into git |
| Person attribute type "Avni Subject UUID" in production Bahmni | JSS IT / us | Same as prerelease (`ceddfccf-…`); check or create |
| Avni integration user in the production JSSCP org | Avni admin | `BAHMNI_AVNI_API_*` environment variables |
| Production bundle (forms + answers) | Us | Rebuild from **production** Bahmni data: the three read-only queries (forms in use, multi-select answers, answers used in visits) must run on the production Bahmni DB — either via someone with VPN access, or JSS IT runs the SQL and shares the output. Then upload the zip to the production JSSCP org |
| Jobs switched on | Us | See "Which jobs run for JSS" |

The Bahmni database and the JSS config repository are only used while **preparing** the bundle, not by the running service.

---

## Errors and reports

| Problem | Who fixes it | Where it shows |
|---|---|---|
| **Bahmni ID problems** — ID not found in Bahmni, matches more than one Bahmni patient, same ID on several Avni people, patient already linked to someone else | **JSSCP team**, by correcting the "Individual ID Update" form in Avni | **Metabase** report on the integration DB (avni-int), query `JSS Bahmni integration/reports/bahmni-link-errors.sql`: one row per person with name, village, cluster, Census ID, Bahmni ID entered, the problem and what to do. Rows disappear by themselves once the person links |
| **Sync problems** — an answer without a mapping, Bahmni timeouts, a broken Bahmni visit | **Avni team** | `error_record` in the integration DB, the integration admin app (error log), and the service logs |

- ID problems are stored per Avni individual (`error_record`, error types `BahmniPatientNotFound`, `MultipleBahmniPatientsWithId`, `BahmniIdOnMultiplePeople`, `BahmniPatientLinkedToAnotherPerson`) and are re-checked whenever the person's ID Update form changes — never by the generic retry job.
- A Bahmni ID that two people have is linked for neither. A Bahmni patient already linked stays linked; a second person claiming it gets "already linked to another person".
- Metabase can e-mail the report to JSSCP on a schedule.
- **Later (needs JSS agreement):** show the link status inside Avni (a read-only field on the person), so field staff see and fix problems without leaving Avni.

---

## Known limitations

| Limitation | Effect |
|---|---|
| Two parts of the same list form in one visit (e.g. ECG Report and Spirometry Report) | Both map to "Bahmni - General Patient Template"; the second may overwrite the first. Rare — check during testing |
| Prescriptions are one text line | Route and dosing instructions are not included (agreed Oct 2026) |
| Photos (Complex obs) | Not synced |

---

## Open questions (parked)

| Question | Needed for |
|---|---|
| How far back should the catch-up go — all history or from a date? | Flow 3 |
| Does Bahmni hold the Census ID anywhere? Should it be shown in Bahmni? | Nice to have |
| Who at JSSCP fixes "not found" / "duplicate" errors, and in what form do they want the report? | Go-live |
| Creating Bahmni patients for people with "Not yet" — clinic, ID generation, required fields | After go-live — see [06-bahmni-patient-creation.md](06-bahmni-patient-creation.md) |
