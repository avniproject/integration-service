# ANC Clinic Visit to Bahmni Conversion Process

## Overview
Step-by-step process for converting ANC Clinic Visit form from Avni to Bahmni-compatible format.

## Input Data
- **Source**: Avni forms dump.csv (4,200+ rows)
- **Target Form**: ANC Clinic Visit (268 concepts, 12 sections)
- **Reference**: Avni Concept Mapping.csv for UUIDs

## Step 1: Filter & Extract
- Filtered ANC Clinic Visit form from complete dump
- Identified 12 sections with 268 total concepts
- Extracted all datatypes: Coded (43), Numeric (19), Date (5), Text (3)

## Step 2: Classification
- **Form**: ANC Clinic Visit → Top-level concept set (ConvSet)
- **Sections**: 12 element groups → Concept sets (ConvSet) 
- **Questions**: 268 individual concepts → Leaf concepts (Misc)
- **Answers**: Coded question options → Answer concepts (Misc)

## Step 3: UUID Management
- **Form/Sections**: Used provided UUIDs from updated CSV file
- **Questions**: Used UUIDs from form dump (all datatypes)
- **Answers**: Used UUIDs from concept mapping file
- **Prefix Strategy**: Added "Avni - " prefix to avoid conflicts

## CSV Generation Details

### concepts.csv Structure
```csv
uuid,name,description,class,shortname,datatype,units,High Normal,Low Normal,synonym.1,answer.1,answer.2,answer.3,answer.4,reference-term-source,reference-term-code,reference-term-relationship
```

**Content**:
- Form concept (ANC Clinic Visit)
- 12 section concepts (ANC Visit Details, Anthropometry, etc.)
- 268 question concepts (Height, Weight, BMI, etc.)
- Answer concepts for coded questions

### concept_sets.csv Structure  
```csv
uuid,name,description,class,shortname,child.1,child.2,child.3,child.4,child.5,child.6,child.7,child.8,child.9,child.10,reference-term-source,reference-term-code,reference-term-relationship
```

**Hierarchy**:
- Row 1: ANC Clinic Visit → 12 sections
- Rows 2-12: Each section → its questions
- Proper parent-child relationships maintained

## Step 5: Critical Issues & Solutions

### 🚨 CRITICAL RULE: NEVER Include Concept Sets in concepts.csv
**Issue**: Form element groups (sections) incorrectly included in concepts.csv
**Problem**: concepts.csv should ONLY contain individual concepts (questions, answers)
**Impact**: Concept sets (sections) appearing in both files → Bahmni conflicts
**Solution**: Strict separation - concepts.csv = Misc class ONLY, concept_sets.csv = Concept Details class
**Prevention**: ALWAYS validate class column - no ConvSet allowed in concepts.csv

### Issue 6: Bahmni Concept Conflicts
**Problem**: "Avni - " prefix insufficient when base concept already exists in Bahmni
**Error**: `'Avni - Urine Microscopy' is a duplicate name in locale 'en'`
**Root Cause**: 20+ ANC concepts (Height, Weight, BMI, etc.) already exist in Bahmni
**Solution**: Enhanced naming strategy with multi-level prefixes:
- **Standard**: "Avni - [Concept Name]" (for most forms)
- **Enhanced**: "Avni - ANC - [Concept Name]" (for ANC-specific conflicts)
- **Fallback**: "AVNI - JSS - [Concept Name]" (for severe ANC conflicts)
**General Rule for Other Forms**: Use "AVNI - " prefix for all new forms
**Prevention**: Automated conflict detection and resolution in validation script

### Issue 5: Duplicate Concept Set Names in Hierarchy
**Problem**: Multiple concept set entries with identical names but different UUIDs in `concept_sets.csv`
**Error**: `'Avni - Next visit details' is a duplicate name in locale 'en'`
**Root Cause**: Data processing created duplicate concept set entries for the same section
**Solution**: Remove duplicate rows, keeping only the entry with actual child concepts
**Prevention**: Add validation check for unique concept set names before CSV generation

### Issue 1: Duplicate Concept Names
**Problem**: `ANC_concepts.csv` contained duplicate concept names
**Error**: Bahmni upload failed with "duplicate available in locale 'en'"
**Root Cause**: Same concept appeared in multiple sections in raw data
**Solution**: Applied `drop_duplicates(subset=['name'], keep='first')` to ensure unique names

### Issue 2: Missing Datatypes
**Problem**: Some concepts had empty datatypes in `ANC_concepts.csv`
**Error**: Bahmni requires datatype for all concepts
**Root Cause**: Concept sets and answer concepts don't have specific datatypes
**Solution**: Filled empty datatypes with "N/A"

### Issue 3: Duplicate Concepts in Hierarchy
**Problem**: Same question concept appeared multiple times as child in `ANC_concept_sets.csv`
**Error**: Bahmni hierarchy requires unique child concepts
**Root Cause**: Same question existed in multiple sections in raw data
**Solution**: Ensured each question appears only once as child to a section

### Issue 4: Bahmni Naming Conflicts
**Problem**: Concept names already existed in Bahmni instance
**Error**: "duplicate available in locale 'en'" during upload
**Root Cause**: Standard concept names without unique identification
**Solution**: Added "Avni - " prefix to all concept and field names for clear identification

## Final Output Files
- **ANC_concepts.csv**: 105 unique concepts with "Avni - " prefix
- **ANC_concept_sets.csv**: 13 concept sets with proper hierarchy
- **Ready for Bahmni import**: No conflicts expected

## Key Learning Points
- Always validate for duplicates before CSV generation
- Empty datatypes must be "N/A" not blank
- Each concept must appear only once in entire hierarchy
- Use prefixes to avoid conflicts with existing systems

## Reference Files
- **CSV Structure Guide**: See `03-Bahmni-CSV-Structure-Guide.md` for detailed format requirements
- **Extraction Process**: See `01-Avni-Forms-Extraction-Process.md` for SQL queries
- **Complete Workflow**: See `04-Complete-Workflow-Summary.md` for end-to-end process
