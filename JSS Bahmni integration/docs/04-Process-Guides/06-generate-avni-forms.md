# Generating Avni Forms from Bahmni Observation Templates

## What This Does

Converts Bahmni observation template definitions into Avni form JSON files that can be uploaded directly to Avni. All fields are read-only (data flows from Bahmni into Avni, not the other way).

---

## Source File

Everything comes from one master file:

```
JSS Bahmni integration/CSV dumps/All Observation Templates/concept_sets.csv
```

This single file contains **all forms**, their sections, and all concept/field references for the entire JSS Bahmni instance. It was exported once from Bahmni Admin → Dictionary → Export.

---

## Commands

### Step 1 — Generate concepts.json (run once, ever)

```bash
python3 scripts/generate_avni_form.py --all-concepts
```

Reads every concept name referenced anywhere in the master file and writes them all to:

```
JSS Bahmni integration/CSV dumps/avni_bundle/concepts.json
```

**Upload this file to Avni once.** It covers all concepts across all forms. You do not need to regenerate or re-upload it unless new fields are added to Bahmni's observation templates.

Output example:
```
Generated 1584 concepts → avni_bundle/concepts.json
```

---

### Step 2 — Generate a form JSON (run once per form)

```bash
python3 scripts/generate_avni_form.py "Form Name"
```

Replace `"Form Name"` with the exact Bahmni form name. Writes to:

```
JSS Bahmni integration/CSV dumps/avni_bundle/forms/Bahmni_-_<FormName>.json
```

This command does **not** touch `concepts.json`.

**Example:**
```bash
python3 scripts/generate_avni_form.py "Diabetes Intake Template"
python3 scripts/generate_avni_form.py "Consultation"
python3 scripts/generate_avni_form.py "RMRCT, Standardized History"
```

**List available forms** (run with no arguments):
```bash
python3 scripts/generate_avni_form.py
```

---

## Available Forms (as of export)

| Form Name |
|---|
| Blood Pressure |
| Obstetrics, P/A (per abdomen) |
| Obstetrics, P/V (per vaginal) |
| Discharge Summary, Surgeries and Procedures |
| Discharge Summary |
| Diabetes Intake Template |
| Diabetes Counselling Template |
| Smart card, blocked package details |
| RMRCT, Microbiology test requirement |
| RMRCT, Standardized History |
| RMRCT, If treatment taken |
| Referral Form, Doctors Name |
| Referral Form, Summary |
| OPD Followup Non attendance Record Template |

---

## After Generating the Form

1. Upload `concepts.json` to Avni (if not already done)
2. Upload the form JSON to Avni
3. Add the encounter type mapping and observation concept mappings to the integration DB — see [06-bahmni-to-avni-sync.md](../03-Technical-Documentation/06-bahmni-to-avni-sync.md) Steps 4–5

---

## How the CSV Parsing Works

This section explains the logic the script uses to turn the Bahmni CSV into an Avni form.

---

### The concept_sets.csv structure

Every row in the file represents a **concept set** — a named grouping that contains other concepts as children. The columns are:

```
uuid, name, description, class, shortname, child.1, child.2, ..., child.N, reference-term-*
```

The `child.*` columns list the **names** of concepts that belong to this set. Children can themselves be concept sets (sections) or leaf concepts (actual fields).

The `class` column tells you what kind of concept set it is:

| class value | Meaning |
|---|---|
| `ConvSet` | Top-level form (the thing you upload to Avni as a form) |
| `Concept Details` | A section or sub-section within a form |
| `Misc` | Another type of section grouper |

---

### How the script finds a form

When you run `python3 scripts/generate_avni_form.py "Diabetes Intake Template"`, the script:

1. Reads all rows from `concept_sets.csv` into a dictionary keyed by `name`
2. Searches for a row where `class == "ConvSet"` AND `name == "Diabetes Intake Template"`
3. That row's `uuid` becomes the Avni form UUID
4. That row's `child.*` columns become the top-level sections

```
Row found:
  uuid  = 60619143-5b49-4c10-92f4-0d080cd10b8a
  name  = Diabetes Intake Template
  class = ConvSet
  child.1 = Diabetes, History
  child.2 = Diabetes, Complaint
  ...
```

---

### How sections and fields are resolved

For each child of the form, the script checks: **does this name have its own row in concept_sets.csv?**

**Yes → it is a section (concept set with its own children)**

The script looks up that row and reads its `child.*` columns to get the fields inside it.

```
Row: Diabetes, History
  child.1 = Diabetes, Diagnosed Date
  child.2 = Diabetes, Treatment Stopped Date
```

**No → it is a leaf concept (an actual form field)**

The name does not appear as a row in concept_sets.csv, which means it is a terminal concept — a real observation field with no sub-structure.

```
"Diabetes, Complaint" → no row found → leaf concept = form field
```

This distinction is the core logic. Visually:

```
Diabetes Intake Template (ConvSet)         ← top-level form
├── Diabetes, History (has row → section)
│   ├── Diabetes, Diagnosed Date           ← leaf = form field
│   └── Diabetes, Treatment Stopped Date   ← leaf = form field
├── Diabetes, Complaint (no row → leaf)    ← leaf = form field
├── Diabetes, Examination (has row → section)
│   ├── Diabetes, Peripheral Pulses        ← leaf = form field
│   └── ...
└── ...
```

---

### How concepts.json is built (--all-concepts)

The `--all-concepts` command collects every unique concept name in the file — both the names of concept set rows themselves and every value in every `child.*` column. This gives a complete set of all concept names referenced anywhere.

For each name, it creates an Avni concept entry:

```json
{
  "name": "Bahmni - Diabetes, Diagnosed Date",
  "uuid": "<generated-uuid>",
  "dataType": "Text",
  "active": true
}
```

All names get the `Bahmni - ` prefix. All datatypes default to `Text` (see limitation below).

---

### How the form JSON is built

For each section, a `formElementGroup` is created. For each leaf concept inside it, a `formElement` is created. Every form element gets:

- `keyValues: [{"key": "editable", "value": false}]` — read-only (data comes from Bahmni)
- `type: "SingleSelect"` — works for all data types
- `mandatory: false`

```json
{
  "name": "Bahmni - Diabetes Intake Template",
  "uuid": "60619143-5b49-4c10-92f4-0d080cd10b8a",
  "formType": "IndividualEncounter",
  "formElementGroups": [
    {
      "name": "Bahmni - Diabetes, History",
      "displayOrder": 1.0,
      "formElements": [
        {
          "name": "Bahmni - Diabetes, Diagnosed Date",
          "concept": { "name": "Bahmni - Diabetes, Diagnosed Date", "dataType": "Text", ... },
          "keyValues": [{"key": "editable", "value": false}],
          "mandatory": false
        }
      ]
    }
  ]
}
```

The form `uuid` is taken directly from the Bahmni `concept_sets.csv` row — this is the UUID the mapping SQL uses as `int_system_value` to match this form.

---

## Known Limitation: Datatypes

The master `concept_sets.csv` file does not contain datatype information for leaf concepts (that lives in a separate `concepts.csv` which is not in this export). All generated concepts default to `dataType: "Text"`.

**This is fine for read-only display fields** — Avni will store and show the value regardless.

However, if a field is a **coded (dropdown) concept** in Bahmni, you may want to update its `dataType` to `"Coded"` and add its `answers` array in the concepts.json before uploading. Without this, Avni will store the raw UUID sent by Bahmni instead of the human-readable answer name.

Whether this matters depends on whether the field needs to be readable in Avni's UI vs. just stored for reference.

---

## Script Location

```
scripts/generate_avni_form.py
```

---

*Last updated: 2026-04-30*
