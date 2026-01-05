# JSS Avni-Bahmni Integration Mapping Configuration

**Document Version:** 1.0  
**Created:** January 2025  
**Status:** Draft - Pending Doctor Review  

---

## Table of Contents

1. [Overview](#1-overview)
2. [Unique Identifier Strategy](#2-unique-identifier-strategy)
3. [Bahmni → Avni Mapping (Clinical Data for Field Service)](#3-bahmni--avni-mapping)
4. [Avni → Bahmni Mapping (Community Health Data)](#4-avni--bahmni-mapping)
5. [Concept-Level Mappings](#5-concept-level-mappings)
6. [New Forms & Encounter Types Required](#6-new-forms--encounter-types-required)
7. [Doctor Questionnaire](#7-doctor-questionnaire)
8. [Implementation Checklist](#8-implementation-checklist)

---

## 1. Overview

### Integration Principles

| Principle | Description |
|-----------|-------------|
| **ID Source** | JSS team surveys community → generates unique IDs → updates Avni Individuals first → then syncs to Bahmni as IDs are added |
| **Bahmni → Avni** | Only sync for patients enrolled in Avni programs (TB, Hypertension, Diabetes, etc.) |
| **Avni → Bahmni** | All clinical program data syncs to Bahmni for research |
| **Concept Mapping** | Concept-level mapping required; use fallbacks for missing concepts, don't error unless mandatory |
| **Forms Strategy** | Create new dedicated "Bahmni Sync" forms in Avni; Create new "Field" visit types in Bahmni |

### Key Decisions Summary

| Decision | Choice |
|----------|--------|
| Sync Lab Results | Only for program-enrolled patients |
| Discharge Summary | New "General" encounter type in Avni |
| Visit Summary | Single record with list of encounters |
| Bahmni Visit Types | New "Field" visit type for Avni data |
| Program-specific Visit Types | Create "Field-TB", "Field-HTN", etc. if sensible |
| Household/Phulwari Sync | **NO** - no primary identifier for integration |
| Existing Avni Lab Forms | **IGNORE** - create new comprehensive forms |

---

## 2. Unique Identifier Strategy

### ID Generation Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         UNIQUE ID GENERATION FLOW                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Step 1: JSS Community Survey                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  JSS team surveys community and generates unique IDs for everyone   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│                                    ▼                                        │
│  Step 2: Update Avni Individuals                                            │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  Unique IDs are first updated to Individuals in Avni                │   │
│  │  Field: "JSS ID" or designated identifier field                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│                                    ▼                                        │
│  Step 3: Phased Sync to Bahmni                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  As IDs get added to patients in Bahmni, sync begins in phases      │   │
│  │  Integration uses JSS ID for bi-directional linking                 │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
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

## 3. Bahmni → Avni Mapping

### Sync Eligibility Rule

**IMPORTANT:** Only sync Bahmni data to Avni for patients who are:
1. Enrolled in at least one Avni program (TB, Hypertension, Diabetes, Sickle Cell, Epilepsy, etc.)
2. Have a valid JSS ID in both systems

### 3.1 Encounter Type Mappings (Bahmni → Avni)

| Bahmni Encounter Type | Bahmni UUID | Avni Encounter Type (NEW) | Avni Program Context | Priority |
|-----------------------|-------------|---------------------------|---------------------|----------|
| LAB_RESULT | `81d6e852-3f10-11e4-adec-0800271c1b75` | **Bahmni Lab Results** | General (all programs) | High |
| RADIOLOGY | `7c3f0372-a586-11e4-9beb-0800271c1b75` | **Bahmni Radiology Report** | General (all programs) | High |
| DISCHARGE | `81d72550-3f10-11e4-adec-0800271c1b75` | **Bahmni Discharge Summary** | General (all programs) | High |
| Consultation | `81d210e8-3f10-11e4-adec-0800271c1b75` | **Bahmni Visit Summary** | General (all programs) | Medium |

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

## 5. Concept-Level Mappings

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

## 6. New Forms & Encounter Types Required

### 6.1 New Avni Encounter Types

| Encounter Type Name | Program Association | Form Type | Description |
|--------------------|---------------------|-----------|-------------|
| **Bahmni Lab Results** | General (Individual) | ProgramEncounter | Lab results synced from Bahmni |
| **Bahmni Radiology Report** | General (Individual) | ProgramEncounter | X-ray/imaging results from Bahmni |
| **Bahmni Discharge Summary** | General (Individual) | ProgramEncounter | Discharge summary from Bahmni |
| **Bahmni Visit Summary** | General (Individual) | ProgramEncounter | Visit summary from Bahmni |

### 6.2 New Avni Forms Required

#### Form: Bahmni Lab Results

```json
{
  "name": "Bahmni Lab Results",
  "formType": "ProgramEncounter",
  "encounterType": "Bahmni Lab Results",
  "formElementGroups": [
    {
      "name": "Lab Metadata",
      "displayOrder": 1,
      "formElements": [
        { "name": "Lab Order Date", "dataType": "Date", "mandatory": true },
        { "name": "Lab Result Date", "dataType": "Date", "mandatory": true },
        { "name": "Lab Name", "dataType": "Text", "mandatory": false },
        { "name": "Bahmni Encounter UUID", "dataType": "Text", "mandatory": true }
      ]
    },
    {
      "name": "Hematology",
      "displayOrder": 2,
      "formElements": [
        { "name": "Haemoglobin", "dataType": "Numeric", "unit": "g/dL" },
        { "name": "WBC Count", "dataType": "Numeric", "unit": "/cumm" },
        { "name": "Platelet Count", "dataType": "Numeric", "unit": "/cumm" },
        { "name": "RBC Count", "dataType": "Numeric", "unit": "million/cumm" },
        { "name": "PCV", "dataType": "Numeric", "unit": "%" },
        { "name": "MCV", "dataType": "Numeric", "unit": "fL" },
        { "name": "MCH", "dataType": "Numeric", "unit": "pg" },
        { "name": "MCHC", "dataType": "Numeric", "unit": "g/dL" },
        { "name": "ESR", "dataType": "Numeric", "unit": "mm/hr" }
      ]
    },
    {
      "name": "Sickle Cell Panel",
      "displayOrder": 3,
      "formElements": [
        { "name": "Sickling Test", "dataType": "Coded", "answers": ["Positive", "Negative"] },
        { "name": "Hb Electrophoresis", "dataType": "Text" },
        { "name": "Hb Electrophoresis Result", "dataType": "Coded", "answers": ["AA", "AS", "SS", "SC", "Other"] }
      ]
    },
    {
      "name": "Diabetes Panel",
      "displayOrder": 4,
      "formElements": [
        { "name": "Fasting Blood Sugar", "dataType": "Numeric", "unit": "mg/dL" },
        { "name": "Post Prandial Blood Sugar", "dataType": "Numeric", "unit": "mg/dL" },
        { "name": "Random Blood Sugar", "dataType": "Numeric", "unit": "mg/dL" },
        { "name": "HbA1c", "dataType": "Numeric", "unit": "%" }
      ]
    },
    {
      "name": "Kidney Function",
      "displayOrder": 5,
      "formElements": [
        { "name": "Serum Creatinine", "dataType": "Numeric", "unit": "mg/dL" },
        { "name": "Blood Urea", "dataType": "Numeric", "unit": "mg/dL" },
        { "name": "Blood Urea Nitrogen", "dataType": "Numeric", "unit": "mg/dL" },
        { "name": "Serum Uric Acid", "dataType": "Numeric", "unit": "mg/dL" }
      ]
    },
    {
      "name": "Liver Function",
      "displayOrder": 6,
      "formElements": [
        { "name": "ALT", "dataType": "Numeric", "unit": "U/L" },
        { "name": "AST", "dataType": "Numeric", "unit": "U/L" },
        { "name": "Total Bilirubin", "dataType": "Numeric", "unit": "mg/dL" },
        { "name": "Direct Bilirubin", "dataType": "Numeric", "unit": "mg/dL" },
        { "name": "Serum Albumin", "dataType": "Numeric", "unit": "g/dL" },
        { "name": "Total Protein", "dataType": "Numeric", "unit": "g/dL" },
        { "name": "Alkaline Phosphatase", "dataType": "Numeric", "unit": "U/L" }
      ]
    },
    {
      "name": "Lipid Profile",
      "displayOrder": 7,
      "formElements": [
        { "name": "Total Cholesterol", "dataType": "Numeric", "unit": "mg/dL" },
        { "name": "Triglycerides", "dataType": "Numeric", "unit": "mg/dL" },
        { "name": "HDL Cholesterol", "dataType": "Numeric", "unit": "mg/dL" },
        { "name": "LDL Cholesterol", "dataType": "Numeric", "unit": "mg/dL" },
        { "name": "VLDL Cholesterol", "dataType": "Numeric", "unit": "mg/dL" }
      ]
    },
    {
      "name": "Thyroid Function",
      "displayOrder": 8,
      "formElements": [
        { "name": "TSH", "dataType": "Numeric", "unit": "mIU/L" },
        { "name": "T3", "dataType": "Numeric", "unit": "ng/dL" },
        { "name": "T4", "dataType": "Numeric", "unit": "µg/dL" },
        { "name": "Free T3", "dataType": "Numeric", "unit": "pg/mL" },
        { "name": "Free T4", "dataType": "Numeric", "unit": "ng/dL" }
      ]
    },
    {
      "name": "TB Panel",
      "displayOrder": 9,
      "formElements": [
        { "name": "Sputum AFB Result", "dataType": "Coded", "answers": ["Positive", "Negative", "Scanty"] },
        { "name": "Sputum AFB Grade", "dataType": "Coded", "answers": ["1+", "2+", "3+", "Scanty"] },
        { "name": "CBNAAT Result", "dataType": "Coded", "answers": ["MTB Detected", "MTB Not Detected", "Invalid"] },
        { "name": "Rifampicin Resistance", "dataType": "Coded", "answers": ["Detected", "Not Detected", "Indeterminate"] },
        { "name": "Culture Result", "dataType": "Coded", "answers": ["Positive", "Negative", "Contaminated"] }
      ]
    },
    {
      "name": "Serology",
      "displayOrder": 10,
      "formElements": [
        { "name": "HIV Test Result", "dataType": "Coded", "answers": ["Positive", "Negative", "Indeterminate"] },
        { "name": "HBsAg", "dataType": "Coded", "answers": ["Positive", "Negative"] },
        { "name": "HCV", "dataType": "Coded", "answers": ["Positive", "Negative"] },
        { "name": "VDRL", "dataType": "Coded", "answers": ["Reactive", "Non-Reactive"] }
      ]
    },
    {
      "name": "Urine Analysis",
      "displayOrder": 11,
      "formElements": [
        { "name": "Urine Protein", "dataType": "Coded", "answers": ["Nil", "Trace", "1+", "2+", "3+", "4+"] },
        { "name": "Urine Sugar", "dataType": "Coded", "answers": ["Nil", "Trace", "1+", "2+", "3+", "4+"] },
        { "name": "Urine Pus Cells", "dataType": "Text" },
        { "name": "Urine RBC", "dataType": "Text" }
      ]
    }
  ]
}
```

#### Form: Bahmni Radiology Report

```json
{
  "name": "Bahmni Radiology Report",
  "formType": "ProgramEncounter",
  "encounterType": "Bahmni Radiology Report",
  "formElementGroups": [
    {
      "name": "Radiology Metadata",
      "displayOrder": 1,
      "formElements": [
        { "name": "Radiology Order Date", "dataType": "Date", "mandatory": true },
        { "name": "Radiology Report Date", "dataType": "Date", "mandatory": true },
        { "name": "Radiology Type", "dataType": "Coded", "answers": ["X-Ray", "USG", "CT Scan", "MRI", "ECG", "Echo", "Other"], "mandatory": true },
        { "name": "Bahmni Encounter UUID", "dataType": "Text", "mandatory": true }
      ]
    },
    {
      "name": "X-Ray Details",
      "displayOrder": 2,
      "formElements": [
        { "name": "X-Ray Site", "dataType": "Coded", "answers": ["Chest PA", "Chest AP", "Chest Lateral", "Abdomen", "Spine", "Extremity", "Other"] },
        { "name": "X-Ray Findings", "dataType": "Text" },
        { "name": "X-Ray Impression", "dataType": "Text" }
      ]
    },
    {
      "name": "USG Details",
      "displayOrder": 3,
      "formElements": [
        { "name": "USG Type", "dataType": "Coded", "answers": ["Abdomen", "Pelvis", "Obstetric", "KUB", "Thyroid", "Other"] },
        { "name": "USG Findings", "dataType": "Text" },
        { "name": "USG Impression", "dataType": "Text" }
      ]
    },
    {
      "name": "ECG/Echo Details",
      "displayOrder": 4,
      "formElements": [
        { "name": "ECG Findings", "dataType": "Text" },
        { "name": "Echo Findings", "dataType": "Text" },
        { "name": "Ejection Fraction", "dataType": "Numeric", "unit": "%" }
      ]
    },
    {
      "name": "Overall Impression",
      "displayOrder": 5,
      "formElements": [
        { "name": "Radiologist Notes", "dataType": "Text" },
        { "name": "Abnormal Findings", "dataType": "Coded", "answers": ["Yes", "No"] },
        { "name": "Follow-up Recommended", "dataType": "Coded", "answers": ["Yes", "No"] }
      ]
    }
  ]
}
```

#### Form: Bahmni Discharge Summary

```json
{
  "name": "Bahmni Discharge Summary",
  "formType": "ProgramEncounter",
  "encounterType": "Bahmni Discharge Summary",
  "formElementGroups": [
    {
      "name": "Admission Details",
      "displayOrder": 1,
      "formElements": [
        { "name": "Admission Date", "dataType": "Date", "mandatory": true },
        { "name": "Discharge Date", "dataType": "Date", "mandatory": true },
        { "name": "Ward", "dataType": "Text" },
        { "name": "Treating Doctor", "dataType": "Text" },
        { "name": "Bahmni Encounter UUID", "dataType": "Text", "mandatory": true }
      ]
    },
    {
      "name": "Diagnosis",
      "displayOrder": 2,
      "formElements": [
        { "name": "Primary Diagnosis", "dataType": "Text", "mandatory": true },
        { "name": "Secondary Diagnoses", "dataType": "Text" },
        { "name": "Procedures Performed", "dataType": "Text" }
      ]
    },
    {
      "name": "Treatment Summary",
      "displayOrder": 3,
      "formElements": [
        { "name": "Treatment Given", "dataType": "Text" },
        { "name": "Condition at Discharge", "dataType": "Coded", "answers": ["Improved", "Unchanged", "Worse", "LAMA", "Expired"] },
        { "name": "Discharge Medications", "dataType": "Text" }
      ]
    },
    {
      "name": "Follow-up Instructions",
      "displayOrder": 4,
      "formElements": [
        { "name": "Follow-up Date", "dataType": "Date" },
        { "name": "Follow-up Instructions", "dataType": "Text" },
        { "name": "Warning Signs", "dataType": "Text" },
        { "name": "Diet Instructions", "dataType": "Text" },
        { "name": "Activity Instructions", "dataType": "Text" }
      ]
    }
  ]
}
```

#### Form: Bahmni Visit Summary

```json
{
  "name": "Bahmni Visit Summary",
  "formType": "ProgramEncounter",
  "encounterType": "Bahmni Visit Summary",
  "formElementGroups": [
    {
      "name": "Visit Details",
      "displayOrder": 1,
      "formElements": [
        { "name": "Visit Date", "dataType": "Date", "mandatory": true },
        { "name": "Visit Type", "dataType": "Coded", "answers": ["OPD", "IPD", "Emergency"], "mandatory": true },
        { "name": "Department", "dataType": "Text" },
        { "name": "Bahmni Visit UUID", "dataType": "Text", "mandatory": true }
      ]
    },
    {
      "name": "Clinical Summary",
      "displayOrder": 2,
      "formElements": [
        { "name": "Chief Complaints", "dataType": "Text" },
        { "name": "Primary Diagnosis", "dataType": "Text" },
        { "name": "Other Diagnoses", "dataType": "Text" }
      ]
    },
    {
      "name": "Encounters in Visit",
      "displayOrder": 3,
      "formElements": [
        { "name": "Encounter List", "dataType": "Text" },
        { "name": "Lab Tests Ordered", "dataType": "Text" },
        { "name": "Radiology Ordered", "dataType": "Text" }
      ]
    },
    {
      "name": "Prescriptions",
      "displayOrder": 4,
      "formElements": [
        { "name": "Medications Prescribed", "dataType": "Text" },
        { "name": "Prescription Notes", "dataType": "Text" }
      ]
    },
    {
      "name": "Follow-up",
      "displayOrder": 5,
      "formElements": [
        { "name": "Next Follow-up Date", "dataType": "Date" },
        { "name": "Follow-up Instructions", "dataType": "Text" },
        { "name": "Referral", "dataType": "Text" }
      ]
    }
  ]
}
```

### 6.3 New Bahmni Observation Templates Required

For each Avni program, create a corresponding observation template in Bahmni:

| Template Name | Source Avni Program | Key Observations |
|---------------|---------------------|------------------|
| **Avni TB Data** | Tuberculosis | Treatment status, adherence, side effects, weight, symptoms |
| **Avni HTN Data** | Hypertension | BP readings, medication adherence, symptoms, lifestyle |
| **Avni DM Data** | Diabetes | Blood sugar, medication adherence, symptoms, foot exam |
| **Avni Epilepsy Data** | Epilepsy | Seizure frequency, medication adherence, triggers |
| **Avni Sickle Cell Data** | Sickle cell | Crisis episodes, transfusions, hydroxyurea adherence |
| **Avni ANC Data** | Pregnancy | ANC visits, weight, BP, fetal heart rate, complications |
| **Avni Referral Data** | Referral encounters | Referral reason, status, outcome |

---

## 7. Doctor Questionnaire

### Purpose
The following questions require clinical input from JSS doctors to finalize the integration mapping. These decisions affect what data is synced and how it's presented to field workers.

---

### Section A: Lab Results for Field Service

**Context:** Lab results from Bahmni will be synced to Avni for field workers to follow up with patients.

#### Q1. Lab Result Priority
Which lab results are **most critical** for field workers to see for patient follow-up?

| Lab Test | Priority (High/Medium/Low/Not Needed) | Notes |
|----------|---------------------------------------|-------|
| Haemoglobin | | |
| Blood Sugar (Fasting/PP) | | |
| HbA1c | | |
| Serum Creatinine | | |
| Sputum AFB | | |
| Sickling Test | | |
| Hb Electrophoresis | | |
| HIV Test | | |
| Liver Function (ALT/AST) | | |
| Lipid Profile | | |
| Thyroid (TSH) | | |
| Urine Routine | | |

#### Q2. Abnormal Value Alerts
Should field workers see **alerts for abnormal values**? If yes, please specify thresholds:

| Lab Test | Low Alert Threshold | High Alert Threshold |
|----------|---------------------|----------------------|
| Haemoglobin | < ___ g/dL | > ___ g/dL |
| Fasting Blood Sugar | < ___ mg/dL | > ___ mg/dL |
| HbA1c | | > ___ % |
| Serum Creatinine | | > ___ mg/dL |
| Systolic BP | < ___ mmHg | > ___ mmHg |
| Diastolic BP | < ___ mmHg | > ___ mmHg |

#### Q3. Lab Result Grouping
Should lab results be grouped by:
- [ ] Date of test
- [ ] Lab panel (CBC, LFT, KFT, etc.)
- [ ] Clinical relevance (Diabetes panel, TB panel, etc.)
- [ ] Other: _______________

---

### Section B: Radiology/X-Ray Results

#### Q4. Radiology Types for Field Service
Which radiology results should be synced to Avni?

| Radiology Type | Sync to Avni (Yes/No) | Use Case |
|----------------|----------------------|----------|
| Chest X-Ray | | |
| USG Abdomen | | |
| USG Pelvis/Obstetric | | |
| ECG | | |
| Echo | | |
| CT Scan | | |
| MRI | | |

#### Q5. Radiology Information Detail
What level of detail should field workers see?
- [ ] Full report text
- [ ] Summary/impression only
- [ ] Abnormal findings only
- [ ] Just the fact that test was done + normal/abnormal flag

#### Q6. TB-Specific X-Ray Findings
For TB patients, should we extract specific findings from chest X-rays?
- [ ] Cavity present (Yes/No)
- [ ] Infiltrates (Yes/No)
- [ ] Pleural effusion (Yes/No)
- [ ] Miliary pattern (Yes/No)
- [ ] Other: _______________

---

### Section C: Discharge Summary

#### Q7. Discharge Summary Content
What information from discharge summaries is essential for field follow-up?

| Field | Include (Yes/No) | Priority |
|-------|------------------|----------|
| Primary Diagnosis | | |
| Secondary Diagnoses | | |
| Procedures Performed | | |
| Discharge Medications | | |
| Follow-up Date | | |
| Follow-up Instructions | | |
| Warning Signs | | |
| Diet Instructions | | |
| Activity Restrictions | | |
| Condition at Discharge | | |

#### Q8. Discharge Follow-up Actions
What actions should field workers take after seeing a discharge summary?
- [ ] Home visit within X days
- [ ] Medication adherence check
- [ ] Vital signs monitoring
- [ ] Referral back if warning signs
- [ ] Other: _______________

---

### Section D: Visit Summary

#### Q9. Visit Summary Content
What should the visit summary include?

| Field | Include (Yes/No) | Notes |
|-------|------------------|-------|
| Visit Date | | |
| Visit Type (OPD/IPD/Emergency) | | |
| Chief Complaints | | |
| Diagnoses | | |
| Medications Prescribed | | |
| Lab Tests Ordered | | |
| Radiology Ordered | | |
| Next Follow-up Date | | |
| Referrals | | |

#### Q10. Visit Frequency
How often should visit summaries be synced?
- [ ] Every visit
- [ ] Only if new diagnosis
- [ ] Only if hospitalization (IPD)
- [ ] Only if specific conditions (specify): _______________

---

### Section E: Program-Specific Questions

#### Q11. TB Program
For TB patients, what Bahmni data is most important for field workers?

| Data Type | Priority | Notes |
|-----------|----------|-------|
| Sputum AFB results | | |
| CBNAAT results | | |
| Chest X-ray findings | | |
| Drug sensitivity results | | |
| Treatment regimen changes | | |
| Side effects documented | | |
| Weight changes | | |

#### Q12. Hypertension Program
For hypertension patients, what Bahmni data is most important?

| Data Type | Priority | Notes |
|-----------|----------|-------|
| BP readings at clinic | | |
| Medication changes | | |
| Kidney function (Creatinine) | | |
| Cardiac evaluation (ECG/Echo) | | |
| Complications documented | | |

#### Q13. Diabetes Program
For diabetes patients, what Bahmni data is most important?

| Data Type | Priority | Notes |
|-----------|----------|-------|
| Blood sugar readings | | |
| HbA1c | | |
| Medication changes | | |
| Kidney function | | |
| Eye examination results | | |
| Foot examination results | | |
| Complications documented | | |

#### Q14. Sickle Cell Program
For sickle cell patients, what Bahmni data is most important?

| Data Type | Priority | Notes |
|-----------|----------|-------|
| Hb levels | | |
| Hb Electrophoresis | | |
| Crisis episodes documented | | |
| Transfusion records | | |
| Hydroxyurea dosing | | |
| Complications | | |

---

### Section F: General Questions

#### Q15. Data Freshness
How recent should synced data be for field workers?
- [ ] Real-time (within hours)
- [ ] Daily sync
- [ ] Weekly sync
- [ ] On-demand only

#### Q16. Historical Data
How much historical data should be visible to field workers?
- [ ] Last visit only
- [ ] Last 3 months
- [ ] Last 6 months
- [ ] Last 1 year
- [ ] All available data

#### Q17. Notification/Alerts
Should field workers receive notifications for:
- [ ] New lab results available
- [ ] Abnormal lab values
- [ ] Discharge from hospital
- [ ] Missed follow-up appointments
- [ ] Other: _______________

#### Q18. Additional Data Needs
Is there any other data from Bahmni that would be valuable for field workers?

_____________________________________________________________
_____________________________________________________________
_____________________________________________________________

---

## 8. Implementation Checklist

### Phase 1: Setup

- [ ] **Avni Configuration**
  - [ ] Create "Bahmni Lab Results" encounter type
  - [ ] Create "Bahmni Radiology Report" encounter type
  - [ ] Create "Bahmni Discharge Summary" encounter type
  - [ ] Create "Bahmni Visit Summary" encounter type
  - [ ] Create corresponding forms for each encounter type
  - [ ] Add "JSS ID" field to Individual registration (if not exists)
  - [ ] Create form mappings

- [ ] **Bahmni Configuration**
  - [ ] Create "Field" visit type
  - [ ] Create "Field-TB" visit type
  - [ ] Create "Field-NCD" visit type
  - [ ] Create "Field-MCH" visit type
  - [ ] Create Avni encounter types (see Section 4.2)
  - [ ] Create Avni observation templates (see Section 6.3)

- [ ] **Integration Service Configuration**
  - [ ] Configure constants in integration DB
  - [ ] Create subject type mapping (Individual ↔ Patient)
  - [ ] Create encounter type mappings (Bahmni → Avni)
  - [ ] Create encounter type mappings (Avni → Bahmni)
  - [ ] Create concept mappings (see Section 5)

### Phase 2: Concept Mapping

- [ ] **Lab Concepts**
  - [ ] Extract all Bahmni lab test UUIDs
  - [ ] Create corresponding Avni concepts
  - [ ] Create mapping entries in integration DB

- [ ] **Vital Signs Concepts**
  - [ ] Map BP, Pulse, Temperature, etc.

- [ ] **Diagnosis Concepts**
  - [ ] Map diagnosis structure

- [ ] **Program-Specific Concepts**
  - [ ] TB concepts
  - [ ] HTN concepts
  - [ ] DM concepts
  - [ ] Sickle cell concepts

### Phase 3: Testing

- [ ] **Bahmni → Avni Testing**
  - [ ] Test lab result sync
  - [ ] Test radiology sync
  - [ ] Test discharge summary sync
  - [ ] Test visit summary sync
  - [ ] Verify only program-enrolled patients receive data

- [ ] **Avni → Bahmni Testing**
  - [ ] Test Individual registration sync
  - [ ] Test program enrolment sync
  - [ ] Test program followup sync
  - [ ] Verify data appears in correct visit types

### Phase 4: Doctor Review

- [ ] Complete doctor questionnaire
- [ ] Incorporate feedback into mappings
- [ ] Finalize alert thresholds
- [ ] Finalize data priority

---

## Appendix A: Bahmni Metadata Reference

### Encounter Types (from extraction)

| ID | Name | UUID |
|----|------|------|
| 1 | REG | 81d865e8-3f10-11e4-adec-0800271c1b75 |
| 2 | Consultation | 81d210e8-3f10-11e4-adec-0800271c1b75 |
| 3 | ADMISSION | 81d6e852-3f10-11e4-adec-0800271c1b75 |
| 4 | DISCHARGE | 81d72550-3f10-11e4-adec-0800271c1b75 |
| 5 | TRANSFER | 7dc96b01-a509-11e4-9beb-0800271c1b75 |
| 6 | RADIOLOGY | 7c3f0372-a586-11e4-9beb-0800271c1b75 |
| 7 | INVESTIGATION | 81d6e852-3f10-11e4-adec-0800271c1b75 |
| 8 | LAB_RESULT | 81d6e852-3f10-11e4-adec-0800271c1b75 |
| 9 | Patient Document | TBD |
| 10 | VALIDATION NOTES | TBD |

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

### Programs (Active)

| Program | UUID | Priority |
|---------|------|----------|
| Tuberculosis | TBD | High |
| Hypertension | TBD | High |
| Diabetes | TBD | High |
| Sickle cell | TBD | High |
| Epilepsy | TBD | High |
| Mental Illness | TBD | Medium |
| Heart Disease | TBD | Medium |
| Stroke | TBD | Medium |
| Pregnancy | TBD | High |
| Child | TBD | Medium |
| TB - INH Prophylaxis | TBD | Medium |

### Subject Types

| Subject Type | UUID | Sync to Bahmni |
|--------------|------|----------------|
| Individual | TBD | Yes |
| Household | TBD | No |
| Phulwari | TBD | No |
| SHG | TBD | No |

---

*Document Version: 1.0*  
*Created: January 2025*  
*Status: Draft - Pending Doctor Review*  
*Authors: Himesh*
