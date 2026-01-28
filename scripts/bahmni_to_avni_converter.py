#!/usr/bin/env python3
"""
Bahmni to Avni Form Converter
Converts Bahmni form CSV exports to Avni bundle JSON format.
Adds 'Bahmni - ' prefix to all entity names.
"""

import pandas as pd
import json
import uuid
import os

# Configuration
PREFIX = "Bahmni - "
SOURCE_DIR = "CSV dumps/Diabetes Intake Template"
OUTPUT_DIR = "avni_bundle"

def load_bahmni_data():
    """Load the Bahmni CSV export files."""
    concepts_df = pd.read_csv(f"{SOURCE_DIR}/concepts.csv", keep_default_na=False)
    concept_sets_df = pd.read_csv(f"{SOURCE_DIR}/concept_sets.csv", keep_default_na=False)
    return concepts_df, concept_sets_df

def get_answer_columns(df):
    """Get all answer column names from a dataframe."""
    return [col for col in df.columns if col.startswith("answer.")]

def get_child_columns(df):
    """Get all child column names from a dataframe."""
    return [col for col in df.columns if col.startswith("child.")]

def create_concepts_json(concepts_df):
    """Convert Bahmni concepts CSV to Avni concepts.json with prefix."""
    answer_columns = get_answer_columns(concepts_df)
    
    # Collect all unique concept names (including answers)
    all_concept_names = set()
    for name in concepts_df["name"]:
        if name and str(name).strip():
            all_concept_names.add(name.strip())
    
    for col in answer_columns:
        for answer in concepts_df[col]:
            if answer and str(answer).strip():
                all_concept_names.add(answer.strip())
    
    print(f"Found {len(all_concept_names)} unique concept names.")
    
    # Build UUID and datatype maps from source
    concept_uuid_map = {}
    concept_datatype_map = {}
    
    for _, row in concepts_df.iterrows():
        name = row["name"].strip() if row["name"] else ""
        if name:
            concept_uuid_map[name] = row["uuid"] if row["uuid"] else str(uuid.uuid4())
            concept_datatype_map[name] = row["datatype"]
    
    # Ensure all unique names have a UUID
    for name in all_concept_names:
        if name not in concept_uuid_map:
            concept_uuid_map[name] = str(uuid.uuid4())
    
    # Build concepts list
    avni_concepts = []
    
    for name in all_concept_names:
        data_type = concept_datatype_map.get(name, "NA")
        if data_type == "N/A":
            data_type = "NA"
        
        prefixed_name = f"{PREFIX}{name}"
        
        concept_obj = {
            "name": prefixed_name,
            "uuid": concept_uuid_map[name],
            "dataType": data_type,
            "active": True
        }
        
        # If coded, add answers with prefixed names
        source_row = concepts_df[concepts_df["name"] == name]
        if not source_row.empty and source_row.iloc[0]["datatype"] == "Coded":
            answers = []
            for col in answer_columns:
                answer_name = source_row.iloc[0][col]
                if answer_name and str(answer_name).strip():
                    answer_name = answer_name.strip()
                    answers.append({
                        "name": f"{PREFIX}{answer_name}",
                        "uuid": concept_uuid_map[answer_name],
                        "order": len(answers) + 1
                    })
            if answers:
                concept_obj["answers"] = answers
        
        avni_concepts.append(concept_obj)
    
    return avni_concepts, concept_uuid_map

def create_form_json(concept_sets_df, concept_uuid_map, avni_concepts):
    """Convert Bahmni concept_sets CSV to Avni form JSON with prefix."""
    child_columns = get_child_columns(concept_sets_df)
    
    # Create concept lookup by prefixed name
    concept_map = {c["name"]: c for c in avni_concepts}
    
    # Find main form (ConvSet)
    main_form_row = concept_sets_df[concept_sets_df["class"] == "ConvSet"].iloc[0]
    original_form_name = main_form_row["name"]
    prefixed_form_name = f"{PREFIX}{original_form_name}"
    
    # Get sections (children of main form)
    sections_in_form = []
    for col in child_columns:
        child = main_form_row[col]
        if child and str(child).strip():
            sections_in_form.append(child.strip())
    
    print(f"Found main form: '{original_form_name}' with {len(sections_in_form)} sections.")
    
    # Build form element groups
    form_element_groups = []
    
    for i, section_name in enumerate(sections_in_form):
        prefixed_section_name = f"{PREFIX}{section_name}"
        
        # Find section in concept_sets to get its children
        section_row = concept_sets_df[concept_sets_df["name"] == section_name]
        
        if not section_row.empty:
            # Section is a concept set with children
            concepts_in_section = []
            for col in child_columns:
                child = section_row.iloc[0][col]
                if child and str(child).strip():
                    concepts_in_section.append(child.strip())
        else:
            # Section is a standalone concept
            concepts_in_section = [section_name]
        
        # Build form elements for this section
        form_elements = []
        for j, concept_name in enumerate(concepts_in_section):
            prefixed_concept_name = f"{PREFIX}{concept_name}"
            
            if prefixed_concept_name in concept_map:
                concept_details = concept_map[prefixed_concept_name]
                
                # Map datatype to form element type
                element_type_map = {
                    "Coded": "SingleSelect",
                    "Numeric": "SingleSelect",
                    "Date": "SingleSelect",
                    "Text": "SingleSelect",
                    "NA": "SingleSelect"
                }
                element_type = element_type_map.get(concept_details["dataType"], "SingleSelect")
                
                form_elements.append({
                    "name": prefixed_concept_name,
                    "uuid": str(uuid.uuid4()),
                    "keyValues": [{"key": "editable", "value": False}],  # Read-only by default for Bahmni data
                    "concept": concept_details,
                    "displayOrder": float(j + 1),
                    "type": element_type,
                    "mandatory": False
                })
        
        form_element_groups.append({
            "uuid": str(uuid.uuid4()),
            "name": prefixed_section_name,
            "displayOrder": float(i + 1),
            "formElements": form_elements,
            "timed": False
        })
    
    avni_form = {
        "name": prefixed_form_name,
        "uuid": main_form_row["uuid"],
        "formType": "ProgramEnrolment",
        "formElementGroups": form_element_groups,
        "decisionRule": "",
        "visitScheduleRule": "",
        "validationRule": "",
        "checklistsRule": "",
        "decisionConcepts": []
    }
    
    return avni_form, prefixed_form_name

def main():
    print("=" * 60)
    print("Bahmni to Avni Form Converter")
    print("=" * 60)
    
    # Ensure output directories exist
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    os.makedirs(f"{OUTPUT_DIR}/forms", exist_ok=True)
    
    # Load source data
    print("\n[Step 1] Loading Bahmni CSV files...")
    concepts_df, concept_sets_df = load_bahmni_data()
    print(f"  - Loaded {len(concepts_df)} rows from concepts.csv")
    print(f"  - Loaded {len(concept_sets_df)} rows from concept_sets.csv")
    
    # Create concepts.json
    print("\n[Step 2] Creating concepts.json with prefix...")
    avni_concepts, concept_uuid_map = create_concepts_json(concepts_df)
    
    concepts_path = f"{OUTPUT_DIR}/concepts.json"
    with open(concepts_path, "w") as f:
        json.dump(avni_concepts, f, indent=2)
    print(f"  - Created {concepts_path} with {len(avni_concepts)} concepts")
    
    # Create form JSON
    print("\n[Step 3] Creating form JSON with prefix...")
    avni_form, form_name = create_form_json(concept_sets_df, concept_uuid_map, avni_concepts)
    
    # Create safe filename
    safe_filename = form_name.replace(", ", "_").replace(" ", "_")
    form_path = f"{OUTPUT_DIR}/forms/{safe_filename}.json"
    with open(form_path, "w") as f:
        json.dump(avni_form, f, indent=2)
    print(f"  - Created {form_path}")
    
    print("\n" + "=" * 60)
    print("Conversion complete!")
    print("=" * 60)
    print(f"\nOutput files:")
    print(f"  - {concepts_path}")
    print(f"  - {form_path}")
    print(f"\nNote: To make a form element read-only, add this to its keyValues:")
    print('  {"key": "editable", "value": false}')

if __name__ == "__main__":
    main()
