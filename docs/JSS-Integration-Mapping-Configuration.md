# JSS Avni-Bahmni Integration - Metadata Mapping Configuration

**Version:** 2.0 (Feed-Based, Metadata-Driven)  
**Last Updated:** January 2025  
**Status:** Technical Reference

---

## Table of Contents

1. [Overview](#1-overview)
2. [Sync Strategy](#2-sync-strategy)
3. [Identifier Mapping](#3-identifier-mapping)
4. [Bahmni → Avni Mapping](#4-bahmni--avni-mapping)
5. [Avni → Bahmni Mapping](#5-avni--bahmni-mapping)
6. [Concept Mappings](#6-concept-mappings)
7. [Appendices](#appendices)

---

## 1. Overview

This document defines all **technical metadata mappings** required by the integration service. For business context and architecture, see [JSS-Avni-Bahmni-Integration-Scoping.md](./JSS-Avni-Bahmni-Integration-Scoping.md).

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
| Lab Results | General Encounter | JSS ID exists in Avni | Encounter Feed (LAB_RESULT) |
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

## 4. Bahmni → Avni Mapping

### Sync Eligibility Rule

**IMPORTANT:** Only sync Bahmni data to Avni for patients who are:
1. Enrolled in at least one Avni program (Pregnancy, TB, Hypertension, Diabetes, Sickle Cell, Epilepsy, etc.)
2. Have a valid JSS ID in both systems

### 4.1 Encounter Type Mappings

| Bahmni Encounter Type | Bahmni UUID | Avni Encounter Type (NEW) | Avni Program Context | Priority |
|-----------------------|-------------|---------------------------|---------------------|----------|
| LAB_RESULT | `81d6e852-3f10-11e4-adec-0800271c1b75` | **Bahmni Lab Results** | General (all programs) | High |
| RADIOLOGY | `7c3f0372-a586-11e4-9beb-0800271c1b75` | **Bahmni Radiology Report** | General (all programs) | High |
| Consultation | `da7a4fe0-0a6a-11e3-939c-8c50edb4be99` | **Bahmni Consultation Notes** | General (all programs) | Medium |

### 4.2 Lab Results Mapping (Bahmni → Avni)

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

### 4.3 Radiology/X-Ray Mapping (Bahmni → Avni)

| Bahmni Radiology Type | Avni Form Section | Priority | Notes |
|-----------------------|-------------------|----------|-------|
| Chest X-Ray | Radiology Results | High | TB, cardiac evaluation |
| USG Abdomen | Radiology Results | Medium | General screening |
| USG Pelvis | Radiology Results | Medium | Pregnancy, gynecology |
| ECG | Cardiology Results | Medium | Heart disease |
| Echo | Cardiology Results | Medium | Heart disease |

---

## 5. Avni → Bahmni Mapping

### 5.1 Subject Types - Sync Scope (Avni → Bahmni)

| Avni Subject Type | Sync to Bahmni | Reason |
|-------------------|----------------|--------|
| **Individual** | ✅ YES | Primary subject, has JSS ID |
| Household | ❌ NO | No primary identifier for integration |
| Phulwari | ❌ NO | No primary identifier for integration |
| SHG | ❌ NO | No primary identifier for integration |
| Monthly Monitoring | ❌ NO | Operational data, not patient-specific |

### 5.2 Program Mapping

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

### 5.3 New Bahmni Visit Types Required (Avni → Bahmni)

| Visit Type Name | Code | Description | Avni Programs |
|-----------------|------|-------------|---------------|
| **Field** | FIELD | General field visit from Avni | All programs |
| **Field-TB** | FIELD_TB | TB program field visits | Tuberculosis, TB-INH Prophylaxis |
| **Field-NCD** | FIELD_NCD | NCD program field visits | Hypertension, Diabetes, Epilepsy, Sickle Cell |
| **Field-MCH** | FIELD_MCH | MCH program field visits | Pregnancy, Child |

### 5.4 New Bahmni Encounter Types Required (Avni → Bahmni)

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


---

## 6. Concept Mappings

### 6.1 Concept Mapping Strategy

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

### 6.2 Concept Mapping Categories

| Category | Domain | Data Type | Mapping Approach | Priority | Examples |
|----------|--------|-----------|------------------|----------|----------|
| **Lab Results** | Hematology, Biochemistry, Microbiology | Numeric, Coded, Text | Direct UUID mapping + unit conversion | High | Hb, Blood Sugar, Sputum AFB, Creatinine |
| **Vital Signs** | Cardiology, Respiratory | Numeric | Direct mapping with unit standardization | High | BP, Pulse, Temperature, SpO2, Weight, Height |
| **Diagnosis** | Clinical | Coded, Text | Concept + answer code mapping | Medium | Visit Diagnoses, Diagnosis Certainty, Diagnosis Order |
| **TB-Specific** | Infectious Disease | Coded, Text | Domain-specific coded mapping | High | AFB Result, Sample Type, Test Type, Treatment Category |

### 6.3 Mapping Execution Strategy

**Phase 1: Metadata Extraction**
- Extract all Bahmni concepts from database: `SELECT concept_id, name, uuid, class, datatype FROM concept`
- Extract all Avni concepts from API: `GET /api/concepts`
- Store mappings in integration DB table: `concept_mapping(bahmni_concept_uuid, avni_concept_uuid, data_type, unit_conversion_factor, priority)`

**Phase 2: Mapping Resolution**
1. **Exact Match:** Bahmni concept name = Avni concept name → Direct mapping
2. **Semantic Match:** Different names, same meaning → Lookup in integration DB mapping table
3. **Missing in Target:** Concept exists in source but not target
   - If **High Priority:** Log error, halt sync for that record
   - If **Medium Priority:** Log warning, store in observation notes field
   - If **Low Priority:** Log info, skip silently
4. **Coded Concept:** Map both concept UUID AND answer codes
5. **Unit Conversion:** Apply conversion factor (e.g., mg/dL to mmol/L for glucose)

**Phase 3: Error Handling**
- Only error if concept is marked **High Priority** and mapping not found
- Non-mandatory missing concepts logged to integration audit table
- Unmapped data stored in `observation.comments` for manual review

**Note:** Specific concept UUID mappings will be populated in integration DB after initial Bahmni/Avni metadata extraction. See Appendix A for Bahmni encounter types and observation templates.

---

## Appendices

### Appendix A: Bahmni Encounter Types Reference

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

### Appendix B: Bahmni Metadata Reference

### Patient Identifier Types

| ID | Name | UUID | Required | Purpose |
|----|------|------|----------|----------|
| 3 | **JSS ID** | `<to-be-created>` | **Yes** | Primary identifier for integration sync |
| 4 | **Patient Identifier (GAN ID)** | b46af68a-c79a-11e2-b284-107d46e7b2c5 | No | Legacy identifier (optional) |


*Document Version: 2.0*  
*Last Updated: January 2025*  
*Status: Technical Reference - Feed-Based Architecture*
