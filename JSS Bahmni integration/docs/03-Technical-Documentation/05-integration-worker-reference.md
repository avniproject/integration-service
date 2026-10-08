# Integration Worker Reference

This document explains every worker class in the Avni-Bahmni integration — what data it handles, how it works, and how to test it.

---

## What Is a Worker?

A worker is the class responsible for syncing one type of data between Avni and Bahmni. Each worker:

- Fetches records from the source system that have changed since the last run
- Finds or creates the matching record in the destination system
- Tracks a sync cursor (last modified date or numeric ID) so it never re-processes the same records
- Records errors in the integration error table if something goes wrong, and retries on the next run

Workers run automatically on a schedule via `AvniBahmniMainJob`. No manual trigger is needed in production.

---

## Avni → Bahmni

Data flows from Avni to Bahmni. Workers poll the Avni API for records updated since the last sync, then write to OpenMRS (Bahmni).

| Entity Type | Worker Class | What It Does | Test Class |
|---|---|---|---|
| Subject | `SubjectWorker` | Polls Avni for updated subjects. Finds the matching Bahmni patient by identifier (prefix + Avni ID). If no patient exists, creates one. Writes the Avni subject UUID to a Bahmni person attribute (used for the "View on Avni" deep link). **Encounter sync is not used for JSS Ganiyari.** | `SubjectWorkerExternalTest` (general run), `SubjectWorkerAvniUuidSyncExternalTest` (UUID attribute write specifically) |
| Program Enrolment | `EnrolmentWorker` | Polls Avni for program enrolments (e.g. a woman enrolled in an ANC programme). Finds the matching Bahmni patient and creates or updates a Bahmni encounter to record the enrolment data. **Not required for JSS Ganiyari.** | `EnrolmentWorkerExternalTest` |
| General Encounter | `GeneralEncounterWorker` | Polls Avni for general (non-programme) encounters. Maps Avni observations to a Bahmni encounter under the appropriate visit. **Not required for JSS Ganiyari.** | `ProgramEncounterWorkerExternalTest` (shared file covers both) |
| Program Encounter | `ProgramEncounterWorker` | Polls Avni for programme-specific visits (e.g. ANC follow-up). Maps observations to a Bahmni encounter. **Not required for JSS Ganiyari.** | `ProgramEncounterWorkerExternalTest` |

### File Locations (Avni → Bahmni)

| Worker | Path |
|---|---|
| `SubjectWorker` | `bahmni/src/main/java/.../worker/avni/SubjectWorker.java` |
| `EnrolmentWorker` | `bahmni/src/main/java/.../worker/avni/EnrolmentWorker.java` |
| `GeneralEncounterWorker` | `bahmni/src/main/java/.../worker/avni/GeneralEncounterWorker.java` |
| `ProgramEncounterWorker` | `bahmni/src/main/java/.../worker/avni/ProgramEncounterWorker.java` |

---

## Bahmni → Avni

Data flows from Bahmni to Avni. There are two mechanisms:

- **Atom feed** — OpenMRS publishes a feed of changes in near-real-time. Workers subscribe to it and process each new event as it arrives.
- **First-run / backfill** — Workers query the Bahmni database directly using configurable SQL. Used once when first setting up the integration to import historical data.

| Entity Type | Worker Class | Mechanism | What It Does | Test Class |
|---|---|---|---|---|
| Patient | `PatientEventWorker` + `PatientWorker` | Atom feed | Listens for new or updated Bahmni patients. Creates or updates the matching Avni subject with name, date of birth, gender, and identifier. | `PatientEventWorkerExternalTest`, `PatientWorkerFullRunTest` |
| Encounter (General / Programme / Enrolment) | `PatientEncounterEventWorker` + `PatientEncounterWorker` | Atom feed | Listens for new or updated Bahmni encounters. Reads the encounter type and uses the mapping table to decide whether to write a general encounter, programme encounter, or enrolment in Avni. One worker handles all encounter types. | `PatientEncounterEventWorkerExternalTest` |
| Lab Result | `LabResultWorker` | DB query | Queries Bahmni DB directly for lab encounters using implementation-specific SQL. Passes each result to `PatientEncounterEventWorker` for the actual Avni write. | No dedicated test class |
| Patient (first-run backfill) | `PatientFirstRunWorker` | DB query | Reads all patients from Bahmni DB using configurable SQL. Processes each through `PatientEventWorker`. Run once during initial setup. | `PatientWorkerFullRunTest` |
| Encounter (first-run backfill) | `PatientEncounterFirstRunWorker` | DB query | Same as above but for encounters. Scans DB to backfill all historical encounter data. | `PatientWorkerFullRunTest` |

### File Locations (Bahmni → Avni)

| Worker | Path |
|---|---|
| `PatientEventWorker` | `bahmni/src/main/java/.../worker/bahmni/atomfeedworker/PatientEventWorker.java` |
| `PatientWorker` | `bahmni/src/main/java/.../worker/bahmni/PatientWorker.java` |
| `PatientEncounterEventWorker` | `bahmni/src/main/java/.../worker/bahmni/atomfeedworker/PatientEncounterEventWorker.java` |
| `PatientEncounterWorker` | `bahmni/src/main/java/.../worker/bahmni/PatientEncounterWorker.java` |
| `LabResultWorker` | `bahmni/src/main/java/.../worker/bahmni/LabResultWorker.java` |
| `PatientFirstRunWorker` | `bahmni/src/main/java/.../worker/bahmni/PatientFirstRunWorker.java` |
| `PatientEncounterFirstRunWorker` | `bahmni/src/main/java/.../worker/bahmni/PatientEncounterFirstRunWorker.java` |

---

## Running Tests

All worker test classes are external integration tests — they require live connections to both JSS Bahmni and Avni prerelease servers.

- All are annotated `@Disabled` by default. Remove the annotation to run a specific test.
- They are not part of the standard CI/CD build.
- Most extend `BaseExternalTest`, which handles loading DB constants and setting up authenticated sessions.

| Test Class | What It Covers |
|---|---|
| `SubjectWorkerExternalTest` | General Avni→Bahmni subject sync (full pagination run) |
| `SubjectWorkerAvniUuidSyncExternalTest` | Verifies Avni subject UUID is written to Bahmni person attribute for 5 known JSS patients |
| `EnrolmentWorkerExternalTest` | Avni→Bahmni enrolment sync for a specific enrolment UUID |
| `ProgramEncounterWorkerExternalTest` | Avni→Bahmni programme encounter and general encounter sync |
| `PatientEventWorkerExternalTest` | Bahmni→Avni patient sync for a specific Bahmni patient UUID |
| `PatientEncounterEventWorkerExternalTest` | Bahmni→Avni encounter sync with mapping verification |
| `PatientWorkerFullRunTest` | Full Bahmni→Avni runs via `PatientWorker` and `PatientEncounterWorker` |

---

## Key Behaviours

**Error recording** — if processing a record fails, the error is saved to the integration error table (via `AvniBahmniErrorService`) and the job continues with the next record. Failed records are retried on the next run.

**Sync cursor** — each worker stores a watermark (last modified datetime or numeric encounter ID) in `IntegratingEntityStatus`. On each run it only fetches records newer than this value.

**Ignored concepts** — observations for concepts listed in `avni_ignored_concepts` are stripped before being written to Bahmni, so internal Avni fields do not leak through.

**Voiding** — if a record is marked voided in Avni, the corresponding Bahmni encounter is voided (not deleted).

**Duplicate check** — `SubjectWorker` checks for duplicate Avni subjects with the same identifier before processing. If duplicates exist, it logs an error instead of syncing.

---

*Last updated: 2026-04-27*
