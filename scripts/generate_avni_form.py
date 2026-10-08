#!/usr/bin/env python3
"""
Generate Avni form JSON from Bahmni observation templates.

TWO COMMANDS — run in this order:

  Step 1 (run ONCE per source):
    python3 scripts/generate_avni_form.py --all-concepts
    python3 scripts/generate_avni_form.py --source "Lab Samples" --all-concepts

  Step 2 (run once per form):
    python3 scripts/generate_avni_form.py "Form Name"
    python3 scripts/generate_avni_form.py --source "Lab Samples" "Lab Samples"

Default source: "All Observation Templates"
  Output: JSS Bahmni integration/CSV dumps/avni_bundle/

Custom source (e.g. "Lab Samples"):
  Output: JSS Bahmni integration/CSV dumps/avni_bundle/Lab Samples/

Examples:
    python3 scripts/generate_avni_form.py --all-concepts
    python3 scripts/generate_avni_form.py "Diabetes Intake Template"
    python3 scripts/generate_avni_form.py --source "Lab Samples" --all-concepts
    python3 scripts/generate_avni_form.py --source "Lab Samples" "Lab Samples"
    python3 scripts/generate_avni_form.py                          # lists available forms
    python3 scripts/generate_avni_form.py --source "Lab Samples"   # lists forms in that source

Notes:
    - All concept UUIDs are taken directly from Bahmni's concepts.csv — never generated
    - All form fields are marked read-only (data comes from Bahmni, not entered in Avni)
    - All names are prefixed with "Bahmni - "
    - Datatypes are taken from concepts.csv (Text, Numeric, Coded, Date, etc.)
    - Form type is Encounter (General Encounter in Avni — matches FormType enum in avni-server)
"""

import sys
import csv
import json
import uuid
import os

# ── Base paths ─────────────────────────────────────────────────────────────────
CSV_DUMPS_DIR = "JSS Bahmni integration/CSV dumps"
AVNI_BUNDLE_DIR = f"{CSV_DUMPS_DIR}/avni_bundle"
DEFAULT_SOURCE = "All Observation Templates"
PREFIX = "Bahmni - "

# Bahmni datatype → Avni dataType mapping
DATATYPE_MAP = {
    "Text": "Text",
    "Numeric": "Numeric",
    "Coded": "Coded",
    "Date": "Date",
    "Boolean": "Text",
    "Document": "Text",
    "N/A": "Text",
    "NA": "Text",
    "": "Text",
}


def get_paths(source_name):
    source_dir = f"{CSV_DUMPS_DIR}/{source_name}"
    output_dir = f"{AVNI_BUNDLE_DIR}/{source_name}"
    return {
        "source_dir": source_dir,
        "concepts_csv": f"{source_dir}/concepts.csv",
        "concept_sets_csv": f"{source_dir}/concept_sets.csv",
        "output_dir": output_dir,
        "concepts_file": f"{output_dir}/concepts.json",
        "forms_dir": f"{output_dir}/forms",
    }


# ── Data loading ───────────────────────────────────────────────────────────────

def load_bahmni_concepts(paths):
    """Load concepts.csv — the authoritative source of concept UUIDs and datatypes."""
    with open(paths["concepts_csv"], newline="", encoding="utf-8-sig") as f:
        rows = list(csv.DictReader(f))
    return {r["name"].strip(): r for r in rows if r["name"].strip()}


def load_concept_sets(paths):
    with open(paths["concept_sets_csv"], newline="", encoding="utf-8-sig") as f:
        all_rows = list(csv.DictReader(f))
    by_name = {r["name"].strip(): r for r in all_rows if r["name"].strip()}
    return by_name, all_rows


def get_children(row):
    child_cols = sorted([c for c in row if c.startswith("child.")],
                        key=lambda c: int(c.split(".")[1]))
    return [row[c].strip() for c in child_cols if row.get(c, "").strip()]


def find_form(form_name, by_name, all_rows):
    for row in all_rows:
        if row["class"].strip() == "ConvSet" and row["name"].strip() == form_name:
            return row
    return by_name.get(form_name)


# ── Concept collection ─────────────────────────────────────────────────────────

def collect_all_concept_names(by_name, all_rows):
    """Collect leaf concept names — those referenced as children but without their own concept_sets row."""
    set_names = {r["name"].strip() for r in all_rows if r["name"].strip()}
    all_names = set()
    for row in all_rows:
        for child in get_children(row):
            all_names.add(child)
    all_names = all_names - set_names
    all_names.discard("")
    return all_names


def collect_leaf_concepts(name, by_name, visited=None):
    """Recursively walk a concept set and return leaf concept names."""
    if visited is None:
        visited = set()
    if name in visited:
        return []
    visited.add(name)

    row = by_name.get(name)
    if row is None:
        return [name]

    children = get_children(row)
    if not children:
        return [name]

    leaves = []
    for child in children:
        leaves.extend(collect_leaf_concepts(child, by_name, visited))
    return leaves


# ── Avni JSON builders ─────────────────────────────────────────────────────────

def make_concept(name, bahmni_concepts):
    """Build an Avni concept entry using UUID and datatype from Bahmni's concepts.csv."""
    bahmni_row = bahmni_concepts.get(name)
    if bahmni_row is None:
        print(f"  WARNING: concept '{name}' not found in concepts.csv — skipping")
        return None

    bahmni_datatype = bahmni_row.get("datatype", "").strip()
    avni_datatype = DATATYPE_MAP.get(bahmni_datatype, "Text")

    return {
        "name": f"{PREFIX}{name}",
        "uuid": bahmni_row["uuid"].strip(),
        "dataType": avni_datatype,
        "active": True
    }


def build_form_element_groups(form_row, by_name, bahmni_concepts):
    concept_registry = {}   # leaf name → concept object (shared across sections)
    seen_uuids = set()      # concept UUIDs already added to the form (Avni forbids duplicates)
    sections = get_children(form_row)
    groups = []

    for i, section_name in enumerate(sections):
        section_row = by_name.get(section_name)

        if section_row is not None:
            leaves = []
            for child in get_children(section_row):
                leaves.extend(collect_leaf_concepts(child, by_name))
        else:
            leaves = [section_name]

        elements = []
        display_order = 1
        for leaf in leaves:
            if leaf not in concept_registry:
                concept_registry[leaf] = make_concept(leaf, bahmni_concepts)
            concept = concept_registry[leaf]
            if concept is None:
                continue
            if concept["uuid"] in seen_uuids:
                continue  # already in another section — skip to avoid Avni duplicate error
            seen_uuids.add(concept["uuid"])
            elements.append({
                "name": concept["name"],
                "uuid": str(uuid.uuid4()),
                "keyValues": [{"key": "editable", "value": False}],
                "concept": concept,
                "displayOrder": float(display_order),
                "type": "SingleSelect",
                "mandatory": False
            })
            display_order += 1

        section_uuid = (
            section_row["uuid"].strip()
            if section_row and section_row.get("uuid", "").strip()
            else str(uuid.uuid4())
        )

        groups.append({
            "uuid": section_uuid,
            "name": f"{PREFIX}{section_name}",
            "displayOrder": float(i + 1),
            "formElements": elements,
            "timed": False
        })

    return groups


# ── Commands ───────────────────────────────────────────────────────────────────

def cmd_all_concepts(by_name, all_rows, bahmni_concepts, paths):
    """Generate concepts.json for this source. Run once per source."""
    all_names = collect_all_concept_names(by_name, all_rows)

    concepts = []
    missing = []
    for name in sorted(all_names):
        c = make_concept(name, bahmni_concepts)
        if c is not None:
            concepts.append(c)
        else:
            missing.append(name)

    os.makedirs(paths["output_dir"], exist_ok=True)
    with open(paths["concepts_file"], "w") as f:
        json.dump(concepts, f, indent=2)

    print(f"Generated {len(concepts)} concepts → {paths['concepts_file']}")
    if missing:
        print(f"WARNING: {len(missing)} names not found in concepts.csv (skipped): {', '.join(missing[:5])}{'...' if len(missing) > 5 else ''}")
    print()
    print("Upload this file to Avni ONCE before uploading any forms from this source.")


def cmd_generate_form(form_name, by_name, all_rows, bahmni_concepts, paths):
    """Generate form JSON for a single form."""
    form_row = find_form(form_name, by_name, all_rows)
    if form_row is None:
        print(f"ERROR: Form '{form_name}' not found.")
        print("Run without a form name to see available forms.")
        sys.exit(1)

    if not os.path.exists(paths["concepts_file"]):
        print(f"WARNING: {paths['concepts_file']} not found.")
        print("Run --all-concepts first, then upload concepts.json to Avni before generating forms.")
        sys.exit(1)

    form_uuid = form_row["uuid"].strip()
    groups = build_form_element_groups(form_row, by_name, bahmni_concepts)
    total_fields = sum(len(g["formElements"]) for g in groups)

    avni_form = {
        "name": f"{PREFIX}{form_name}",
        "uuid": form_uuid,
        "formType": "Encounter",
        "formElementGroups": groups,
        "decisionRule": "",
        "visitScheduleRule": "",
        "validationRule": "",
        "checklistsRule": "",
        "decisionConcepts": []
    }

    os.makedirs(paths["forms_dir"], exist_ok=True)
    safe_name = form_name.replace("/", "_").replace(" ", "_").replace(",", "")
    form_path = f"{paths['forms_dir']}/Bahmni_-_{safe_name}.json"
    with open(form_path, "w") as f:
        json.dump(avni_form, f, indent=2)

    print(f"Form: {PREFIX}{form_name}")
    print(f"UUID: {form_uuid}")
    print(f"Sections ({len(groups)}):")
    for g in groups:
        print(f"  {g['name']} → {len(g['formElements'])} fields")
    print(f"\nTotal fields: {total_fields}")
    print(f"Output: {form_path}")
    print()
    print("Next: Upload this form JSON to Avni, then add mapping SQL to integration DB.")


def cmd_list_forms(all_rows, source_name):
    print(f"Available forms in '{source_name}':")
    for row in all_rows:
        if row["class"].strip() == "ConvSet":
            print(f"  - {row['name'].strip()}")


# ── Entry point ────────────────────────────────────────────────────────────────

def parse_args():
    """Returns (source_name, command) where command is '--all-concepts', a form name, or None (list)."""
    args = sys.argv[1:]
    source_name = DEFAULT_SOURCE

    if "--source" in args:
        idx = args.index("--source")
        if idx + 1 >= len(args):
            print("ERROR: --source requires a directory name argument.")
            sys.exit(1)
        source_name = args[idx + 1]
        args = args[:idx] + args[idx + 2:]

    command = args[0].strip() if args else None
    return source_name, command


def main():
    source_name, command = parse_args()
    paths = get_paths(source_name)

    if not os.path.exists(paths["concept_sets_csv"]):
        print(f"ERROR: Source not found: {paths['source_dir']}")
        print(f"Expected files: concepts.csv and concept_sets.csv in that folder.")
        sys.exit(1)

    by_name, all_rows = load_concept_sets(paths)
    bahmni_concepts = load_bahmni_concepts(paths)

    if command is None:
        print("Usage:")
        print("  python3 scripts/generate_avni_form.py --all-concepts")
        print("  python3 scripts/generate_avni_form.py \"Form Name\"")
        print("  python3 scripts/generate_avni_form.py --source \"Lab Samples\" --all-concepts")
        print("  python3 scripts/generate_avni_form.py --source \"Lab Samples\" \"Lab Samples\"")
        print()
        cmd_list_forms(all_rows, source_name)
        sys.exit(0)

    if command == "--all-concepts":
        cmd_all_concepts(by_name, all_rows, bahmni_concepts, paths)
    else:
        cmd_generate_form(command, by_name, all_rows, bahmni_concepts, paths)


if __name__ == "__main__":
    main()
