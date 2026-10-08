# Runbook: Adding a New Bahmni Form to Avni Sync

**Purpose:** Step-by-step guide for syncing a new Bahmni form (encounter type) to Avni.  
Follow this when a new clinical form is added to Bahmni and needs to appear in Avni for JSS Ganiyari.

---

## Overview

Each Bahmni form is a **ConceptSet** — a group of concepts (fields). To sync it to Avni:

1. Export the Bahmni concept definitions (dump from Bahmni)
2. Create the Avni bundle (form JSON + concepts JSON)
3. Upload the bundle to Avni
4. Create the encounter type in Avni and map the form
5. Add a SQL migration for the DB mappings
6. Write a test to verify the sync
7. Deploy to production

---

## Step 1: Export Concepts from Bahmni

### 1a. Find the ConceptSet UUID
In Bahmni Admin → Manage Concepts, search for the form name (e.g., "Blood Pressure").  
Open the concept set — the UUID is shown in the URL or concept details.  
Note this UUID — it will be used as `int_system_value` in the SQL mapping.

### 1b. Export to CSV
Go to Bahmni Admin → Manage Concepts → Export.  
Export the concept set and its children as `concepts.csv` and `concept_sets.csv`.

**Alternatively:** Use the Bahmni REST API:
```
GET /openmrs/ws/rest/v1/concept?v=full&q=<ConceptSetName>
```

Save the CSVs to:
```
JSS Bahmni integration/CSV dumps/<FormName>/concepts.csv
JSS Bahmni integration/CSV dumps/<FormName>/concept_sets.csv
```

The UUID column in these CSVs is the Bahmni UUID — used in the SQL mapping.

---

## Step 2: Create the Avni Bundle

The bundle is a zip containing:
- `concepts.json` — concept definitions for Avni
- `forms/<FormName>.json` — form layout JSON

### 2a. Create concepts.json

Each concept from Bahmni maps to an Avni concept. Use the **same UUID** as Bahmni.  
Name the concept with the `Bahmni - ` prefix.

**File:** `JSS Bahmni integration/CSV dumps/avni_bundle/<FormName>/concepts.json`

```json
[
  {
    "name": "Bahmni - <ConceptName>",
    "uuid": "<same-uuid-as-bahmni>",
    "dataType": "Numeric",
    "active": true
  },
  ...
]
```

**dataType values:** `Numeric`, `Text`, `Coded`, `Date`, `DateTime`, `Boolean`

**For Coded concepts** — also add the answer concepts:
```json
{
  "name": "Bahmni - <AnswerName>",
  "uuid": "<answer-uuid-from-bahmni>",
  "dataType": "Coded",
  "active": true
}
```

**Rules:**
- Always include `"Bahmni Entity UUID"` as the first field in the form (not in concepts.json — it's already seeded in the DB)
- Mark all fields `"editable": false` since data comes from Bahmni, not entered in Avni
- Use the SAME UUID as in Bahmni — this is the key that links both systems

### 2b. Create the form JSON

**File:** `JSS Bahmni integration/CSV dumps/avni_bundle/<FormName>/forms/Bahmni_-_<FormName>.json`

```json
{
  "name": "Bahmni - <FormName>",
  "uuid": "<ConceptSet-UUID-from-Bahmni>",
  "formType": "Encounter",
  "formElementGroups": [
    {
      "uuid": "<generate-new-uuid>",
      "name": "Bahmni - <GroupName>",
      "displayOrder": 1.0,
      "formElements": [
        {
          "name": "Bahmni - <ConceptName>",
          "uuid": "<generate-new-uuid>",
          "keyValues": [{"key": "editable", "value": false}],
          "concept": {
            "name": "Bahmni - <ConceptName>",
            "uuid": "<bahmni-concept-uuid>",
            "dataType": "Numeric",
            "active": true
          },
          "displayOrder": 1.0,
          "type": "SingleSelect",
          "mandatory": false
        }
      ],
      "timed": false
    }
  ]
}
```

**Important:** Do NOT include `"encounterType"` in the form JSON — the encounter type is linked during Step 4, not here.

### 2c. Zip the bundle

```bash
cd "JSS Bahmni integration/CSV dumps/avni_bundle"
zip -r "<FormName>.zip" "<FormName>/"
```

Or run the generation script (if it exists):
```bash
python3 scripts/generate_avni_bundle.py "<FormName>"
```

---

## Step 3: Upload the Bundle to Avni

1. Go to **Avni Admin → Import** (bundle upload section)
2. Upload the `<FormName>.zip` file
3. Verify the form appears in **Admin → Forms**

---

## Step 4: Create Encounter Type in Avni and Map the Form

> ⚠️ **Critical order:** Upload bundle FIRST, then create encounter type.  
> If you create the encounter type first, the form mapping will break.

1. Go to **Avni Admin → Encounter Types → New**
2. Name: `Bahmni - <FormName>` (must match `avni_value` in the SQL mapping)
3. Subject type: **Individual**
4. **Form:** Select the uploaded form from the dropdown (this links them)
5. Save

If you see a "Duplicate form mapping" error when saving:
- Go to Admin → Forms → open the form → check Form Mappings section
- Void any stale/broken mapping
- Then save the encounter type again

---

## Step 5: Add the SQL Migration

Create a new migration file. Increment the version number from the latest:
```
integration-data/src/main/resources/db/migration/V2_4_XX__<FormName>Mappings.sql
```

### EncounterType mapping (ConceptSet → Avni encounter type):
```sql
-- <FormName>: maps Bahmni ConceptSet to Avni encounter type
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
SELECT '<ConceptSet-UUID-from-Bahmni>', 'Bahmni - <FormName>', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'GeneralEncounter'
        AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'EncounterType'
        AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false
WHERE NOT EXISTS (
    SELECT 1 FROM mapping_metadata WHERE int_system_value = '<ConceptSet-UUID-from-Bahmni>' AND is_voided = false
);
```

### Concept mappings (individual fields):
```sql
-- <ConceptName>
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
SELECT '<bahmni-concept-uuid>', 'Bahmni - <ConceptName>', '<Numeric|Text|Coded|Date|NULL>',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation'
        AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept'
        AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false
WHERE NOT EXISTS (
    SELECT 1 FROM mapping_metadata WHERE int_system_value = '<bahmni-concept-uuid>' AND is_voided = false
);
```

**For Coded concepts** — also add answer mappings (same SQL, no data_type_hint):
```sql
SELECT '<answer-uuid>', 'Bahmni - <AnswerName>', NULL, ...
```

### Apply the migration

The migration runs automatically when the integration service starts (Flyway).  
To apply manually to the integration DB:
```bash
psql -U postgres -d avni_int -f V2_4_XX__<FormName>Mappings.sql
```

---

## Step 6: Write and Run the Sync Test

Add a test method in:
```
bahmni/src/test/java/org/avni_integration_service/bahmni/worker/bahmni/PatientEncounterEventWorkerExternalTest.java
```

```java
@Test
@org.junit.jupiter.api.Tag("external")
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void process<FormName>() {
    System.out.println("\n========== <FormName> Sync — GAN279731 (Laxmi Prajapati) ==========");
    // Bahmni encounter UUID for a <FormName> encounter for this patient
    patientEncounterEventWorker.process(encounterEvent("<bahmni-encounter-uuid>"));
    System.out.println("→ Check Avni for 'Bahmni - <FormName>' encounter on GAN279731");
    System.out.println("========== Done ==========\n");
}
```

**To find the Bahmni encounter UUID:**  
Query the Bahmni DB:
```sql
SELECT e.uuid
FROM encounter e
JOIN encounter_type et ON e.encounter_type_id = et.id
JOIN patient_identifier pi ON e.patient_id = pi.patient_id
WHERE pi.identifier = 'GAN279731'
  AND et.name = 'Consultation'
  AND e.voided = 0
ORDER BY e.encounter_datetime DESC
LIMIT 5;
```
Then check which of those encounters has observations for your form's ConceptSet.

**Run the test:**
```bash
./gradlew :bahmni:test --tests "...PatientEncounterEventWorkerExternalTest.process<FormName>"
```

Verify in Avni that the encounter was created with the expected observations.

---

## Step 7: Production Deployment Checklist

When deploying to production (JSS Ganiyari production Bahmni + Avni):

- [ ] Upload `<FormName>.zip` bundle to **production Avni**
- [ ] Create encounter type `Bahmni - <FormName>` in **production Avni**, map the form during creation
- [ ] Apply the SQL migration to **production integration DB** (`avni_int` on production server)
- [ ] Restart the integration service so Flyway picks up the new migration
- [ ] Run a smoke test: trigger a sync for a known patient with a `<FormName>` encounter in production Bahmni
- [ ] Check the production Avni record for the patient and verify observations appear correctly

---

## Quick Reference: Key Concepts

| Term | Meaning |
|---|---|
| ConceptSet UUID | Bahmni's UUID for the form (root concept). Used as `int_system_value` in EncounterType mapping |
| `int_system_value` | Bahmni-side identifier in `mapping_metadata` |
| `avni_value` | Avni-side name in `mapping_metadata` (must exactly match the encounter type / concept name in Avni) |
| `mapping_type = EncounterType` | Maps a ConceptSet to an Avni encounter type |
| `mapping_type = Concept` | Maps an individual Bahmni concept to an Avni concept name |
| `mapping_group = GeneralEncounter` | Used for encounter type mappings |
| `mapping_group = Observation` | Used for concept/observation mappings |
| avni_bundle | Folder/zip of JSON files uploaded to Avni to create concepts and forms |

## Quick Reference: Already-Synced Forms (as of May 2026)

| Form | Avni Encounter Type | Migration |
|---|---|---|
| All_Tests_and_Panels (lab results) | Bahmni - All_Tests_and_Panels | V2_4_30, V2_4_31 |
| Drug Orders (Medication) | Bahmni - Medication | V2_4_32, V2_4_34 |
| Blood Pressure | Bahmni - Blood Pressure | V2_4_35 |
| Visit Diagnoses | Bahmni - Visit Diagnoses | V2_4_35 |
| OPD Information | Bahmni - OPD Information | V2_4_35 |
| Other Diagnoses | Bahmni - Other Diagnoses | V2_4_35 |
| Lab Samples | Bahmni - Lab Samples | V2_4_35 |
| Discharge Summary | Bahmni - Discharge Summary | V2_4_35 |
| Discharge Summary, Surgeries and Procedures | Bahmni - Discharge Summary, Surgeries and Procedures | V2_4_35 |
| Obstetrical History | Bahmni - Obstetrical History | V2_4_35 |
| Obstetrics, P/A (per abdomen) | Bahmni - Obstetrics, P/A (per abdomen) | V2_4_35 |
| Obstetrics, P/V (per vaginal) | Bahmni - Obstetrics, P/V (per vaginal) | V2_4_35 |
| Ante Natal Care Program, Outcomes | Bahmni - Ante Natal Care Program, Outcomes | V2_4_35 |
| Diabetes Intake Template | Bahmni - Diabetes Intake Template | V2_4_35 |
| Diabetes Counselling Template | Bahmni - Diabetes Counselling Template | V2_4_35 |
| CDST Forms | Bahmni - CDST Forms | V2_4_35 |
| All Disease Templates | Bahmni - All Disease Templates | V2_4_35 |
| All Observation Templates | Bahmni - All Observation Templates | V2_4_35 |
| Referral Form, Summary | Bahmni - Referral Form, Summary | V2_4_35 |
| Surgeries and Procedures | Bahmni - Surgeries and Procedures | V2_4_35 |
| Nutritional Values | Bahmni - Nutritional Values | V2_4_35 |
| Endoscopy | Bahmni - Endoscopy | V2_4_35 |
| Advanced Procedure | Bahmni - Advanced Procedure | V2_4_35 |
| Normal Procedure | Bahmni - Normal Procedure | V2_4_35 |
| REGISTRATION_CONCEPTS | Bahmni - REGISTRATION_CONCEPTS | V2_4_35 |

*Last updated: 2026-05-14*
