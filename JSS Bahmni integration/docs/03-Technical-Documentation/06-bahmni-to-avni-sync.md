# Bahmni → Avni Sync: JSS Ganiyari

## What This Covers

When a clinician at JSS Hospital records a consultation, lab result, or other encounter in Bahmni, that data needs to flow back to Avni so the field team can see what happened at the hospital. This document covers how we will achieve that sync, what steps are required per encounter type, what code changes are needed, and how to test it all.

---

## Key Design Decision: No Patient Creation

For JSS, patients already exist in Avni before they visit the hospital. The integration does **not** need to create new Avni subjects from Bahmni patients. The sync flow is:

1. Bahmni publishes an encounter event on its Atom feed
2. `PatientEncounterEventWorker` picks it up
3. It finds the matching Avni subject using the patient's Bahmni identifier (GAN prefix + number)
4. It creates or updates a General Encounter in Avni with the observation data

If no matching Avni subject is found, the record is logged as an error and skipped — no new subject is created.

---

## Encounters to Sync

*(Fill in the full list once confirmed — examples below from existing docs and tests)*

| Bahmni Encounter Type | Bahmni UUID | Avni Encounter Type to Create | Status |
|---|---|---|---|
| Consultation | `da7a4fe0-0a6a-11e3-939c-8c50edb4be99` | Bahmni - Consultation | To do |
| LAB_RESULT | `960469a8-9bc6-11e3-927e-8840ab96f0f1` | Bahmni - Lab Results | To do |
| RADIOLOGY | `949dba36-9bc6-11e3-927e-8840ab96f0f1` | Bahmni - Radiology | To do |
| Diabetes Intake Template | `60619143-5b49-4c10-92f4-0d080cd10b8a` | Bahmni - Diabetes Intake | Partially tested |
| *(add remaining)* | | | |

---

## What Is Already in Place

| Component | Status |
|---|---|
| `PatientEncounterEventWorker` — reads Bahmni encounter feed, finds Avni subject, creates general encounter | **Exists, working** |
| `PatientEventWorker` — reads Bahmni patient feed, finds or creates Avni subject | **Exists** (patient creation not needed for JSS) |
| Mapping infrastructure (`mapping_metadata` table, `BahmniMappingGroup`, `BahmniMappingType`) | **Exists** |
| Diabetes Intake encounter sync (observations only) | **Partially tested** — `debugDiabetesIntakeSync` ran for GAN279732 |
| Atom feed markers table — tracks last-read position per feed | **Exists** |
| Error recording — failed records logged, retried on next run | **Exists** |

**Not yet in place:**
- Avni forms and encounter types for most Bahmni encounters
- Mapping config for those encounter types and their observations
- Drug order / prescription sync (separate concern — see section below)
- Clean runnable tests with assertions for each encounter type

---

## Steps for Each Encounter Type

Repeat these steps for every encounter in the list above.

---

### Step 1 — Export the Bahmni Form

Export the Bahmni form as a CSV zip from Bahmni Admin → Dictionary → Export. You get:
- `concepts.csv` — all concepts (questions + answers)
- `concept_sets.csv` — form hierarchy

Full process: [04-Process-Guides/05-bahmni-to-avni-conversion.md](../04-Process-Guides/05-bahmni-to-avni-conversion.md)

---

### Step 2 — Convert to Avni Form Bundle

Run the conversion script to produce the Avni concepts and form JSON:

```bash
cd /Users/nupoorkhandelwal/Avni/integration-service
python3 scripts/bahmni_to_avni_converter.py
```

Edit the script config at the top to point to the right source folder:
```python
PREFIX = "Bahmni - "
SOURCE_DIR = "CSV dumps/<FormName>/"
OUTPUT_DIR = "avni_bundle"
```

Output:
- `avni_bundle/concepts.json` — all concepts prefixed with `Bahmni - `
- `avni_bundle/forms/Bahmni_-_<FormName>.json` — the form definition

**All fields must be read-only** (data comes from Bahmni, not entered in Avni):
```json
"keyValues": [{"key": "editable", "value": false}]
```

The form type should be `IndividualEncounter` (General Encounter) unless specifically otherwise.

---

### Step 3 — Upload to Avni

1. Upload `concepts.json` first (creates all concepts)
2. Upload the form JSON (creates the encounter type and form)
3. Verify the form renders correctly in Avni DEA

---

### Step 4 — Add Encounter Type Mapping (Integration DB)

In the integration service database, add a mapping that tells the code: "when you see Bahmni encounter type UUID X, create an Avni encounter of type Y".

```sql
-- Map Bahmni encounter type → Avni encounter type
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint,
    integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES (
    '<bahmni-encounter-type-uuid>',    -- from Bahmni encounter types list
    '<Avni encounter type name>',      -- exactly as created in Avni (e.g. 'Bahmni - Consultation')
    NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'GeneralEncounter'
        AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'PatientEncounter_EncounterType'
        AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(),
    false
);
```

For lab encounters, use mapping group `LabEncounter` and type `LabEncounter_EncounterType` instead. See [03-technical-guide.md](03-technical-guide.md) section 6.5 for full SQL templates.

---

### Step 5 — Add Observation Concept Mappings

For each observation field in the form, add a mapping row. For numeric and text fields:

```sql
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint,
    integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES (
    '<bahmni-concept-uuid>',
    'Bahmni - <Avni concept name>',  -- must match exactly what was uploaded to Avni
    'Numeric',                        -- or 'Text', 'Date'
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation'
        AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept'
        AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(),
    false
);
```

For **coded (dropdown) fields**, you need TWO types of entries — one for the question and one per answer option:

```sql
-- Question mapping (data_type_hint = 'Coded')
INSERT INTO mapping_metadata ... VALUES ('<question-uuid>', 'Bahmni - Some Question', 'Coded', ...);

-- Answer mappings (one per answer, no data_type_hint)
INSERT INTO mapping_metadata ... VALUES ('<answer-uuid-1>', 'Bahmni - Yes', NULL, ...);
INSERT INTO mapping_metadata ... VALUES ('<answer-uuid-2>', 'Bahmni - No', NULL, ...);
```

Without answer mappings, coded observations will fail with "Answer concept not found".

---

### Step 6 — Verify Mappings

```sql
-- Count mappings for this encounter type
SELECT COUNT(*) FROM mapping_metadata
WHERE avni_value = '<Avni encounter type name>'
AND is_voided = false
AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni');

-- List all observation mappings
SELECT mm.int_system_value, mm.avni_value, mm.data_type_hint, mt.name as type
FROM mapping_metadata mm
JOIN mapping_type mt ON mm.mapping_type_id = mt.id
WHERE mm.integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')
AND mm.is_voided = false
ORDER BY mt.name, mm.avni_value;
```

---

### Step 7 — Test (see Testing section below)

---

## Medications and Prescriptions

Drug orders (prescriptions) in Bahmni are **separate from observations**. A Consultation encounter in Bahmni contains observations AND drug orders, but the two are returned via different API endpoints:
- Observations: inside the encounter JSON
- Drug orders: via `/openmrs/ws/rest/v1/order?patient={uuid}&t=drugorder`

**Current state:** The integration code processes observations from encounters but does **not** handle drug orders. The test file has a test resource `defaultEncounterWithDrugOrders.json` but no active test or handler for it.

**What needs to happen:**
1. Decide whether prescriptions should go into the same Avni encounter (as extra fields) or a separate "Bahmni - Prescriptions" encounter type
2. The `PatientEncounterEventWorker` or a new `DrugOrderWorker` would need to:
   - Fetch drug orders for the patient from OpenMRS
   - Map drug name/dose/frequency concepts
   - Write them to the appropriate Avni encounter

This is a separate work item — **do not include prescription logic in the initial encounter sync**. Get observation sync working per encounter type first, then handle prescriptions.

---

## Code Changes Required

### No changes needed for standard observation sync

The existing `PatientEncounterEventWorker` already handles: encounter lookup → find Avni subject → map observations → create/update general encounter. Adding new encounter types is purely a **mapping config** task (Steps 4 and 5 above), not a code task.

### Changes needed for Prescription sync (future)

**`PatientEncounterEventWorker.java`**
`bahmni/src/main/java/.../worker/bahmni/atomfeedworker/PatientEncounterEventWorker.java`

After processing observations, add a call to fetch and map drug orders:
```java
// After encounter observation sync completes:
if (encounterTypeIsPrescriptionRelevant(encounter)) {
    drugOrderService.syncPrescriptions(bahmniPatient, avniSubject, encounter, constants);
}
```

**New `DrugOrderService.java`** (or extend `BahmniEncounterService`)
- Fetches drug orders via OpenMRS REST: `GET /openmrs/ws/rest/v1/order?patient={uuid}&t=drugorder&v=full`
- Maps drug concept UUID → Avni concept name via `mapping_metadata`
- Creates or updates Avni encounter observations for each drug order

**New mapping group and type** in `BahmniMappingGroup` and `BahmniMappingType`:
- Group: `DrugOrder`
- Type: `DrugOrder_Concept`

---

## Testing

### Test Class: `PatientEncounterEventWorkerExternalTest`

`bahmni/src/test/java/org/avni_integration_service/bahmni/worker/bahmni/PatientEncounterEventWorkerExternalTest.java`

#### Current state

| Test | Status | What it does |
|---|---|---|
| `processEncounter()` | `@Disabled` | Syncs one hardcoded patient + encounter |
| `processEncounterWithCodedDiagnosis()` | `@Disabled` | Coded diagnosis |
| `processLabEncounter()` | `@Disabled` | Lab result sync |
| `processDrugPrescriptionEncounter()` | `@Disabled` | Drug order (no handler yet) |
| `processProgramEncounter()` | `@Disabled` | ANC programme encounter |
| `debugDiabetesIntakeSync()` | Active | Observation-only sync for GAN279732 |
| `stepByStepDiabetesSync()` | Active | Step-by-step diagnostic for GAN279732 |

#### Changes needed per encounter type

For each encounter type being synced, add a dedicated test method in `PatientEncounterEventWorkerExternalTest`. The pattern for each test is:

```java
@Test
@Disabled  // remove @Disabled when ready to run
public void process<EncounterTypeName>Encounter() {
    // Step 1: Sync the patient first (ensures subject exists in Avni)
    patientEventWorker.process(patientEvent("<bahmni-patient-uuid>"));

    // Step 2: Sync the encounter
    patientEncounterEventWorker.process(encounterEvent("<bahmni-encounter-uuid>"));

    // Step 3: Verify in Avni (manual check or assertion)
    // - Find subject in Avni by patient identifier
    // - Load their general encounters
    // - Assert the expected encounter type exists
    // - Assert key observation values match
}
```

**Tests to add (one per encounter type in scope):**

| Test method to add | Encounter type | Patient to use |
|---|---|---|
| `processConsultationEncounter()` | Consultation | JSS patient with known consultation UUID |
| `processLabResultEncounter()` | LAB_RESULT | JSS patient with known lab result UUID |
| `processRadiologyEncounter()` | RADIOLOGY | JSS patient with known radiology UUID |
| `process<FormName>Encounter()` | *(each encounter in your list)* | JSS patient from prerelease |

**For each test, remove the existing `System.out.println` diagnostic pattern and add assertions:**
```java
// After sync, load the Avni encounter and assert
Subject subject = avniSubjectRepository.getSubjectByIdentifier("GAN279731");
assertNotNull(subject, "Subject should exist in Avni");

// Verify encounter was created (query Avni for general encounters)
// Assert specific observation values match Bahmni source data
```

#### Test class to update: `PatientEventWorkerExternalTest`

Since JSS patients already exist in Avni, add a test that verifies the worker correctly **finds** an existing subject rather than creating a new one:

```java
@Test
public void processPatientWhoAlreadyExistsInAvni() {
    // patient GAN279731 should already be in Avni
    // worker should find the existing subject, not create a duplicate
    openMrsPatientEventWorker.process(
        new Event("0", "/openmrs/ws/rest/v1/patient/<uuid-of-GAN279731>?v=full")
    );
    // verify no duplicate subjects exist
}
```

---

## Reference: Mapping Config Already Documented

The full SQL templates for adding encounter type and observation mappings are in:
- [03-technical-guide.md](03-technical-guide.md) — sections 6.4 (encounter type SQL), 6.5.1–6.5.3 (full templates)
- [04-mapping-configuration.md](04-mapping-configuration.md) — sections 4.1–4.3 (mapping groups, types, data types)

The Bahmni form to Avni conversion process is in:
- [04-Process-Guides/05-bahmni-to-avni-conversion.md](../04-Process-Guides/05-bahmni-to-avni-conversion.md)

---

*Last updated: 2026-04-29*
