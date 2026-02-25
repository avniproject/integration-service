# Comprehensive Technical Guide: Avni-Bahmni Integration for JSS

**Version:** 1.2
**Last Updated:** February 4, 2025
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
12. [ANC Clinic Visit Mapping Implementation - Case Study](#12-anc-clinic-visit-mapping-implementation---case-study)
13. [Subject to Bahmni Mapping Implementation - Case Study](#13-subject-to-bahmni-mapping-implementation---case-study)
14. [Summary of SQL Mapping Files](#14-summary-of-sql-mapping-files)

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
BAHMNI_AVNI_API_URL=https://prerelease.avniproject.org
BAHMNI_AVNI_API_USER=nupoork@jsscp
BAHMNI_AVNI_API_PASSWORD=password
BAHMNI_AVNI_IDP_TYPE=Cognito  # or Keycloak
```

#### Bahmni/OpenMRS Connection
```bash
OPENMRS_BASE_URL=https://jss-bahmni-prerelease.avniproject.org/openmrs/
OPENMRS_USER=apiuser
OPENMRS_PASSWORD=Apiuser23
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
- **Method:** Metadata upload via Avni admin interface
- **Concept UUID:** abdac5eb-dded-4da9-b59e-4d285690a8c4
- **Concept Name:** Avni Bahmni JSS ID
- **Data Type:** Text
- **Reference:** See concept.json in avni_bundle folder

**Step 2: Create JSS ID Identifier Type in Bahmni**
- **Method:** Created via Bahmni admin UI interface
- **Identifier Type Name:** JSS ID
- **Description:** Avni-Bahmni Integration Identifier
- **Required:** No
- **Note:** Created in Bahmni, then exported and uploaded to Avni

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

## 12. ANC Clinic Visit Mapping Implementation - Case Study

### 12.1 Overview

This section documents the complete process of implementing ANC Clinic Visit mappings from Avni to Bahmni on the local avni-int database. This serves as a practical example of the mapping configuration process.

**Completed:** February 3, 2025  
**Scope:** 70 total mappings (2 encounter + 68 observation)  
**Direction:** Avni → Bahmni (Outbound)

### 12.2 Pre-Implementation Analysis

#### 12.2.1 Form Complexity Assessment
- **Form Name:** ANC Clinic Visit
- **Total Concepts:** 268 concepts across 12 sections
- **Data Types:** Coded (43), Numeric (19), Date (5), Text (3)
- **Sections:** Visit Details, Anthropometry, Complaints, Examination, Investigations, etc.

#### 12.2.2 Bahmni Target Analysis
- **Target Encounter Type:** `Pregnancy- ANC Clinic Visit [A]`
- **Target Form:** `Avni - JSS ANC Clinic Visit` (Concept Set)
- **Visit Type:** Field-MCH (Maternal Child Health)

### 12.3 Implementation Process

#### Step 1: Database Structure Preparation
```sql
-- Verified required mapping groups and types exist
INSERT INTO mapping_group (name, integration_system_id, uuid, is_voided)
SELECT 'Observation', id, uuid_generate_v4(), false
FROM integration_system WHERE name = 'bahmni'
ON CONFLICT DO NOTHING;

INSERT INTO mapping_group (name, integration_system_id, uuid, is_voided)
SELECT 'ProgramEncounter', id, uuid_generate_v4(), false
FROM integration_system WHERE name = 'bahmni'
ON CONFLICT DO NOTHING;
```

#### Step 2: Encounter Type Mapping
```sql
-- Maps Avni "ANC Clinic Visit" to Bahmni "Pregnancy- ANC Clinic Visit [A]"
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint,
    integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES (
    '2d79e469-88e1-4bd8-9f39-743109962db8',  -- Bahmni encounter type UUID
    'ANC Clinic Visit',                       -- Avni encounter type name
    NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'ProgramEncounter' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'CommunityProgramEncounter_EncounterType' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(),
    false
);
```

#### Step 3: Form Mapping
```sql
-- Maps ANC Clinic Visit form to Bahmni concept set
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint,
    integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES (
    '29a946e8-9153-474d-86e7-a0b3d26474c5',  -- Bahmni concept set UUID
    'ANC Clinic Visit',                       -- Avni encounter type name
    NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'ProgramEncounter' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'CommunityProgramEncounter_BahmniForm' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(),
    false
);
```

#### Step 4: Observation Mappings (68 concepts)

**Key Mapping Categories:**

1. **Anthropometry (5 mappings)**
   - Height → `Avni - Height` (Numeric)
   - Weight → `Avni - Weight at diagnosis` (Numeric)
   - BMI → `Avni - ANC - BMI` (Numeric)

2. **Vitals & Examination (15 mappings)**
   - Blood Pressure (systolic/diastolic) → `Avni - Blood Pressure` (Numeric)
   - FHS, Fetal movement → `Avni - FHS`, `Avni - Foetus movement` (Coded)

3. **Laboratory Investigations (25 mappings)**
   - Haemoglobin → `Avni - Hb` (Numeric)
   - Urine tests → `Avni - Urine Albumin`, `Avni - Urine sugar` (Coded)
   - Blood tests → `Avni - HIV (Elisa)`, `Avni - VDRL` (Coded)

4. **Medications & Supplements (8 mappings)**
   - Iron & Folic Acid → `Avni - Iron & Folic Acid` (Numeric)
   - TT1, TT2 → `Avni - TT 1`, `Avni - TT 2` (Coded)

5. **Referrals & Follow-up (10 mappings)**
   - Referral required → `Avni - Does woman require referral?` (Coded)
   - Next ANC visit → `Avni - Date of next ANC Visit` (Date)

6. **Special Cases (5 mappings)**
   - Multiple concept name variations handled
   - Alternative names mapped to same Bahmni UUID

### 12.4 Critical Implementation Decisions

#### 12.4.1 Data Type Classification
- **Numeric:** Height, Weight, BP values, Lab values
- **Coded:** Yes/No questions, Multiple choice, Lab results
- **Date:** Next visit dates, Follow-up dates
- **Text:** "Other specify" fields, Free text comments

#### 12.4.2 Naming Strategy for Conflicts
- **Standard:** "Avni - [Concept Name]" for most concepts
- **Enhanced:** "Avni - ANC - [Concept Name]" for conflicts with existing Bahmni concepts
- **Example:** "BMI" → "Avni - ANC - BMI" (to avoid conflict with existing BMI concept)

#### 12.4.3 Duplicate Concept Handling
Some Avni concepts had multiple names pointing to same Bahmni concept:
```sql
-- Example: Next visit date had multiple names in Avni
INSERT INTO mapping_metadata ... VALUES ('6e50431c-6cb0-495f-9735-dd431c9970ff', 'Date of next ANC Visit', 'Date', ...);
INSERT INTO mapping_metadata ... VALUES ('6e50431c-6cb0-495f-9735-dd431c9970ff', 'Date of next ANC Clinic Visit', 'Date', ...);
```

### 12.5 Quality Assurance Process

#### 12.5.1 Mapping Validation
```sql
-- Verify all mappings created successfully
SELECT COUNT(*) as total_mappings
FROM mapping_metadata 
WHERE avni_value = 'ANC Clinic Visit' 
AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')
AND is_voided = false;

-- Expected: 70 mappings (2 encounter + 68 observation)
```

#### 12.5.2 Data Type Verification
```sql
-- Verify data types are correctly assigned
SELECT mm.avni_value, mm.data_type_hint, COUNT(*) as count
FROM mapping_metadata mm
JOIN mapping_group mg ON mm.mapping_group_id = mg.id
WHERE mg.name = 'Observation'
AND mm.integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')
AND mm.avni_value IN (
    SELECT avni_value FROM mapping_metadata 
    WHERE avni_value = 'ANC Clinic Visit' 
    AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')
)
GROUP BY mm.avni_value, mm.data_type_hint
ORDER BY mm.data_type_hint;
```

#### 12.5.3 UUID Consistency Check
```sql
-- Verify no duplicate Bahmni UUIDs for different Avni concepts
SELECT int_system_value, COUNT(*) as concept_count
FROM mapping_metadata
WHERE integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')
AND avni_value IN (
    SELECT avni_value FROM mapping_metadata 
    WHERE avni_value = 'ANC Clinic Visit' 
    AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')
)
GROUP BY int_system_value
HAVING COUNT(*) > 1;
```

### 12.6 Integration Testing

#### 12.6.1 Test Data Preparation
- **Test Subject:** Created individual with JSS ID in Avni
- **Test Program:** Enrolled in Pregnancy program
- **Test Encounter:** Created ANC Clinic Visit with sample data

#### 12.6.2 Sync Verification
```sql
-- Check if encounter was synced to Bahmni
SELECT * FROM integrating_entity_status 
WHERE entity_type = 'ProgramEncounter'
AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni');

-- Check for any mapping errors
SELECT er.entity_id, et.name as error_type, erl.error_msg, erl.logged_at
FROM error_record er
JOIN error_record_log erl ON erl.error_record_id = er.id
JOIN error_type et ON erl.error_type_id = et.id
WHERE erl.logged_at > NOW() - INTERVAL '1 hour'
ORDER BY erl.logged_at DESC;
```

### 12.7 Performance Considerations

#### 12.7.1 Mapping Efficiency
- **Batch Size:** 70 mappings created in single SQL execution
- **Index Usage:** All queries use indexed columns (integration_system_id, mapping_group_id, mapping_type_id)
- **UUID Generation:** Used PostgreSQL's uuid_generate_v4() for consistency

#### 12.7.2 Memory Optimization
- **No Temporary Tables:** Direct INSERT statements used
- **Minimal Joins:** Subqueries for ID resolution instead of complex joins
- **Bulk Operations:** All mappings created in one transaction

### 12.8 Lessons Learned

#### 12.8.1 Critical Success Factors
1. **Complete Concept Analysis:** Understanding all 268 concepts before mapping
2. **Bahmni UUID Verification:** Ensuring target UUIDs exist in Bahmni instance
3. **Data Type Accuracy:** Correct classification prevents sync errors
4. **Naming Strategy:** Proactive conflict avoidance saves rework

#### 12.8.2 Common Pitfalls to Avoid
1. **Assuming Concept Names:** Never assume Avni and Bahmni concept names match
2. **Ignoring Data Types:** Wrong data types cause sync failures
3. **Missing Alternate Names:** Multiple Avni names can map to same Bahmni concept
4. **Skipping Validation:** Always verify mapping counts and UUID uniqueness

#### 12.8.3 Best Practices Established
1. **Mapping Documentation:** Include comments in SQL for each mapping category
2. **Version Control:** Store mapping files with timestamps and versions
3. **Testing Protocol:** Always test with real data before production deployment
4. **Rollback Strategy:** Keep voided flag for easy mapping deactivation

### 12.9 Files Created/Modified

| File | Purpose | Size |
|------|---------|------|
| `ANCClinicVisitMappings.sql` | Complete mapping configuration | 619 lines, 70 mappings |
| `ANCVisitConcepts.csv` | Concept reference data | 79 concepts |
| `02-ANC-to-Bahmni-Conversion-Process.md` | Process documentation | 117 lines |

### 12.10 Next Steps for Similar Implementations

1. **Template Creation:** Use ANC mappings as template for other complex forms
2. **Automation:** Develop scripts for automatic mapping generation
3. **Validation Tools:** Create automated validation for mapping completeness
4. **Documentation:** Standardize documentation format for all mappings

---

## Sources

- [Avni Integration Developer Guide](https://avni.readme.io/docs/integration-developer-guide)
- [Avni-Bahmni Integration Specific](https://avni.readme.io/docs/avni-bahmni-integration-specific)
- [Cross-System Field Mapping](https://avni.readme.io/docs/cross-system-field-mapping)
- [Build, Deployment, Configuration](https://avni.readme.io/docs/build-deployment-and-configuration)
- JSS Bahmni integration folder documentation
- Codebase analysis of integration-service repository

---

## 13. Subject to Bahmni Mapping Implementation - Case Study

### 13.1 Overview

This section documents the complete process of implementing Subject (Individual Registration) mappings from Avni to Bahmni. This enables Avni individuals to be synced as Bahmni patients with their registration observations.

**Completed:** February 4, 2025
**Scope:** ~120 total mappings (3 base + 36 observations + ~80 answer concepts)
**Direction:** Avni → Bahmni (Outbound)

### 13.2 Pre-Implementation Requirements

#### 13.2.1 Bahmni Setup Required
Before creating mappings, the following must exist in Bahmni:

1. **Encounter Type:** `Patient Registration [A]` (UUID: `b805a574-f22a-4a6f-95f8-6ff24c4a8d59`)
2. **Patient Identifier Type:** `Avni Bahmni JSS ID` (UUID: `15c84573-8294-4e93-8d34-1028848eadca`)
3. **Concepts:** Upload via Bahmni Admin → Concept Dictionary → Import

#### 13.2.2 Bahmni Concept CSV Files

Create two CSV files for Bahmni concept import:

**File 1: `avni_registration_concepts.csv`** (17 columns)
```csv
uuid,name,description,class,shortname,datatype,units,High Normal,Low Normal,synonym.1,answer.1,answer.2,answer.3,answer.4,reference-term-source,reference-term-code,reference-term-relationship
4b23dc0e-3ae0-4d41-8546-35797063e123,Avni - JSS Registration Form,Avni Registration Form,ConvSet,Avni - JSS Registration Form,N/A,,,,,,,,,,,
821ba930-505c-4fd3-9f24-66b60ed45bac,Avni - Birth Order,,Misc,Avni - Birth Order,Numeric,,,,,,,,,,,
...
```

**File 2: `avni_registration_concept_sets.csv`** (17 columns)
```csv
uuid,name,description,class,shortname,child.1,child.2,child.3,child.4,child.5,child.6,child.7,child.8,child.9,child.10,child.11
4b23dc0e-3ae0-4d41-8546-35797063e123,Avni - JSS Registration Form,Avni Registration Form sections,Concept Details,Avni - JSS Registration Form,Avni - Personal Details,Avni - Individual Information,Avni - Socio-Economic Details,,,,,,,,
...
```

**Important CSV Format Rules:**
- Header must have exactly 17 columns
- `class` for concept_sets must be "Concept Details" (not "ConvSet")
- ConvSet datatype is "N/A" for grouping concepts
- Answer concepts have datatype "N/A"

### 13.3 Step-by-Step Implementation Process

#### Step 1: Upload Concepts to Bahmni

1. Go to Bahmni Admin → Concept Dictionary → Import
2. Upload `avni_registration_concepts.csv` first
3. Upload `avni_registration_concept_sets.csv` second
4. Verify concepts appear in Bahmni Admin → Concept Dictionary

#### Step 2: Create Patient Identifier Type in Bahmni

Run in OpenMRS database or via Admin UI:
```sql
INSERT INTO patient_identifier_type (name, description, format, check_digit, creator, date_created, required, uuid)
VALUES ('Avni Bahmni JSS ID', 'Shared identifier for Avni-Bahmni integration', NULL, 0, 1, NOW(), 0, '15c84573-8294-4e93-8d34-1028848eadca');
```

#### Step 3: Create Integration Mappings

Run the SQL mapping file `SubjectToBahmniMappings.sql` on avni-int database:

```bash
psql -h localhost -p 5455 -U avni-int -d avni_int -f integration-data/src/main/resources/db/onetime/SubjectToBahmniMappings.sql
```

### 13.4 Mapping Structure

#### 13.4.1 Mapping Groups Used
| Group | Purpose |
|-------|---------|
| `PatientSubject` | Subject/Patient level mappings |
| `Observation` | Individual observation concept mappings |

#### 13.4.2 Mapping Types Used
| Type | Group | Purpose |
|------|-------|---------|
| `Subject_EncounterType` | PatientSubject | Maps Avni Subject to Bahmni Encounter Type |
| `Subject_BahmniForm` | PatientSubject | Maps Avni Subject to Bahmni Form/Concept Set |
| `PatientIdentifier_Concept` | PatientSubject | Maps shared identifier between systems |
| `Concept` | Observation | Maps individual observation concepts |

#### 13.4.3 Core Mappings

**1. Subject to Encounter Type:**
```sql
-- Maps Individual (Avni) to "Patient Registration [A]" (Bahmni)
INSERT INTO mapping_metadata (int_system_value, avni_value, ...)
VALUES (
    'b805a574-f22a-4a6f-95f8-6ff24c4a8d59',  -- Bahmni encounter type UUID
    'Individual',                              -- Avni subject type
    ...
);
```

**2. Subject to Form:**
```sql
-- Maps Individual form to Bahmni concept set
INSERT INTO mapping_metadata (int_system_value, avni_value, ...)
VALUES (
    '4b23dc0e-3ae0-4d41-8546-35797063e123',  -- Bahmni form concept set UUID
    'Individual',                              -- Avni subject type
    ...
);
```

**3. Patient Identifier:**
```sql
-- Maps Avni Bahmni JSS ID to Bahmni Patient Identifier Type
INSERT INTO mapping_metadata (int_system_value, avni_value, ...)
VALUES (
    '15c84573-8294-4e93-8d34-1028848eadca',  -- Bahmni patient identifier type UUID
    'Avni Bahmni JSS ID',                     -- Avni identifier concept name
    ...
);
```

### 13.5 Registration Concepts Mapped

#### 13.5.1 Personal Details (11 concepts)
| Avni Concept | Bahmni UUID | Data Type |
|--------------|-------------|-----------|
| Birth Order | 821ba930-505c-4fd3-9f24-66b60ed45bac | Numeric |
| Father's Name | 9e6983b8-06ef-4648-b360-6684100b1be1 | Text |
| Father's Occupation | bf564151-63f9-4176-917f-f37de34b9bae | Coded |
| Mother's Name | 74a554d8-5b87-4d27-9ae5-272ab326608f | Text |
| Mother's Occupation | ea760e4f-c12f-490b-9865-9c6e4510ce64 | Coded |
| Father's Education Level | b1001c4d-0449-464a-947f-a04c4fdcc651 | Coded |
| Mother's Education Level | d98aae1a-ce33-4e51-b031-66e13bc0ba11 | Coded |
| Caste Category | 9ad4b520-4e33-4b1b-a056-37ae6418988f | Coded |
| Sub Caste | 047877ac-dba7-4acf-8c77-97c979c2fc26 | Coded |
| Other Sub Caste | ae7d54e9-fac0-4898-b334-87664bd055d2 | Text |
| Religion | b2c60cb8-983c-4e0e-a90d-4b21e87e10bd | Coded |

#### 13.5.2 Individual Information (14 concepts)
| Avni Concept | Bahmni UUID | Data Type |
|--------------|-------------|-----------|
| Aadhaar ID | 681fce2b-ea38-4651-a0b8-2cddd307ade7 | Numeric |
| Contact Number | 0a725832-b21c-4151-b017-7e6af770ba54 | Text |
| Date of Marriage | 9d958124-09bb-466c-a4b4-db8d285def1f | Date |
| Education | 673d65bd-6dc4-4aac-8e1e-1ee355ac081b | Coded |
| Occupation | 20ef261a-f110-4eaa-a592-2a1eeb0bf061 | Coded |
| Other Occupation | 4c429211-634e-4c2b-9a31-3f0a395f8f8d | Text |
| Marital Status | 9e995ea6-a5f7-410f-adc2-2d2ce6d5e19b | Coded |
| Father/Husband's Name | ecdf3c54-2808-494d-87be-8fb744d5c3bc | Text |
| First Name (Hindi) | de490ab6-5c24-4de5-9f95-fe78be1b0c11 | Text |
| Last Name (Hindi) | 9131372e-6e9b-4d07-b088-d7e961c61f76 | Text |
| Individual Id | 8033840e-a347-474d-a6ad-861ebffcec00 | Text |
| Relation to Head of Household | eaee156e-8ef3-4148-a80c-a466cd059ae3 | Coded |
| Whether Any Disability | bab107f6-fc0e-4be7-ab71-658a92d72f35 | Coded |
| Type of Disability | 7061c675-c2ba-4016-886d-eeb432548378 | Coded |

#### 13.5.3 Socio-Economic Details (11 concepts)
| Avni Concept | Bahmni UUID | Data Type |
|--------------|-------------|-----------|
| Status of Individual | d333f2a2-717e-478f-acbc-173bc7374d66 | Coded |
| Electricity in House | e23ef639-5d54-46bc-811c-ee1886bce81f | Coded |
| Smart Card (Insurance) | 2a445ac8-56e7-4eda-8756-0a9c4fa9a77b | Coded |
| Is Sterilization Done | 852f4e54-4969-4724-94e0-cddef0ac1f66 | Coded |
| Non Programme Village Name | 1c710642-4f37-4f47-9df9-393127eaafc9 | Text |
| Ration Card | 86fc3018-8eeb-4a58-a9d9-a40fff839305 | Coded |
| Land Possession | b984ad33-05d8-4621-adf3-152e72a0db1b | Coded |
| Land Area | 430ebb19-831d-470d-80eb-7969814f13e4 | Numeric |
| Property | aa88dba4-4f5d-4d35-9dc1-2390969cc5f3 | Coded |
| Other Property | 32609e0f-f3c8-4dcb-af7c-5e8a96e8e89d | Text |
| Type of Residence | c5d2673b-0f5c-48bf-93e4-f1a1ae820732 | Coded |

### 13.6 Answer Concept Mappings (~80 concepts)

All coded field answer options must also be mapped. Categories include:

| Category | Example Answers | Count |
|----------|-----------------|-------|
| Yes/No | Yes, No | 2 |
| Occupation | Business, Farming, Job, Labour, Housework, Other | 8 |
| Caste Category | General, OBC, SC, ST | 4 |
| Religion | Hindu, Muslim, Christian, Sikh, Jain | 5 |
| Marital Status | Unmarried, Currently Married, Divorced, Widowed, etc. | 6 |
| Education | Illiterate, Education 1-5, 6-7, 8-10, 11-12, Graduation, etc. | 10 |
| Occupation (Detailed) | Daily Wage Labourer, Farmer, Govt Job, Private Job, etc. | 8 |
| Ration Card | Antyodaya, APL, BPL | 3 |
| Type of Residence | Kaccha, Pakka, Aadha Kacha Pakka | 3 |
| Property | 2 Wheeler, 4 Wheeler, Cycle, Fridge, Generator, Tractor, None | 7 |
| Status of Individual | Birth Status, Death Status, In Migrant, Out Migrant, Resident | 6 |
| Type of Disability | Arthritis, Back Pain, Cerebral Palsy, Hearing Impairment, etc. | 13 |
| Sub Caste | Baiga, Gond, Oraon, Patel, Satnami, Yadav | 6 |

### 13.7 Files Created

| File | Location | Purpose |
|------|----------|---------|
| `SubjectToBahmniMappings.sql` | `integration-data/src/main/resources/db/onetime/` | Complete mapping SQL |
| `avni_registration_concepts.csv` | `JSS Bahmni integration/bahmni_import/` | Bahmni concept import |
| `avni_registration_concept_sets.csv` | `JSS Bahmni integration/bahmni_import/` | Bahmni concept set import |

### 13.8 Verification Steps

#### 13.8.1 Verify Bahmni Concepts
```sql
-- In OpenMRS database
SELECT concept_id, uuid, name FROM concept_name
WHERE name LIKE 'Avni - %' AND locale = 'en';
```

#### 13.8.2 Verify Integration Mappings
```sql
-- In avni-int database
SELECT mm.avni_value, mm.int_system_value, mm.data_type_hint, mg.name as mapping_group, mt.name as mapping_type
FROM mapping_metadata mm
JOIN mapping_group mg ON mm.mapping_group_id = mg.id
JOIN mapping_type mt ON mm.mapping_type_id = mt.id
WHERE mg.name = 'PatientSubject'
AND mm.is_voided = false
ORDER BY mt.name;

-- Expected results:
-- 1 Subject_EncounterType mapping
-- 1 Subject_BahmniForm mapping
-- 1 PatientIdentifier_Concept mapping
```

#### 13.8.3 Verify Observation Mappings Count
```sql
-- In avni-int database
SELECT COUNT(*) as total_mappings
FROM mapping_metadata mm
JOIN mapping_group mg ON mm.mapping_group_id = mg.id
WHERE mg.name = 'Observation'
AND mm.is_voided = false;

-- Expected: ~116 mappings (36 questions + ~80 answers)
```

### 13.9 Integration Testing

#### 13.9.1 Test Sync Flow
1. Create Individual in Avni with JSS ID
2. Fill registration form with sample data
3. Trigger integration sync
4. Verify patient created in Bahmni with:
   - Correct patient identifier (JSS ID)
   - Registration encounter with observations

#### 13.9.2 Troubleshooting
```sql
-- Check for sync errors
SELECT er.entity_id, et.name as error_type, erl.error_msg, erl.logged_at
FROM error_record er
JOIN error_record_log erl ON erl.error_record_id = er.id
JOIN error_type et ON erl.error_type_id = et.id
WHERE er.avni_entity_type = 'Subject'
ORDER BY erl.logged_at DESC
LIMIT 10;
```

### 13.10 Key Differences from Program Encounter Mappings

| Aspect | Subject Mapping | Program Encounter Mapping |
|--------|-----------------|---------------------------|
| Mapping Group | PatientSubject | ProgramEncounter |
| Encounter Type Mapping | Subject_EncounterType | CommunityProgramEncounter_EncounterType |
| Form Mapping | Subject_BahmniForm | CommunityProgramEncounter_BahmniForm |
| Identifier Mapping | PatientIdentifier_Concept | N/A |
| Creates | Patient + Encounter | Encounter only (Patient must exist) |

---

## 14. Summary of SQL Mapping Files

| File | Direction | Mappings | Description |
|------|-----------|----------|-------------|
| `SubjectToBahmniMappings.sql` | Avni → Bahmni | ~120 | Individual registration to Bahmni patient |
| `ANCClinicVisitMappings.sql` | Avni → Bahmni | 70 | ANC Clinic Visit program encounter |
| `ANCVisitMappings.sql` | Avni → Bahmni | - | ANC Home Visit program encounter (reference) |

### Running All Mappings

```bash
# Connect to avni-int database
psql -h localhost -p 5455 -U avni-int -d avni_int

# Run subject mappings (required first)
\i integration-data/src/main/resources/db/onetime/SubjectToBahmniMappings.sql

# Run ANC Clinic Visit mappings
\i integration-data/src/main/resources/db/onetime/ANCClinicVisitMappings.sql
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

*Document Version: 1.2*
*Created: January 2025*
*Updated: February 4, 2025 (Subject to Bahmni mapping case study added)*
