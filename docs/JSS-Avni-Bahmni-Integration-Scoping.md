# JSS Avni-Bahmni Integration - Scoping Document

**Version:** 2.0 (Feed-Based Architecture)  
**Last Updated:** January 2025  
**Status:** Active Development

## Executive Summary

Bi-directional integration between **Avni** (community health platform) and **Bahmni** (hospital EMR) for **Jan Swasthya Sahyog (JSS)**.

**Architecture:** Feed-based synchronization using Atom feeds (Bahmni) and REST APIs (Avni)  
**Sync Strategy:** 1-to-1 entity mapping with no business logic in integration service

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
| **Integration Service** | Spring Boot (Java) | jss_ganiyari_dev branch | To be deployed on EC2 |
| **Integration DB** | PostgreSQL | 15.x | To be configured |
| **Avni** | Cloud hosted | prerelease.avniproject.org | Bi-directional sync |

**Key Change:** Using **local MySQL 5.6 Docker container** instead of external RDS for better performance and simplicity.

### Key Repositories

| Repository | Purpose | Branch/Tag |
|------------|---------|------------|
| [avniproject/integration-service](https://github.com/avniproject/integration-service/tree/jss_ganiyari) | Integration service code | jss_ganiyari_dev |
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

## 3. Sync Scope Summary

> **Detailed mappings:** See [JSS-Integration-Mapping-Configuration.md](./JSS-Integration-Mapping-Configuration.md)

### 3.1 Sync Direction Overview

| Direction | What Syncs | Purpose |
|-----------|------------|--------|
| **Avni → Bahmni** | Individual, Program Enrolment, Program Encounter | Doctors see field data |
| **Bahmni → Avni** | Lab Results, Radiology, Consultation | Field workers see hospital data |

### 3.2 Key Decisions

- **7 Programs in scope:** TB, Hypertension, Diabetes, Sickle Cell, Epilepsy, Pregnancy, Child
- **Only Individual subject type syncs** (has JSS ID)
- **DISCHARGE encounters excluded** — only contains "Adt Notes" (335 obs), PDF-based
- **Consultation contains prescriptions** — Drug Orders, not separate encounter

---

## 4. Feed-Based Synchronization

### 4.1 Bahmni Atom Feeds

**Reference:** [Atom Feed Synchronization in Bahmni](https://bahmni.atlassian.net/wiki/spaces/BAH/pages/3506200)

| Feed | Endpoint | Use Case |
|------|----------|----------|
| **Patient** | `/openmrs/ws/atomfeed/patient/recent` | Patient create/update → sync to Avni |
| **Encounter** | `/openmrs/ws/atomfeed/encounter/recent` | Filter by encounter type (Consultation, LAB_RESULT) |
| **Lab** | `/openmrs/ws/atomfeed/lab/recent` | Lab result notifications |

**Feed Processing:**
1. `AtomFeedClient` polls feed endpoint
2. `EventWorker` filters by encounter type UUID
3. `markers` table tracks last processed entry
4. `failed_events` table stores errors for retry

### 4.2 Bahmni REST APIs

**Reference:** [Bahmni REST API](https://bahmni.atlassian.net/wiki/spaces/BAH/pages/6488066)

| API | Endpoint | Purpose |
|-----|----------|---------|
| **Patient** | `/openmrs/ws/rest/v1/patient/{uuid}` | Get patient details |
| **Encounter** | `/openmrs/ws/rest/v1/encounter/{uuid}?v=full` | Get encounter with observations + drug orders |
| **Drug Orders** | `/openmrs/ws/rest/v1/order?patient={uuid}&t=drugorder` | Get prescriptions |

### 4.3 Avni REST APIs

| API | Method | Purpose |
|-----|--------|---------|
| `/api/subjects` | GET/POST | Individual sync |
| `/api/programEnrolments` | GET/POST | Program enrolment sync |
| `/api/programEncounters` | GET/POST | Program encounter sync |
| `/api/encounters` | POST | Create general encounter (for lab results, prescriptions) |

---

## 5. New Entity Types Required

> **Full mapping details:** See [JSS-Integration-Mapping-Configuration.md](./JSS-Integration-Mapping-Configuration.md)

### Summary

| System | New Entity Types | Count |
|--------|------------------|-------|
| **Avni** | Bahmni Lab Results, Bahmni Radiology Results, Bahmni Consultation | 3 |
| **Bahmni** | Field visit types (Field-TB, Field-NCD, Field-MCH) | 3 |
| **Bahmni** | Avni encounter types (per program enrolment/followup) | 14 |

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
- [x] Regular Real-time (within hours)
- [x] Error retrial Weekly sync
- [x] On-demand sync for specific criteria

**Q2:** How much historical data should be visible?
- [ ] All available data
- [ ] Other: _______________

**Q3:** Should field workers see indicators on dashboard for:
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
- [ ] Test Bahmni → Avni Consultation sync
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