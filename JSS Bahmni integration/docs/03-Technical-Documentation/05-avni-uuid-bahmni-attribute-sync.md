# Avni Subject UUID → Bahmni Person Attribute Sync

## What This Does

When an Avni subject is synced to Bahmni, the Avni subject's UUID is written to a dedicated Bahmni person attribute on the corresponding patient. This UUID is the permanent link between the two systems and enables a "View on Avni" deep link from the Bahmni patient dashboard.

---

## Why It Is Needed

Bahmni and Avni are two separate systems. Without an explicit link:
- You cannot navigate from a Bahmni patient to their Avni record
- It is hard to verify that the correct Avni subject is linked to a Bahmni patient
- The "View on Avni" button (Phase 2) has no data to work with

Storing the Avni UUID as a person attribute is the simplest and most reliable link — it survives encounter changes, programme changes, and demographic updates.

---

## Key Values

| Item | Value |
|---|---|
| Bahmni Person Attribute Type UUID | `ceddfccf-7913-4a63-b60e-d3114f867e4b` |
| Integration DB constant key | `AvniSubjectUuidBahmniAttributeTypeUuid` |
| Avni subject URL (for deep link) | `https://app.avniproject.org/#/app/subject?uuid=<avni_subject_uuid>` |

---

## Where the Code Lives

| File | Role |
|---|---|
| `bahmni/src/main/java/.../worker/avni/SubjectWorker.java` | Orchestrates the sync; calls `PatientService` methods |
| `bahmni/src/main/java/.../service/PatientService.java` | Contains `writeAvniSubjectUuidToBahmni()` — the actual write logic |
| `bahmni/src/main/java/.../repository/openmrs/OpenMRSPersonRepository.java` | `setPersonAttribute()` — POSTs to OpenMRS REST API |
| `bahmni/src/main/java/.../ConstantKey.java` | Enum value `AvniSubjectUuidBahmniAttributeTypeUuid` |

---

## When the Write Happens

The attribute is written in three situations, all inside `PatientService`:

| Situation | Method | When |
|---|---|---|
| New Avni subject, patient already exists in Bahmni | `createSubject()` | After encounter is created |
| Avni subject already synced before (update) | `updateSubject()` | After encounter is updated |
| New Avni subject, no Bahmni patient yet | `createPatientAndSubject()` | After patient + encounter are created |
| Encounter inconsistency (SubjectIdChangedException) | `writeAvniUuidToPatient()` via `SubjectWorker` catch block | Even when encounter sync fails, the person link is still written |

The fourth case is important — even if the encounter state in Bahmni is inconsistent, the UUID attribute is still written so the deep link is not lost.

---

## How the Write Works

```
SubjectWorker.processSubject()
  └── PatientService.createSubject() / updateSubject() / createPatientAndSubject()
        └── writeAvniSubjectUuidToBahmni(subject, patient, constants)
              ├── reads AvniSubjectUuidBahmniAttributeTypeUuid from constants
              ├── checks if attribute already exists on the person
              │     └── if yes → POST to person/{uuid}/attribute/{existingAttrUuid}  (update)
              │     └── if no  → POST to person/{uuid}/attribute                     (create)
              └── OpenMRSPersonRepository.setPersonAttribute()
```

The check for an existing attribute avoids creating duplicate person attributes on repeated syncs.

---

## DB Setup Required

The constant must be seeded in the integration service database for the JSS organisation:

```sql
INSERT INTO constants (key, value, organisation_id)
SELECT 'AvniSubjectUuidBahmniAttributeTypeUuid',
       'ceddfccf-7913-4a63-b60e-d3114f867e4b',
       id
FROM organisations
WHERE name = 'JSS'
ON CONFLICT (key, organisation_id) DO NOTHING;
```

The Flyway migration for this is:
`integration-data/src/main/resources/db/migration/V2_4_29__AvniSubjectUuidBahmniAttributeType.sql`

---

## Bahmni Setup Required

The person attribute type must exist in Bahmni with UUID `ceddfccf-7913-4a63-b60e-d3114f867e4b`. It should be:

- **Name:** Avni Subject UUID (or similar)
- **Format:** Text
- **Searchable:** No (internal link, not for clinical use)

Verify it exists: Bahmni Admin → Person Attribute Types → search for the UUID.

### Make the Attribute Read-Only in Bahmni UI

The UUID is written by the integration service and must not be editable by Bahmni users. Add the attribute name to `"readOnlyExtraIdentifiers"` in `app.json` — the value will be visible on the registration screen but cannot be edited:

```json
"patientConfig": {
  "readOnlyExtraIdentifiers": ["Avni Subject UUID"]
}
```

**How to apply:** The `app.json` file lives on the Bahmni server (path varies by installation, typically under the Bahmni configuration directory). Requires server file access to edit and a cache clear / app restart to take effect.

---

## Testing

The end-to-end test for this feature is:

**Class:** `SubjectWorkerAvniUuidSyncExternalTest`
**Path:** `bahmni/src/test/java/.../worker/avni/SubjectWorkerAvniUuidSyncExternalTest.java`

**What it does:**
1. For each of 5 known JSS identifier suffixes (279731–279735):
   - Searches Avni for the subject by identifier concept value
   - Finds the matching Bahmni patient by `BahmniIdentifierPrefix + suffix`
   - Calls `subjectWorker.processSubject(avniSubject, false)` — the `false` means the sync cursor is NOT advanced
   - Reloads the Bahmni patient and checks that the person attribute equals the Avni subject UUID
2. Asserts at least one patient was successfully synced

**Note:** `updateSyncStatus = false` is critical in tests — it prevents the sync cursor from advancing, so the scheduled job is not affected.

**Result as of 2026-04-27:** GAN279731 — PASSED. GAN279732–279735 test run in progress.

---

## What Is NOT Yet Tested

| Gap | Risk |
|---|---|
| Create new Bahmni patient path (`createPatientAndSubject`) | Not tested — registration mapping not configured yet |
| All 5 patients passing | Test run in progress at time of writing |
| Production scheduled job run | Not run — registration mapping must be in place first |

---

---

## Phase 3: Code Changes

These are the Java changes required to implement the simplified Avni → Bahmni sync (patient creation + UUID write only, no encounter sync).

---

### 1. `ConstantKey.java`
`bahmni/src/main/java/org/avni_integration_service/bahmni/ConstantKey.java`

Add a new enum value:

```java
AvniSubjectUuidBahmniAttributeTypeUuid
```

Currently missing from the enum — the constant is already seeded in the DB (migration `V2_4_29`) but the Java enum does not declare it yet, so it cannot be read via `constants.getValue(ConstantKey.AvniSubjectUuidBahmniAttributeTypeUuid.name())`.

---

### 2. `OpenMRSPersonRepository.java`
`bahmni/src/main/java/org/avni_integration_service/bahmni/repository/openmrs/OpenMRSPersonRepository.java`

Currently only has `createPerson()`. Add two methods:

**`getPersonAttributes(String personUuid)`**
- GET `/openmrs/ws/rest/v1/person/{personUuid}/attribute?v=default`
- Returns the list of existing person attributes so the caller can check if the UUID attribute already exists before writing
- Needed to avoid creating duplicate attributes on repeated syncs

**`setPersonAttribute(String personUuid, String attributeTypeUuid, String value)`**
- POST `/openmrs/ws/rest/v1/person/{personUuid}/attribute`
- Payload: `{ "attributeType": "<attributeTypeUuid>", "value": "<value>" }`
- Used for both create (new attribute) and update (existing attribute — OpenMRS upserts on POST to this endpoint)

---

### 3. `PatientService.java`
`bahmni/src/main/java/org/avni_integration_service/bahmni/service/PatientService.java`

Currently has no UUID-write logic. Add two methods:

**`writeAvniSubjectUuidToBahmni(Subject subject, OpenMRSPatient patient, Constants constants)`**
- Reads `AvniSubjectUuidBahmniAttributeTypeUuid` from constants
- Calls `openMRSPersonRepository.getPersonAttributes(patient.getUuid())` to check for an existing attribute
- Calls `openMRSPersonRepository.setPersonAttribute(personUuid, attributeTypeUuid, subject.getUuid())` to create or update

**`createPatientOnly(Subject subject, SubjectToPatientMetaData metaData, Constants constants)`**
- Replaces the old `createPatientAndSubject()` for JSS (which created patient + encounter)
- Calls the existing private `createPatient(subject, metaData, constants)` — no change to that method
- Calls `getPatient(newPatient.getUuid())` to fetch the full patient object
- Calls `writeAvniSubjectUuidToBahmni(subject, fullPatient, constants)`
- Calls `avniBahmniErrorService.successfullyProcessed(subject)`

---

### 4. `SubjectWorker.processSubject()`
`bahmni/src/main/java/org/avni_integration_service/bahmni/worker/avni/SubjectWorker.java`

**Current logic (lines 106–127):** calls `findSubject()` which returns `Pair<OpenMRSPatient, OpenMRSFullEncounter>`, then routes into 4 branches (updateSubject / createSubject / createPatientAndSubject / SubjectIdChangedException catch).

**Replace with:**

```java
OpenMRSPatient patient = patientService.findPatient(subject, constants, metaData);
if (patient == null) {
    patientService.createPatientOnly(subject, metaData, constants);
} else {
    patientService.writeAvniSubjectUuidToBahmni(subject, patient, constants);
}
```

**Also remove these now-unused imports:**
- `OpenMRSFullEncounter`
- `Pair` (from javatuples)
- `PatientEncounterEventWorker` (was only needed for the SubjectIdChangedException catch)

---

### 5. Workers not scheduled for JSS Ganiyari

`EnrolmentWorker`, `GeneralEncounterWorker`, and `ProgramEncounterWorker` are not needed. No code deletion required — they simply will not be wired into the scheduled job for the JSS org.

---

*Last updated: 2026-04-29*
