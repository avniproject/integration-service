# Comprehensive Technical Guide: Avni-Bahmni Integration for JSS

**Version:** 1.0
**Last Updated:** January 2025
**Purpose:** Technical reference for mapping config changes, integration config changes, and integration code changes

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Codebase Structure](#2-codebase-structure)
3. [Database Schema & Entities](#3-database-schema--entities)
4. [Bidirectional Mapping Configuration](#4-bidirectional-mapping-configuration)
5. [Avni to Bahmni Mapping (Outbound)](#5-avni-to-bahmni-mapping-outbound)
6. [Bahmni to Avni Mapping (Inbound)](#6-bahmni-to-avni-mapping-inbound)
7. [Integration Configuration Guide](#7-integration-configuration-guide)
8. [Code Changes Guide](#8-code-changes-guide)
9. [JSS-Specific Setup](#9-jss-specific-setup)
10. [Troubleshooting & Error Handling](#10-troubleshooting--error-handling)
11. [Quick Reference](#11-quick-reference)

---

## 1. Architecture Overview

### 1.1 Integration Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        AVNI-BAHMNI INTEGRATION ARCHITECTURE                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────┐                              ┌──────────────┐             │
│  │    AVNI      │                              │   BAHMNI     │             │
│  │  (Community  │◄────────────────────────────►│  (Hospital   │             │
│  │   Health)    │     Bidirectional Sync       │     EMR)     │             │
│  └──────────────┘                              └──────────────┘             │
│         │                                              │                    │
│         │ REST API                          AtomFeed + REST API             │
│         ▼                                              ▼                    │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                     INTEGRATION SERVICE                              │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌────────────┐  │   │
│  │  │SubjectWorker│  │EnrolmentWorker│ │EncounterWorker│ │PatientWorker│ │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └────────────┘  │   │
│  │                          │                                           │   │
│  │                          ▼                                           │   │
│  │  ┌─────────────────────────────────────────────────────────────┐    │   │
│  │  │              MAPPING & TRANSFORMATION LAYER                  │    │   │
│  │  │   SubjectMapper | EncounterMapper | ObservationMapper        │    │   │
│  │  └─────────────────────────────────────────────────────────────┘    │   │
│  │                          │                                           │   │
│  │                          ▼                                           │   │
│  │  ┌─────────────────────────────────────────────────────────────┐    │   │
│  │  │              INTEGRATION DATABASE                            │    │   │
│  │  │   mapping_metadata | constants | error_record                │    │   │
│  │  └─────────────────────────────────────────────────────────────┘    │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Data Flow Directions

| Direction | Source | Target | Mechanism | Data Types |
|-----------|--------|--------|-----------|------------|
| **Avni → Bahmni** | Avni REST API | OpenMRS REST API | Polling | Subjects, Enrolments, Program Encounters |
| **Bahmni → Avni** | OpenMRS AtomFeed | Avni REST API | Event-driven | Lab Results, Radiology, Consultations |

### 1.3 Key Design Principles

1. **Immutability**: Data flows one direction without updates - new records created for changes
2. **JSS ID as Primary Key**: Shared identifier links patients/individuals across systems
3. **Metadata-Driven Mapping**: Concept mappings stored in database, not code
4. **Error Isolation**: Failed records don't block other records from processing

---

## 2. Codebase Structure

### 2.1 Module Overview

```
integration-service/
├── avni/                    # Avni API client and domain models
├── bahmni/                  # Bahmni/OpenMRS integration logic
├── integration-data/        # Database entities, repositories, migrations
├── integrator/              # Main Spring Boot application & REST APIs
├── integration-common/      # Shared exceptions and mappers
├── metadata-migrator/       # CLI tool for bidirectional metadata migration
├── util/                    # Common utilities (JSON, dates, HTTP)
├── scripts/                 # Python utilities and AWS scripts
├── JSS Bahmni integration/  # JSS-specific configs and documentation
└── work/                    # Runtime working directories
```

### 2.2 Module Details

#### **avni/** - Avni Client Module
| Component | Path | Purpose |
|-----------|------|---------|
| AvniSession | `src/main/java/.../client/` | Manages Avni API authentication (Cognito/Keycloak) |
| AvniHttpClient | `src/main/java/.../client/` | REST API communication |
| AvniSubjectRepository | `src/main/java/.../repository/` | Subject CRUD operations |
| AvniEncounterRepository | `src/main/java/.../repository/` | Encounter CRUD operations |
| Subject, Enrolment | `src/main/java/.../domain/` | Domain models |

#### **bahmni/** - Bahmni Integration Module
| Component | Path | Purpose |
|-----------|------|---------|
| AvniBahmniMainJob | `src/main/java/.../job/` | Main job orchestrator |
| SubjectWorker | `src/main/java/.../worker/avni/` | Avni → Bahmni subject sync |
| EnrolmentWorker | `src/main/java/.../worker/avni/` | Avni → Bahmni enrolment sync |
| ProgramEncounterWorker | `src/main/java/.../worker/avni/` | Avni → Bahmni program encounter sync |
| PatientWorker | `src/main/java/.../worker/bahmni/` | Bahmni → Avni patient sync |
| PatientEncounterWorker | `src/main/java/.../worker/bahmni/` | Bahmni → Avni encounter sync |
| LabResultWorker | `src/main/java/.../worker/bahmni/` | Bahmni → Avni lab results sync |
| SubjectMapper | `src/main/java/.../mapper/avni/` | Maps Avni Subject to OpenMRS Encounter |
| OpenMRSPatientMapper | `src/main/java/.../mapper/bahmni/` | Maps OpenMRS Patient to Avni Encounter |
| BahmniMappingGroup | `src/main/java/.../service/` | Defines mapping groups |
| BahmniMappingType | `src/main/java/.../service/` | Defines mapping types |

#### **integration-data/** - Data Layer Module
| Component | Path | Purpose |
|-----------|------|---------|
| MappingMetaData | `src/main/java/.../domain/` | Core mapping entity |
| MappingGroup | `src/main/java/.../domain/` | Groups related mappings |
| MappingType | `src/main/java/.../domain/` | Categorizes mapping types |
| SQL Migrations | `src/main/resources/db/migration/` | Flyway migrations |
| Onetime Scripts | `src/main/resources/db/onetime/` | One-time setup SQL |

#### **scripts/** - Utility Scripts
| Script | Purpose |
|--------|---------|
| `apply_standard_prefix.py` | Adds "AVNI - " prefix to Bahmni concepts |
| `bahmni_to_avni_converter.py` | Converts Bahmni CSV to Avni JSON bundle |
| `generate_concept_summary.py` | Creates consolidated concept summary CSV |
| `validate_bahmni_csv.py` | Validates CSV before Bahmni import |

---

## 3. Database Schema & Entities

### 3.1 Core Tables

```sql
-- Integration System (multi-tenant support)
integration_system (id, name, system_type, uuid, is_voided)

-- Mapping Configuration
mapping_group (id, name, integration_system_id, uuid, is_voided)
mapping_type (id, name, integration_system_id, uuid, is_voided)
mapping_metadata (id, mapping_group_id, mapping_type_id, int_system_value,
                  avni_value, about, data_type_hint, integration_system_id, uuid, is_voided)

-- Constants
constants (id, name, value, integration_system_id, uuid, is_voided)

-- Error Tracking
error_record (id, avni_entity_type, integrating_entity_type, entity_id,
              processing_disabled, integration_system_id, uuid, is_voided)
error_record_log (id, error_record_id, error_type_id, error_msg, logged_at, body, uuid, is_voided)
error_type (id, name, comparison_operator, comparison_value, follow_up_step,
            integration_system_id, uuid, is_voided)

-- Sync Progress
integrating_entity_status (id, entity_type, read_upto_numeric, read_upto_date_time,
                           integration_system_id, uuid, is_voided)
```

### 3.2 Entity Relationships

```
integration_system (1) ─────┬───── (N) mapping_group
                            ├───── (N) mapping_type
                            ├───── (N) mapping_metadata
                            ├───── (N) constants
                            ├───── (N) error_record
                            └───── (N) integrating_entity_status

mapping_group (1) ────────────────── (N) mapping_metadata
mapping_type (1) ─────────────────── (N) mapping_metadata
error_record (1) ─────────────────── (N) error_record_log
error_type (1) ───────────────────── (N) error_record_log
```

---

## 4. Bidirectional Mapping Configuration

### 4.1 Mapping Groups (BahmniMappingGroup.java)

| Group Name | Purpose | Direction | Example Usage |
|------------|---------|-----------|---------------|
| `PatientSubject` | Maps patient/subject identifiers and attributes | Both | JSS ID, Name, DOB, Gender |
| `GeneralEncounter` | General encounter type mappings | Both | Registration, Referral |
| `ProgramEnrolment` | Program enrolment mappings | Avni → Bahmni | TB Enrolment, Pregnancy Enrolment |
| `ProgramEncounter` | Program encounter mappings | Avni → Bahmni | ANC Visit, TB Followup |
| `Observation` | Concept/observation mappings | Both | Lab results, Vitals |
| `Common` | Shared system-level mappings | Both | UUID concepts, Event dates |
| `LabEncounter` | Lab result mappings | Bahmni → Avni | LAB_RESULT encounter |
| `DrugOrder` | Prescription mappings | Bahmni → Avni | Consultation drug orders |

### 4.2 Mapping Types (BahmniMappingType.java)

| Type Name | Group | Direction | Purpose |
|-----------|-------|-----------|---------|
| `Concept` | Observation | Both | Maps observation concepts |
| `EncounterType` | PatientSubject | Both | Maps encounter types |
| `PersonAttributeType` | PatientSubject | Both | Maps person attributes |
| `CommunityRegistration_SubjectType` | PatientSubject | Avni → Bahmni | Subject type mapping |
| `CommunityRegistration_EncounterType` | PatientSubject | Avni → Bahmni | Registration encounter type |
| `CommunityProgramEnrolment_BahmniForm` | ProgramEnrolment | Avni → Bahmni | Program form mapping |
| `CommunityProgramEnrolment_EncounterType` | ProgramEnrolment | Avni → Bahmni | Program encounter type |
| `CommunityProgramEncounter_EncounterType` | ProgramEncounter | Avni → Bahmni | Followup encounter type |
| `CommunityProgramEncounter_BahmniForm` | ProgramEncounter | Avni → Bahmni | Followup form mapping |
| `PatientEncounter_EncounterType` | GeneralEncounter | Bahmni → Avni | Patient encounter type |
| `LabEncounter_EncounterType` | LabEncounter | Bahmni → Avni | Lab result encounter type |

### 4.3 Mapping Data Types

| Data Type | When to Use | Example |
|-----------|-------------|---------|
| `Coded` | Concept has coded answers | Blood Group, Gender |
| `Numeric` | Numeric values | Height, Weight, BP, Lab values |
| `Text` | Free text | Comments, Other (specify) |
| `Date` | Date values | Next visit date |
| `NA` | Grouping concepts (ConvSet) | Form sections |

---

## 5. Avni to Bahmni Mapping (Outbound)

### 5.1 Sync Scope Summary

| Avni Entity | Bahmni Entity | Visit Type | Encounter Type | Notes |
|-------------|---------------|------------|----------------|-------|
| Individual (with JSS ID) | Patient | - | - | Create if not exists |
| Program Enrolment | Encounter | Program-specific | Avni {Program} Enrolment | Maps to program encounter |
| Program Encounter | Encounter | Program-specific | Avni {Program} Followup | Maps to followup encounter |

### 5.2 Programs to Sync (7 Programs)

| Avni Program | Bahmni Visit Type | Enrolment Encounter | Followup Encounters |
|--------------|-------------------|---------------------|---------------------|
| Tuberculosis | Field-TB | Avni TB Enrolment | Avni TB Followup, TB Lab results |
| TB - INH Prophylaxis | Field-TB | Avni TB INH Enrolment | INH Prophylaxis follow up |
| Hypertension | Field-NCD | Avni HTN Enrolment | Hypertension Followup |
| Diabetes | Field-NCD | Avni DM Enrolment | Diabetes Followup, Diabetes lab test |
| Epilepsy | Field-NCD | Avni Epilepsy Enrolment | Epilepsy Followup |
| Sickle Cell | Field-NCD | Avni Sickle Cell Enrolment | Sickle cell lab test |
| Pregnancy | Field-MCH | Avni Pregnancy Enrolment | ANC Home Visit, ANC Clinic Visit, Delivery |
| Child | Field-MCH | Avni Child Enrolment | Child PNC, HBNC Encounter |

### 5.3 Avni Entities to Sync

#### 5.3.1 Program Enrolments (27 forms)

| Avni Enrolment Form | Bahmni Encounter Type | Priority |
|---------------------|----------------------|----------|
| Tuberculosis Enrolment | Avni TB Enrolment | High |
| TB - INH Prophylaxis Enrolment | Avni TB INH Enrolment | High |
| Hypertension Enrolment | Avni HTN Enrolment | High |
| Diabetes Enrolment | Avni DM Enrolment | High |
| Epilepsy Enrolment | Avni Epilepsy Enrolment | High |
| Sickle cell Enrolment | Avni Sickle Cell Enrolment | High |
| Pregnancy Enrolment | Avni Pregnancy Enrolment | High |
| Child Enrolment | Avni Child Enrolment | High |
| Heart Disease Enrolment | Avni Heart Disease Enrolment | Medium |
| Mental Illness Enrolment | Avni Mental Illness Enrolment | Medium |
| Stroke Enrolment | Avni Stroke Enrolment | Medium |

#### 5.3.2 Program Encounters (42 forms)

| Avni Program Encounter | Bahmni Encounter Type | Program |
|------------------------|----------------------|---------|
| TB Followup | Avni TB Followup | Tuberculosis |
| TB Lab results | Avni TB Lab Results | Tuberculosis |
| CBNAAT Result Form | Avni TB CBNAAT Results | Tuberculosis |
| LJ Result Form | Avni TB LJ Results | Tuberculosis |
| LPA Result Form | Avni TB LPA Results | Tuberculosis |
| TB referral status | Avni TB Referral | Tuberculosis |
| Hypertension Followup | Avni HTN Followup | Hypertension |
| Hypertension referral status | Avni HTN Referral | Hypertension |
| Diabetes Followup | Avni DM Followup | Diabetes |
| Diabetes lab test | Avni DM Lab Test | Diabetes |
| ANC Home Visit | Avni ANC Home Visit | Pregnancy |
| ANC Clinic Visit | Avni ANC Clinic Visit | Pregnancy |
| USG Report | Avni USG Report | Pregnancy |
| Lab Investigations | Avni Pregnancy Labs | Pregnancy |
| Delivery | Avni Delivery | Pregnancy |
| Mother PNC | Avni Mother PNC | Pregnancy |
| Child PNC | Avni Child PNC | Child |
| HBNC Encounter | Avni HBNC | Child |
| Sickle cell lab test | Avni Sickle Cell Lab | Sickle Cell |
| Epilepsy referral status form | Avni Epilepsy Referral | Epilepsy |

### 5.4 SQL Templates for Avni → Bahmni Mappings

#### 5.4.1 Program Enrolment Mapping

```sql
-- Step 1: Ensure mapping groups and types exist
INSERT INTO mapping_group (name, integration_system_id, uuid, is_voided)
SELECT 'ProgramEnrolment', id, uuid_generate_v4(), false
FROM integration_system WHERE name = 'bahmni'
ON CONFLICT DO NOTHING;

INSERT INTO mapping_type (name, integration_system_id, uuid, is_voided)
SELECT 'CommunityProgramEnrolment_EncounterType', id, uuid_generate_v4(), false
FROM integration_system WHERE name = 'bahmni'
ON CONFLICT DO NOTHING;

INSERT INTO mapping_type (name, integration_system_id, uuid, is_voided)
SELECT 'CommunityProgramEnrolment_BahmniForm', id, uuid_generate_v4(), false
FROM integration_system WHERE name = 'bahmni'
ON CONFLICT DO NOTHING;

-- Step 2: Create Program Enrolment → Encounter Type Mapping
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint,
    integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES (
    '<bahmni-encounter-type-uuid>',    -- e.g., UUID of "Avni TB Enrolment" encounter type
    'Tuberculosis',                     -- Avni program name
    NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'ProgramEnrolment'
        AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'CommunityProgramEnrolment_EncounterType'
        AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(),
    false
);

-- Step 3: Create Program Enrolment → Form Mapping (Concept Set)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint,
    integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES (
    '<bahmni-form-concept-set-uuid>',  -- UUID of Bahmni form/concept-set
    'Tuberculosis',                     -- Avni program name
    NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'ProgramEnrolment'
        AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'CommunityProgramEnrolment_BahmniForm'
        AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(),
    false
);
```

#### 5.4.2 Program Encounter Mapping

```sql
-- Program Encounter Type Mapping
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint,
    integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES (
    '<bahmni-encounter-type-uuid>',    -- UUID of "Avni TB Followup" encounter type
    'TB Followup',                      -- Avni encounter type name
    NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'ProgramEncounter'
        AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'CommunityProgramEncounter_EncounterType'
        AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(),
    false
);

-- Program Encounter Form Mapping
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint,
    integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES (
    '<bahmni-form-concept-set-uuid>',  -- UUID of Bahmni form/concept-set
    'TB Followup',                      -- Avni encounter type name
    NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'ProgramEncounter'
        AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'CommunityProgramEncounter_BahmniForm'
        AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(),
    false
);
```

#### 5.4.3 Observation/Concept Mapping

```sql
-- Template for observation/concept mapping (Avni → Bahmni)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint,
    integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES (
    '<bahmni-concept-uuid>',           -- Bahmni concept UUID
    '<avni-concept-name>',             -- Avni concept name (exactly as in form)
    '<Coded|Numeric|Text|Date>',       -- Data type
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation'
        AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept'
        AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(),
    false
);
```

### 5.5 Visit Types to Create in Bahmni

```sql
-- Run in OpenMRS database
INSERT INTO visit_type (name, description, uuid, creator, date_created) VALUES
('Field', 'General field visit from Avni', UUID(), 1, NOW()),
('Field-TB', 'TB program field visits', UUID(), 1, NOW()),
('Field-NCD', 'NCD program field visits (HTN, DM, Epilepsy, Sickle Cell)', UUID(), 1, NOW()),
('Field-MCH', 'MCH program field visits (Pregnancy, Child)', UUID(), 1, NOW());
```

### 5.6 Encounter Types to Create in Bahmni

```sql
-- Run in OpenMRS database
-- Registration
INSERT INTO encounter_type (name, description, uuid, creator, date_created) VALUES
('Avni Registration', 'Demographics from Avni field registration', UUID(), 1, NOW());

-- TB Program
INSERT INTO encounter_type (name, description, uuid, creator, date_created) VALUES
('Avni TB Enrolment', 'TB program enrollment from Avni', UUID(), 1, NOW()),
('Avni TB Followup', 'TB treatment followup from Avni', UUID(), 1, NOW()),
('Avni TB Lab Results', 'TB lab results from Avni', UUID(), 1, NOW()),
('Avni TB INH Enrolment', 'TB INH prophylaxis enrollment from Avni', UUID(), 1, NOW());

-- NCD Programs
INSERT INTO encounter_type (name, description, uuid, creator, date_created) VALUES
('Avni HTN Enrolment', 'Hypertension enrollment from Avni', UUID(), 1, NOW()),
('Avni HTN Followup', 'Hypertension followup from Avni', UUID(), 1, NOW()),
('Avni DM Enrolment', 'Diabetes enrollment from Avni', UUID(), 1, NOW()),
('Avni DM Followup', 'Diabetes followup from Avni', UUID(), 1, NOW()),
('Avni Epilepsy Enrolment', 'Epilepsy enrollment from Avni', UUID(), 1, NOW()),
('Avni Sickle Cell Enrolment', 'Sickle cell enrollment from Avni', UUID(), 1, NOW());

-- MCH Programs
INSERT INTO encounter_type (name, description, uuid, creator, date_created) VALUES
('Avni Pregnancy Enrolment', 'Pregnancy enrollment from Avni', UUID(), 1, NOW()),
('Avni ANC Visit', 'Antenatal care visit from Avni', UUID(), 1, NOW()),
('Avni Delivery', 'Delivery details from Avni', UUID(), 1, NOW()),
('Avni PNC', 'Postnatal care from Avni', UUID(), 1, NOW()),
('Avni Child Enrolment', 'Child enrollment from Avni', UUID(), 1, NOW()),
('Avni Child Followup', 'Child followup from Avni', UUID(), 1, NOW());
```

---

## 6. Bahmni to Avni Mapping (Inbound)

### 6.1 Sync Scope Summary

| Bahmni Entity | Avni Entity | Condition | Notes |
|---------------|-------------|-----------|-------|
| LAB_RESULT Encounter | General Encounter (Bahmni Lab Results) | Patient has JSS ID in Avni | Lab results from OpenELIS |
| RADIOLOGY Encounter | General Encounter (Bahmni Radiology) | Patient has JSS ID in Avni | X-ray, USG, ECG results |
| Consultation Encounter | General Encounter (Bahmni Consultation) | Patient has JSS ID in Avni | Diagnosis, Vitals, Prescriptions |

### 6.2 Bahmni Encounter Types to Sync

| Bahmni Encounter Type | UUID | Avni Encounter Type | Priority |
|-----------------------|------|---------------------|----------|
| LAB_RESULT | `81d6e852-3f10-11e4-adec-0800271c1b75` | Bahmni Lab Results | High |
| RADIOLOGY | `7c3f0372-a586-11e4-9beb-0800271c1b75` | Bahmni Radiology Report | High |
| Consultation | `da7a4fe0-0a6a-11e3-939c-8c50edb4be99` | Bahmni Consultation Notes | Medium |

### 6.3 Bahmni Observation Templates to Sync

| Template | UUID | Category | Priority |
|----------|------|----------|----------|
| Visit Diagnoses | `56104bb2-9bc6-11e3-927e-8840ab96f0f1` | Diagnosis | High |
| Nutritional Values | `3ccfba5b-82b6-43c3-939b-449f228b66d1` | Vitals | High |
| Blood Pressure | `f4762e56-e349-11e3-983a-91270dcbd3bf` | Vitals | High |

### 6.4 Lab Results to Sync (High Priority)

| Lab Panel | Bahmni Concepts | Avni Field | Programs |
|-----------|-----------------|------------|----------|
| Complete Blood Count (CBC) | Haemoglobin, WBC, Platelets | Hb, WBC, Platelet Count | All |
| Blood Sugar | FBS, PPBS, RBS, HbA1c | Fasting Sugar, PP Sugar, HbA1c | Diabetes |
| Kidney Function | Serum Creatinine, BUN | Creatinine, BUN | HTN, DM |
| Liver Function | ALT, AST, Bilirubin | SGPT, SGOT, Bilirubin | All |
| TB Panel | Sputum AFB, CBNAAT | AFB Result, CBNAAT Result | TB |
| Sickle Cell Panel | Sickling Test, Hb Electrophoresis | Sickling Result, Electrophoresis | Sickle Cell |
| Lipid Profile | Total Cholesterol, Triglycerides | Cholesterol, TG | HTN, DM |
| Thyroid | TSH, T3, T4 | TSH, T3, T4 | All |
| Urine Routine | Protein, Sugar, Pus Cells | Urine Protein, Urine Sugar | Pregnancy, DM |

### 6.5 SQL Templates for Bahmni → Avni Mappings

#### 6.5.1 Lab Result Encounter Type Mapping

```sql
-- Step 1: Ensure mapping groups exist
INSERT INTO mapping_group (name, integration_system_id, uuid, is_voided)
SELECT 'LabEncounter', id, uuid_generate_v4(), false
FROM integration_system WHERE name = 'bahmni'
ON CONFLICT DO NOTHING;

INSERT INTO mapping_type (name, integration_system_id, uuid, is_voided)
SELECT 'LabEncounter_EncounterType', id, uuid_generate_v4(), false
FROM integration_system WHERE name = 'bahmni'
ON CONFLICT DO NOTHING;

-- Step 2: Map Bahmni LAB_RESULT to Avni "Bahmni Lab Results"
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint,
    integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES (
    '81d6e852-3f10-11e4-adec-0800271c1b75',  -- Bahmni LAB_RESULT encounter type UUID
    'Bahmni Lab Results',                     -- Avni general encounter type (to create)
    NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'LabEncounter'
        AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'LabEncounter_EncounterType'
        AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(),
    false
);
```

#### 6.5.2 Radiology Encounter Type Mapping

```sql
-- Map Bahmni RADIOLOGY to Avni "Bahmni Radiology Report"
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint,
    integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES (
    '7c3f0372-a586-11e4-9beb-0800271c1b75',  -- Bahmni RADIOLOGY encounter type UUID
    'Bahmni Radiology Report',                -- Avni general encounter type (to create)
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

#### 6.5.3 Lab Concept Mapping (Bahmni → Avni)

```sql
-- Template for lab concept mapping
-- Maps Bahmni lab concept to Avni concept name
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint,
    integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES (
    '<bahmni-lab-concept-uuid>',       -- e.g., UUID for "Haemoglobin" in Bahmni
    'Hb',                              -- Avni concept name
    'Numeric',                         -- Data type
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation'
        AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept'
        AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(),
    false
);

-- Example: Sputum AFB Result (Coded)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint,
    integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES (
    '<bahmni-afb-concept-uuid>',
    'AFB Result',
    'Coded',  -- Coded because it has answers like Positive, Negative
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation'
        AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept'
        AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(),
    false
);
```

### 6.6 Avni Encounter Types to Create

These general encounter types need to be created in Avni to receive Bahmni data:

| Avni Encounter Type | Subject Type | Purpose |
|---------------------|--------------|---------|
| Bahmni Lab Results | Individual | Receives lab results from Bahmni |
| Bahmni Radiology Report | Individual | Receives radiology results from Bahmni |
| Bahmni Consultation Notes | Individual | Receives consultation notes from Bahmni |
| Bahmni Prescriptions | Individual | Receives drug orders from Bahmni |

### 6.7 Atom Feed Configuration

The integration service uses Bahmni's Atom feeds to detect changes:

| Feed | Endpoint | Processing |
|------|----------|------------|
| Patient Feed | `/openmrs/ws/atomfeed/patient/recent` | PatientEventWorker |
| Encounter Feed | `/openmrs/ws/atomfeed/encounter/recent` | PatientEncounterEventWorker |
| Lab Feed | `/openmrs/ws/atomfeed/lab/recent` | LabResultWorker |

**Feed Markers Table:**
```sql
-- Tracks last read position for each feed
SELECT * FROM markers WHERE feed_uri LIKE '%bahmni%';
```

---

## 7. Integration Configuration Guide

### 7.1 Environment Variables

#### Database Configuration
```bash
AVNI_INT_DATABASE=avni_int
AVNI_INT_DATABASE_PORT=5432
AVNI_INT_DB_USER=avni_int
AVNI_INT_DB_PASSWORD=<password>
```

#### Avni Connection
```bash
BAHMNI_AVNI_API_URL=https://app.avniproject.org
BAHMNI_AVNI_API_USER=<avni-user>
BAHMNI_AVNI_API_PASSWORD=<avni-password>
BAHMNI_AVNI_IDP_TYPE=Cognito  # or Keycloak
```

#### Bahmni/OpenMRS Connection
```bash
OPENMRS_BASE_URL=https://bahmni.example.org/openmrs
OPENMRS_USER=<openmrs-user>
OPENMRS_PASSWORD=<openmrs-password>
```

#### Scheduler Configuration
```bash
BAHMNI_SCHEDULE_CRON=0 */5 * * * ?      # Every 5 minutes
BAHMNI_SCHEDULE_CRON_FULL_ERROR=0 0 2 * * ?  # Daily at 2 AM
```

### 7.2 Constants Table Configuration

```sql
-- Required constants for JSS integration
INSERT INTO constants (name, value, integration_system_id, uuid, is_voided) VALUES
-- Bahmni Patient Identifier
('BahmniIdentifierPrefix', 'GAN',
    (SELECT id FROM integration_system WHERE name = 'bahmni'), uuid_generate_v4(), false),
('IntegrationBahmniIdentifierType', '<patient-identifier-type-uuid>',
    (SELECT id FROM integration_system WHERE name = 'bahmni'), uuid_generate_v4(), false),

-- Bahmni Provider & Location
('IntegrationBahmniProvider', '<provider-name>',
    (SELECT id FROM integration_system WHERE name = 'bahmni'), uuid_generate_v4(), false),
('IntegrationBahmniEncounterRole', '<encounter-role-uuid>',
    (SELECT id FROM integration_system WHERE name = 'bahmni'), uuid_generate_v4(), false),
('IntegrationBahmniLocation', '<location-uuid>',
    (SELECT id FROM integration_system WHERE name = 'bahmni'), uuid_generate_v4(), false),
('IntegrationBahmniVisitType', '<visit-type-uuid>',
    (SELECT id FROM integration_system WHERE name = 'bahmni'), uuid_generate_v4(), false),

-- Avni Configuration
('IntegrationAvniSubjectType', 'Individual',
    (SELECT id FROM integration_system WHERE name = 'bahmni'), uuid_generate_v4(), false),
('IntegrationAvniIdentifierConcept', 'JSS ID',
    (SELECT id FROM integration_system WHERE name = 'bahmni'), uuid_generate_v4(), false),

-- Outpatient Visit Types (for lab results, prescriptions)
('OutpatientVisitTypes', '<opd-visit-type-uuid>,<lab-visit-type-uuid>',
    (SELECT id FROM integration_system WHERE name = 'bahmni'), uuid_generate_v4(), false);
```

### 7.3 Build and Deploy

**Build Integration Service:**
```bash
make build-server
# Output: integrator/build/libs/integrator-0.0.2-SNAPSHOT.jar
```

**Run Server:**
```bash
java -jar integrator/build/libs/integrator-0.0.2-SNAPSHOT.jar
```

---

## 8. Code Changes Guide

### 8.1 When Code Changes Are Needed

| Scenario | Solution | Files to Modify |
|----------|----------|-----------------|
| New mapping group | Add to BahmniMappingGroup | `bahmni/.../BahmniMappingGroup.java` |
| New mapping type | Add to BahmniMappingType | `bahmni/.../BahmniMappingType.java` |
| New Avni → Bahmni entity | Create new Worker in `worker/avni/` | e.g., `NewEntityWorker.java` |
| New Bahmni → Avni entity | Create new Worker in `worker/bahmni/` | e.g., `NewBahmniEntityWorker.java` |
| Custom transformation | Extend Mapper | `bahmni/.../mapper/` |
| New error type | Add to BahmniErrorType | `bahmni/.../BahmniErrorType.java` |

### 8.2 Key Worker Classes

| Worker | Direction | Purpose |
|--------|-----------|---------|
| `SubjectWorker` | Avni → Bahmni | Syncs Avni Subjects to Bahmni Patients |
| `EnrolmentWorker` | Avni → Bahmni | Syncs Avni Program Enrolments to Bahmni Encounters |
| `ProgramEncounterWorker` | Avni → Bahmni | Syncs Avni Program Encounters to Bahmni Encounters |
| `GeneralEncounterWorker` | Avni → Bahmni | Syncs Avni General Encounters to Bahmni |
| `PatientWorker` | Bahmni → Avni | Syncs Bahmni Patients to Avni Subjects |
| `PatientEncounterWorker` | Bahmni → Avni | Syncs Bahmni Encounters to Avni General Encounters |
| `LabResultWorker` | Bahmni → Avni | Syncs Bahmni Lab Results to Avni |

### 8.3 Key Mapper Classes

| Mapper | Direction | Purpose |
|--------|-----------|---------|
| `SubjectMapper` | Avni → Bahmni | Maps Avni Subject to OpenMRS Encounter |
| `EnrolmentMapper` | Avni → Bahmni | Maps Avni Enrolment to OpenMRS Encounter |
| `EncounterMapper` | Avni → Bahmni | Maps Avni Encounter to OpenMRS Encounter |
| `OpenMRSPatientMapper` | Bahmni → Avni | Maps OpenMRS Patient to Avni Subject |
| `BahmniModuleObservationMapper` | Bahmni → Avni | Maps OpenMRS Observations to Avni format |

---

## 9. JSS-Specific Setup

### 9.1 JSS ID Setup

**Step 1: Create JSS ID Concept in Avni**
```sql
-- See: JSS Bahmni integration/avni_bundle/insert_jss_id_concept.sql
-- Concept UUID: abdac5eb-dded-4da9-b59e-4d285690a8c4
-- Concept Name: Avni Bahmni JSS ID
```

**Step 2: Create JSS ID Identifier Type in Bahmni**
```sql
-- Run in OpenMRS database
INSERT INTO patient_identifier_type (name, description, required, uuid, creator, date_created)
VALUES ('JSS ID', 'Avni-Bahmni Integration Identifier', 0, UUID(), 1, NOW());
```

### 9.2 Sync Eligibility Rules

**Avni → Bahmni:**
- Individual must have JSS ID populated
- Individual must be enrolled in one of the 7 programs in scope
- Program encounter must belong to a synced program

**Bahmni → Avni:**
- Patient must have JSS ID identifier
- JSS ID must exist in Avni (Individual found)
- Encounter type must be in sync scope (LAB_RESULT, RADIOLOGY, Consultation)

### 9.3 JSS CSV Files Reference

| File | Location | Purpose |
|------|----------|---------|
| Avni forms dump | `CSV dumps/Avni forms dump.csv` | Complete Avni forms with concepts |
| Avni Concept Mapping | `CSV dumps/Avni Concept Mapping.csv` | Concept answers for coded fields |
| ANC concepts | `CSV dumps/ANC_concepts.csv` | ANC form concepts |
| All Bahmni Obs Summary | `CSV dumps/All_Bahmni_Obs_Summary.csv` | Bahmni observation templates |

---

## 10. Troubleshooting & Error Handling

### 10.1 Common Error Types

| Error Type | Direction | Cause | Resolution |
|------------|-----------|-------|------------|
| `NoPatientWithId` | Bahmni → Avni | Patient not found in Avni | Verify JSS ID exists in Avni |
| `SubjectIdChanged` | Both | Avni Subject UUID changed | Re-map patient |
| `PatientIdChanged` | Both | Bahmni Patient UUID changed | Re-map subject |
| `MultipleSubjectsWithId` | Both | Duplicate JSS IDs | De-duplicate in source |
| `EntityIsDeleted` | Both | Entity voided in source | Skip or handle deletion |
| `NotACommunityMember` | Avni → Bahmni | Patient not in community scope | Verify eligibility |
| `ConceptNotMapped` | Both | Missing mapping for concept | Add mapping to mapping_metadata |

### 10.2 Error Queries

```sql
-- View recent errors
SELECT er.entity_id, et.name as error_type, erl.error_msg, erl.logged_at
FROM error_record er
JOIN error_record_log erl ON erl.error_record_id = er.id
JOIN error_type et ON erl.error_type_id = et.id
WHERE erl.logged_at > NOW() - INTERVAL '24 hours'
ORDER BY erl.logged_at DESC;

-- Disable processing for problematic record
UPDATE error_record SET processing_disabled = true WHERE entity_id = '<entity-id>';

-- Re-enable processing after fix
UPDATE error_record SET processing_disabled = false WHERE entity_id = '<entity-id>';
```

### 10.3 Sync Status Monitoring

```sql
-- Check sync progress
SELECT entity_type, read_upto_date_time, read_upto_numeric
FROM integrating_entity_status
WHERE integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni');

-- Check Atom feed markers
SELECT feed_uri, last_read_entry_id, feed_uri_for_last_read_entry
FROM markers;
```

---

## 11. Quick Reference

### 11.1 Key File Paths

| Purpose | Path |
|---------|------|
| Mapping SQL | `integration-data/src/main/resources/db/onetime/` |
| Migrations | `integration-data/src/main/resources/db/migration/` |
| Avni→Bahmni Workers | `bahmni/src/main/java/.../worker/avni/` |
| Bahmni→Avni Workers | `bahmni/src/main/java/.../worker/bahmni/` |
| Mappers | `bahmni/src/main/java/.../mapper/` |
| JSS Docs | `JSS Bahmni integration/` |
| Scripts | `scripts/` |

### 11.2 Common SQL Patterns

```sql
-- Get integration system ID
SELECT id FROM integration_system WHERE name = 'bahmni';

-- List all mappings for Avni → Bahmni
SELECT mm.avni_value, mm.int_system_value, mm.data_type_hint, mg.name as mapping_group, mt.name as mapping_type
FROM mapping_metadata mm
JOIN mapping_group mg ON mm.mapping_group_id = mg.id
JOIN mapping_type mt ON mm.mapping_type_id = mt.id
WHERE mm.integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')
AND mg.name IN ('ProgramEnrolment', 'ProgramEncounter')
AND mm.is_voided = false;

-- List all mappings for Bahmni → Avni
SELECT mm.avni_value, mm.int_system_value, mm.data_type_hint, mg.name as mapping_group, mt.name as mapping_type
FROM mapping_metadata mm
JOIN mapping_group mg ON mm.mapping_group_id = mg.id
JOIN mapping_type mt ON mm.mapping_type_id = mt.id
WHERE mm.integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')
AND (mg.name IN ('LabEncounter', 'GeneralEncounter') OR mt.name LIKE 'PatientEncounter%')
AND mm.is_voided = false;

-- Verify mapping exists
SELECT * FROM mapping_metadata
WHERE avni_value = 'TB Followup'
AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni');
```

### 11.3 Utility Scripts Usage

```bash
# Validate Bahmni CSV before import
python scripts/validate_bahmni_csv.py concepts.csv concept_sets.csv

# Apply AVNI prefix to concepts
python scripts/apply_standard_prefix.py 'Form Name' concepts.csv concept_sets.csv

# Convert Bahmni CSV to Avni JSON bundle
python scripts/bahmni_to_avni_converter.py concepts.csv concept_sets.csv

# Generate concept summary
python scripts/generate_concept_summary.py
```

---

## Sources

- [Avni Integration Developer Guide](https://avni.readme.io/docs/integration-developer-guide)
- [Avni-Bahmni Integration Specific](https://avni.readme.io/docs/avni-bahmni-integration-specific)
- [Cross-System Field Mapping](https://avni.readme.io/docs/cross-system-field-mapping)
- [Build, Deployment, Configuration](https://avni.readme.io/docs/build-deployment-and-configuration)
- JSS Bahmni integration folder documentation
- Codebase analysis of integration-service repository

---

*Document Version: 1.0*
*Created: January 2025*
