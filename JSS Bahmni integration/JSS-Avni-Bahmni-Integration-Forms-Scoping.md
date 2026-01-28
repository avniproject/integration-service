# JSS Avni-Bahmni Integration Scoping - Individual/Patient Level Entities

## Methodology for Entity Extraction

### Data Sources
- **Avni Metadata**: `/scripts/aws/avni-metadata/` directory
- **Bahmni Metadata**: `/scripts/aws/bahmni-metadata/` directory

### Filtering Criteria Applied
1. **Individual/Patient Level Only**: Excluded group, household, village, SHG, and other non-individual subject types
2. **Active Entities Only**: Excluded voided (`"voided": true`) and retired (`"retired": 1`) entities
3. **Clinical Data Focus**: Excluded configuration-only metadata (dashboards, reports, roles, etc.)

### Entity Classification Rules
- **Registration**: Individual/Patient registration and program enrolment forms
- **Visit**: Routine visits, follow-ups, clinic visits (general encounters)
- **Program Encounter**: Encounters recorded as part of specific programs
- **Encounter**: Other clinical encounters not covered above

### Extraction Process
1. **Avni Entities**:
   - Filtered `subjectTypes.json` for `"type": "Person"` only
   - Filtered forms by `"formType"`: `IndividualProfile`, `ProgramEnrolment`, `ProgramEncounter`, `Encounter`
   - Filtered `encounterTypes.json` for non-voided entities
   - Cross-referenced with `formMappings.json` for subject type assignments

2. **Bahmni Entities**:
   - Extracted all encounter types from `encounter_types/encounter_types.json`
   - Extracted all visit types from `encounter_types/visit_types.json`
   - Extracted forms from `forms/forms.json` (non-retired only)
   - Extracted patient identifiers and attributes from `patient_identifiers/` directory

### Avni Data Extraction Queries

#### Forms and Form Elements Query
```sql
set role jsscp;

select
    f.name  as FormName,
    f.uuid as FormUUID,
    feg.name as FormElementGroup,
    feg.uuid as FormElementGroupUUID,
    c.name as Concept,
    c.data_type as Datatype,
    c.uuid as ConceptUUID
from form f
         inner join form_element_group feg on feg.form_id = f.id
         inner join form_element fe on fe.form_element_group_id = feg.id
         inner join concept c on fe.concept_id = c.id
         inner join organisation co on co.id = c.organisation_id
         inner join organisation feo on feo.id = fe.organisation_id
         left join concept_answer ca on ca.concept_id = c.id
         left join concept a on ca.answer_concept_id = a.id
         left join non_applicable_form_element rem
                   on rem.form_element_id = fe.id
                       and rem.is_voided <> true
where rem.id is null
  and f.name in
      (
                 'JSSCP Registration Form',
                 'Child Enrolment',
                 'Sickle cell Enrolment',
                 'Heart Disease Enrolment',
                 'Diabetes Enrolment',
                 'Thyroidism Enrolment',
                 'Palliative Care Enrolment',
                 'Mental Illness Enrolment',
                 'Asthma Enrolment',
                 'Arthritis Enrolment',
                 'Stroke Enrolment',
                 'TB Study Enrollment',
                 'Hypertension Enrolment',
                 'Pregnancy Enrolment',
                 'Tuberculosis Enrolment',
                 'Epilepsy Enrolment',
                 'Women''s Health Camp Enrolment',
                 'Cancer Enrolment',
                 'COPD Enrolment',
                 'Child Enrolment Phulwari',
                 'TB - INH Prophylaxis Enrolment',
                 'Eligible couple Enrolment',
                 'Treatment review',
                 'Birth',
                 'Mother PNC',
                 'Abortion',
                 'Abortion followup',
                 'ANC Clinic Visit',
                 'USG Report',
                 'Lab Investigations',
                 'ANC Home Visit',
                 'Referral Status',
                 'Child PNC',
                 'Child Birth',
                 'Delivery',
                 'Lab Investigations Results for pregnancy induced diabetes',
                 'Hypertension Followup',
                 'Lab test',
                 'Hypertension referral status',
                 'Diabetes lab test',
                 'Anthropometry Assessment',
                 'Birth form',
                 'LJ Result Form',
                 'USG Report',
                 'Referral Status',
                 'LPA Result Form',
                 'TB Followup',
                 'TB Study Baseline Family Encounter',
                 'Lab test form',
                 'TB Study Baseline History Encounter',
                 'Abortion',
                 'Ganiyari Lab Result Form',
                 'Clinic Lab Result',
                 'CBNAAT Result Form',
                 'Albendazole Tracking',
                 'Epilepsy referral status form',
                 'TB referral status',
                 'Hypertension Followup',
                 'Abortion Followup',
                 'TB Lab results',
                 'Diabetes Followup',
                 'INH Prophylaxis follow up',
                 'Sickle cell lab test',
                 'TB Study Food Supplement Encounter',
                 'Eligible Couple Follow-up Encounter',
                 'Child Birth Form',
                 'TB Study Baseline Barrier and Social support assessment Encounter',
                 'TB Study Baseline Exam Encounter',
                 'TB Study Baseline Labs Encounter',
                 'TB Study Baseline Treatment Encounter',
                 'TB Study Followup',
                 'TB Study Follow Up Family',
                 'TB Study Health Seeking',
                 'TB Study Risk Factors Assessment',
                 'TB Family Screening Form',
                 'Child Absent followup Form',
                 'Monthly monitoring',
                 'HBNC Encounter',
                 'TB Mantoux test result',
                 'Eye screening',
                 'Verbal Autopsy Adult',
                 'Verbal Autopsy Newborn',
                 'PPMC Encounter',
                 'Verbal Autopsy Child',
                 'Treatment Form',
                 'Verbal Autopsy Maternal',
                 'Death',
                 'Women''s Health Camp'
      )
and f.is_voided = false
order by
    f.name,
    feg.display_order asc,
    fe.display_order asc;
```

**Purpose**: Extracts all form structures, element groups, individual form elements, concepts, and their UUIDs from Avni database for comprehensive form mapping.

**Key Features**:
- Extracts all concepts regardless of datatype (Numeric, Date, Text, Coded)
- Includes comprehensive list of 90+ clinical forms
- Excludes voided/non-applicable elements
- Preserves display order for proper hierarchy
- Joins form → form_element_group → form_element → concept relationships
- Provides complete UUID mapping for all form concepts

#### Concept Answers Query (Coded Concepts Only)
```sql
set role jsscp;

select
    c.name as concept,
    c.uuid as conceptuuid,
    a.name as conceptanswer,
    a.uuid as conceptansweruuid
from concept_answer ca
         inner join concept c on ca.concept_id = c.id
         inner join concept a on ca.answer_concept_id = a.id
         inner join organisation co on co.id = c.organisation_id
where c.data_type = 'Coded'
  and c.name in (
      -- List of concepts from the forms query above
      select distinct c.name
      from form f
               inner join form_element_group feg on feg.form_id = f.id
               inner join form_element fe on fe.form_element_group_id = feg.id
               inner join concept c on fe.concept_id = c.id
               inner join organisation co on co.id = c.organisation_id
               left join non_applicable_form_element rem
                         on rem.form_element_id = fe.id
                             and rem.is_voided <> true
      where rem.id is null
        and f.name in (/* same form list as above */)
        and f.is_voided = false
  )
order by c.name, ca.answer_order;
```

**Purpose**: Extracts concept answers and their UUIDs for all coded concepts from the specified forms.

**Key Features**:
- Filters for coded concepts only (c.data_type = 'Coded')
- Provides answer concepts and their UUIDs
- Maintains answer order for proper display
- References the same form list as the main query

---

## Complete Process for Generating Avni Forms for Bahmni Upload

### Step 1: Extract Forms Data Using Queries

#### 1.1 Main Forms and Concepts Query
Use the **"Forms and Form Elements Query"** above to extract:
- All form names and UUIDs
- Form element groups (sections) and their UUIDs  
- Individual form elements and their concept UUIDs
- All datatypes (Numeric, Date, Text, Coded)

#### 1.2 Concept Answers Query  
Use the **"Concept Answers Query"** above to extract:
- Answer concepts and UUIDs for all coded questions
- Maintains proper answer ordering

### Step 2: Generate Bahmni CSV Files

#### 2.1 Create concepts.csv
Combine data from both queries to create:
```csv
uuid,name,description,class,shortname,datatype,units,High Normal,Low Normal,synonym.1,answer.1,answer.2,answer.3,answer.4,reference-term-source,reference-term-code,reference-term-relationship
```

**Structure:**
- **Form-level concepts** (class=ConvSet, datatype=N/A)
- **Section-level concepts** (class=ConvSet, datatype=N/A)  
- **Question-level concepts** (class=Misc, datatype=actual_type)
- **Answer concepts** (class=Misc, datatype=N/A)

**Critical Rules:**
- **No duplicate UUIDs** - each UUID must be unique
- **No duplicate concept names** - each concept name must be unique across entire file
- **Empty datatypes** must be filled with "N/A"
- **Form/Section concepts** must have class="ConvSet" and datatype="N/A"
- **Question concepts** must have class="Misc" and actual datatype
- **Answer concepts** must have class="Misc" and datatype="N/A"

#### 2.2 Create concept_sets.csv
Create hierarchy relationships:
```csv
uuid,name,description,class,shortname,child.1,child.2,child.3,child.4,child.5,child.6,child.7,child.8,child.9,child.10,reference-term-source,reference-term-code,reference-term-relationship
```

**Structure:**
- **Row 1**: Form → Sections relationship (all sections as children)
- **Rows 2-N**: Each Section → Questions relationship (unique questions as children)
- **No duplicate concepts** - each concept appears only once in entire hierarchy

**Critical Rules:**
- **Each concept appears only once** across all child columns
- **Proper parent-child relationships** maintained
- **Maximum 10 children per row** (child.1 through child.10)
- **Empty child columns** left blank
- **Generate relationship UUIDs** for each row

### Step 3: Validation Checks

#### 3.1 Data Validation
- ✅ **No duplicate UUIDs** in concepts.csv
- ✅ **No duplicate concept names** in concepts.csv  
- ✅ **All concepts have UUIDs**
- ✅ **No empty datatypes** (use "N/A" where needed)
- ✅ **Proper class assignments** (ConvSet for forms/sections, Misc for questions/answers)
- ✅ **All datatypes mapped correctly** (Numeric, Date, Text, Coded, N/A)

#### 3.2 File Validation  
- ✅ **CSV format matches Bahmni requirements**
- ✅ **All required columns present**
- ✅ **No empty critical fields** (uuid, name, class, datatype)
- ✅ **No duplicate concepts in concept_sets.csv**
- ✅ **Proper hierarchy maintained**

#### 3.3 Hierarchy Validation
- ✅ **Form appears only once** as parent in concept_sets.csv
- ✅ **Each section appears only once** as child and once as parent
- ✅ **Each question appears only once** as child
- ✅ **Total unique concepts match** between concepts.csv and concept_sets.csv

### Step 4: Upload and Test

#### 4.1 Upload to Bahmni
1. **Login to Bahmni admin interface**
2. **Navigate to Concepts module**
3. **Upload concepts.csv first**
4. **Upload concept_sets.csv second**
5. **Verify successful import**

#### 4.2 Form Validation
1. **Check form appears in Bahmni form list**
2. **Verify form structure matches Avni hierarchy**
3. **Test form functionality with sample data**
4. **Confirm all sections and questions display correctly**

### Step 5: Ready for Production

The generated **concepts.csv** and **concept_sets.csv** files are now ready for production import into Bahmni system with complete form structures, UUIDs, and hierarchical relationships preserved.

---

## Example: ANC Clinic Visit Form Migration

### Input Data
- **Form**: ANC Clinic Visit (268 concepts, 12 sections)
- **Source**: Avni forms dump.csv + Avni Concept Mapping.csv

### Output Files
- **ANC_concepts.csv**: 105 rows (no duplicates, proper datatypes)
- **ANC_concept_sets.csv**: 13 rows (1 form + 12 sections, no duplicate concepts)

### Key Fixes Applied
- **Removed duplicate UUIDs** and concept names
- **Fixed empty datatypes** to "N/A"
- **Eliminated duplicate concept assignments** in hierarchy
- **Ensured each concept appears only once** in entire structure

### Result
- **Clean, unique data** ready for Bahmni import
- **Proper hierarchical relationships** maintained
- **All validation checks passed**

---

## Avni – Individual-Level Entities

| Avni Entity Name | Subject | File / Folder Source | Entity Type | Notes |
|---|---|---|---|---|
| **Individual Registration** | Individual | forms/Individual Registration.json | Registration | Patient registration form |
| **JSSCP Registration Form** | Individual | forms/JSSCP Registration Form.json | Registration | Clinical registration form |
| **TB Study Registration** | Individual | forms/TB Study Registration.json | Registration | Study registration form |
| **Phulwari Registration** | Individual | forms/Phulwari Registration.json | Registration | Phulwari registration form |
| **Monthly Monitoring Registration** | Individual | forms/Monthly Monitoring Registration.json | Registration | Monitoring registration form |
| **SHG Registration** | Individual | forms/SHG Registration.json | Registration | SHG registration form |
| **SHG member Registration** | Individual | forms/SHG member Registration.json | Registration | SHG member registration form |
| **Child Enrolment** | Individual | forms/Child Enrolment.json | Registration | Child program enrolment |
| **Heart Disease Enrolment** | Individual | forms/Heart Disease Enrolment.json | Registration | Heart disease program enrolment |
| **Sickle cell Enrolment** | Individual | forms/Sickle cell Enrolment.json | Registration | Sickle cell program enrolment |
| **Diabetes Enrolment** | Individual | forms/Diabetes Enrolment.json | Registration | Diabetes program enrolment |
| **Thyroidism Enrolment** | Individual | forms/Thyroidism Enrolment.json | Registration | Thyroidism program enrolment |
| **Palliative Care Enrolment** | Individual | forms/Palliative Care Enrolment.json | Registration | Palliative care program enrolment |
| **Mental Illness Enrolment** | Individual | forms/Mental Illness Enrolment.json | Registration | Mental illness program enrolment |
| **Asthma Enrolment** | Individual | forms/Asthma Enrolment.json | Registration | Asthma program enrolment |
| **Arthritis Enrolment** | Individual | forms/Arthritis Enrolment.json | Registration | Arthritis program enrolment |
| **Stroke Enrolment** | Individual | forms/Stroke Enrolment.json | Registration | Stroke program enrolment |
| **TB Study Enrollment** | Individual | forms/TB Study Enrollment.json | Registration | TB study enrolment |
| **Hypertension Enrolment** | Individual | forms/Hypertension Enrolment.json | Registration | Hypertension program enrolment |
| **Pregnancy Enrolment** | Individual | forms/Pregnancy Enrolment.json | Registration | Pregnancy program enrolment |
| **Tuberculosis Enrolment** | Individual | forms/Tuberculosis Enrolment.json | Registration | Tuberculosis program enrolment |
| **Epilepsy Enrolment** | Individual | forms/Epilepsy Enrolment.json | Registration | Epilepsy program enrolment |
| **Women's Health Camp Enrolment** | Individual | forms/Women's Health Camp Enrolment.json | Registration | Women's health camp enrolment |
| **Cancer Enrolment** | Individual | forms/Cancer Enrolment.json | Registration | Cancer program enrolment |
| **COPD Enrolment** | Individual | forms/COPD Enrolment.json | Registration | COPD program enrolment |
| **Child Enrolment Phulwari** | Individual | forms/Child Enrolment Phulwari.json | Registration | Child enrolment in Phulwari |
| **TB - INH Prophylaxis Enrolment** | Individual | forms/TB - INH Prophylaxis Enrolment.json | Registration | TB INH prophylaxis enrolment |
| **Eligible couple Enrolment** | Individual | forms/Eligible couple Enrolment.json | Registration | Eligible couple enrolment |
| **Treatment review** | Individual | encounterTypes.json | Visit | Treatment review encounter |
| **Birth** | Individual | encounterTypes.json | Visit | Birth encounter |
| **Mother PNC** | Individual | encounterTypes.json | Visit | Mother post-natal care |
| **Abortion** | Individual | encounterTypes.json | Visit | Abortion encounter |
| **Abortion followup** | Individual | encounterTypes.json | Visit | Abortion follow-up |
| **ANC Clinic Visit** | Individual | encounterTypes.json | Visit | Antenatal clinic visit |
| **USG Report** | Individual | encounterTypes.json | Visit | Ultrasound report |
| **Lab Investigations** | Individual | encounterTypes.json | Visit | Lab investigations |
| **ANC Home Visit** | Individual | encounterTypes.json | Visit | Antenatal home visit |
| **Referral Status** | Individual | encounterTypes.json | Visit | Referral status tracking |
| **Child PNC** | Individual | encounterTypes.json | Visit | Child post-natal care |
| **Child Birth** | Individual | encounterTypes.json | Visit | Child birth encounter |
| **Delivery** | Individual | encounterTypes.json | Visit | Delivery encounter |
| **Lab Investigations Results for pregnancy induced diabetes** | Individual | encounterTypes.json | Visit | Lab results for pregnancy diabetes |
| **Hypertension Followup** | Individual | encounterTypes.json | Visit | Hypertension follow-up |
| **Lab test** | Individual | encounterTypes.json | Visit | Lab test encounter |
| **Hypertension referral status** | Individual | encounterTypes.json | Visit | Hypertension referral status |
| **Diabetes lab test** | Individual | forms/Diabetes lab test.json | Program Encounter | Diabetes lab test |
| **Anthropometry Assessment** | Individual | forms/Anthropometry Assessment.json | Program Encounter | Anthropometry assessment |
| **Birth form** | Individual | forms/Birth form.json | Program Encounter | Birth form |
| **LJ Result Form** | Individual | forms/LJ Result Form.json | Program Encounter | LJ lab result |
| **USG Report** | Individual | forms/USG Report.json | Program Encounter | Ultrasound report |
| **Referral Status** | Individual | forms/Referral Status.json | Program Encounter | Referral status |
| **LPA Result Form** | Individual | forms/LPA Result Form.json | Program Encounter | LPA lab result |
| **TB Followup** | Individual | forms/TB Followup.json | Program Encounter | TB follow-up |
| **TB Study Baseline Family Encounter** | Individual | forms/TB Study Baseline Family Encounter.json | Program Encounter | TB study baseline family |
| **Lab test form** | Individual | forms/Lab test form.json | Program Encounter | Lab test form |
| **TB Study Baseline History Encounter** | Individual | forms/TB Study Baseline History Encounter.json | Program Encounter | TB study baseline history |
| **Abortion** | Individual | forms/Abortion.json | Program Encounter | Abortion encounter |
| **Ganiyari Lab Result Form** | Individual | forms/Ganiyari Lab Result Form.json | Program Encounter | Ganiyari lab result |
| **Clinic Lab Result** | Individual | forms/Clinic Lab Result.json | Program Encounter | Clinic lab result |
| **CBNAAT Result Form** | Individual | forms/CBNAAT Result Form.json | Program Encounter | CBNAAT lab result |
| **Albendazole Tracking** | Individual | forms/Albendazole Tracking.json | Program Encounter | Albendazole tracking |
| **Epilepsy referral status form** | Individual | forms/Epilepsy referral status form.json | Program Encounter | Epilepsy referral status |
| **TB referral status** | Individual | forms/TB referral status.json | Program Encounter | TB referral status |
| **Hypertension Followup** | Individual | forms/Hypertension Followup.json | Program Encounter | Hypertension follow-up |
| **Abortion Followup** | Individual | forms/Abortion Followup.json | Program Encounter | Abortion follow-up |
| **TB Lab results** | Individual | forms/TB Lab results.json | Program Encounter | TB lab results |
| **Diabetes Followup** | Individual | forms/Diabetes Followup.json | Program Encounter | Diabetes follow-up |
| **INH Prophylaxis follow up** | Individual | forms/INH Prophylaxis follow up.json | Program Encounter | INH prophylaxis follow-up |
| **Sickle cell lab test** | Individual | forms/Sickle cell lab test.json | Program Encounter | Sickle cell lab test |
| **TB Study Food Supplement Encounter** | Individual | forms/TB Study Food Supplement Encounter.json | Program Encounter | TB study food supplement |
| **Eligible Couple Follow-up Encounter** | Individual | forms/Eligible Couple Follow-up Encounter.json | Program Encounter | Eligible couple follow-up |
| **Child Birth Form** | Individual | forms/Child Birth Form.json | Program Encounter | Child birth form |
| **TB Study Baseline Barrier and Social support assessment Encounter** | Individual | forms/TB Study Baseline Barrier and Social support assessment Encounter.json | Program Encounter | TB study barrier assessment |
| **TB Study Baseline Exam Encounter** | Individual | forms/TB Study Baseline Exam Encounter.json | Program Encounter | TB study baseline exam |
| **TB Study Baseline Labs Encounter** | Individual | forms/TB Study Baseline Labs Encounter.json | Program Encounter | TB study baseline labs |
| **TB Study Baseline Treatment Encounter** | Individual | forms/TB Study Baseline Treatment Encounter.json | Program Encounter | TB study baseline treatment |
| **TB Study Followup** | Individual | forms/TB Study Followup.json | Program Encounter | TB study follow-up |
| **TB Study Follow Up Family** | Individual | forms/TB Study Follow Up Family.json | Program Encounter | TB study follow-up family |
| **TB Study Health Seeking** | Individual | forms/TB Study Health Seeking.json | Program Encounter | TB study health seeking |
| **TB Study Risk Factors Assessment** | Individual | forms/TB Study Risk Factors Assessment.json | Program Encounter | TB study risk factors |
| **TB Family Screening Form** | Individual | forms/TB Family Screening Form.json | Encounter | TB family screening |
| **Child Absent followup Form** | Individual | forms/Child Absent followup Form.json | Encounter | Child absent follow-up |
| **Monthly monitoring** | Individual | encounterTypes.json | Encounter | Monthly monitoring |
| **HBNC Encounter** | Individual | forms/HBNC Encounter.json | Encounter | Home-based newborn care |
| **TB Mantoux test result** | Individual | forms/TB Mantoux test result form.json | Encounter | TB Mantoux test result |
| **MCH Worker Followup** | Individual | forms/MCH Worker Followup Encounter.json | Encounter | MCH worker follow-up |
| **Subcenter Followup** | Individual | forms/Subcenter Followup.json | Encounter | Subcenter follow-up |
| **Monthly Animal Health Worker Reporting** | Individual | forms/Monthly Animal Health Worker Reporting form.json | Encounter | Animal health reporting |
| **Eye screening** | Individual | forms/Eye screening form Encounter.json | Encounter | Eye screening |
| **VHW Followup** | Individual | forms/VHW Followup.json | Encounter | VHW follow-up |
| **Verbal Autopsy Adult** | Individual | forms/Verbal Autopsy Adult Encounter.json | Encounter | Verbal autopsy adult |
| **Verbal Autopsy Newborn** | Individual | forms/Verbal Autopsy (Newborn - up to 28 days).json | Encounter | Verbal autopsy newborn |
| **Referral Communication from VHW** | Individual | forms/Referral Communication from VHW.json | Encounter | Referral from VHW |
| **Senior Health Worker Weekly Reporting** | Individual | forms/Senior Health Worker Weekly Reporting Encounter.json | Encounter | Weekly reporting |
| **Referral Communication** | Individual | forms/Referral Communication Encounter.json | Encounter | Referral communication |
| **PPMC Encounter** | Individual | forms/PPMC Encounter.json | Encounter | Post-partum maternal care |
| **Observation Checklist** | Individual | forms/Observation Checklist Encounter.json | Encounter | Observation checklist |
| **Verbal Autopsy Child** | Individual | forms/Verbal Autopsy (Child - 29 days to 14 years).json | Encounter | Verbal autopsy child |
| **SHG monthly data collection** | Individual | forms/SHG monthly data collection.json | Encounter | SHG data collection |
| **Treatment Form** | Individual | forms/Treatment Form.json | Encounter | Treatment form |
| **Monthly Village Health Worker report** | Individual | forms/Monthly Village Health Worker report Encounter.json | Encounter | Village health reporting |
| **MCH Worker Followup** | Individual | forms/MCH Worker Followup.json | Encounter | MCH worker follow-up |
| **Verbal Autopsy Maternal** | Individual | forms/Verbal Autopsy form - Maternal death.json | Encounter | Verbal autopsy maternal |
| **Death** | Individual | forms/Death.json | Encounter | Death encounter |
| **Monthly Cluster monitoring** | Individual | forms/Monthly Cluster monitoring Encounter.json | Encounter | Cluster monitoring |
| **Senior Health Worker Monthly Reporting** | Individual | forms/Senior Health Worker Monthly Reporting.json | Encounter | Monthly reporting |
| **Weekly Village Health Worker Reporting** | Individual | forms/Weekly Village Health Worker Reporting Encounter.json | Encounter | Weekly village reporting |
| **Women's Health Camp** | Individual | forms/Women's Health Camp Encounter.json | Encounter | Women's health camp |
| **Village Round** | Individual | forms/Village Round Encounter.json | Encounter | Village round |
| **Daily Attendance Form** | Individual | forms/Daily Attendance Form.json | Encounter | Daily attendance |
| **Cluster Coordinator Followup** | Individual | forms/Cluster Coordinator Followup.json | Encounter | Cluster coordinator follow-up |

## Bahmni – Patient-Level Entities

| Bahmni Entity Name | Subject | File / Folder Source | Entity Type | Notes |
|---|---|---|---|---|
| **REG** | Patient | encounter_types/encounter_types.json | Registration | Registration encounter |
| **Consultation** | Patient | encounter_types/encounter_types.json | Visit | Consultation encounter |
| **ADMISSION** | Patient | encounter_types/encounter_types.json | Visit | Admission encounter |
| **DISCHARGE** | Patient | encounter_types/encounter_types.json | Visit | Discharge encounter |
| **TRANSFER** | Patient | encounter_types/encounter_types.json | Visit | Transfer encounter |
| **RADIOLOGY** | Patient | encounter_types/encounter_types.json | Visit | Radiology encounter |
| **INVESTIGATION** | Patient | encounter_types/encounter_types.json | Visit | Investigation encounter |
| **LAB_RESULT** | Patient | encounter_types/encounter_types.json | Visit | Lab result encounter |
| **Patient Document** | Patient | encounter_types/encounter_types.json | Encounter | Patient document |
| **VALIDATION NOTES** | Patient | encounter_types/encounter_types.json | Encounter | Validation notes |
| **EMERGENCY** | Patient | encounter_types/visit_types.json | Visit | Emergency visit |
| **LAB VISIT** | Patient | encounter_types/visit_types.json | Visit | Lab visit |
| **PHARMACY VISIT** | Patient | encounter_types/visit_types.json | Visit | Pharmacy visit |
| **IPD** | Patient | encounter_types/visit_types.json | Visit | Inpatient visit |
| **OPD** | Patient | encounter_types/visit_types.json | Visit | Outpatient visit |
| **ANC** | Patient | encounter_types/visit_types.json | Visit | Antenatal visit |
| **Field** | Patient | encounter_types/visit_types.json | Visit | Field visit |
| **Immunization Incident Record** | Patient | forms/forms.json | Encounter | Immunization incident |
| **Prescription Upload** | Patient | forms/forms.json | Encounter | Prescription upload |
| **Hypertension Program Registration Form** | Patient | forms/forms.json | Registration | Hypertension program registration |
| **OpenMRS Identification Number** | Patient | patient_identifiers/identifier_types.json | Registration | OpenMRS ID |
| **Old Identification Number** | Patient | patient_identifiers/identifier_types.json | Registration | Old ID |
| **Patient Identifier** | Patient | patient_identifiers/identifier_types.json | Registration | Patient identifier |
| **ABHA Number** | Patient | patient_identifiers/identifier_types.json | Registration | ABHA number |
| **ABHA Address** | Patient | patient_identifiers/identifier_types.json | Registration | ABHA address |
| **caste** | Patient | patient_identifiers/person_attribute_types.json | Registration | Caste attribute |
| **education** | Patient | patient_identifiers/person_attribute_types.json | Registration | Education attribute |
| **occupation** | Patient | patient_identifiers/person_attribute_types.json | Registration | Occupation attribute |
| **primaryContact** | Patient | patient_identifiers/person_attribute_types.json | Registration | Primary contact |
| **secondaryContact** | Patient | patient_identifiers/person_attribute_types.json | Registration | Secondary contact |
| **primaryRelative** | Patient | patient_identifiers/person_attribute_types.json | Registration | Primary relative |
| **Pulse Abnormal** | Patient | concepts/concepts.json | Encounter | Clinical concept |
| **Systolic Abnormal** | Patient | concepts/concepts.json | Encounter | Clinical concept |
| **Diastolic Abnormal** | Patient | concepts/concepts.json | Encounter | Clinical concept |
| **Temperature Abnormal** | Patient | concepts/concepts.json | Encounter | Clinical concept |
| **RR Abnormal** | Patient | concepts/concepts.json | Encounter | Clinical concept |
| **SPO2 Abnormal** | Patient | concepts/concepts.json | Encounter | Clinical concept |
| **BMI ABNORMAL** | Patient | concepts/concepts.json | Encounter | Clinical concept |
| **BMI STATUS ABNORMAL** | Patient | concepts/concepts.json | Encounter | Clinical concept |
| **Haemoglobin Range Abnormal** | Patient | concepts/concepts.json | Encounter | Clinical concept |
| **Urine Output Abnormal** | Patient | concepts/concepts.json | Encounter | Clinical concept |
| **General Medicine-M100001-Acute gastroenteritis with moderate dehydration-0** | Patient | concepts/concepts.json | Encounter | Ayushman Bharat package |
| **General Medicine-M100002-Recurrent vomiting with dehydration-0** | Patient | concepts/concepts.json | Encounter | Ayushman Bharat package |
| **General Medicine-M100003-Dysentery-0** | Patient | concepts/concepts.json | Encounter | Ayushman Bharat package |
| **REGISTRATION_CONCEPTS** | Patient | forms/observation_templates.json | Encounter | Registration concept set |
| **Lab Samples** | Patient | forms/observation_templates.json | Encounter | Lab sample concept set |
| **Visit Diagnoses** | Patient | forms/observation_templates.json | Encounter | Diagnosis concept set |
| **Lab Departments** | Patient | forms/observation_templates.json | Encounter | Lab department concept set |
| **Other Investigations** | Patient | forms/observation_templates.json | Encounter | Other investigations concept set |
| **Other Investigations Categories** | Patient | forms/observation_templates.json | Encounter | Investigation categories concept set |
| **COMMENTS** | Patient | concepts/concept_sets.json | Encounter | Registration comment concept |
| **REGISTRATION FEES** | Patient | concepts/concept_sets.json | Encounter | Registration fees concept |
| **Non-coded Diagnosis** | Patient | concepts/concept_sets.json | Encounter | Non-coded diagnosis concept |
| **Coded Diagnosis** | Patient | concepts/concept_sets.json | Encounter | Coded diagnosis concept |
| **Diagnosis Certainty** | Patient | concepts/concept_sets.json | Encounter | Diagnosis certainty concept |
| **Diagnosis order** | Patient | concepts/concept_sets.json | Encounter | Diagnosis order concept |
| **Numeric** | Patient | concepts/concept_datatypes.json | Encounter | Numeric datatype |
| **Coded** | Patient | concepts/concept_datatypes.json | Encounter | Coded datatype |
| **Text** | Patient | concepts/concept_datatypes.json | Encounter | Text datatype |
| **N/A** | Patient | concepts/concept_datatypes.json | Encounter | Not applicable datatype |
| **Document** | Patient | concepts/concept_datatypes.json | Encounter | Document datatype |
| **Date** | Patient | concepts/concept_datatypes.json | Encounter | Date datatype |
| **Time** | Patient | concepts/concept_datatypes.json | Encounter | Time datatype |
| **Datetime** | Patient | concepts/concept_datatypes.json | Encounter | Datetime datatype |
| **Boolean** | Patient | concepts/concept_datatypes.json | Encounter | Boolean datatype |
| **Rule** | Patient | concepts/concept_datatypes.json | Encounter | Rule datatype |
| **Structured Numeric** | Patient | concepts/concept_datatypes.json | Encounter | Structured numeric datatype |
| **Complex** | Patient | concepts/concept_datatypes.json | Encounter | Complex datatype |

## Summary Statistics

### Avni Individual-Level Entities
- **Registration**: 27 forms
- **Visit**: 16 encounter types
- **Program Encounter**: 42 forms
- **Encounter**: 33 forms
- **Total**: 118 individual-level entities

### Bahmni Patient-Level Entities
- **Registration**: 6 entities (3 forms + 3 identifiers)
- **Visit**: 9 encounter/visit types
- **Encounter**: 50 entities (3 forms + 47 concepts/templates/datatypes/sets)
- **Total**: 65 patient-level entities

## Key Findings
1. **Rich Clinical Data**: Avni has extensive program-specific encounters (TB, Diabetes, Hypertension, etc.)
2. **Comprehensive Coverage**: Both systems cover registration, visits, and clinical encounters
3. **Integration Potential**: Strong alignment between Avni program encounters and Bahmni visit types
4. **Data Volume**: Avni has more individual-level entities (118 vs 65) due to program-specific forms, while Bahmni has more clinical concepts and datatypes