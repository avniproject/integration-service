# Complete Avni to Bahmni Workflow Summary

## Overview
End-to-end process for converting any Avni form to Bahmni-compatible format.

## Phase 1: Data Extraction
1. **Run Forms Query** - Extract form structures, sections, concepts
2. **Run Concept Answers Query** - Extract coded answer options
3. **Validate Data** - Check for completeness, UUIDs, datatypes

## Phase 2: Form Selection
1. **Choose Target Form** - Select specific form to convert
2. **Filter Data** - Extract only target form data
3. **Analyze Structure** - Identify sections and concepts

## Phase 3: CSV Generation
1. **Create concepts.csv** with individual concepts ONLY (class: Misc)
2. **Create concept_sets.csv** with forms and sections ONLY (class: Concept Details)
3. **🚨 CRITICAL RULE**: NEVER include concept sets (ConvSet) in concepts.csv
4. **Apply naming strategy**:
   - **Standard forms**: "AVNI - [Form/Section/Concept Name]"
   - **ANC form**: "AVNI - JSS - [Section/Concept Name]" (due to conflicts)
5. **Validate format** against Bahmni requirements

## Phase 4: Quality Assurance & Validation
**Objective**: Ensure CSV files are error-free before Bahmni upload

#### Validation Checks:
1. **Concepts CSV Validation**:
   - ✅ No duplicate UUIDs
   - ✅ No duplicate names  
   - ✅ All datatypes populated (or "N/A")
   - ✅ Required columns present
   - ✅ "Avni - " prefix applied
   - 🚨 **CRITICAL**: NO concept sets (ConvSet) allowed - only Misc class

2. **Concept Sets CSV Validation**:
   - ✅ No duplicate UUIDs
   - ✅ **CRITICAL**: No duplicate names
   - ✅ Required columns present
   - ✅ "Avni - " prefix applied
   - ✅ Proper child relationships

#### Automated Validation:
```bash
python scripts/validate_bahmni_csv.py CSV_dumps/ANC_concepts.csv CSV_dumps/ANC_concept_sets.csv
```

#### Manual Checks:
- Row counts match expectations
- Hierarchy structure preserved
- No empty critical fields

## Phase 5: Bahmni Upload
1. **Upload concepts.csv** - All concepts and relationships
2. **Upload concept_sets.csv** - Hierarchical structure
3. **Test Import** - Verify successful upload
4. **Validate Forms** - Check form functionality

## Files Generated
- **Input**: Avni forms dump.csv, Avni Concept Mapping.csv
- **Output**: concepts.csv, concept_sets.csv
- **Location**: /CSV dumps/ directory

## Success Criteria
✅ All concepts have UUIDs
✅ No duplicate names or UUIDs
✅ Proper datatypes assigned
✅ Hierarchy relationships correct
✅ CSV format matches Bahmni requirements
✅ No conflicts with existing concepts

## Team Handoff
Any team member can follow this documented process to convert Avni forms to Bahmni format consistently.
