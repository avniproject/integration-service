# JSS Avni-Bahmni Integration - Metadata Mapping Configuration

**Version:** 2.0 (Feed-Based, Metadata-Driven)  
**Last Updated:** January 2025  
**Status:** Technical Reference

---

## Table of Contents

1. [Overview](#1-overview)
2. [Sync Strategy](#2-sync-strategy)
3. [Identifier Mapping](#3-identifier-mapping)
4. [Encounter Type Mappings](#4-encounter-type-mappings)
5. [Concept Mappings](#5-concept-mappings)
6. [Program Mappings](#6-program-mappings)
7. [Database Configuration](#7-database-configuration)

---

## 1. Overview

This document defines all metadata mappings required by the integration service to perform 1-to-1 entity synchronization between Avni and Bahmni. No business logic or data transformation is performed in the integration service—only metadata-driven mapping.

### Core Principles

1. **No Business Data in Integration Service:** Only metadata mapping and entity transformation
2. **Avni → Bahmni:** Sync to existing Patient with same JSS ID, or create new Patient with JSS ID
3. **Bahmni → Avni:** Sync only if JSS ID exists and Individual already exists (no new Individual creation)
4. **Feed-Based:** Bahmni uses Atom feeds; Avni uses REST APIs
5. **Metadata-Driven:** All mappings defined in integration DB, not hardcoded

---

## 2. Sync Strategy

### Programs in Scope (7 Programs)

| Avni Program | Bahmni Visit Type |
|--------------|-------------------|
| Tuberculosis | Field-TB |
| Hypertension | Field-NCD |
| Diabetes | Field-NCD |
| Sickle Cell | Field-NCD |
| Epilepsy | Field-NCD |
| Pregnancy | Field-MCH |
| Child | Field-MCH |

### Avni → Bahmni (Clinically Important Data)

| Avni Entity | Bahmni Entity | Condition | Notes |
|-------------|---------------|-----------|-------|
| Individual | Patient | Has JSS ID | Create if not exists |
| Program Enrolment | Encounter | Program in scope | Program-specific encounter type |
| Program Encounter | Encounter | Program in scope | Followup data for doctors |

### Bahmni → Avni (Field Service Data)

| Bahmni Data | Avni Entity | Condition | Source |
|-------------|-------------|-----------|--------|
| Consultation | General Encounter | JSS ID exists in Avni | Encounter Feed (CONSULTATION) |
| Radiology | General Encounter | JSS ID exists in Avni | Encounter Feed (RADIOLOGY) |
| Discharge | General Encounter | JSS ID exists in Avni | Encounter Feed (DISCHARGE) |
| Lab Results | General Encounter | JSS ID exists in Avni | Encounter Feed (LAB_RESULT) |
| Prescriptions | General Encounter | JSS ID exists in Avni | Drug Orders in Encounter |

---

## 3. Identifier Mapping

### ID Generation Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         UNIQUE ID GENERATION FLOW                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Step 1: JSS Community Survey                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  JSS team surveys community and generates unique IDs for everyone   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                    │                                        │
│                                    ▼                                        │
│  Step 2: Update Avni Individuals                                            │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Unique IDs are first updated to Individuals in Avni                │    │
│  │  Field: "JSS ID" or designated identifier field                     │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                    │                                        │
│                                    ▼                                        │
│  Step 3: Phased Sync to Bahmni                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  As IDs get added to patients in Bahmni, sync begins in phases      │    │
│  │  Integration uses JSS ID for bi-directional linking                 │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Identifier Mapping

| System | Identifier Field | Type | Notes |
|--------|------------------|------|-------|
| **Avni** | `JSS ID` (Individual registration) | Text | Primary identifier for integration |
| **Bahmni** | `Patient Identifier` (type_id=3) | String | JSS ID stored here |

### Integration Service Constants

```sql
-- Integration DB Constants
INSERT INTO constant (name, value) VALUES 
    ('BahmniIdentifierPrefix', 'GAN'),
    ('IntegrationBahmniIdentifierType', '<uuid-of-patient-identifier-type-3>'),
    ('IntegrationAvniSubjectType', 'Individual'),
    ('IntegrationAvniIdentifierConcept', 'JSS ID');
```

---

## 4. Encounter Type Mappings

### Sync Eligibility Rule

**IMPORTANT:** Only sync Bahmni data to Avni for patients who are:
1. Enrolled in at least one Avni program (Pregnancy, TB, Hypertension, Diabetes, Sickle Cell, Epilepsy, etc.)
2. Have a valid JSS ID in both systems

### 3.1 Encounter Type Mappings (Bahmni → Avni)

| Bahmni Encounter Type | Bahmni UUID | Avni Encounter Type (NEW) | Avni Program Context | Priority |
|-----------------------|-------------|---------------------------|---------------------|----------|
| LAB_RESULT | `81d6e852-3f10-11e4-adec-0800271c1b75` | **Bahmni Lab Results** | General (all programs) | High |
| RADIOLOGY | `7c3f0372-a586-11e4-9beb-0800271c1b75` | **Bahmni Radiology Report** | General (all programs) | High |
| DISCHARGE | `58c22773-9bc6-11e3-927e-8840ab96f0f1` | **Bahmni Discharge Summary** | General (all programs) | High |
| Consultation | `da7a4fe0-0a6a-11e3-939c-8c50edb4be99` | **Bahmni Consultation Notes** | General (all programs) | Medium |
| Prescription | NOT availabe | **Bahmni Prescription** | General (all programs) | Medium |

### 3.2 Visit Summary Structure

Create a single **Bahmni Visit Summary** encounter in Avni containing:

| Field | Source | Description |
|-------|--------|-------------|
| Visit Date | `visit.start_datetime` | Date of hospital visit |
| Visit Type | `visit_type.name` | OPD, IPD, Emergency, etc. |
| Encounter List | Aggregated | List of all encounters in the visit |
| Primary Diagnosis | `obs` where concept = 'Visit Diagnoses' | Main diagnosis |
| Prescriptions Summary | Drug orders | Current medications |
| Next Follow-up Date | `obs` where concept = 'Follow up Date' | Scheduled follow-up |

### 3.3 Lab Results Mapping

**Source:** Bahmni LAB_RESULT encounter type + OpenELIS integration

| Bahmni Lab Panel | Avni Form Section | Priority | Notes |
|------------------|-------------------|----------|-------|
| Complete Blood Count (CBC) | Hematology | High | Hb, WBC, Platelets, etc. |
| Liver Function Tests (LFT) | Biochemistry | High | ALT, AST, Bilirubin, etc. |
| Kidney Function Tests (KFT) | Biochemistry | High | Creatinine, BUN, etc. |
| Blood Sugar | Diabetes Panel | High | FBS, PPBS, HbA1c |
| Lipid Profile | Cardiovascular | Medium | Cholesterol, Triglycerides |
| Sputum AFB | TB Panel | High | TB diagnosis |
| Sickling Test | Sickle Cell Panel | High | Sickle cell diagnosis |
| Hb Electrophoresis | Sickle Cell Panel | High | Sickle cell confirmation |
| Thyroid Function | Endocrine | Medium | TSH, T3, T4 |
| Urine Routine | General | Medium | Protein, Sugar, etc. |

### 3.4 Radiology/X-Ray Mapping

| Bahmni Radiology Type | Avni Form Section | Priority | Notes |
|-----------------------|-------------------|----------|-------|
| Chest X-Ray | Radiology Results | High | TB, cardiac evaluation |
| USG Abdomen | Radiology Results | Medium | General screening |
| USG Pelvis | Radiology Results | Medium | Pregnancy, gynecology |
| ECG | Cardiology Results | Medium | Heart disease |
| Echo | Cardiology Results | Medium | Heart disease |

### 3.5 Discharge Summary Mapping

| Field | Source | Description |
|-----------------------|-------------------|----------|-------|


---

## 4. Avni → Bahmni Mapping

### 4.1 New Bahmni Visit Types Required

| Visit Type Name | Code | Description | Avni Programs |
|-----------------|------|-------------|---------------|
| **Field** | FIELD | General field visit from Avni | All programs |
| **Field-TB** | FIELD_TB | TB program field visits | Tuberculosis, TB-INH Prophylaxis |
| **Field-NCD** | FIELD_NCD | NCD program field visits | Hypertension, Diabetes, Epilepsy, Sickle Cell |
| **Field-MCH** | FIELD_MCH | MCH program field visits | Pregnancy, Child |

### 4.2 New Bahmni Encounter Types Required

| Encounter Type Name | Visit Type | Avni Source | Description |
|--------------------|------------|-------------|-------------|
| **Avni Registration** | Field | Individual Registration | Demographics from Avni |
| **Avni TB Enrolment** | Field-TB | TB Program Enrolment | TB program enrollment data |
| **Avni TB Followup** | Field-TB | TB Followup encounter | TB treatment followup |
| **Avni TB Lab** | Field-TB | TB Lab test results | TB lab results from field |
| **Avni HTN Enrolment** | Field-NCD | Hypertension Enrolment | HTN program enrollment |
| **Avni HTN Followup** | Field-NCD | Hypertension Followup | HTN followup data |
| **Avni DM Enrolment** | Field-NCD | Diabetes Enrolment | Diabetes enrollment |
| **Avni DM Followup** | Field-NCD | Diabetes Followup | Diabetes followup |
| **Avni Epilepsy Enrolment** | Field-NCD | Epilepsy Enrolment | Epilepsy enrollment |
| **Avni Epilepsy Followup** | Field-NCD | Epilepsy followup | Epilepsy followup |
| **Avni Sickle Cell Enrolment** | Field-NCD | Sickle cell Enrolment | Sickle cell enrollment |
| **Avni Sickle Cell Followup** | Field-NCD | Sickle cell followup | Sickle cell followup |
| **Avni ANC Visit** | Field-MCH | ANC Home Visit | Antenatal care |
| **Avni Delivery** | Field-MCH | Delivery | Delivery details |
| **Avni PNC** | Field-MCH | Mother PNC, Child PNC | Postnatal care |
| **Avni Referral** | Field | Referral Status, Referral Communication | Referral information |

### 4.3 Program Mapping (Avni → Bahmni)

| Avni Program | Bahmni Visit Type | Bahmni Encounter Type (Enrolment) | Bahmni Encounter Type (Followup) |
|--------------|-------------------|-----------------------------------|----------------------------------|
| Tuberculosis | Field-TB | Avni TB Enrolment | Avni TB Followup |
| TB - INH Prophylaxis | Field-TB | Avni TB INH Enrolment | Avni TB INH Followup |
| Hypertension | Field-NCD | Avni HTN Enrolment | Avni HTN Followup |
| Diabetes | Field-NCD | Avni DM Enrolment | Avni DM Followup |
| Epilepsy | Field-NCD | Avni Epilepsy Enrolment | Avni Epilepsy Followup |
| Sickle cell | Field-NCD | Avni Sickle Cell Enrolment | Avni Sickle Cell Followup |
| Mental Illness | Field-NCD | Avni Mental Illness Enrolment | Avni Mental Illness Followup |
| Heart Disease | Field-NCD | Avni Heart Disease Enrolment | Avni Heart Disease Followup |
| Stroke | Field-NCD | Avni Stroke Enrolment | Avni Stroke Followup |
| Pregnancy | Field-MCH | Avni Pregnancy Enrolment | Avni ANC Visit |
| Child | Field-MCH | Avni Child Enrolment | Avni Child Followup |

### 4.4 Subject Types - Sync Scope

| Avni Subject Type | Sync to Bahmni | Reason |
|-------------------|----------------|--------|
| **Individual** | ✅ YES | Primary subject, has JSS ID |
| Household | ❌ NO | No primary identifier for integration |
| Phulwari | ❌ NO | No primary identifier for integration |
| SHG | ❌ NO | No primary identifier for integration |
| Monthly Monitoring | ❌ NO | Operational data, not patient-specific |

---

## 6. Program Mappings

### 5.1 Mapping Strategy

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        CONCEPT MAPPING STRATEGY                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. EXACT MATCH: Avni concept name matches Bahmni concept name              │
│     → Direct mapping, sync value as-is                                      │
│                                                                             │
│  2. SEMANTIC MATCH: Different names, same meaning                           │
│     → Create explicit mapping in integration DB                             │
│                                                                             │
│  3. MISSING IN TARGET: Concept exists in source but not target              │
│     → Use fallback (store in notes/comments) OR ignore if not mandatory     │
│                                                                             │
│  4. CODED CONCEPT: Concept has coded answers                                │
│     → Map both concept AND answer codes                                     │
│                                                                             │
│  5. ERROR HANDLING: Only error if concept is marked mandatory               │
│     → Non-mandatory missing concepts should be logged and skipped           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 Lab Test Concept Mappings (Bahmni → Avni)

| Bahmni Concept | Bahmni UUID | Avni Concept (NEW) | Data Type | Unit | Priority |
|----------------|-------------|-------------------|-----------|------|----------|
| Haemoglobin | `9a5f0c74-3f10-11e4-adec-0800271c1b75` | Haemoglobin | Numeric | g/dL | High |
| Platelet Count | `TBD` | Platelet Count | Numeric | /cumm | High |
| WBC Count | `TBD` | WBC Count | Numeric | /cumm | High |
| RBC Count | `TBD` | RBC Count | Numeric | million/cumm | Medium |
| MCV | `TBD` | MCV | Numeric | fL | Medium |
| MCH | `TBD` | MCH | Numeric | pg | Medium |
| MCHC | `TBD` | MCHC | Numeric | g/dL | Medium |
| Packed Cell Volume (PCV) | `TBD` | PCV | Numeric | % | Medium |
| ESR | `TBD` | ESR | Numeric | mm/hr | Medium |
| Sickling Test | `TBD` | Sickling Test | Coded | - | High |
| Hb Electrophoresis | `TBD` | Hb Electrophoresis | Text | - | High |
| Blood Sugar (Fasting) | `TBD` | Fasting Blood Sugar | Numeric | mg/dL | High |
| Blood Sugar (PP) | `TBD` | Post Prandial Blood Sugar | Numeric | mg/dL | High |
| HbA1c | `TBD` | HbA1c | Numeric | % | High |
| Serum Creatinine | `TBD` | Serum Creatinine | Numeric | mg/dL | High |
| Blood Urea | `TBD` | Blood Urea | Numeric | mg/dL | Medium |
| ALT (SGPT) | `TBD` | ALT | Numeric | U/L | Medium |
| AST (SGOT) | `TBD` | AST | Numeric | U/L | Medium |
| Total Bilirubin | `TBD` | Total Bilirubin | Numeric | mg/dL | Medium |
| Serum Albumin | `TBD` | Serum Albumin | Numeric | g/dL | Medium |
| Total Cholesterol | `TBD` | Total Cholesterol | Numeric | mg/dL | Medium |
| Triglycerides | `TBD` | Triglycerides | Numeric | mg/dL | Medium |
| HDL Cholesterol | `TBD` | HDL Cholesterol | Numeric | mg/dL | Medium |
| LDL Cholesterol | `TBD` | LDL Cholesterol | Numeric | mg/dL | Medium |
| TSH | `TBD` | TSH | Numeric | mIU/L | Medium |
| Sputum AFB | `TBD` | Sputum AFB Result | Coded | - | High |
| HIV Test | `TBD` | HIV Test Result | Coded | - | High |
| Urine Protein | `TBD` | Urine Protein | Coded | - | Medium |
| Urine Sugar | `TBD` | Urine Sugar | Coded | - | Medium |

### 5.3 Vital Signs Concept Mappings

| Bahmni Concept | Avni Concept | Data Type | Unit |
|----------------|--------------|-----------|------|
| Systolic Blood Pressure | Systolic BP | Numeric | mmHg |
| Diastolic Blood Pressure | Diastolic BP | Numeric | mmHg |
| Pulse | Pulse Rate | Numeric | /min |
| Temperature | Temperature | Numeric | °F |
| Respiratory Rate | Respiratory Rate | Numeric | /min |
| Weight | Weight | Numeric | kg |
| Height | Height | Numeric | cm |
| BMI | BMI | Numeric | kg/m² |
| SpO2 | SpO2 | Numeric | % |

### 5.4 Diagnosis Concept Mappings

| Bahmni Diagnosis | Avni Concept | Mapping Type |
|------------------|--------------|--------------|
| Visit Diagnoses (ConvSet) | Diagnosis | Parent concept |
| Coded Diagnosis | Diagnosis Name | Coded |
| Non-Coded Diagnosis | Diagnosis Name (Text) | Text |
| Diagnosis Certainty | Diagnosis Certainty | Coded |
| Diagnosis Order | Diagnosis Order | Coded |

### 5.5 TB-Specific Concept Mappings

| Bahmni Concept | Avni Concept | Notes |
|----------------|--------------|-------|
| Tuberculosis, AFB (Zn Stain) | Sputum AFB Result | TB diagnosis |
| Tuberculosis, Sample Type | Sample Type | Sputum, BAL, etc. |
| Tuberculosis, Test Type | TB Test Type | AFB, CBNAAT, Culture |
| Tuberculosis, Basis of Diagnosis | TB Diagnosis Basis | Clinical, Bacteriological |
| Tuberculosis, Site | TB Site | Pulmonary, Extrapulmonary |
| Tuberculosis, Treatment Category | TB Treatment Category | New, Retreatment |

---

## 6. Bahmni Encounter Types Reference

**Source:** `scripts/aws/bahmni-metadata/encounter_types/encounter_types.json`

| ID | Name | UUID | Sync to Avni |
|----|------|------|--------------|
| 1 | REG | `b469afaa-c79a-11e2-b284-107d46e7b2c5` | ❌ (use Patient feed) |
| 2 | **Consultation** | `da7a4fe0-0a6a-11e3-939c-8c50edb4be99` | ✅ (contains diagnosis, prescriptions) |
| 3 | ADMISSION | `57efc389-9bc6-11e3-927e-8840ab96f0f1` | ❌ |
| 4 | DISCHARGE | `58c22773-9bc6-11e3-927e-8840ab96f0f1` | ❌ (PDF only) |
| 5 | TRANSFER | `6f6b5658-9bc6-11e3-927e-8840ab96f0f1` | ❌ |
| 6 | **RADIOLOGY** | `949dba36-9bc6-11e3-927e-8840ab96f0f1` | ✅ |
| 7 | INVESTIGATION | `952c63ca-9bc6-11e3-927e-8840ab96f0f1` | ❌ |
| 8 | **LAB_RESULT** | `960469a8-9bc6-11e3-927e-8840ab96f0f1` | ✅ |
| 9 | Patient Document | `e34027b5-a663-4370-bb6d-fe6d9cd31107` | ❌ |
| 10 | VALIDATION NOTES | `e6166d69-e349-11e3-983a-91270dcbd3bf` | ❌ |

### Consultation Encounter Structure

The **Consultation** encounter contains:
- **Observations:** Vitals, diagnosis, clinical notes
- **Drug Orders:** Prescriptions (accessed via `/openmrs/ws/rest/v1/order?patient={uuid}&t=drugorder`)
- **Observation Templates:** Visit Diagnoses, Nutritional Values, etc.

### Key Observation Templates

| Template | UUID | Purpose |
|----------|------|---------|
| Visit Diagnoses | `56104bb2-9bc6-11e3-927e-8840ab96f0f1` | Diagnosis |
| Nutritional Values | `3ccfba5b-82b6-43c3-939b-449f228b66d1` | Weight, Height |
| Discharge Summary | `f709eca4-e349-11e3-983a-91270dcbd3bf` | Discharge notes (observation, not encounter) |
| Blood Pressure | `f4762e56-e349-11e3-983a-91270dcbd3bf` | BP readings |

---

## Appendix A: Bahmni Metadata Reference

### Patient Identifier Types

| ID | Name | UUID | Required |
|----|------|------|----------|
| 1 | OpenMRS Identification Number | 8d793bee-c2cc-11de-8d13-0010c6dffd0f | No |
| 2 | Old Identification Number | 8d79403a-c2cc-11de-8d13-0010c6dffd0f | No |
| 3 | **Patient Identifier (GAN ID)** | b46af68a-c79a-11e2-b284-107d46e7b2c5 | **Yes** |
| 3 | **JSS ID** | <jss_id_uuid> | **Yes** |
| 4 | ABHA Number | 2e26304d-b715-11ee-8631-0242ac130002 | No |
| 5 | ABHA Address | 2e29440d-b715-11ee-8631-0242ac130002 | No |

---

## Appendix B: Avni Metadata Reference

### Programs in Scope (7 Programs)

| Program | Bahmni Visit Type | In Scope |
|---------|-------------------|----------|
| **Tuberculosis** | Field-TB | ✅ Yes |
| **Hypertension** | Field-NCD | ✅ Yes |
| **Diabetes** | Field-NCD | ✅ Yes |
| **Sickle Cell** | Field-NCD | ✅ Yes |
| **Epilepsy** | Field-NCD | ✅ Yes |
| **Pregnancy** | Field-MCH | ✅ Yes |
| **Child** | Field-MCH | ✅ Yes (Growth Monitoring) |
| Mental Illness | - | ❌ No |
| Heart Disease | - | ❌ No |
| Stroke | - | ❌ No |
| TB - INH Prophylaxis | - | ❌ No |

### Subject Types

| Subject Type | UUID | Sync to Bahmni |
|--------------|------|----------------|
| Individual | TBD | Yes |
| Household | TBD | No |
| Phulwari | TBD | No |
| SHG | TBD | No |

---

*Document Version: 2.0*  
*Last Updated: January 2025*  
*Status: Technical Reference - Feed-Based Architecture*
