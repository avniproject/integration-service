# JSS Avni-Bahmni Integration - Scoping Document

⚠️ **SECURITY NOTICE:** All credentials, passwords, and API keys have been moved to a separate secure file: `docs/CREDENTIALS_REFERENCE.md`. This file should be stored in a secure vault (password manager, encrypted storage, etc.) and **NEVER committed to the repository**. See `.gitignore` for protection rules.

## Executive Summary

This document outlines the integration between **Avni** (field-based community health platform) and **Bahmni** (hospital EMR) for **Jan Swasthya Sahyog (JSS)**, a healthcare organization serving tribal and rural communities in Chhattisgarh, India.

**Integration Type:** Bi-directional (asymmetric)  
**Primary Goal:** Enable clinical data synchronization for field service and unified research data

### Key Principles
- **ID Generation:** Always on Bahmni (GAN ID)
- **Avni receives:** Only information needed for field service (lab reports, X-rays, visits list, discharge summaries for followup)
- **Bahmni receives:** All data from Avni (complete community health data)
- **Research:** Bahmni serves as the unified repository for clinical + community data research

---

## Table of Contents

1. [Organization Context](#1-organization-context)
2. [Technical Architecture](#2-technical-architecture)
3. [Integration Scope](#3-integration-scope)
4. [Phase Breakdown](#4-phase-breakdown)
5. [Development Environment Setup](#5-development-environment-setup)
6. [AWS RDS Setup](#6-aws-rds-setup)
7. [Bahmni Docker Setup](#7-bahmni-docker-setup)
8. [Integration Service Configuration](#8-integration-service-configuration)
9. [Mapping Configuration](#9-mapping-configuration)
10. [Dev Environment Respawn Procedures](#10-dev-environment-respawn-procedures)
11. [Testing & Verification](#11-testing--verification)
12. [Deliverables Checklist](#12-deliverables-checklist)

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

## 3. Integration Scope

### Data Flow Directions

#### Avni → Bahmni (Complete - All Community Data)
- Subject registration data → Patient creation
- Program enrolments → Bahmni encounters
- Program encounters → Bahmni encounters
- Field observations → Clinical observations
- All community health data for research purposes

#### Bahmni → Avni (Selective - Field Service Data Only)
- Lab reports → Avni encounters
- X-ray reports → Avni encounters  
- Visits list → Avni encounters
- Discharge summaries → Avni encounters (for followup)

### 3.1 Bahmni Forms Analysis

**Objective:** Identify clinical data in Bahmni that should sync to Avni for field service.

#### Analysis Methodology

1. **Access Bahmni Forms Configuration**
   ```bash
   # Clone JSS config repository
   git clone https://github.com/JanSwasthyaSahyog/jss-config.git
   cd jss-config
   
   # Key configuration files to examine:
   # - openmrs/apps/clinical/app.json (clinical app settings)
   # - openmrs/apps/clinical/formConditions.js (form visibility rules)
   # - openmrs/apps/clinical/formDetails.json (form definitions)
   # - openmrs/apps/registration/app.json (patient registration)
   # - bahmni_config/openmrs/apps/ (all app configurations)
   # - bahmni_config/openmrs/concepts/ (concept definitions)
   ```

2. **Query OpenMRS Database for Forms/Concepts**
   
   **Access prerelease database:**
   ```bash
   ssh -i ~/.ssh/openchs-infra.pem ubuntu@jss-bahmni-prerelease.avniproject.org
   docker exec bahmni-docker-openmrsdb-1 mysql -u openmrs_admin -pOpenMRS_JSS2024 openmrs
   ```

   **List all active encounter types:**
   ```sql
   SELECT encounter_type_id, name, uuid FROM encounter_type WHERE retired = 0 ORDER BY name;
   ```

   **List all active forms:**
   ```sql
   SELECT form_id, name, uuid, version FROM form WHERE retired = 0 ORDER BY name;
   ```

   **Map forms to encounter types:**
   ```sql
   SELECT f.form_id, f.name as form_name, et.name as encounter_type, f.uuid
   FROM form f
   LEFT JOIN encounter_type et ON f.encounter_type = et.encounter_type_id
   WHERE f.retired = 0
   ORDER BY et.name, f.name;
   ```

   **List concept sets (form structure):**
   ```sql
   SELECT DISTINCT c.concept_id, cn.name as concept_name, c.uuid
   FROM concept c
   JOIN concept_name cn ON c.concept_id = cn.concept_id
   JOIN concept_class cc ON c.class_id = cc.concept_class_id
   WHERE cc.name = 'ConvSet' AND cn.locale = 'en' AND cn.concept_name_type = 'FULLY_SPECIFIED'
   ORDER BY cn.name;
   ```

   **List lab test concepts:**
   ```sql
   SELECT c.concept_id, cn.name, c.uuid, cc.name as class_name
   FROM concept c
   JOIN concept_name cn ON c.concept_id = cn.concept_id
   JOIN concept_class cc ON c.class_id = cc.concept_class_id
   WHERE cc.name IN ('Test', 'LabSet', 'Finding', 'Procedure')
   AND cn.locale = 'en' AND cn.concept_name_type = 'FULLY_SPECIFIED'
   ORDER BY cc.name, cn.name;
   ```

   **List radiology/imaging concepts:**
   ```sql
   SELECT c.concept_id, cn.name, c.uuid
   FROM concept c
   JOIN concept_name cn ON c.concept_id = cn.concept_id
   WHERE cn.name LIKE '%X-Ray%' OR cn.name LIKE '%Radiology%' OR cn.name LIKE '%USG%' OR cn.name LIKE '%Imaging%'
   AND cn.locale = 'en'
   ORDER BY cn.name;
   ```

   **List drug orders (prescriptions):**
   ```sql
   SELECT DISTINCT c.concept_id, cn.name, c.uuid
   FROM concept c
   JOIN concept_name cn ON c.concept_id = cn.concept_id
   WHERE c.concept_id IN (SELECT drug_id FROM drug WHERE retired = 0)
   AND cn.locale = 'en'
   ORDER BY cn.name
   LIMIT 50;
   ```

3. **Review Bahmni Clinical App Configuration**
   
   **Check clinical app features:**
   ```bash
   cat jss-config/openmrs/apps/clinical/app.json | jq '.sections'
   ```
   
   **Key sections to identify:**
   - Lab Results section
   - Radiology/Imaging section
   - Discharge Summary section
   - Prescriptions section
   - Diagnoses section
   - Vitals section
   - Observations section

4. **Document Findings in Tables Below**

#### Bahmni Clinical Data for Avni (Field Service Sync)

| Data Type | Bahmni Source | Avni Target Encounter | Priority | Sync Direction | Notes |
|-----------|---------------|----------------------|----------|-----------------|-------|
| Lab Results | OpenELIS / Lab Module | Lab Results | High | Bahmni → Avni | For field followup |
| X-Ray/Imaging Reports | PACS / Radiology Module | Radiology Report | High | Bahmni → Avni | For field followup |
| Discharge Summary | Discharge Template / Visit Notes | Discharge Summary | High | Bahmni → Avni | For followup care |
| Prescriptions | Drug Orders | Prescription | Medium | Bahmni → Avni | Current medications |
| Diagnoses | Diagnosis Module | Diagnosis | Medium | Bahmni → Avni | Clinical diagnoses |
| Visit Summary | Visit Notes / Consultation | Visit Summary | Medium | Bahmni → Avni | Clinical summary |
| Vital Signs | Vitals Observation | Vitals | Low | Bahmni → Avni | Reference data |

#### Bahmni Encounter Types (To Be Populated After Analysis)

| Encounter Type | UUID | Form Name | Sync to Avni | Field Service Use | Notes |
|----------------|------|-----------|--------------|-------------------|-------|
| *To be populated after DB analysis* | | | | | |

**Instructions for population:**
- Run the "Map forms to encounter types" query above
- For each encounter type, determine if it contains field-service-relevant data
- Mark "Sync to Avni" as Yes/No based on clinical relevance
- Document the field service use case

#### Bahmni Concept Mapping (To Be Populated After Analysis)

| Bahmni Concept | Bahmni UUID | Concept Class | Avni Concept | Avni UUID | Notes |
|----------------|-------------|---------------|--------------|-----------|-------|
| *To be populated after DB analysis* | | | | | |

**Instructions for population:**
- Run concept queries above to identify key clinical concepts
- Map to corresponding Avni concepts
- Prioritize lab tests, diagnoses, and vital signs
- Document any concept name mismatches

#### Bahmni Observation Templates (To Be Populated)

| Template Name | Observations | Sync to Avni | Notes |
|---------------|--------------|--------------|-------|
| *To be populated after app.json review* | | | |

**Key templates to identify:**
- Lab Results Template
- Discharge Summary Template
- Vitals Template
- Radiology Report Template

### Patient Matching Strategy

**Primary Identifier:** GAN ID (Ganiyari ID)
- Bahmni patients have GAN ID as patient identifier
- Avni subjects must store GAN ID for matching
- Integration service uses GAN ID for bi-directional linking

### Avni Subject Types (JSSCP)

| Subject Type | Category | Integration Scope |
|--------------|----------|-------------------|
| **Individual** | Clinical | Primary - maps to Bahmni Patient |
| Household | Non-Clinical | Sync to Bahmni for research (P1) |
| Phulwari | Non-Clinical | Not in Phase 1 |
| SHG | Non-Clinical | Not in Phase 1 |
| Monthly Monitoring | Operational | Not in Phase 1 |

### Avni Programs - Data Categorization

#### Clinical Programs (P1 - Sync to Bahmni)

| Program | Priority | Notes |
|---------|----------|-------|
| Tuberculosis | High | Active treatment tracking |
| Hypertension | High | Chronic disease management |
| Sickle Cell | High | Chronic disease management |
| Diabetes | High | Chronic disease management |
| Epilepsy | High | Chronic disease management |
| Mental Illness | Medium | Chronic disease management |
| Heart Disease | Medium | Chronic disease management |
| Stroke | Medium | Chronic disease management |
| Asthma | Medium | Chronic disease management |
| Cancer | Medium | Chronic disease management |
| Thyroidism | Medium | Chronic disease management |
| Arthritis | Medium | Chronic disease management |
| COPD | Medium | Chronic disease management |
| TB-INH Prophylaxis | Medium | Preventive treatment |
| Pregnancy | High | MCH - clinical tracking |

#### Non-Clinical Programs (P1 - Sync to Bahmni for Research)

| Program | Category | Notes |
|---------|----------|-------|
| Eligible Couple | Reproductive Health | Family planning tracking |
| Child | Growth Monitoring | Nutrition surveillance |
| Women's Health Camp | Community Health | Camp-based screening |

#### Non-Clinical Programs (Exclude from P1)

| Program | Category | Reason |
|---------|----------|--------|
| Phulwari | Nutrition/Childcare | Operational - not patient-specific |
| TB Study | Research | Separate research protocol |

### Avni Encounter Types - Data Categorization

#### Clinical Encounters (P1 - Sync to Bahmni)

| Encounter Type | Program Context |
|----------------|----------------|
| Treatment review | TB |
| TB followup | TB |
| TB Lab test results | TB |
| TB Family Screening Form | TB |
| TB Mantoux test result | TB |
| TB referral status | TB |
| CBNAAT result, LPA result, LJ result | TB Lab |
| INH Prophylaxis follow up | TB Prevention |
| Hypertension Followup | Hypertension |
| Hypertension referral status | Hypertension |
| Diabetes Followup | Diabetes |
| Diabetes lab test | Diabetes |
| Diabetes referral status | Diabetes |
| Epilepsy followup | Epilepsy |
| Epilepsy referral status | Epilepsy |
| Sickle cell followup | Sickle Cell |
| Sickle cell lab test | Sickle Cell |
| Sickle cell referral status | Sickle Cell |
| Lab test | General |
| Lab Investigations | Pregnancy |
| ANC Clinic Visit | Pregnancy |
| ANC Home Visit | Pregnancy |
| USG Report | Pregnancy |
| Delivery | Pregnancy |
| Birth | Pregnancy |
| Mother PNC | Pregnancy |
| Child PNC | Pregnancy |
| Child Birth | Pregnancy |
| Abortion | Pregnancy |
| Abortion followup | Pregnancy |
| Referral Status | General |

#### Non-Clinical Encounters (Sync to Bahmni for Research)

| Encounter Type | Category |
|----------------|----------|
| Death | Mortality surveillance |
| Verbal autopsy (newborn/child/maternal/adult) | Mortality surveillance |
| Eligible Couple Follow-up | Reproductive health |
| Growth Monitoring | Child nutrition |
| Eye screening form | Screening |
| Women's Health Camp | Community screening |
| HBNC | Newborn care |
| PPMC | Postpartum care |

#### Operational Encounters (Exclude from P1)

| Encounter Type | Reason |
|----------------|--------|
| Village Round | Household survey - not patient-specific |
| Daily Attendance Form | Phulwari operations |
| Observation Checklist | Phulwari quality |
| Child Absent followup Form | Phulwari tracking |
| Monthly monitoring | Operational reporting |
| Monthly Cluster monitoring | Operational reporting |
| SHG monthly data collection | SHG operations |
| Monthly/Weekly VHW reports | Staff reporting |
| Senior Health Worker reports | Staff reporting |
| Monthly Animal Health Worker Reporting | Animal health |
| Albendazole | Mass drug administration |
| TB Study encounters | Research protocol |

---

## 4. Phase Breakdown

### Phase 1 (P1): Clinically Important Data Integration

**Objective:** Integrate clinically important data between Avni and Bahmni

| Deliverable | Description | Owner | Status |
|-------------|-------------|-------|--------|
| Bahmni System Assessment | Understand JSS Bahmni - network topology, server setup, versions | Himesh | ✓ Completed |
| Setup Bahmni on Samanvay | EC2 instance with Bahmni Docker + local MySQL 5.6 | Himesh | ✓ Completed |
| Integration Service Deployment | Linux service setup with DB configuration | Himesh | Not Started |
| Integration Service Learning | Understand codebase, Avni-Bahmni integration patterns | Himesh/Nupoor | Started |
| Integration Service Module | Merged codebase with fixed unit tests, CI running | Himesh | Not Started |
| DB Configuration | PostgreSQL setup for integration service | Himesh | Not Started |
| Makefile Commands | Logging, DB dump, restart, build, deploy operations | Himesh | Not Started |

| Admin Console App | Web app for mapping concepts, encounter types, programs | Nupoor | Not Started |
| Tech Analysis & Flow Definition | Analyze Bahmni-Avni form compatibility, define MainJob/ErrorJobs | Himesh/Nupoor | Not Started |
| Bahmni Forms Analysis | Identify clinical data in Bahmni to sync to Avni (see Section 3.1) | Himesh/Nupoor | Not Started |
| Data-fix Analysis | Data updates needed on both systems for integration | Himesh/Nupoor | Not Started |
| Mapping Configuration | Concept, encounter type, program mappings | Nupoor | Not Started |
| Data Correction Endpoints | Idempotent endpoints for data fixes | Nupoor | Not Started |
| Postman Collection | API requests with documentation | Himesh | Not Started |

### Documentation & Training (Across Phases)

| Deliverable | Description | Owner | Status |
|-------------|-------------|-------|--------|
| Comprehensive Integration Document | Architecture, setup, troubleshooting | Nupoor | Not Started |
| Maintenance Commands Handbook | Quick reference for operations | Nupoor | Not Started |
| Learning & Maintenance Videos | YouTube training for IT admin | Nupoor | Not Started |
| IT Admin Training | Train Ramnarayan on operations | Nupoor | Not Started |
| End User Training | Train-the-trainer for field workers | Nupoor | Not Started |

### Review Process

1. **Internal Review:** Scoping document review within team
2. **External Review:** Share with JSS stakeholders (Gajanan, Ravindra, Ramnarayan, Subhangi)

---

## 5. Development Environment Setup

### Prerequisites

- AWS CLI configured with appropriate credentials
- Docker & Docker Compose installed
- Java 17+ (for integration service)
- PostgreSQL client
- Git access to all repositories

### Environment Variables Reference

⚠️ **CREDENTIALS NOTICE:** All credentials and passwords have been moved to `docs/credential_jss_bahmni_prerelease_integration.md` (secure file, not in repository).

```bash
# Integration Service
export AVNI_INT_DATABASE=bahmni_avni
export AVNI_INT_DB_USER=bahmni_avni_admin
export AVNI_INT_DB_PASSWORD=[FROM_SECURE_STORE]

# Avni Connection
export BAHMNI_AVNI_API_URL=https://prerelease.avniproject.org
export BAHMNI_AVNI_API_USER=[FROM_SECURE_STORE]
export BAHMNI_AVNI_API_PASSWORD=[FROM_SECURE_STORE]
export BAHMNI_AVNI_IDP_TYPE=Cognito

# OpenMRS/Bahmni Connection
export OPENMRS_BASE_URL=http://localhost:8080
export OPENMRS_USER=[FROM_SECURE_STORE]
export OPENMRS_PASSWORD=[FROM_SECURE_STORE]

# Scheduling (disabled for dev)
export BAHMNI_SCHEDULE_CRON=-
export BAHMNI_SCHEDULE_CRON_FULL_ERROR=-
```

**See `docs/credential_jss_bahmni_prerelease_integration.md` for actual credential values.**

---

## 6. AWS EC2 Instance Setup

### Current Instance Configuration

**Status:** ✓ Fully Operational

```
Instance ID: i-0e128ab9da4c8d30f
Instance Type: t3.large (7.6 GiB RAM)
OS: Ubuntu 22.04 LTS (x86_64)
Volume: 100GB gp3
Swap: 8GB (configured)
Public IP: 3.110.219.176
DNS: jss-bahmni-prerelease.avniproject.org
VPC: vpc-0132b5c63278b2c52 (prerelease VPC)
Subnet: ap-south-1a
Security Group: Allows HTTP/HTTPS/SSH
```

### Instance Setup Commands

```bash
# Create EC2 instance (t3.large recommended)
./jss-infra-setup.sh ec2 jss-avni-bahmni-prerelease subnet-xxx sg-xxx t3.large 100

# SSH to instance
make ssh-prerelease

# Configure swap (8GB recommended for t3.large)
sudo fallocate -l 8G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile

# Verify swap
free -h
```

### Database Configuration (Local MySQL)

**Key Change:** Using **local MySQL 5.6 Docker container** instead of external RDS

```bash
# Create external volume for database persistence
docker volume create openmrs_db_data

# Start MySQL container
docker compose -f docker-compose.yml -f docker-compose.override.yml up -d openmrsdb
sleep 30

# Restore database backup
gunzip -c /path/to/backup.sql.gz | docker exec -i bahmni-docker-openmrsdb-1 mysql -u root -pOpenMRS_JSS2024 openmrs

# Verify restore
docker exec bahmni-docker-openmrsdb-1 mysql -u root -pOpenMRS_JSS2024 openmrs -e "SELECT COUNT(*) as tables FROM information_schema.tables WHERE table_schema='openmrs';"
```

### Database Credentials

| Item | Value |
|------|-------|
| **Host** | openmrsdb (Docker container) |
| **Port** | 3306 |
| **Database** | openmrs |
| **Tables** | 246 (from production backup) |

---

## 7. Bahmni Docker Setup

### Current Deployment Status

**✓ FULLY OPERATIONAL**

```
URL: https://jss-bahmni-prerelease.avniproject.org/bahmni/home/index.html#/dashboard
Login: admin / test
OpenMRS: 1.0.0-644 (running)
MySQL: 5.6 (local Docker container)
Bahmni Web: 1.1.0-696
Configuration: jss-config from GitHub
SSL: Let's Encrypt HTTPS
```

### Docker Compose Configuration

**Location:** `/home/ubuntu/bahmni-docker/`

**Key Files:**
- `.env.prerelease` - Environment variables (copy from scripts/aws/bahmni/)
- `docker-compose.override.yml` - Local MySQL 5.6 configuration (copy from scripts/aws/bahmni/)
- `docker-compose.yml` - Main Bahmni services (from bahmni-docker repo)

### Configuration Details

```bash
# .env.prerelease - Key Settings
COMPOSE_PROFILES=emr
OPENMRS_IMAGE_TAG=1.0.0-644
OPENMRS_DB_IMAGE_NAME=mysql:5.6  # Local container (not RDS)
BAHMNI_WEB_IMAGE_TAG=1.1.0-696
CONFIG_VOLUME=/home/ubuntu/bahmni-docker/jss-config
OPENMRS_DB_HOST=openmrsdb
OPENMRS_DB_USERNAME=openmrs_admin
OPENMRS_DB_PASSWORD=[FROM_SECURE_STORE]
MYSQL_ROOT_PASSWORD=[FROM_SECURE_STORE]
OMRS_DB_EXTRA_ARGS=&zeroDateTimeBehavior=convertToNull
```

**See `docs/credential_jss_bahmni_prerelease_integration.md` for actual credential values.**

### Startup Procedure

```bash
# Step 1: SSH to instance
make ssh-prerelease

# Step 2: Clone repositories (on EC2)
cd /home/ubuntu
git clone https://github.com/JanSwasthyaSahyog/bahmni-docker.git
git clone https://github.com/JanSwasthyaSahyog/jss-config.git
cd bahmni-docker

# Step 3: Copy config files from this repo
scp -i ~/.ssh/openchs-infra.pem scripts/aws/bahmni/.env.prerelease ubuntu@jss-bahmni-prerelease.avniproject.org:/home/ubuntu/bahmni-docker/.env
scp -i ~/.ssh/openchs-infra.pem scripts/aws/bahmni/docker-compose.override.yml ubuntu@jss-bahmni-prerelease.avniproject.org:/home/ubuntu/bahmni-docker/

# Step 4: Configure swap (8GB recommended)
sudo fallocate -l 8G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile

# Step 5: Create volume and restore database
docker volume create openmrs_db_data
docker compose -f docker-compose.yml -f docker-compose.override.yml up -d openmrsdb
sleep 30
gunzip -c /path/to/backup.sql.gz | docker exec -i bahmni-docker-openmrsdb-1 mysql -u root -p[PASSWORD] openmrs

# Step 6: Start all services
docker compose -f docker-compose.yml -f docker-compose.override.yml --profile emr up -d

# Step 7: Wait for OpenMRS (10-15 minutes)
make bahmni-wait
# Or manually: docker logs bahmni-docker-openmrs-1 -f | grep "Server startup"

# Step 8: Create AMI snapshot
make create-ami
```

**Note:** Use credentials from `docs/credential_jss_bahmni_prerelease_integration.md` for actual passwords.

### Important Notes

**HOUR_OF_DAY Errors (Non-Fatal):**
- OpenMRS logs show HOUR_OF_DAY errors during Hibernate Search indexing
- These are **background indexing errors** (production has them too)
- OpenMRS becomes fully functional after search index update (~5-10 minutes)
- **Do NOT disable search indexing** - it's required in production
- **Do NOT change timezone or JDBC settings** - they're correct as-is

**Memory Requirements:**
- Minimum: 8 GiB RAM + 8 GiB swap
- Recommended: 16 GiB RAM
- t3.large (7.6 GiB) works but startup takes 10-15 minutes
- t3.xlarge (16 GiB) recommended for faster startup

**Startup Timeline:**
- MySQL ready: ~30 seconds
- OpenMRS Tomcat startup: ~2-3 minutes
- Database initialization: ~2-5 minutes
- Hibernate Search indexing: ~5-10 minutes
- **Total: 10-15 minutes**

### Verification Commands

```bash
# Check container status
make bahmni-status

# View OpenMRS logs
make bahmni-logs-openmrs

# Test OpenMRS API
curl -u admin:test http://localhost:8080/openmrs/ws/rest/v1/session

# Verify database
docker exec bahmni-docker-openmrsdb-1 mysql -u root -pOpenMRS_JSS2024 openmrs -e "SELECT COUNT(*) as tables FROM information_schema.tables WHERE table_schema='openmrs';"

# Access Bahmni UI
https://jss-bahmni-prerelease.avniproject.org/bahmni/home/index.html#/dashboard
Login: admin / test
```

### SSL Certificate Setup

```bash
# Install certbot
sudo apt update && sudo apt install -y certbot

# Generate certificate
sudo certbot certonly --standalone -d jss-bahmni-prerelease.avniproject.org

# Copy certs to Bahmni location
sudo mkdir -p /bahmni/certs
sudo cp /etc/letsencrypt/live/jss-bahmni-prerelease.avniproject.org/fullchain.pem /bahmni/certs/cert.pem
sudo cp /etc/letsencrypt/live/jss-bahmni-prerelease.avniproject.org/privkey.pem /bahmni/certs/key.pem

# Restart proxy to load certs
docker compose restart proxy
```

---

## 8. Integration Service Configuration

### Clone and Setup

```bash
# Clone integration service
git clone https://github.com/avniproject/integration-service.git
cd integration-service

# Checkout JSS branch
git checkout jss_ganiyari

# Build
make build-server
```

### Database Setup

```bash
# Create integration database
make build-db

# Or manually:
psql -h localhost -U postgres -c "CREATE USER bahmni_avni_admin WITH PASSWORD 'password' CREATEROLE;"
psql -h localhost -U postgres -c "CREATE DATABASE bahmni_avni WITH OWNER bahmni_avni_admin;"

# Run migrations
./gradlew migrateDb
```

### Configuration File

Create `/etc/avni-integration/jss.conf`:

```bash
# Integration Service Database
export AVNI_INT_DATABASE=bahmni_avni
export AVNI_INT_DATABASE_PORT=5432
export AVNI_INT_DB_USER=bahmni_avni_admin
export AVNI_INT_DB_PASSWORD=<password>

# Avni API
export BAHMNI_AVNI_API_URL=https://prerelease.avniproject.org
export BAHMNI_AVNI_API_USER=<jss-integration-user>@jsscp
export BAHMNI_AVNI_API_PASSWORD=<password>
export BAHMNI_AVNI_IDP_TYPE=Cognito

# OpenMRS API
export OPENMRS_BASE_URL=http://<bahmni-host>:8080
export OPENMRS_USER=avni_int_user
export OPENMRS_PASSWORD=<password>

# Scheduling
export BAHMNI_SCHEDULE_CRON="0 */5 * * * ?"  # Every 5 minutes
export BAHMNI_SCHEDULE_CRON_FULL_ERROR="0 0 2 * * ?"  # 2 AM daily

# Server
export AVNI_INT_SERVER_PORT=6013
```

### Run Integration Service

```bash
# Source config
source /etc/avni-integration/jss.conf

# Run (development)
java -jar integrator/build/libs/integrator-0.0.1-SNAPSHOT.jar

# Run as systemd service (production)
# See systemd service file below
```

### Systemd Service File

Create `/etc/systemd/system/avni-bahmni-integration.service`:

```ini
[Unit]
Description=Avni Bahmni Integration Service
After=network.target postgresql.service

[Service]
Type=simple
User=avni
EnvironmentFile=/etc/avni-integration/jss.conf
ExecStart=/usr/bin/java -jar /opt/avni-integration/integrator-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

---

## 9. Mapping Configuration

### Database Constants Setup

These must be set directly in the integration database:

```sql
-- Connect to integration DB
psql -h localhost -U bahmni_avni_admin -d bahmni_avni

-- Insert constants
INSERT INTO constant (name, value) VALUES 
    ('BahmniIdentifierPrefix', 'GAN'),
    ('IntegrationBahmniIdentifierType', '<uuid-of-gan-identifier-type>'),
    ('IntegrationBahmniProvider', 'Avni Integration'),
    ('IntegrationBahmniEncounterRole', 'Unknown'),
    ('IntegrationBahmniLocation', '<uuid-of-avni-location>'),
    ('IntegrationBahmniVisitType', '<uuid-of-opd-visit-type>'),
    ('IntegrationAvniSubjectType', 'Individual');

-- For lab results (if needed)
INSERT INTO constant (name, value) VALUES 
    ('OutpatientVisitTypes', '<uuid-of-opd-visit-type>');
```

### Core Mappings Required

#### 1. Subject Type to Patient Mapping

```
Mapping Group: PatientSubject
Mapping Type: Subject_EncounterType
Avni Value: Individual
Bahmni Value: <UUID of encounter type for Avni data>
```

#### 2. Concept Mappings

For each concept that needs to sync:

```
Mapping Group: Observation
Mapping Type: Concept
Avni Value: <Avni Concept Name>
Bahmni Value: <OpenMRS Concept UUID>
Is Coded: true/false (for coded concepts)
```

#### 3. Program Mappings

For each program:

```
Mapping Group: ProgramEnrolment
Mapping Type: <Program Name>_EncounterType
Avni Value: <Avni Program Name>
Bahmni Value: <OpenMRS Encounter Type UUID>
```

### Admin App Access

After starting integration service:

```
URL: http://localhost:6013/
Login: Create user via database (see docs)
```

---

## 10. Dev Environment Respawn Procedures

### Quick Respawn Script

Create `respawn-dev-env.sh`:

```bash
#!/bin/bash
set -e

echo "=== JSS Avni-Bahmni Dev Environment Respawn ==="

# Configuration
RDS_INSTANCE_ID="jss-bahmni-openmrs-dev"
SNAPSHOT_ID="jss-bahmni-baseline-YYYYMMDD"  # Update with actual snapshot
AWS_REGION="ap-south-1"
INTEGRATION_DB="bahmni_avni"

# Step 1: Restore RDS from Snapshot
echo "Step 1: Restoring RDS from snapshot..."
aws rds restore-db-instance-from-db-snapshot \
    --db-instance-identifier "${RDS_INSTANCE_ID}-new" \
    --db-snapshot-identifier $SNAPSHOT_ID \
    --region $AWS_REGION

echo "Waiting for RDS to be available..."
aws rds wait db-instance-available \
    --db-instance-identifier "${RDS_INSTANCE_ID}-new" \
    --region $AWS_REGION

# Step 2: Restart Bahmni Docker
echo "Step 2: Restarting Bahmni Docker services..."
cd /path/to/bahmni-docker
docker-compose down
# Update .env with new RDS endpoint if needed
docker-compose --profile emr up -d

echo "Waiting for Bahmni to start..."
sleep 60

# Step 3: Clear Integration DB
echo "Step 3: Clearing Integration DB..."
psql -h localhost -U bahmni_avni_admin -d $INTEGRATION_DB << EOF
TRUNCATE TABLE error_record CASCADE;
TRUNCATE TABLE avni_entity_status CASCADE;
TRUNCATE TABLE bahmni_entity_status CASCADE;
-- Keep mappings and constants
EOF

# Step 4: Restart Integration Service
echo "Step 4: Restarting Integration Service..."
sudo systemctl restart avni-bahmni-integration

echo "=== Respawn Complete ==="
echo "Bahmni URL: http://<ec2-ip>/bahmni/home"
echo "Integration Service: http://<ec2-ip>:6013/"
```

### Manual Steps

#### 1. Restore RDS Snapshot

```bash
# Delete existing instance (if needed)
aws rds delete-db-instance \
    --db-instance-identifier $DB_INSTANCE_ID \
    --skip-final-snapshot \
    --region $AWS_REGION

# Restore from snapshot
aws rds restore-db-instance-from-db-snapshot \
    --db-instance-identifier $DB_INSTANCE_ID \
    --db-snapshot-identifier $SNAPSHOT_ID \
    --db-instance-class db.t3.medium \
    --region $AWS_REGION
```

#### 2. Restart Bahmni Services

```bash
cd /path/to/bahmni-docker

# Stop all
docker-compose down

# Start fresh
docker-compose --profile emr up -d

# Verify
docker-compose ps
docker-compose logs -f openmrs
```

#### 3. Clear Integration DB

```bash
psql -h localhost -U bahmni_avni_admin -d bahmni_avni

-- Clear sync status (keeps mappings)
TRUNCATE TABLE error_record CASCADE;
TRUNCATE TABLE avni_entity_status CASCADE;
TRUNCATE TABLE bahmni_entity_status CASCADE;

-- Reset bookmarks
UPDATE integration_system SET 
    patient_feed_offset = NULL,
    encounter_feed_offset = NULL,
    lab_feed_offset = NULL;
```

#### 4. Restart Integration Service

```bash
sudo systemctl restart avni-bahmni-integration
sudo journalctl -u avni-bahmni-integration -f
```

---

## 11. Testing & Verification

### Verification Checklist

#### Infrastructure
- [ ] RDS instance accessible from EC2
- [ ] Bahmni Docker containers running
- [ ] OpenMRS accessible at `/openmrs`
- [ ] Bahmni UI accessible at `/bahmni/home`
- [ ] Integration service running on port 6013
- [ ] Integration DB migrations applied

#### Connectivity
- [ ] Integration service can reach Avni API
- [ ] Integration service can reach OpenMRS API
- [ ] Atom feeds accessible (`/openmrs/ws/atomfeed/patient/recent`)

#### Mappings
- [ ] Constants configured in DB
- [ ] Subject type mapping created
- [ ] At least one concept mapping created
- [ ] At least one program mapping created

#### Data Sync Tests

**Avni → Bahmni:**
1. Create new Individual in Avni with GAN ID
2. Trigger sync (or wait for cron)
3. Verify patient created in Bahmni with matching GAN ID
4. Verify observations synced

**Bahmni → Avni:**
1. Create patient in Bahmni
2. Add lab results / prescription
3. Trigger sync
4. Verify encounter created in Avni

### Useful Commands

```bash
# Check integration service logs
tail -f /var/log/abi/integration-service.log

# Check error records
psql -h localhost -U bahmni_avni_admin -d bahmni_avni \
    -c "SELECT * FROM error_record ORDER BY created_at DESC LIMIT 10;"

# Check sync status
psql -h localhost -U bahmni_avni_admin -d bahmni_avni \
    -c "SELECT * FROM avni_entity_status ORDER BY last_modified_date DESC LIMIT 10;"

# Trigger manual sync (via API)
curl -X POST http://localhost:6013/int/bahmni/trigger-sync \
    -H "Content-Type: application/json"
```

---

## 12. Deliverables Checklist

### P1 Deliverables

- [ ] Bahmni system assessment document
- [ ] EC2 instance with Bahmni Docker running
- [ ] RDS instance with restored OpenMRS data
- [ ] RDS baseline snapshot created
- [ ] Integration service deployed and running
- [ ] Integration DB configured
- [ ] All required concept/encounter mappings created
- [ ] Clinical data sync verified (lab reports, X-rays, visits, discharge summaries)
- [ ] Community data sync to Bahmni verified
- [ ] Internal review completed
- [ ] External review with JSS stakeholders completed

---

## Appendix A: Key URLs

| Resource | URL |
|----------|-----|
| Avni (Prerelease) | https://prerelease.avniproject.org |
| Avni Integration Docs | https://avni.readme.io/docs/avni-bahmni-integration-specific |
| Bahmni Docs | https://bahmni.atlassian.net/wiki/spaces/BAH/overview |
| JSS Website | https://www.jssbilaspur.org/ |
| Integration Service Repo | https://github.com/avniproject/integration-service |
| JSS Bahmni Docker | https://github.com/JanSwasthyaSahyog/bahmni-docker |
| JSS Config | https://github.com/JanSwasthyaSahyog/jss-config |

## Appendix B: Contact & Support

| Role | Contact |
|------|---------|
| Avni Support | support@avniproject.org |
| JSS IT Admin | Ram Narayan |

---

*Document Version: 1.0*  
*Last Updated: December 2024*  
*Authors: Himesh, Nupoor*
