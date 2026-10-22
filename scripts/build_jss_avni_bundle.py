#!/usr/bin/env python3
"""
Build the JSS Bahmni -> Avni metadata bundle and the matching integration mapping migration.

One run produces, from the same inputs:
  1. An Avni "Metadata Zip" for the target org containing
       - the one-time bridge setup ("Bahmni Patient Registration" + "Bahmni Entity UUID"),
         taken from JSS Bahmni integration/avni_org_setup/bundle/
       - one Avni form + encounter type + form mappings per Bahmni form marked `build`
         in JSS Bahmni integration/final_forms.csv
       - the "Bahmni - Medication" form used by the prescription (drug order) sync
       - all question and answer concepts, with Bahmni UUIDs
  2. A Flyway migration with every mapping the integration needs for those forms
  3. A report of anything skipped or suspicious, and docs/02-final-form-status.md

Rules applied (see .claude/skills/jss-bahmni-form-sync/SKILL.md lessons):
  - "Bahmni Entity UUID" is the first field of every form; every field is read-only
  - coded questions listed in multiselect_concepts.csv become MultiSelect
  - concept UUIDs are Bahmni's; Avni-side UUIDs (forms, groups, elements, encounter types,
    mappings) are derived deterministically, so re-running gives the same UUIDs
  - one encounter type per real form; catalogue sets are never mapped
  - SQL looks up ids by name and is idempotent (WHERE NOT EXISTS)

Usage:
    python3 scripts/build_jss_avni_bundle.py --subject-type-uuid <uuid> --migration-version 2_5_0
"""

import argparse
import csv
import glob
import json
import os
import sys
import uuid
import zipfile
from collections import OrderedDict

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JSS_DIR = os.path.join(ROOT, "JSS Bahmni integration")
DUMPS_DIR = os.path.join(JSS_DIR, "CSV dumps")
FORMS_CSV = os.path.join(JSS_DIR, "final_forms.csv")
MULTISELECT_CSV = os.path.join(DUMPS_DIR, "multiselect_concepts.csv")
# Answers read from the Bahmni DB (defined answers + every answer used in real visits) - fills gaps in the CSV exports
DB_ANSWERS_CSV = os.path.join(DUMPS_DIR, "bahmni_db_answers.csv")
SETUP_BUNDLE_DIR = os.path.join(JSS_DIR, "avni_org_setup", "bundle")
MIGRATION_DIR = os.path.join(ROOT, "integration-data", "src", "main", "resources", "db", "migration")
OUT_DIR = os.path.join(JSS_DIR, "avni_org_setup")
STATUS_DOC = os.path.join(JSS_DIR, "docs", "02-final-form-status.md")

PREFIX = "Bahmni - "
BAHMNI_ENTITY_UUID_CONCEPT = {"name": "Bahmni Entity UUID", "uuid": "a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d", "dataType": "Text"}
SUBJECT_TYPE_PLACEHOLDER = "{{SUBJECT_TYPE_UUID}}"
UUID_NAMESPACE = uuid.UUID("7f3c2a9e-1b4d-4c6a-9e8f-2d5b6a7c8e90")

LAB_RESULT_ENCOUNTER_TYPE = "960469a8-9bc6-11e3-927e-8840ab96f0f1"
CONSULTATION_ENCOUNTER_TYPE = "da7a4fe0-0a6a-11e3-939c-8c50edb4be99"
# Lab results and prescriptions sync only from these Bahmni visit types (constant "OutpatientVisitTypes", one row each).
# JSS records them under every visit type below; "Avni - ANC Clinic Visit" (2ee11869-...) is left out on purpose -
# those visits were created from Avni by the old Avni -> Bahmni sync and must not be copied back.
LAB_AND_PRESCRIPTION_VISIT_TYPES = [
    "f6ce7bf9-e349-11e3-983a-91270dcbd3bf",  # OPD
    "97b80034-9bc6-11e3-927e-8840ab96f0f1",  # LAB VISIT
    "a52293a5-9bc6-11e3-927e-8840ab96f0f1",  # IPD
    "b1a60bef-487a-11e3-82fc-4f0c5172470d",  # EMERGENCY
    "98b2c48d-9bc6-11e3-927e-8840ab96f0f1",  # PHARMACY VISIT
    "fbafed22-3fa6-4925-a4ab-c2f91f5ee8f2",  # Field
    "3acaab1c-6307-4f17-83ee-ded945f37602",  # ANC
    "a58eec3d-9bc6-11e3-927e-8840ab96f0f1",  # ANC (retired)
    "a571c3b1-9bc6-11e3-927e-8840ab96f0f1",  # CHRONIC (retired)
]
LAB_FORM = "All_Tests_and_Panels"
MEDICATION_FORM = {"name": "Bahmni - Medication", "uuid": "c7d8e9f0-a1b2-4c3d-8e4f-5a6b7c8d9e0f",
                   "concept": {"name": "Bahmni - Medications", "uuid": "b8c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e", "dataType": "Text"}}

# Bahmni datatype -> (Avni dataType, integration data_type_hint). None = cannot be synced, skip.
DATATYPES = {
    "Coded": ("Coded", "Coded"),
    "Numeric": ("Numeric", "Numeric"),
    "Text": ("Text", "Text"),
    "Date": ("Date", "Date"),
    "Datetime": ("DateTime", "DateTime"),
    # Bahmni sends its True/False concept. Avni keeps a Text question (a concept's type cannot change once uploaded) and the
    # mapping treats it as coded, so the answer arrives as the text "Bahmni - Yes" / "Bahmni - No" (BOOLEAN_ANSWERS)
    "Boolean": ("Text", "Coded"),
    "Complex": None,               # images/documents - not synced
    "N/A": None,                   # empty concept set
}

# Visits copied from Bahmni are read-only in Avni: this rule hides "new visit" for these encounter types in the
# Avni apps, so users cannot create them by hand. The integration writes through the API, which does not run it.
NOT_USER_FILLABLE_RULE = "'use strict';\n({params, imports}) => {\n  const individual = params.entity;\n  return false;\n};"

# Bahmni's boolean answers are its global concept.true / concept.false concepts
BOOLEAN_ANSWERS = [("11a7fd99-73a2-4b5c-87e1-b9505b4b899f", "Yes"), ("eb4d2cbc-8798-4073-83d6-9eb1934231ca", "No")]

# Bahmni's own bookkeeping inside Visit Diagnoses - meaningless to a field worker, so not synced
EXCLUDED_QUESTIONS = {"Bahmni Initial Diagnosis", "Bahmni Diagnosis Revised", "Bahmni Diagnosis Status"}

# Coded questions that Bahmni shows as a search box over a concept class instead of a fixed answer list.
# Their answers are every concept of those classes in the dumps.
ANSWERS_BY_CLASS = {
    "Coded Diagnosis": ["Diagnosis"],
    "Chief Complaint": ["Symptom", "Symptom/Finding"],
    "Surgical Procedure": ["Procedure"],
}

CONSTANTS = [
    ("BahmniIdentifierPrefix", "GAN"),
    ("IntegrationBahmniIdentifierType", "b46af68a-c79a-11e2-b284-107d46e7b2c5"),
    ("IntegrationAvniSubjectType", "Individual"),
    ("AvniSubjectUuidBahmniAttributeTypeUuid", "ceddfccf-7913-4a63-b60e-d3114f867e4b"),
] + [("OutpatientVisitTypes", uuid_) for uuid_ in LAB_AND_PRESCRIPTION_VISIT_TYPES]
MAPPING_GROUPS = ["GeneralEncounter", "Observation", "Common"]
MAPPING_TYPES = ["EncounterType", "Concept", "BahmniUUID_Concept", "LabEncounterType", "DrugOrderEncounterType", "DrugOrderConcept"]


def stable_uuid(*parts):
    return str(uuid.uuid5(UUID_NAMESPACE, "|".join(parts)))


def sql_str(value):
    return "NULL" if value is None else "'" + str(value).replace("'", "''") + "'"


# ── Bahmni dump ────────────────────────────────────────────────────────────────

class BahmniDump:
    def __init__(self):
        self.sets = {}       # lower name -> {uuid, name, children}
        self.concepts = {}   # lower name -> concepts.csv row
        for path in glob.glob(os.path.join(DUMPS_DIR, "*", "concept_sets.csv")):
            with open(path, newline="", encoding="utf-8-sig") as f:
                for row in csv.DictReader(f):
                    name = row["name"].strip()
                    if not name:
                        continue
                    children = [row[c].strip() for c in sorted((c for c in row if c and c.startswith("child.")), key=lambda c: int(c.split(".")[1])) if (row.get(c) or "").strip()]
                    self.sets.setdefault(name.lower(), {"uuid": row["uuid"].strip(), "name": name, "children": children})
        for path in glob.glob(os.path.join(DUMPS_DIR, "*", "concepts.csv")):
            with open(path, newline="", encoding="utf-8-sig") as f:
                for row in csv.DictReader(f):
                    if row["name"].strip():
                        self.concepts.setdefault(row["name"].strip().lower(), row)

    def find_set(self, name):
        return self.sets.get(name.strip().lower())

    def find_concept(self, name):
        return self.concepts.get(name.strip().lower())

    def is_set(self, name):
        s = self.find_set(name)
        return s is not None and len(s["children"]) > 0

    def leaves(self, name, visited):
        key = name.strip().lower()
        if key in visited:
            return []
        visited.add(key)
        if self.is_set(name):
            out = []
            for child in self.find_set(name)["children"]:
                out.extend(self.leaves(child, visited))
            return out
        return [name]

    def answers(self, row):
        cols = sorted((c for c in row if c and c.startswith("answer.") and (row.get(c) or "").strip()), key=lambda c: int(c.split(".")[1]))
        answers = [row[c].strip() for c in cols]
        if not answers and row["name"].strip() in ANSWERS_BY_CLASS:
            classes = ANSWERS_BY_CLASS[row["name"].strip()]
            answers = sorted(r["name"].strip() for r in self.concepts.values() if r["class"].strip() in classes)
        return answers


# ── Builder ────────────────────────────────────────────────────────────────────

class Builder:
    def __init__(self, dump, multiselect_uuids, subject_type_uuid):
        self.dump = dump
        self.multiselect = multiselect_uuids
        self.subject_type_uuid = subject_type_uuid
        self.concepts = OrderedDict()   # uuid -> avni concept json (questions)
        self.answer_concepts = OrderedDict()  # uuid -> avni concept json (answer-only)
        self.question_hints = OrderedDict()   # bahmni uuid -> (avni name, hint)
        self.answer_names = OrderedDict()     # bahmni uuid -> avni name
        self.forms = []            # (file name, form json)
        self.encounter_types = []  # (name, uuid)
        self.form_mappings = []
        self.encounter_type_mappings = []  # (bahmni set uuid, avni encounter type name)
        self.report = []
        self.name_owner = {}   # Avni concept name -> uuid (Avni needs unique names)
        self.uuid_name = {}    # uuid -> Avni concept name
        self.db_answers = {}   # question uuid -> [(answer uuid, answer name)]
        if os.path.exists(DB_ANSWERS_CSV):
            with open(DB_ANSWERS_CSV, newline="", encoding="utf-8") as f:
                for r in csv.DictReader(f):
                    self.db_answers.setdefault(r["question_uuid"], []).append((r["answer_uuid"], r["answer_name"]))

    def avni_name(self, concept_uuid, bahmni_name):
        """Avni concept name for a Bahmni concept; made unique when Bahmni has two concepts with the same name."""
        if concept_uuid in self.uuid_name:
            return self.uuid_name[concept_uuid]
        name = PREFIX + bahmni_name.strip()
        if self.name_owner.get(name, concept_uuid) != concept_uuid:
            name = f"{name} [{concept_uuid[:8]}]"
        self.name_owner[name] = concept_uuid
        self.uuid_name[concept_uuid] = name
        return name

    def warn(self, form, message):
        self.report.append(f"[{form}] {message}")

    def question_concept(self, form_name, leaf_name):
        if leaf_name.strip() in EXCLUDED_QUESTIONS:
            return None
        row = self.dump.find_concept(leaf_name)
        if row is None:
            self.warn(form_name, f"'{leaf_name}' not found in concepts.csv - skipped")
            return None
        datatype = DATATYPES.get(row["datatype"].strip())
        if datatype is None:
            self.warn(form_name, f"'{leaf_name}' has datatype {row['datatype'] or '(blank)'} - cannot be synced, skipped")
            return None
        avni_type, hint = datatype
        concept_uuid = row["uuid"].strip()
        if concept_uuid in self.concepts:
            return self.concepts[concept_uuid]
        concept = OrderedDict(name=self.avni_name(concept_uuid, row["name"]), uuid=concept_uuid, dataType=avni_type, active=True)
        if avni_type == "Numeric":
            for csv_col, key in (("units", "unit"), ("Low Normal", "lowNormal"), ("High Normal", "highNormal")):
                value = (row.get(csv_col) or "").strip()
                if value:
                    concept[key] = value if key == "unit" else float(value)
        if row["datatype"].strip() == "Boolean":
            for answer_uuid, answer_bahmni_name in BOOLEAN_ANSWERS:
                self.answer_names[answer_uuid] = self.avni_name(answer_uuid, answer_bahmni_name)
        if avni_type == "Coded":
            candidates = []
            for answer_name in self.dump.answers(row):
                answer_row = self.dump.find_concept(answer_name)
                if answer_row is None:
                    self.warn(form_name, f"answer '{answer_name}' of '{leaf_name}' not found in concepts.csv - skipped")
                    continue
                candidates.append((answer_row["uuid"].strip(), answer_row["name"]))
            candidates += self.db_answers.get(concept_uuid, [])
            answers, seen = [], set()
            for answer_uuid, answer_bahmni_name in candidates:
                if answer_uuid in seen:
                    continue
                seen.add(answer_uuid)
                answer_avni_name = self.avni_name(answer_uuid, answer_bahmni_name)
                answers.append(OrderedDict(name=answer_avni_name, uuid=answer_uuid, order=float(len(answers) + 1)))
                self.answer_names[answer_uuid] = answer_avni_name
                if answer_uuid not in self.concepts:
                    self.answer_concepts.setdefault(answer_uuid, OrderedDict(name=answer_avni_name, uuid=answer_uuid, dataType="NA", active=True))
            if not answers:
                self.warn(form_name, f"coded '{leaf_name}' has no answers in the dump")
            concept["answers"] = answers
        self.concepts[concept_uuid] = concept
        self.answer_concepts.pop(concept_uuid, None)
        self.question_hints[concept_uuid] = (concept["name"], hint)
        return concept

    def element(self, form_uuid, concept, order):
        is_multi = concept["dataType"] == "Coded" and concept["uuid"] in self.multiselect
        return OrderedDict(
            name=concept["name"],
            uuid=stable_uuid(form_uuid, "element", concept["uuid"]),
            keyValues=[{"key": "editable", "value": False}],
            concept=concept,
            displayOrder=float(order),
            type="MultiSelect" if is_multi else "SingleSelect",
            mandatory=False,
        )

    def entity_uuid_element(self, form_uuid):
        concept = OrderedDict(BAHMNI_ENTITY_UUID_CONCEPT, active=True)
        return OrderedDict(name=concept["name"], uuid=stable_uuid(form_uuid, "element", concept["uuid"]),
                           keyValues=[{"key": "editable", "value": False}], concept=concept,
                           displayOrder=1.0, type="SingleSelect", mandatory=False)

    def add_form(self, form_name, form_uuid, avni_name, groups_spec):
        """groups_spec: list of (group name, [concept json])"""
        groups = []
        seen = set()
        for index, (group_name, concepts) in enumerate(groups_spec):
            elements = []
            if index == 0:
                elements.append(self.entity_uuid_element(form_uuid))
            for concept in concepts:
                if concept["uuid"] in seen:
                    continue
                seen.add(concept["uuid"])
                elements.append(self.element(form_uuid, concept, len(elements) + 1))
            if not elements:
                continue
            groups.append(OrderedDict(uuid=stable_uuid(form_uuid, "group", group_name), name=group_name,
                                      displayOrder=float(len(groups) + 1), formElements=elements, timed=False))
        question_count = len(seen)
        if question_count == 0:
            self.warn(form_name, "no syncable questions - form not built")
            return False

        form = OrderedDict(name=avni_name, uuid=form_uuid, formType="Encounter", formElementGroups=groups,
                           decisionRule="", visitScheduleRule="", validationRule="", checklistsRule="", decisionConcepts=[])
        cancel_uuid = stable_uuid(form_uuid, "cancellation-form")
        cancel_form = OrderedDict(name=f"{avni_name} Encounter Cancellation", uuid=cancel_uuid,
                                  formType="IndividualEncounterCancellation", formElementGroups=[], decisionConcepts=[])
        encounter_type_uuid = stable_uuid("encounter-type", avni_name)
        self.forms.append((f"forms/{avni_name}_{form_uuid}.json", form))
        self.forms.append((f"forms/{avni_name} Encounter Cancellation_{cancel_uuid}.json", cancel_form))
        self.encounter_types.append((avni_name, encounter_type_uuid))
        for fuuid, ftype, fname in ((form_uuid, "Encounter", avni_name), (cancel_uuid, "IndividualEncounterCancellation", cancel_form["name"])):
            self.form_mappings.append(OrderedDict(uuid=stable_uuid("form-mapping", fuuid), formUUID=fuuid, encounterTypeUUID=encounter_type_uuid,
                                                  subjectTypeUUID=self.subject_type_uuid, formType=ftype, formName=fname, enableApproval=False))
        return True

    def build_bahmni_form(self, form_name, extra_top_level):
        form_set = self.dump.find_set(form_name)
        if form_set is None:
            self.warn(form_name, "concept set not found in dumps - not built")
            return
        avni_name = PREFIX + form_set["name"]
        groups_spec = []
        loose = []
        visited = {form_set["name"].lower()}
        for child in form_set["children"]:
            if self.dump.is_set(child):
                concepts = [c for c in (self.question_concept(form_name, leaf) for leaf in self.dump.leaves(child, visited)) if c]
                groups_spec.append((PREFIX + self.dump.find_set(child)["name"], concepts))
            else:
                concept = self.question_concept(form_name, child)
                if concept:
                    loose.append(concept)
                visited.add(child.strip().lower())
        if loose:
            groups_spec.insert(0, (avni_name, loose))
        if not groups_spec:
            groups_spec = [(avni_name, [])]
        if not self.add_form(form_name, form_set["uuid"], avni_name, groups_spec):
            return
        if form_set["name"] != LAB_FORM:
            self.encounter_type_mappings.append((form_set["uuid"], avni_name))
        for extra in extra_top_level:
            extra_set = self.dump.find_set(extra)
            if extra_set is None:
                self.warn(form_name, f"extra top-level set '{extra}' not found - not mapped")
            else:
                self.encounter_type_mappings.append((extra_set["uuid"], avni_name))

    def build_medication_form(self):
        concept = OrderedDict(MEDICATION_FORM["concept"], active=True)
        self.concepts[concept["uuid"]] = concept
        self.add_form("Medication", MEDICATION_FORM["uuid"], MEDICATION_FORM["name"], [(MEDICATION_FORM["name"], [concept])])


# ── Outputs ────────────────────────────────────────────────────────────────────

def read_setup_bundle(subject_type_uuid):
    files = {}
    for dirpath, _, filenames in os.walk(SETUP_BUNDLE_DIR):
        for filename in filenames:
            path = os.path.join(dirpath, filename)
            with open(path, encoding="utf-8") as f:
                files[os.path.relpath(path, SETUP_BUNDLE_DIR)] = json.loads(f.read().replace(SUBJECT_TYPE_PLACEHOLDER, subject_type_uuid))
    return files


def write_zip(builder, out_zip, include_setup):
    setup = read_setup_bundle(builder.subject_type_uuid) if include_setup else {
        "concepts.json": [], "encounterTypes.json": [], "formMappings.json": [], "operationalEncounterTypes.json": {"operationalEncounterTypes": []}}
    concepts = setup["concepts.json"] + list(builder.answer_concepts.values()) + list(builder.concepts.values())
    encounter_types = setup["encounterTypes.json"] + [OrderedDict(name=n, uuid=u, entityEligibilityCheckRule=NOT_USER_FILLABLE_RULE, active=True, immutable=False) for n, u in builder.encounter_types]
    operational = setup["operationalEncounterTypes.json"]["operationalEncounterTypes"] + [
        OrderedDict(uuid=stable_uuid("operational-encounter-type", u), encounterType={"uuid": u, "voided": False}, name=n, voided=False) for n, u in builder.encounter_types]
    form_mappings = setup["formMappings.json"] + builder.form_mappings
    with zipfile.ZipFile(out_zip, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("concepts.json", json.dumps(concepts, indent=2))
        for name, content in setup.items():
            if name.startswith("forms/"):
                z.writestr(name, json.dumps(content, indent=2))
        for name, form in builder.forms:
            z.writestr(name, json.dumps(form, indent=2))
        z.writestr("encounterTypes.json", json.dumps(encounter_types, indent=2))
        z.writestr("operationalEncounterTypes.json", json.dumps({"operationalEncounterTypes": operational}, indent=2))
        z.writestr("formMappings.json", json.dumps(form_mappings, indent=2))


def ensure_by_name_sql(table, names):
    rows = ",\n    ".join(f"({sql_str(n)})" for n in names)
    return f"""INSERT INTO {table} (name, integration_system_id, uuid, is_voided)
SELECT v.name, (SELECT id FROM integration_system WHERE name = 'bahmni'), uuid_generate_v4(), false
FROM (VALUES
    {rows}
) AS v(name)
WHERE NOT EXISTS (SELECT 1 FROM {table} t WHERE t.name = v.name AND t.integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni'));
"""


def mapping_sql(comment, group, mapping_type, rows):
    """rows: list of (int_system_value, avni_value, data_type_hint)"""
    if not rows:
        return ""
    values = ",\n    ".join(f"({sql_str(a)}, {sql_str(b)}, {sql_str(c)})" for a, b, c in rows)
    return f"""-- {comment} ({len(rows)} rows)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
SELECT v.int_system_value, v.avni_value, v.data_type_hint, b.system_id, b.group_id, b.type_id, uuid_generate_v4(), false
FROM (VALUES
    {values}
) AS v(int_system_value, avni_value, data_type_hint)
CROSS JOIN (
    SELECT s.id AS system_id,
           (SELECT g.id FROM mapping_group g WHERE g.name = {sql_str(group)} AND g.integration_system_id = s.id LIMIT 1) AS group_id,
           (SELECT t.id FROM mapping_type t WHERE t.name = {sql_str(mapping_type)} AND t.integration_system_id = s.id LIMIT 1) AS type_id
    FROM integration_system s WHERE s.name = 'bahmni'
) AS b
WHERE NOT EXISTS (
    SELECT 1 FROM mapping_metadata m
    WHERE m.int_system_value = v.int_system_value AND m.mapping_type_id = b.type_id AND m.is_voided = false
);

"""


def write_sql(builder, out_sql, forms_built, full_build):
    constants = ",\n    ".join(f"({sql_str(k)}, {sql_str(v)})" for k, v in CONSTANTS)
    concept_rows = [(u, name, hint) for u, (name, hint) in builder.question_hints.items()]
    answer_rows = [(u, name, None) for u, name in builder.answer_names.items() if u not in builder.question_hints]
    header = [
        "-- JSS Ganiyari Bahmni -> Avni mappings for the final form list.\n"
        "-- GENERATED by scripts/build_jss_avni_bundle.py from final_forms.csv - do not edit by hand; regenerate instead.\n"
        f"-- Forms: {', '.join(forms_built)}\n\n"]
    shared = [
        "-- Mapping groups and types used by the Bahmni module\n" + ensure_by_name_sql("mapping_group", MAPPING_GROUPS) + "\n" + ensure_by_name_sql("mapping_type", MAPPING_TYPES) + "\n",
        f"""-- Constants
INSERT INTO constants (key, value, uuid, is_voided)
SELECT v.key, v.value, uuid_generate_v4(), false
FROM (VALUES
    {constants}
) AS v(key, value)
WHERE NOT EXISTS (SELECT 1 FROM constants c WHERE c.key = v.key AND c.is_voided = false
                  AND (c.key <> 'OutpatientVisitTypes' OR c.value = v.value));

""",
        mapping_sql("Avni concept that stores the Bahmni entity (patient/encounter) UUID", "Common", "BahmniUUID_Concept", [(BAHMNI_ENTITY_UUID_CONCEPT["uuid"], BAHMNI_ENTITY_UUID_CONCEPT["name"], None)]),
        mapping_sql("Lab results: LAB_RESULT encounter type -> Avni lab form", "GeneralEncounter", "LabEncounterType", [(LAB_RESULT_ENCOUNTER_TYPE, PREFIX + LAB_FORM, None)]),
        mapping_sql("Prescriptions: drug orders on Consultation encounters -> Avni medication encounter", "GeneralEncounter", "DrugOrderEncounterType", [(CONSULTATION_ENCOUNTER_TYPE, MEDICATION_FORM["name"], None)]),
        mapping_sql("Prescriptions: Avni concept holding the formatted drug list", "GeneralEncounter", "DrugOrderConcept", [("drug-orders-text", MEDICATION_FORM["concept"]["name"], None)]),
    ]
    per_form = [
        mapping_sql("Form (Bahmni concept set) -> Avni encounter type", "GeneralEncounter", "EncounterType", [(u, n, None) for u, n in builder.encounter_type_mappings]),
        mapping_sql("Questions", "Observation", "Concept", concept_rows),
        mapping_sql("Coded answers", "Observation", "Concept", answer_rows),
    ]
    with open(out_sql, "w", encoding="utf-8") as f:
        f.write("".join(header + (shared if full_build else []) + per_form))


STATUS_LABELS = {"build": "✅ In the Avni bundle", "covered": "✅ Covered by Bahmni - Medication",
                 "answers": "✅ Synced as answers", "not-built": "⛔ Not built", "decide": "❓ Decision pending"}


def write_status_doc(rows):
    """docs/02-final-form-status.md - one row per form on JSS's list, straight from final_forms.csv."""
    out = ["# JSS Final Form List — Status", "",
           "Source: the final form list shared by the JSS team ([Google Sheet](https://docs.google.com/spreadsheets/d/1ZRTp0mB2xPG5vlQMetj6Tp35jS9pQS2kBvV1I1tS7f0/edit?gid=2110362113#gid=2110362113)). "
           f"{len(rows)} forms after removing duplicates.", "",
           "**Generated** by `scripts/build_jss_avni_bundle.py` from `JSS Bahmni integration/final_forms.csv` — edit that file, not this one. "
           "Usage figures are from the prerelease Bahmni copy (JSS data up to Dec 2025), so production may hold more.", "",
           "| # | Bahmni form | Status | Notes |", "|---|---|---|---|"]
    for i, r in enumerate(rows, 1):
        extra = f" Also synced from: {r['extra_top_level_sets'].replace(';', ', ')}." if r.get("extra_top_level_sets") else ""
        out.append(f"| {i} | {r['bahmni_form']} | {STATUS_LABELS.get(r['status'].strip(), r['status'])} | {(r.get('note') or '').strip()}{extra} |")
    out += ["", "## What the statuses mean", "", "| Status | Meaning |", "|---|---|",
            "| ✅ In the Avni bundle | Avni form + visit type created; Bahmni visits for this form sync to Avni |",
            "| ✅ Covered by Bahmni - Medication | Drop-down values inside a prescription; prescriptions sync as one readable line per medicine |",
            "| ✅ Synced as answers | Not a form; its values are the answer options of another form's question |",
            "| ⛔ Not built | Cannot be built from the current Bahmni dump, or never recorded. Revisit if JSS confirm production has data for it |",
            "", "## Also in the bundle (not on the list, needed by the sync)", "", "| Avni form | Why |", "|---|---|",
            "| Bahmni Patient Registration | Bridge record linking an Avni individual to a Bahmni patient |",
            "| Bahmni - Medication | Receives prescriptions (drug orders) |", ""]
    with open(STATUS_DOC, "w", encoding="utf-8") as f:
        f.write("\n".join(out))


def validate(builder):
    problems = []
    names = {}
    for concept in list(builder.concepts.values()) + list(builder.answer_concepts.values()):
        if concept["name"] in names and names[concept["name"]] != concept["uuid"]:
            problems.append(f"concept name used by two UUIDs: {concept['name']}")
        names[concept["name"]] = concept["uuid"]
    for _, form in builder.forms:
        if form["formType"] != "Encounter":
            continue
        if "encounterType" in form:
            problems.append(f"{form['name']}: has encounterType key")
        first = form["formElementGroups"][0]["formElements"][0]
        if first["concept"]["uuid"] != BAHMNI_ENTITY_UUID_CONCEPT["uuid"]:
            problems.append(f"{form['name']}: first field is not Bahmni Entity UUID")
        for group in form["formElementGroups"]:
            for element in group["formElements"]:
                if element["keyValues"] != [{"key": "editable", "value": False}]:
                    problems.append(f"{form['name']}: {element['name']} is editable")
    avni_values = [n for n, _ in builder.question_hints.values()] + list(builder.answer_names.values())
    problems += [f"avni_value with 'Avni - ' prefix: {v}" for v in avni_values if v.startswith("Avni - ")]
    return problems


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--subject-type-uuid", required=True, help="UUID of the 'Individual' subject type in the target Avni org")
    parser.add_argument("--migration-version", required=True, help="Flyway version, e.g. 2_5_0 (must be above origin/main's highest)")
    parser.add_argument("--org-label", default="org", help="Used in the zip file name")
    parser.add_argument("--forms", help="Add-on build: only these forms (';'-separated names from final_forms.csv). "
                                        "Produces a small zip and a NEW migration; the bridge setup and shared rows are left out")
    parser.add_argument("--overwrite", action="store_true", help="Allow replacing an existing migration file (only if it has never run anywhere)")
    args = parser.parse_args()
    only = {f.strip() for f in args.forms.split(";") if f.strip()} if args.forms else None
    full_build = only is None

    with open(MULTISELECT_CSV, newline="", encoding="utf-8") as f:
        multiselect = {r["uuid"].strip() for r in csv.DictReader(f)}
    builder = Builder(BahmniDump(), multiselect, args.subject_type_uuid)

    built, skipped = [], []
    with open(FORMS_CSV, newline="", encoding="utf-8") as f:
        rows = list(csv.DictReader(f))
    if only:
        buildable = {r["bahmni_form"] for r in rows if r["status"].strip() == "build"}
        unknown = sorted(only - buildable)
        if unknown:
            sys.exit(f"Not in final_forms.csv with status 'build': {', '.join(unknown)}. Add the row first.")
    for row in rows:
        if only and row["bahmni_form"] not in only:
            continue
        if row["status"].strip() != "build":
            skipped.append(f"{row['bahmni_form']} ({row['status']}: {row['note']})")
            continue
        extras = [e.strip() for e in (row.get("extra_top_level_sets") or "").split(";") if e.strip()]
        before = len(builder.encounter_types)
        builder.build_bahmni_form(row["bahmni_form"], extras)
        if len(builder.encounter_types) > before:
            built.append(row["bahmni_form"])
    if full_build:
        builder.build_medication_form()

    problems = validate(builder)
    if problems:
        print("VALIDATION FAILED:\n  " + "\n  ".join(problems))
        sys.exit(1)

    suffix = "" if full_build else f"_addon_V{args.migration_version}"
    out_zip = os.path.join(OUT_DIR, f"jss-avni-bundle_{args.org_label}{suffix}.zip")
    out_sql = os.path.join(MIGRATION_DIR, f"V{args.migration_version}__JSSBahmniFormMappings{'' if full_build else '_Addon'}.sql")
    out_report = os.path.join(OUT_DIR, f"jss-avni-bundle_{args.org_label}{suffix}_report.md")
    if os.path.exists(out_sql) and not args.overwrite:
        sys.exit(f"{os.path.basename(out_sql)} already exists. A migration that has run anywhere must never change - "
                 f"use a new --migration-version (with --forms for new forms). Pass --overwrite only if it has never run.")
    write_zip(builder, out_zip, include_setup=full_build)
    write_status_doc(rows)
    write_sql(builder, out_sql, built, full_build)

    multi = sorted({e["name"] for _, f in builder.forms for g in f["formElementGroups"] for e in g["formElements"] if e["type"] == "MultiSelect"})
    with open(out_report, "w", encoding="utf-8") as f:
        f.write(f"# JSS Avni bundle report ({args.org_label})\n\n")
        f.write(f"- Forms built: {len(built)} + Bahmni - Medication + bridge setup\n")
        f.write(f"- Question concepts: {len(builder.question_hints)}, answer-only concepts: {len(builder.answer_concepts)}\n")
        f.write(f"- Encounter-type mappings: {len(builder.encounter_type_mappings)}\n\n")
        f.write("## Forms\n\n| Avni form | Questions | Multi-select |\n|---|---|---|\n")
        for _, form in builder.forms:
            if form["formType"] == "Encounter":
                elements = [e for g in form["formElementGroups"] for e in g["formElements"]]
                f.write(f"| {form['name']} | {len(elements) - 1} | {sum(1 for e in elements if e['type'] == 'MultiSelect')} |\n")
        f.write("\n## Multi-select questions\n\n" + "\n".join(f"- {m}" for m in multi) + "\n")
        f.write("\n## Not built (need a decision)\n\n" + "\n".join(f"- {s}" for s in skipped) + "\n")
        f.write("\n## Warnings\n\n" + ("\n".join(f"- {w}" for w in builder.report) or "- none") + "\n")

    print(f"Forms built: {len(built)}{' (+ Medication, + bridge setup)' if full_build else ' (add-on)'}; not built: {len(skipped)}")
    print(f"Concepts: {len(builder.question_hints)} questions, {len(builder.answer_concepts)} answer-only; multi-select fields: {len(multi)}")
    print(f"Warnings: {len(builder.report)}")
    print(f"Zip:       {out_zip}\nMigration: {out_sql}\nReport:    {out_report}")


if __name__ == "__main__":
    main()
