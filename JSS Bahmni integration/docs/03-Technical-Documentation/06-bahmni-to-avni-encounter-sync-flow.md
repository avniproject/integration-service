# Bahmni → Avni Encounter Sync: Complete Flow

Use this as a navigation guide when reading the code. Follow the call chain top-to-bottom.

---

## The Test Class

**File:** `bahmni/src/test/java/org/avni_integration_service/bahmni/worker/bahmni/PatientEncounterEventWorkerExternalTest.java`

This is an "external" test — it connects to real running systems (Bahmni prerelease and Avni prerelease). No mocks. Running it actually creates data in Avni.

Other tests in the same class you will encounter:
- `processEncounter()` — general encounter sync (disabled, older)
- `processLabEncounter()` — older lab sync test (disabled)
- `debugDiabetesIntakeSync()` — diabetes form sync
- `stepByStepDiabetesSync()` — same but with verbose step-by-step output
- `processAllTestsAndPanels()` — **the one we wrote** — syncs lab results for GAN279731

---

## How Every Test Starts: `@BeforeEach`

```
bahmniAvniSessionFactory.createSession()
  → authenticates with AWS Cognito using credentials from bahmni-secret.properties
  → gives avniHttpClient a token so it can call the Avni API

patientEncounterEventWorker.cacheRunImmutables(getConstants())
  → loads constants table from DB (e.g. OutpatientVisitTypes UUID)
  → loads ALL mapping_metadata rows from DB into memory (metaData object)
  → these stay cached for the whole test run
```

**File:** `bahmni/src/test/java/org/avni_integration_service/bahmni/BaseExternalTest.java`
- `getConstants()` — reads `constants` table from `avni_int_test` DB
- `encounterEvent(uuid)` — builds a fake atom feed Event with URL `/openmrs/ws/rest/v1/encounter/{uuid}?v=full` and title `"Encounter"`
- `patientEvent(uuid)` — same but for patient events

---

## The Complete Call Chain

### 1. Test calls `patientEncounterEventWorker.process(encounterEvent(uuid))`

`encounterEvent(uuid)` builds a fake atom feed Event:
```
title   = "Encounter"
content = "/openmrs/ws/rest/v1/encounter/{uuid}?v=full"
```

In production, Bahmni publishes these events automatically when data changes.
In the test, we build them manually.

---

### 2. `PatientEncounterEventWorker.process(event)` — lines 48–61

**File:** `bahmni/src/main/java/org/avni_integration_service/bahmni/worker/bahmni/atomfeedworker/PatientEncounterEventWorker.java`

```java
if (!"Encounter".equals(event.getTitle())) return;   // skip non-encounter events

BahmniEncounter bahmniEncounter = encounterService.getEncounter(event, metaData);
if (bahmniEncounter == null) return;                 // encounter was deleted

processEncounter(bahmniEncounter);
```

---

### 3. Fetch encounter from Bahmni — `BahmniEncounterService.getEncounter()`

**File:** `bahmni/src/main/java/org/avni_integration_service/bahmni/service/BahmniEncounterService.java` — lines 22–26

Delegates to `OpenMRSEncounterRepository.getEncounter(event)`:

**File:** `bahmni/src/main/java/org/avni_integration_service/bahmni/repository/OpenMRSEncounterRepository.java` — line 71

Makes a real HTTP GET to Bahmni:
```
GET https://jss-bahmni-prerelease.avniproject.org/openmrs/ws/rest/v1/encounter/{uuid}?v=full
```

Response is deserialized into `OpenMRSFullEncounter` — contains encounter type, patient, visit type, all observations.

Then wraps it: `new BahmniEncounter(encounter, metaData)`

`BahmniEncounter` can split the observations into separate logical forms using the ConvSet mappings in metaData (relevant for form-grouped obs, not for lab results).

---

### 4. Route the encounter — `processEncounter()` — lines 63–90

**File:** `PatientEncounterEventWorker.java`

```java
// First: find the Avni patient record using Bahmni patient UUID
GeneralEncounter avniPatient = subjectService.findPatient(metaData, bahmniPatientUuid);

// Then: decide which path to take
if (isProcessableLabEncounter(...)) {
    processLabEncounter(...)                      // ← our path for All_Tests_and_Panels

} else {
    if (isProcessablePrescriptionEncounter(...)) {
        processDrugOrderEncounter(...)            // drug prescriptions
    }
    for (BahmniSplitEncounter split : splitEncounters) {
        switch (mapping.getMappingGroup()) {
            case "GeneralEncounter" → processGeneralEncounter(...)   // diabetes intake etc.
            case "ProgramEncounter" → processProgramEncounter(...)   // ANC etc.
        }
    }
}
```

**`isProcessableLabEncounter`** checks two things (BahmniEncounterService.java lines 34–36):
1. Visit type UUID is in `OutpatientVisitTypes` constants (OPD visit = `f6ce7bf9-e349-11e3-983a-91270dcbd3bf`)
2. Encounter type UUID matches `labEncounterTypeMapping.getIntSystemValue()` (LAB_RESULT = `960469a8-9bc6-11e3-927e-8840ab96f0f1`)

Both must be true. The LAB_RESULT UUID comes from the `LabEncounterType` mapping in the DB (seeded by `V2_4_31__FixLabEncounterMapping.sql`).

**Finding the Avni patient:** `SubjectService.findPatient()` queries Avni for a GeneralEncounter where the `Bahmni Entity UUID` observation equals the Bahmni patient UUID. This works because the patient was previously synced via `PatientEventWorker`.

---

### 5. `processLabEncounter()` — lines 109–122

**File:** `PatientEncounterEventWorker.java`

```java
// Idempotency check: does this encounter already exist in Avni?
GeneralEncounter existingAvniEncounter = avniEncounterService.getLabResultGeneralEncounter(...);

if (existingAvniEncounter == null && avniPatient != null)  → createLabEncounter()   // first sync
if (existingAvniEncounter != null && avniPatient != null)  → updateLabEncounter()   // re-sync
if (existingAvniEncounter == null && avniPatient == null)  → NoSubjectWithIdException  // patient not synced
if (existingAvniEncounter != null && avniPatient == null)  → SubjectIdChangedException
```

`getLabResultGeneralEncounter` queries Avni: find a `Bahmni - All_Tests_and_Panels` encounter where `Bahmni Entity UUID` = this Bahmni encounter UUID.

This 4-branch pattern (existing×patient) is used by ALL encounter processing methods in this worker.

---

### 6. Map observations and POST to Avni — `AvniEncounterService.createLabEncounter()`

**File:** `bahmni/src/main/java/org/avni_integration_service/bahmni/service/AvniEncounterService.java` — lines 78–83

```java
GeneralEncounter encounter = openMRSEncounterMapper.mapToAvniEncounter(openMRSFullEncounter, metaData, avniPatient);
avniEncounterRepository.create(encounter);
```

**`mapToAvniEncounter`** — File: `bahmni/src/main/java/org/avni_integration_service/bahmni/mapper/OpenMRSEncounterMapper.java` — lines 42–51

Builds the Avni encounter object:
- `encounterType` = `"Bahmni - All_Tests_and_Panels"` (from `labEncounterTypeMapping.getAvniValue()` in DB)
- `subjectId` = Avni subject UUID
- Each Bahmni observation (Haemoglobin UUID → "Bahmni - Haemoglobin") mapped via `mapping_metadata` rows

**`avniEncounterRepository.create()`** — File: `avni/src/main/java/org/avni_integration_service/avni/repository/AvniEncounterRepository.java` — line 38

Makes a real HTTP POST to Avni:
```
POST https://prerelease.avniproject.org/api/encounter
Body: { encounterType, subjectId, encounterDateTime, observations: [...] }
```

---

## Full Call Chain Summary

```
Test
 └─ encounterEvent(uuid)                              builds fake atom feed Event
     └─ PatientEncounterEventWorker.process()         checks title, fetches encounter
         └─ BahmniEncounterService.getEncounter()
             └─ OpenMRSEncounterRepository             GET Bahmni /encounter/{uuid}?v=full
         └─ SubjectService.findPatient()               GET Avni (find patient by Bahmni UUID)
         └─ isProcessableLabEncounter()                checks constants + LabEncounterType DB mapping
         └─ processLabEncounter()
             └─ AvniEncounterService.getLabResultGeneralEncounter()   GET Avni (idempotency check)
             └─ AvniEncounterService.createLabEncounter()
                 └─ OpenMRSEncounterMapper.mapToAvniEncounter()       maps obs via DB mappings
                 └─ AvniEncounterRepository.create()                  POST Avni /api/encounter
```

---

## Key Files Reference

| Role | File |
|---|---|
| Test | `bahmni/src/test/.../worker/bahmni/PatientEncounterEventWorkerExternalTest.java` |
| Test base helpers | `bahmni/src/test/.../BaseExternalTest.java` |
| Main worker (router) | `bahmni/src/main/.../worker/bahmni/atomfeedworker/PatientEncounterEventWorker.java` |
| Fetch from Bahmni | `bahmni/src/main/.../service/BahmniEncounterService.java` |
| Bahmni HTTP calls | `bahmni/src/main/.../repository/OpenMRSEncounterRepository.java` |
| Create/update in Avni | `bahmni/src/main/.../service/AvniEncounterService.java` |
| Observation mapping | `bahmni/src/main/.../mapper/OpenMRSEncounterMapper.java` |
| Load DB mappings | `bahmni/src/main/.../service/MappingMetaDataService.java` |
| Avni HTTP calls | `avni/src/main/.../repository/AvniEncounterRepository.java` |

---

## DB Tables That Drive the Sync

All mappings are in the `avni_int_test` (test) / `avni_int` (production) databases.

| Table | Purpose |
|---|---|
| `mapping_type` | Defines types: `EncounterType`, `LabEncounterType`, `Concept`, etc. |
| `mapping_group` | Defines groups: `GeneralEncounter`, `ProgramEncounter`, etc. |
| `mapping_metadata` | The actual mappings: Bahmni UUID → Avni name |
| `constants` | Key-value config: `OutpatientVisitTypes`, `IntegrationBahmniProvider`, etc. |

**Critical rows for All_Tests_and_Panels** (seeded by `V2_4_31__FixLabEncounterMapping.sql`):
- `mapping_type.name = 'LabEncounterType'`
- `mapping_metadata`: `int_system_value = '960469a8-9bc6-11e3-927e-8840ab96f0f1'`, `avni_value = 'Bahmni - All_Tests_and_Panels'`
- `constants`: `key = 'OutpatientVisitTypes'`, `value = 'f6ce7bf9-e349-11e3-983a-91270dcbd3bf'`

---

## Why There Are Two Encounter Paths

| Path | When used | Observations structure |
|---|---|---|
| `isProcessableLabEncounter` → `processLabEncounter` | LAB_RESULT encounter type | Flat — each test result is a top-level obs directly on the encounter |
| `getSplitEncounters` → `processGeneralEncounter` / `processProgramEncounter` | All other forms | Grouped — obs are nested under a ConvSet UUID (the form's concept set) |

Lab results in JSS Bahmni are flat. That's why they need the `LabEncounterType` mapping path, not the `EncounterType` path.

---

*Last updated: 2026-05-13*
