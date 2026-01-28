# Bahmni to Avni Form Conversion Process

This document provides a complete guide for converting Bahmni form exports into Avni form bundles.

---

## Overview

| Source (Bahmni) | Target (Avni) |
|-----------------|---------------|
| `concepts.csv` | `concepts.json` |
| `concept_sets.csv` | `forms/<FormName>.json` |

**Prefix Convention**: All entities are prefixed with `Bahmni - ` to identify them as originating from Bahmni.

---

## Phase 1: Prerequisites

### 1.1 Required Input Files

Export the Bahmni form as a CSV zip containing:
- `concepts.csv` - All concepts (questions and answers)
- `concept_sets.csv` - Form hierarchy (form → sections → concepts)

Place the unzipped folder in: `CSV dumps/<FormName>/`

### 1.2 Avni Bundle Structure

```
avni_bundle/
├── concepts.json           # All concepts with UUIDs
└── forms/
    └── <FormName>.json     # Form definition
```

---

## Phase 2: Understanding the Data

### 2.1 Bahmni concepts.csv Structure

| Field | Description |
|-------|-------------|
| `uuid` | Unique identifier (preserved in Avni) |
| `name` | Concept name |
| `datatype` | `Coded`, `Date`, `Text`, `Numeric`, `N/A` |
| `answer.1`, `answer.2`, ... | Answer options for Coded types |

### 2.2 Bahmni concept_sets.csv Structure

| Field | Description |
|-------|-------------|
| `uuid` | Unique identifier |
| `name` | Section or form name |
| `class` | `ConvSet` (main form) or `Misc` (section) |
| `child.1`, `child.2`, ... | Child concepts or sections |

### 2.3 Avni Datatype Mapping

| Bahmni Datatype | Avni dataType | Form Element Type |
|-----------------|---------------|-------------------|
| `Coded` | `Coded` | `SingleSelect` or `MultiSelect` |
| `Date` | `Date` | `SingleSelect` |
| `Text` | `Text` | `SingleSelect` |
| `Numeric` | `Numeric` | `SingleSelect` |
| `N/A` | `NA` | (Answer concept only) |

---

## Phase 3: Conversion Process

### 3.1 Convert Concepts

For each concept in `concepts.csv`:

1. Add prefix: `Bahmni - <original_name>`
2. Preserve UUID from source
3. Map datatype (`N/A` → `NA`)
4. For Coded concepts, include `answers` array with prefixed answer names

**Example Output:**
```json
{
  "name": "Bahmni - Diabetes, Complaint",
  "uuid": "01badc59-8cd7-4c6e-846b-e1a923c223ee",
  "dataType": "Coded",
  "active": true,
  "answers": [
    {"name": "Bahmni - Weakness", "uuid": "fc5a1852-...", "order": 1},
    {"name": "Bahmni - Tingling", "uuid": "4e294e8c-...", "order": 2}
  ]
}
```

### 3.2 Convert Form Structure

From `concept_sets.csv`:

1. Find `ConvSet` row → This is the main form
2. Children of `ConvSet` → These are sections (`formElementGroups`)
3. Children of sections → These are form elements

**Form JSON Structure:**
```json
{
  "name": "Bahmni - Diabetes Intake Template",
  "uuid": "60619143-5b49-4c10-92f4-0d080cd10b8a",
  "formType": "ProgramEnrolment",
  "formElementGroups": [
    {
      "uuid": "<generated>",
      "name": "Bahmni - Diabetes, History",
      "displayOrder": 1.0,
      "formElements": [...],
      "timed": false
    }
  ]
}
```

---

## Phase 4: Read-Only Form Elements

To mark a form element as **read-only**, add the following to its `keyValues` array:

```json
{
  "name": "Bahmni - Some Field",
  "uuid": "...",
  "keyValues": [
    {"key": "editable", "value": false}
  ],
  "concept": {...},
  "displayOrder": 1.0,
  "type": "SingleSelect",
  "mandatory": false
}
```

**Note**: The `keyValues` array supports various configurations:
- `{"key": "editable", "value": false}` - Read-only field
- `{"key": "repeatable", "value": true}` - Repeatable group

---

## Phase 5: Automated Conversion Script

A Python script is available at: `scripts/bahmni_to_avni_converter.py`

### Usage:
```bash
cd /Users/nupoorkhandelwal/Avni/integration-service
python3 scripts/bahmni_to_avni_converter.py
```

### Configuration (edit script if needed):
```python
PREFIX = "Bahmni - "
SOURCE_DIR = "CSV dumps/Diabetes Intake Template"
OUTPUT_DIR = "avni_bundle"
```

### Output:
- `avni_bundle/concepts.json` - All concepts with prefix
- `avni_bundle/forms/Bahmni_-_<FormName>.json` - Form definition

---

## Phase 6: Generated Files Summary

### Diabetes Intake Template Conversion

| Metric | Value |
|--------|-------|
| Total Concepts | 54 |
| Form Sections | 7 |
| Form Type | ProgramEnrolment |

**Sections (formElementGroups):**
1. Bahmni - Diabetes, History
2. Bahmni - Diabetes, Complaint
3. Bahmni - Diabetes, Other Complaints
4. Bahmni - Diabetes, Complications
5. Bahmni - Diabetes, Other Complications
6. Bahmni - Diabetes, Examination
7. Bahmni - Diabetes, Investigations

---

## Phase 7: Upload to Avni

1. **Upload concepts first**: Upload `concepts.json` to Avni
2. **Upload form**: Upload the form JSON file
3. **Verify**: Check that the form renders correctly in Avni

---

## Quick Reference

### Prefix Convention
All Bahmni-originated entities use: `Bahmni - <original_name>`

### Key Files
| File | Location |
|------|----------|
| Converter Script | `scripts/bahmni_to_avni_converter.py` |
| Concepts Output | `avni_bundle/concepts.json` |
| Form Output | `avni_bundle/forms/Bahmni_-_*.json` |
| Source Data | `CSV dumps/<FormName>/` |

### Read-Only Configuration
```json
"keyValues": [{"key": "editable", "value": false}]
```
