# JSS Avni-Bahmni Integration - Scoping Document

**Version:** 2.0 (Feed-Based Architecture)  
**Last Updated:** January 2025  
**Status:** Active Development

## Executive Summary

Bi-directional integration between **Avni** (community health platform) and **Bahmni** (hospital EMR) for **Jan Swasthya Sahyog (JSS)**.

**Architecture:** Feed-based synchronization using Atom feeds (Bahmni) and REST APIs (Avni)  
**Sync Strategy:** 1-to-1 entity mapping with no business logic in integration service

### Core Principles

1. **No Business Data in Integration Service:** Only metadata mapping and entity transformation
2. **Avni → Bahmni:** Sync to existing Patient with same JSS ID, or create new Patient with JSS ID
3. **Bahmni → Avni:** Sync only if JSS ID exists and Individual already exists (no new Individual creation)
4. **Feed-Based:** Bahmni uses Atom feeds; Avni uses REST APIs
5. **Metadata-Driven:** All mappings defined in integration DB, not hardcoded

---

## Table of Contents

1. [Organization Context](#1-organization-context)
2. [Technical Architecture](#2-technical-architecture)
3. [Entity Types & Sync Scope](#3-entity-types--sync-scope)
4. [Feed-Based Synchronization](#4-feed-based-synchronization)
5. [New Encounter Types & Visit Types](#5-new-encounter-types--visit-types)
6. [Metadata Mapping Steps](#6-metadata-mapping-steps)
7. [Doctor Questionnaire](#7-doctor-questionnaire)
8. [Implementation Checklist](#8-implementation-checklist)

---

## 1. Organization Context

### About JSS (Jan Swasthya Sahyog)

- **Location:** Bilaspur, Chhattisgarh, India
- **Mission:** Affordable healthcare for tribal & rural communities
- **Key Programs:**
  - Ganiyari Outpatient Clinic (referral health centre)
  - Rural Outreach Clinics (3 sub-centres, up to 60km from Ganiyari)
  - Village Health Program (110 VHWs across 54 villages)
  - Health System Strengthening initiatives

### Current Systems

| System | Purpose | Users |
|--------|---------|-------|
| **Avni** | Field-based community health data collection | Village Health Workers (VHWs), Field staff |
| **Bahmni** | Hospital EMR at Ganiyari | Doctors, nurses, lab technicians |

---

## 2. Technical Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLOUD (Samanvay)                               │
│  ┌─────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐  │
│  │                 │Sync│                     │Sync│                     │  │
│  │  Avni Server    │◄──►│  Integration        │◄──►│  Bahmni (Docker)    │  │
│  │  (Production)   │    │  Service            │    │  + MySQL RDS        │  │
│  │                 │    │                     │    │                     │  │
│  └─────────────────┘    └─────────────────────┘    └─────────────────────┘  │
│         ▲                                                    ▲              │
└─────────│────────────────────────────────────────────────────│──────────────┘
          │                                                    │
          │ (Sync)                                    (Network Whitelist)
          │                                                    │
    ┌─────▼─────┐                                    ┌─────────▼─────────┐
    │   Avni    │                                    │   JSS Ganiyari    │
    │   Mobile  │                                    │   LAN (Bahmni     │
    │   App     │                                    │   Production)     │
    └───────────┘                                    └───────────────────┘
```

### Technology Stack

| Component | Technology | Version | Notes |
|-----------|------------|---------|-------|
| **EC2 Instance** | AWS t3.large | 7.6 GiB RAM + 8 GiB swap | 100GB gp3 volume |
| **OS** | Ubuntu 22.04 LTS | x86_64 | Docker pre-installed |
| **Docker Compose** | Docker Compose | Latest | Local MySQL setup (not RDS) |
| **OpenMRS** | Core EMR | 1.0.0-644 | Bahmni OpenMRS image |
| **MySQL** | OpenMRS DB | 5.6 | Local Docker container |
| **Bahmni Web** | UI Frontend | 1.1.0-696 | Apache HTTPD proxy |
| **SSL/HTTPS** | Let's Encrypt | Current | Certbot auto-renewal |
| **Integration Service** | Spring Boot (Java) | jss_ganiyari branch | To be deployed on EC2 |
| **Integration DB** | PostgreSQL | 15.x | To be configured |
| **Avni** | Cloud hosted | prerelease.avniproject.org | Bi-directional sync |

**Key Change:** Using **local MySQL 5.6 Docker container** instead of external RDS for better performance and simplicity.

### Key Repositories

| Repository | Purpose | Branch/Tag |
|------------|---------|------------|
| [avniproject/integration-service](https://github.com/avniproject/integration-service/tree/jss_ganiyari) | Integration service code | jss_ganiyari |
| [JanSwasthyaSahyog/bahmni-docker](https://github.com/JanSwasthyaSahyog/bahmni-docker) | JSS Bahmni Docker setup | master |
| [JanSwasthyaSahyog/jss-config](https://github.com/JanSwasthyaSahyog/jss-config) | JSS Bahmni configuration (UI, forms, concepts) | master |
| [avniproject/integration-service (scripts/aws)](https://github.com/avniproject/integration-service/tree/master/scripts/aws) | AWS infrastructure automation | master |

### Current Prerelease Server Details

| Item | Value |
|------|-------|
| **Instance ID** | i-0e128ab9da4c8d30f |
| **Instance Type** | t3.large (7.6 GiB RAM) |
| **Public IP** | 3.110.219.176 |
| **DNS** | jss-bahmni-prerelease.avniproject.org |
| **URL** | https://jss-bahmni-prerelease.avniproject.org/bahmni/home/index.html#/dashboard |
| **Database** | 246 tables (restored from production backup) |
| **Status** | ✓ Fully Operational |

---

## 3. Entity Types & Sync Scope

### 3.1 Avni → Bahmni (All Community Data)

| Avni Entity | Bahmni Entity | Sync Condition | Notes |
|-------------|---------------|----------------|-------|
| **Individual** | Patient | Always | Create if not exists with JSS ID |
| **Program Enrolment** | Encounter | Always | Maps to program-specific encounter type |
| **Program Encounter** | Encounter | Always | Maps to program-specific encounter type |
| **General Encounter** | Encounter | Always | Maps to general encounter type |

### 3.2 Bahmni → Avni (Field Service Data Only)

| Bahmni Entity | Avni Entity | Sync Condition | Notes |
|---------------|-------------|----------------|-------|
| **Patient** | Individual | Only if JSS ID exists in Avni | No new Individual creation |
| **Lab Result Encounter** | General Encounter | Only if Patient has JSS ID | Lab results for followup |
| **Radiology Encounter** | General Encounter | Only if Patient has JSS ID | X-ray/imaging reports |
| **Discharge Encounter** | General Encounter | Only if Patient has JSS ID | Discharge summaries |
| **Visit Summary** | General Encounter | Only if Patient has JSS ID | Visit list aggregation |

### 3.3 Subject Type Sync Scope

| Avni Subject Type | Sync to Bahmni | Reason |
|-------------------|----------------|--------|
| **Individual** | ✅ YES | Primary subject with JSS ID |
| Household | ❌ NO | No primary identifier |
| Phulwari | ❌ NO | No primary identifier |
| SHG | ❌ NO | No primary identifier |

---

## 4. Feed-Based Synchronization

### 4.1 Bahmni Atom Feeds

Bahmni publishes events via Atom feeds. Integration service consumes these feeds:

| Feed | Endpoint | Event Type | Frequency |
|------|----------|-----------|-----------|
| **Patient Feed** | `/openmrs/ws/atomfeed/patient/recent` | Patient creation/update | Real-time |
| **Encounter Feed** | `/openmrs/ws/atomfeed/encounter/recent` | Encounter creation/update | Real-time |

**Processing:**
- AtomFeedClient reads feed entries
- EventWorker processes each event
- Markers track processed events (no re-processing)
- Failed events stored in error table

### 4.2 Avni REST APIs

Integration service calls Avni APIs for sync:

| API | Method | Purpose |
|-----|--------|---------|
| `/api/subject` | GET/POST | Individual sync |
| `/api/programEnrolment` | GET/POST | Program enrolment sync |
| `/api/programEncounter` | GET/POST | Program encounter sync |
| `/api/generalEncounter` | GET/POST | General encounter sync |

**Processing:**
- Fetch entities from Avni
- Transform to Bahmni format
- Create/update via OpenMRS REST API
- Track sync status in integration DB

### 4.3 Sync Flow

```
Bahmni Atom Feed                    Avni REST API
       │                                  │
       ├─ Patient Event                   ├─ Individual
       │  └─ PatientEventWorker           │  └─ AvniIndividualWorker
       │     └─ Create/Update in Avni     │     └─ Create/Update Patient in Bahmni
       │
       ├─ Encounter Event                 ├─ Program Enrolment
       │  └─ PatientEncounterEventWorker  │  └─ AvniEnrolmentWorker
       │     └─ Create/Update in Avni     │     └─ Create Encounter in Bahmni
       │                                  │
       │                                  ├─ Program Encounter
       │                                  │  └─ AvniEncounterWorker
       │                                  │     └─ Create Encounter in Bahmni
       │                                  │
       │                                  └─ General Encounter
       │                                     └─ AvniGeneralEncounterWorker
       │                                        └─ Create Encounter in Bahmni
```

---

## 5. New Encounter Types & Visit Types

### 5.1 New Avni Encounter Types (for Bahmni → Avni sync)

| Encounter Type | Program | Purpose | Form Type |
|----------------|---------|---------|-----------|
| **Bahmni Lab Results** | Individual (General) | Lab results from Bahmni | GeneralEncounter |
| **Bahmni Radiology Report** | Individual (General) | X-ray/imaging from Bahmni | GeneralEncounter |
| **Bahmni Discharge Summary** | Individual (General) | Discharge info from Bahmni | GeneralEncounter |
| **Bahmni Visit Summary** | Individual (General) | Visit aggregation from Bahmni | GeneralEncounter |

### 5.2 New Bahmni Visit Types (for Avni → Bahmni sync)

| Visit Type | Code | Purpose | Programs |
|------------|------|---------|----------|
| **Field** | FIELD | General field visits from Avni | All |
| **Field-TB** | FIELD_TB | TB program field visits | TB, TB-INH Prophylaxis |
| **Field-NCD** | FIELD_NCD | NCD program field visits | HTN, DM, Epilepsy, Sickle Cell, etc. |
| **Field-MCH** | FIELD_MCH | MCH program field visits | Pregnancy, Child |

### 5.3 New Bahmni Encounter Types (for Avni → Bahmni sync)

| Encounter Type | Visit Type | Avni Source | Purpose |
|----------------|-----------|-------------|---------|
| **Avni Registration** | Field | Individual Registration | Demographics sync |
| **Avni TB Enrolment** | Field-TB | TB Program Enrolment | TB enrollment data |
| **Avni TB Followup** | Field-TB | TB Followup Encounter | TB treatment followup |
| **Avni HTN Enrolment** | Field-NCD | Hypertension Enrolment | HTN enrollment |
| **Avni HTN Followup** | Field-NCD | Hypertension Followup | HTN followup |
| **Avni DM Enrolment** | Field-NCD | Diabetes Enrolment | DM enrollment |
| **Avni DM Followup** | Field-NCD | Diabetes Followup | DM followup |
| **Avni Epilepsy Enrolment** | Field-NCD | Epilepsy Enrolment | Epilepsy enrollment |
| **Avni Epilepsy Followup** | Field-NCD | Epilepsy Followup | Epilepsy followup |
| **Avni Sickle Cell Enrolment** | Field-NCD | Sickle cell Enrolment | Sickle cell enrollment |
| **Avni Sickle Cell Followup** | Field-NCD | Sickle cell Followup | Sickle cell followup |
| **Avni ANC Visit** | Field-MCH | ANC Home Visit | Antenatal care |
| **Avni Delivery** | Field-MCH | Delivery | Delivery details |
| **Avni PNC** | Field-MCH | Mother PNC, Child PNC | Postnatal care |

---

## 6. Metadata Mapping Steps

### 6.1 Concept Mapping

**Step 1:** Extract all Bahmni concepts from database
- Query: `SELECT concept_id, name, uuid, class FROM concept`
- Output: `bahmni-metadata/concepts/concepts.json`

**Step 2:** Extract all Avni concepts from REST API
- Endpoint: `/api/concept`
- Output: `avni-metadata/concepts.json`

**Step 3:** Create mapping entries in integration DB
- Table: `concept_mapping`
- Columns: `bahmni_concept_uuid`, `avni_concept_name`, `mapping_type` (EXACT, SEMANTIC, FALLBACK)

**Step 4:** Validate mappings
- Ensure all mandatory concepts are mapped
- Document fallback concepts for missing mappings

### 6.2 Encounter Type Mapping

**Step 1:** Extract Bahmni encounter types
- Query: `SELECT encounter_type_id, name, uuid FROM encounter_type`
- Output: `bahmni-metadata/encounter_types/encounter_types.json`

**Step 2:** Extract Avni encounter types
- Endpoint: `/api/encounterType`
- Output: `avni-metadata/encounterTypes.json`

**Step 3:** Create mapping entries
- Table: `encounter_type_mapping`
- Columns: `bahmni_encounter_type_uuid`, `avni_encounter_type_name`, `direction` (BAHMNI_TO_AVNI, AVNI_TO_BAHMNI)

### 6.3 Program Mapping

**Step 1:** Extract Avni programs
- Endpoint: `/api/program`
- Output: `avni-metadata/programs.json`

**Step 2:** Create mapping entries
- Table: `program_mapping`
- Columns: `avni_program_name`, `bahmni_encounter_type_uuid`, `bahmni_visit_type_uuid`

### 6.4 Patient/Subject Identifier Mapping

**Step 1:** Identify identifier fields
- Bahmni: `Patient Identifier` (type_id = 3) = JSS ID
- Avni: `JSS ID` field in Individual registration

**Step 2:** Create mapping constants
- Table: `constant`
- Entries:
  - `BahmniIdentifierType`: UUID of Patient Identifier type
  - `AvniIdentifierConcept`: "JSS ID"

---

## 7. Doctor Questionnaire

### Purpose
Clinical input to finalize sync decisions for field service data.

### Section A: Lab Results Priority

Which lab results are **most critical** for field workers?

| Lab Test | Priority (High/Medium/Low) | Notes |
|----------|---------------------------|-------|
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

### Section B: Abnormal Value Alerts

Should field workers see alerts for abnormal values? If yes, specify thresholds:

| Lab Test | Low Alert | High Alert |
|----------|-----------|------------|
| Haemoglobin | < ___ g/dL | > ___ g/dL |
| Fasting Blood Sugar | < ___ mg/dL | > ___ mg/dL |
| HbA1c | | > ___ % |
| Serum Creatinine | | > ___ mg/dL |
| Systolic BP | < ___ mmHg | > ___ mmHg |
| Diastolic BP | < ___ mmHg | > ___ mmHg |

### Section C: Radiology Results

Which radiology results should sync to Avni?

| Radiology Type | Sync (Yes/No) | Detail Level |
|----------------|---------------|--------------|
| Chest X-Ray | | Full/Summary/Abnormal only |
| USG Abdomen | | Full/Summary/Abnormal only |
| USG Pelvis | | Full/Summary/Abnormal only |
| ECG | | Full/Summary/Abnormal only |
| Echo | | Full/Summary/Abnormal only |

### Section D: Discharge Summary

What discharge information is essential for field followup?

| Field | Include (Yes/No) |
|-------|------------------|
| Primary Diagnosis | |
| Secondary Diagnoses | |
| Procedures Performed | |
| Discharge Medications | |
| Follow-up Date | |
| Follow-up Instructions | |
| Warning Signs | |
| Diet Instructions | |
| Activity Restrictions | |

### Section E: Program-Specific Questions

#### TB Program
What Bahmni data is most important for TB field workers?

| Data Type | Priority | Notes |
|-----------|----------|-------|
| Sputum AFB results | | |
| CBNAAT results | | |
| Chest X-ray findings | | |
| Drug sensitivity results | | |
| Treatment regimen changes | | |
| Side effects documented | | |
| Weight changes | | |

#### Hypertension Program
What Bahmni data is most important for HTN field workers?

| Data Type | Priority | Notes |
|-----------|----------|-------|
| BP readings at clinic | | |
| Medication changes | | |
| Kidney function (Creatinine) | | |
| Cardiac evaluation (ECG/Echo) | | |
| Complications documented | | |

#### Diabetes Program
What Bahmni data is most important for DM field workers?

| Data Type | Priority | Notes |
|-----------|----------|-------|
| Blood sugar readings | | |
| HbA1c | | |
| Medication changes | | |
| Kidney function | | |
| Eye examination results | | |
| Foot examination results | | |
| Complications documented | | |

#### Sickle Cell Program
What Bahmni data is most important for Sickle Cell field workers?

| Data Type | Priority | Notes |
|-----------|----------|-------|
| Hb levels | | |
| Hb Electrophoresis | | |
| Crisis episodes documented | | |
| Transfusion records | | |
| Hydroxyurea dosing | | |
| Complications | | |

### Section F: General Questions

**Q1:** How recent should synced data be for field workers?
- [ ] Real-time (within hours)
- [ ] Daily sync
- [ ] Weekly sync
- [ ] On-demand only

**Q2:** How much historical data should be visible?
- [ ] Last visit only
- [ ] Last 3 months
- [ ] Last 6 months
- [ ] Last 1 year
- [ ] All available data

**Q3:** Should field workers receive notifications for:
- [ ] New lab results available
- [ ] Abnormal lab values
- [ ] Discharge from hospital
- [ ] Missed follow-up appointments
- [ ] Other: _______________

**Q4:** Any other data from Bahmni that would be valuable for field workers?

_________________________________________________________________

---

## 8. Implementation Checklist

### Phase 1: Infrastructure & Setup

- [ ] Bahmni Docker running on EC2
- [ ] MySQL 5.6 database restored
- [ ] Integration service deployed
- [ ] PostgreSQL integration DB configured
- [ ] Atom feed endpoints accessible
- [ ] Avni API endpoints accessible

### Phase 2: Metadata Mapping

- [ ] Extract Bahmni concepts
- [ ] Extract Avni concepts
- [ ] Create concept mappings
- [ ] Extract encounter types
- [ ] Create encounter type mappings
- [ ] Create program mappings
- [ ] Configure identifier mapping constants

### Phase 3: New Entity Types

- [ ] Create Avni encounter types (Lab Results, Radiology, Discharge, Visit Summary)
- [ ] Create Bahmni visit types (Field, Field-TB, Field-NCD, Field-MCH)
- [ ] Create Bahmni encounter types (Avni Registration, Avni TB Enrolment, etc.)

### Phase 4: Testing

- [ ] Test Avni → Bahmni Patient sync
- [ ] Test Avni → Bahmni Program Enrolment sync
- [ ] Test Bahmni → Avni Lab Result sync
- [ ] Test Bahmni → Avni Radiology sync
- [ ] Test Bahmni → Avni Discharge sync
- [ ] Verify error handling and retry logic

### Phase 5: Doctor Review & Finalization

- [ ] Complete doctor questionnaire
- [ ] Incorporate feedback into mappings
- [ ] Finalize alert thresholds
- [ ] Finalize data priority

---

*Document Version: 2.0*  
*Last Updated: January 2025*  
*Focus: Feed-based architecture, entity types, metadata mapping*
