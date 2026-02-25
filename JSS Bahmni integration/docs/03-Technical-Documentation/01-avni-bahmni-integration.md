# Avni-Bahmni Integration Guide

## Overview

This document describes the bidirectional sync between Avni (community health worker app) and Bahmni (hospital management system). The integration enables:

1. **Bahmni → Avni**: Clinical encounters (e.g., Diabetes Intake) created in Bahmni are synced to Avni
2. **Avni → Bahmni**: Program encounters (e.g., ANC Clinic Visit) created in Avni are synced back to Bahmni as visits

This prevents duplicate data entry and keeps both systems in sync.

---

## How the Integration Works

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Avni-Bahmni Sync Service                 │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  Avni Database ←→ Sync Workers ←→ Bahmni (OpenMRS)          │
│    (incoming)      (mapping)       (outgoing)                │
│                                                               │
└─────────────────────────────────────────────────────────────┘

Core Components:
─────────────────
1. PatientEncounterEventWorker  → Bahmni → Avni (encounters)
2. ProgramEncounterWorker       → Avni → Bahmni (visits)
3. SubjectWorker                → Avni → Bahmni (patients)
4. Mapping Service              → Maps concepts, IDs, types
```

### Sync Directions

#### 1. Bahmni → Avni (Encounters to GeneralEncounters)

**Use Case**: Diabetes Intake encounter in Bahmni is synced to Avni

**Flow**:
```
Bahmni Encounter (Diabetes Intake)
    ↓
PatientEncounterEventWorker receives event
    ↓
Find patient in Avni by patient identifier (with GAN prefix)
    ↓
Create/Update GeneralEncounter in Avni
    ↓
Map observations to Avni form fields
    ↓
Store Bahmni UUID in "Bahmni Entity UUID" field (prevents duplicates)
```

**Key Points**:
- Patient identifier in Bahmni: `GAN279731` (with prefix)
- Patient identifier in Avni: `279731` (without prefix)
- Only encounters with "Encounter date time" are synced (incomplete/scheduled are filtered)
- Observations are mapped using concept UUIDs

#### 2. Avni → Bahmni (ProgramEncounters to Visits)

**Use Case**: ANC Clinic Visit program encounter in Avni is synced to Bahmni as a Visit

**Flow**:
```
Avni ProgramEncounter (ANC Clinic Visit)
    ↓
ProgramEncounterWorker checks if completed (has encounter date)
    ↓
Find/Get patient in Bahmni (using identifier without GAN prefix)
    ↓
Get or Create Visit in Bahmni (using program-specific visit type)
    ↓
Map encounter observations to visit observations
    ↓
Store Avni UUID in visit attributes (prevents duplicates)
```

**Key Points**:
- Only completed encounters (with encounter date time) are synced
- Creates a Bahmni Visit with encounters as children
- Uses the same visit type for all programs (currently "Avni Bahmni Visit Type")
- Stores Avni UUID in visit attributes for deduplication

---

## Mapping Configuration

All mappings are stored in the `mapping_metadata` table with three components:

1. **Mapping Group**: Category of data being mapped (Common, ProgramEncounter, etc.)
2. **Mapping Type**: Type of mapping (Concept, VisitType, etc.)
3. **Mapping Metadata**: The actual mapping (Bahmni UUID → Avni value)

### Core Mappings (Common Group)

These mappings are used across both sync directions:

| Mapping Type | Bahmni UUID | Avni Value | Purpose |
|---|---|---|---|
| `AvniUUID_Concept` | `3c474750-312b-4d79-b449-6e486ae7f34b` | Avni Entity UUID | Stores Avni encounter UUID in Bahmni observations |
| `AvniEventDate_Concept` | `94d9e354-8fe7-487d-9262-7807f76eb18c` | Avni Event Date | Stores Avni event date in Bahmni observations |
| `AvniUUID_VisitAttributeType` | `7e9ed688-f2c1-46f2-b904-bc528dee335a` | Avni UUID | Stores Avni UUID in visit attributes |
| `AvniEventDate_VisitAttributeType` | `7c24d353-718c-460f-8afc-3967461c8a01` | Avni Event Date | Stores event date in visit attributes |
| `BahmniUUID_Concept` | *(custom)* | Bahmni Entity UUID | Stores Bahmni UUID in Avni form fields |

### Program-Specific Mappings

#### ANC Clinic Visit

**Program Encounter Mappings**:
```
ProgramEncounter Group:
  - CommunityProgramEncounter_EncounterType:
    UUID 2d79e469-88e1-4bd8-9f39-743109962db8 → "ANC Clinic Visit"
  - CommunityProgramEncounter_BahmniForm:
    UUID 29a946e8-9153-474d-86e7-a0b3d26474c5 → "ANC Clinic Visit"
```

**Observation/Concept Mappings**:
Maps 68 Bahmni concepts to Avni form fields (Height, Weight, BMI, Blood Pressure, etc.)

**Visit Type Mapping**:
```
ProgramEnrolment Group:
  - CommunityEnrolment_VisitType:
    UUID 2ee11869-f426-44a5-9766-1fb195a1c56f (Bahmni) → "Pregnancy" (program name)
```

### Constants (Key-Value Pairs)

These are configuration values stored in the `constants` table:

| Constant | Value | Purpose |
|---|---|---|
| `IntegrationBahmniLocation` | `58638451-1102-4846-8462-503e0ddd792f` | Location UUID for visits/encounters |
| `IntegrationBahmniVisitType` | `2ee11869-f426-44a5-9766-1fb195a1c56f` | Default visit type for subject-based visits |
| `IntegrationBahmniProvider` | `apiuser` | Provider name for encounters |
| `IntegrationBahmniEncounterRole` | `apiuser` | Encounter role for provider |
| `IntegrationBahmniIdentifierType` | `b46af68a-c79a-11e2-b284-107d46e7b2c5` | Patient identifier type UUID |
| `BahmniIdentifierPrefix` | `GAN` | Prefix for patient identifiers in Bahmni |
| `IntegrationAvniSubjectType` | `Individual` | Avni subject type for patients |

---

## Key Concepts

### 1. Entity UUID Tracking

Both systems need to store references to each other's entities to prevent duplicates:

**Avni → Bahmni**:
- Avni encounter UUID is stored in Bahmni observation with concept `3c474750-312b-4d79-b449-6e486ae7f34b`
- On next sync, this UUID is checked to avoid creating duplicates

**Bahmni → Avni**:
- Bahmni encounter UUID is stored in Avni form field "Bahmni Entity UUID" (`a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d`)
- On next sync, this UUID is checked to avoid creating duplicates

### 2. Visit vs Encounter

**Bahmni**:
- **Visit**: Top-level container for a patient's visit (e.g., outpatient clinic visit on date X)
- **Encounter**: Clinical event within a visit (e.g., "ANC Clinic Visit" encounter)

**Avni**:
- **ProgramEncounter**: An encounter within a program enrollment (e.g., ANC Clinic Visit)
- Maps to Bahmni Visit + Encounter structure

### 3. Patient Identifier Format

| System | Format | Example | Notes |
|---|---|---|---|
| Avni | No prefix | `279731` | Used in SubjectWorker lookups |
| Bahmni | With prefix | `GAN279731` | Prefix defined in constants |
| Mapping | Uses Avni format | `279731` | PatientIdentifier_Concept maps to Avni value |

### 4. Completion Status

Encounters must have an "Encounter date time" to be synced:

```java
public boolean isCompleted() {
    return getEncounterDateTime() != null;  // true if date is set
}
```

**Implications**:
- Scheduled encounters (no date) are filtered out
- Only completed clinical encounters are synced
- This is working as designed - prevents incomplete data transfer

---

## Setup Steps

### Step 1: Create Concepts in Bahmni

Create the following concepts if they don't exist:

1. **Avni Entity UUID** (Text)
   - UUID: `3c474750-312b-4d79-b449-6e486ae7f34b`
   - Used to store Avni encounter UUIDs

2. **Avni Event Date** (Date)
   - UUID: `94d9e354-8fe7-487d-9262-7807f76eb18c`
   - Used to store event date from Avni

### Step 2: Create Visit Attribute Types

Create these visit attribute types in Bahmni Admin:

1. **Avni UUID** (Text)
   - UUID: `7e9ed688-f2c1-46f2-b904-bc528dee335a`
   - Stores Avni encounter/enrolment UUID

2. **Avni Event Date** (Date)
   - UUID: `7c24d353-718c-460f-8afc-3967461c8a01`
   - Stores event date from Avni

### Step 3: Add Concepts to Bahmni Forms

For each form that will sync with Avni (e.g., ANC Clinic Visit):
1. Open the form concept set in Bahmni
2. Add "Avni Entity UUID" as a member
3. This allows the observation to be saved within the form

### Step 4: Create Avni Forms

For each Bahmni form that will sync to Avni:
1. Create a form in Avni with the same structure
2. **IMPORTANT**: Add "Bahmni Entity UUID" (`a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d`) as the FIRST field
3. Mark all fields as `"readOnly": true` (data comes from Bahmni)
4. This stores the Bahmni encounter UUID for deduplication

### Step 5: Configure Integration Database

Run the SQL migration scripts:

```bash
# ANC Visit Type and Core Mappings
psql integration-db < integration-data/src/main/resources/db/onetime/ANCVisitTypeMappings.sql

# ANC Clinic Visit Form Mappings
psql integration-db < integration-data/src/main/resources/db/onetime/ANCClinicVisitMappings.sql

# Diabetes Intake Mappings (Bahmni → Avni)
psql integration-db < integration-data/src/main/resources/db/onetime/DiabetesIntakeMappings.sql
psql integration-db < integration-data/src/main/resources/db/onetime/DiabetesIntakeAnswerMappings.sql
```

### Step 6: Configure Environment

Set these environment variables in your profile (`jss_ganiyari` or equivalent):

```properties
# Avni Configuration
avni.url=https://avni.example.com
avni.username=integration-user
avni.password=password

# Bahmni Configuration
bahmni.url=https://bahmni.example.com
bahmni.username=admin
bahmni.password=password
```

### Step 7: Test the Sync

**Test Bahmni → Avni (Diabetes Intake)**:
```bash
cd /Users/nupoorkhandelwal/Avni/integration-service
JAVA_HOME=/Users/nupoorkhandelwal/Library/Java/JavaVirtualMachines/ms-17.0.16/Contents/Home \
./gradlew :bahmni:test --tests "PatientEncounterEventWorkerExternalTest.testSync" \
-Dspring.profiles.active=jss_ganiyari -i
```

**Test Avni → Bahmni (ANC Clinic Visit)**:
```bash
./gradlew :bahmni:test --tests "ProgramEncounterWorkerExternalTest.debugANCClinicVisitSync" \
-Dspring.profiles.active=jss_ganiyari -i
```

---

## Adding New Program Syncs

To add a new program (e.g., "Maternal Health"):

### 1. Create Mappings

**Encounter Type Mapping**:
```sql
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, ...)
VALUES (
    '<BAHMNI_MATERNAL_HEALTH_ENCOUNTER_TYPE_UUID>',
    'Maternal Health',  -- Program name in Avni
    NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'ProgramEncounter' ...),
    (SELECT id FROM mapping_type WHERE name = 'CommunityProgramEncounter_EncounterType' ...),
    uuid_generate_v4(),
    false
);
```

**Visit Type Mapping**:
```sql
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, ...)
VALUES (
    '2ee11869-f426-44a5-9766-1fb195a1c56f',  -- Reuse same visit type for all programs
    'Maternal Health',  -- Program name in Avni
    NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'ProgramEnrolment' ...),
    (SELECT id FROM mapping_type WHERE name = 'CommunityEnrolment_VisitType' ...),
    uuid_generate_v4(),
    false
);
```

### 2. Map Form Observations

Create mappings for each form field (similar to ANCClinicVisitMappings.sql)

### 3. Create Avni Form

Create form in Avni with:
- "Bahmni Entity UUID" as first field (readOnly)
- All fields marked readOnly
- Same structure as Bahmni form

### 4. Update Bahmni Form

Add "Avni Entity UUID" concept to the form's concept set

---

## Important Notes for Form Setup

### When Creating Avni Forms (for Bahmni → Avni):
- **ALWAYS** add "Bahmni Entity UUID" (UUID: `a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d`) as the **FIRST** field
- Mark ALL fields as `"readOnly": true` (data comes from Bahmni, not user-editable)
- This stores the Bahmni encounter UUID for deduplication

### When Creating Bahmni Forms (for Avni → Bahmni):
- **ALWAYS** add "Avni Entity UUID" (UUID: `3c474750-312b-4d79-b449-6e486ae7f34b`) as a member of the form's concept set
- This allows the observation to be saved within the form
- This stores the Avni encounter UUID for deduplication

---

## Current Configuration

### Configured Syncs

1. **Diabetes Intake** (Bahmni → Avni)
   - Bahmni encounter type: `60619143-5b49-4c10-92f4-0d080cd10b8a`
   - Avni form: "Diabetes Intake"
   - Status: ✅ Configured and tested

2. **ANC Clinic Visit** (Avni → Bahmni)
   - Avni program: "ANC"
   - Visit type: "Avni Bahmni Visit Type" (`2ee11869-f426-44a5-9766-1fb195a1c56f`)
   - Form: "ANC Clinic Visit" with 68 observation mappings
   - Status: ✅ Configured

### Configuration Summary

**Bahmni Location**: Avni Integration Location (`58638451-1102-4846-8462-503e0ddd792f`)

**Visit Type**: Avni Bahmni Visit Type (`2ee11869-f426-44a5-9766-1fb195a1c56f`)
- Used for ALL program encounters
- Single visit type simplifies configuration

**Provider/Role**: apiuser
- Used for all encounters created by integration

**Patient Identifier**: GAN + Avni ID (e.g., GAN279731)
- Prefix: `GAN`
- Identifier type: `b46af68a-c79a-11e2-b284-107d46e7b2c5`

---

## Troubleshooting

### Issue: "Post failed" error when creating visit

**Cause**: Missing or incorrect visit type mapping

**Solution**:
1. Verify `CommunityEnrolment_VisitType` mapping exists for the program
2. Check that the visit type UUID is correct in Bahmni
3. Ensure `IntegrationBahmniLocation` and `IntegrationBahmniVisitType` constants are set

### Issue: Encounters don't sync from Bahmni

**Possible causes**:
1. Encounter has no "Encounter date time" (scheduled, not completed)
2. Patient identifier doesn't match (check GAN prefix)
3. Concept mappings are missing

**Solution**:
1. Ensure encounter has a date set in Bahmni
2. Verify patient exists in Avni with correct identifier
3. Check observation concept UUIDs match mappings

### Issue: "Answer concept not found" error

**Cause**: Missing answer mappings for coded concepts

**Solution**:
For each coded concept with answer options, create TWO mappings:
1. **Question mapping**: Bahmni question UUID → Avni question name (with `data_type_hint = 'Coded'`)
2. **Answer mappings**: ONE for each possible answer (without data_type_hint)

Example:
```sql
-- Question mapping
INSERT INTO mapping_metadata (..., data_type_hint = 'Coded', ...)
VALUES ('question-uuid', 'Bahmni - Eye Exam', ...);

-- Answer mapping 1
INSERT INTO mapping_metadata (..., data_type_hint = NULL, ...)
VALUES ('answer1-uuid', 'Bahmni - Normal', ...);

-- Answer mapping 2
INSERT INTO mapping_metadata (..., data_type_hint = NULL, ...)
VALUES ('answer2-uuid', 'Bahmni - Abnormal', ...);
```

---

## Reference Documentation

- **Avni-Bahmni Integration Docs**: https://avni.readme.io/docs/avni-bahmni-integration-specific#core-mappings
- **OpenMRS REST API**: https://rest.openmrs.org/

## Code References

### Workers
- [ProgramEncounterWorker.java](../bahmni/src/main/java/org/avni_integration_service/bahmni/worker/avni/ProgramEncounterWorker.java) - Avni → Bahmni sync
- [PatientEncounterEventWorker.java](../bahmni/src/main/java/org/avni_integration_service/bahmni/worker/bahmni/PatientEncounterEventWorker.java) - Bahmni → Avni sync
- [SubjectWorker.java](../bahmni/src/main/java/org/avni_integration_service/bahmni/worker/avni/SubjectWorker.java) - Patient sync

### Services
- [VisitService.java](../bahmni/src/main/java/org/avni_integration_service/bahmni/service/VisitService.java) - Visit management
- [ProgramEncounterService.java](../bahmni/src/main/java/org/avni_integration_service/bahmni/service/ProgramEncounterService.java) - Program encounter sync
- [MappingService.java](../bahmni/src/main/java/org/avni_integration_service/bahmni/repository/intmapping/MappingService.java) - Mapping lookups

### Mapping Configuration
- [ANCVisitTypeMappings.sql](../integration-data/src/main/resources/db/onetime/ANCVisitTypeMappings.sql) - Visit type and common mappings
- [ANCClinicVisitMappings.sql](../integration-data/src/main/resources/db/onetime/ANCClinicVisitMappings.sql) - Observation mappings
- [DiabetesIntakeMappings.sql](../integration-data/src/main/resources/db/onetime/DiabetesIntakeMappings.sql) - Diabetes intake mappings

---

## Summary

The Avni-Bahmni integration provides bidirectional sync between the two systems:

1. **Mapping-based**: All data transformations are configuration-driven (no code changes needed for new fields)
2. **Idempotent**: UUID tracking prevents duplicate syncs
3. **Scalable**: Single visit type can handle multiple programs
4. **Flexible**: Easy to add new programs or forms

The key to success is:
- Proper mapping configuration (all 3 levels: group, type, metadata)
- Entity UUID tracking in both systems
- Correct patient identifier handling (with/without prefix)
- Form setup following the documented rules
