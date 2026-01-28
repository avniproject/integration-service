#!/usr/bin/env python3
"""
Generate a consolidated CSV from Bahmni All Observation Templates export.
Creates one row per concept with form, section, and hierarchy information.
"""

import pandas as pd
import os

SOURCE_DIR = "CSV dumps/All Observation Templates"
OUTPUT_FILE = "CSV dumps/All_Bahmni_Obs_Summary.csv"

def get_child_columns(df):
    """Get all child column names from a dataframe."""
    return [col for col in df.columns if col.startswith("child.")]

def load_data():
    """Load the Bahmni CSV export files."""
    concepts_df = pd.read_csv(f"{SOURCE_DIR}/concepts.csv", keep_default_na=False)
    concept_sets_df = pd.read_csv(f"{SOURCE_DIR}/concept_sets.csv", keep_default_na=False)
    return concepts_df, concept_sets_df

def build_concept_lookup(concepts_df):
    """Build a lookup dictionary for concepts by name."""
    concept_lookup = {}
    for _, row in concepts_df.iterrows():
        name = row["name"].strip() if row["name"] else ""
        if name and name not in concept_lookup:
            concept_lookup[name] = {
                "uuid": row["uuid"],
                "datatype": row["datatype"],
                "class": row["class"],
                "shortname": row["shortname"] if row["shortname"] else ""
            }
    return concept_lookup

def build_hierarchy(concept_sets_df):
    """Build parent-child relationships from concept_sets."""
    child_columns = get_child_columns(concept_sets_df)
    
    # Map: child_name -> list of parent info
    child_to_parents = {}
    
    # Map: concept_set_name -> its info
    concept_set_info = {}
    
    for _, row in concept_sets_df.iterrows():
        parent_name = row["name"].strip() if row["name"] else ""
        parent_uuid = row["uuid"]
        parent_class = row["class"]
        
        if not parent_name:
            continue
            
        concept_set_info[parent_name] = {
            "uuid": parent_uuid,
            "class": parent_class,
            "shortname": row["shortname"] if row["shortname"] else ""
        }
        
        for col in child_columns:
            child = row[col]
            if child and str(child).strip():
                child_name = child.strip()
                if child_name not in child_to_parents:
                    child_to_parents[child_name] = []
                child_to_parents[child_name].append({
                    "parent_name": parent_name,
                    "parent_uuid": parent_uuid,
                    "parent_class": parent_class
                })
    
    return child_to_parents, concept_set_info

def find_form_for_concept(concept_name, child_to_parents, concept_set_info, visited=None):
    """Recursively find the form (ConvSet) that a concept belongs to."""
    if visited is None:
        visited = set()
    
    if concept_name in visited:
        return None, []
    visited.add(concept_name)
    
    if concept_name not in child_to_parents:
        # Check if concept itself is a ConvSet
        if concept_name in concept_set_info and concept_set_info[concept_name]["class"] == "ConvSet":
            return concept_name, []
        return None, []
    
    parents = child_to_parents[concept_name]
    for parent_info in parents:
        parent_name = parent_info["parent_name"]
        if parent_info["parent_class"] == "ConvSet":
            return parent_name, [parent_name]
        else:
            form, path = find_form_for_concept(parent_name, child_to_parents, concept_set_info, visited)
            if form:
                return form, [parent_name] + path
    
    return None, []

def main():
    print("=" * 60)
    print("Generating Concept Summary CSV")
    print("=" * 60)
    
    # Load data
    print("\n[Step 1] Loading data...")
    concepts_df, concept_sets_df = load_data()
    print(f"  - Loaded {len(concepts_df)} concepts")
    print(f"  - Loaded {len(concept_sets_df)} concept sets")
    
    # Build lookups
    print("\n[Step 2] Building hierarchy...")
    concept_lookup = build_concept_lookup(concepts_df)
    child_to_parents, concept_set_info = build_hierarchy(concept_sets_df)
    
    # Generate rows
    print("\n[Step 3] Generating summary rows...")
    rows = []
    
    # Process each concept
    processed = set()
    for _, row in concepts_df.iterrows():
        concept_name = row["name"].strip() if row["name"] else ""
        if not concept_name or concept_name in processed:
            continue
        processed.add(concept_name)
        
        concept_uuid = row["uuid"]
        concept_datatype = row["datatype"]
        concept_class = row["class"]
        
        # Find the form and parent concept set
        form_name, path = find_form_for_concept(concept_name, child_to_parents, concept_set_info)
        
        # Get immediate parent (concept set)
        immediate_parent = ""
        immediate_parent_uuid = ""
        if concept_name in child_to_parents:
            parents = child_to_parents[concept_name]
            if parents:
                immediate_parent = parents[0]["parent_name"]
                immediate_parent_uuid = parents[0]["parent_uuid"]
        
        # Get form info
        form_uuid = ""
        form_class = ""
        if form_name and form_name in concept_set_info:
            form_uuid = concept_set_info[form_name]["uuid"]
            form_class = concept_set_info[form_name]["class"]
        
        rows.append({
            "Form Name": form_name or "",
            "Form UUID": form_uuid,
            "Form Type": form_class,
            "Concept Set (Section)": immediate_parent,
            "Concept Set UUID": immediate_parent_uuid,
            "Concept Name": concept_name,
            "Concept UUID": concept_uuid,
            "Datatype": concept_datatype,
            "Class": concept_class,
            "Hierarchy Path": " > ".join(reversed(path)) if path else ""
        })
    
    # Create DataFrame and save
    print(f"\n[Step 4] Saving to {OUTPUT_FILE}...")
    summary_df = pd.DataFrame(rows)
    
    # Sort by Form Name, then Concept Set, then Concept Name
    summary_df = summary_df.sort_values(["Form Name", "Concept Set (Section)", "Concept Name"])
    
    summary_df.to_csv(OUTPUT_FILE, index=False)
    
    print(f"  - Created {len(rows)} rows")
    
    # Print stats
    forms = summary_df["Form Name"].unique()
    forms = [f for f in forms if f]
    print(f"\n[Stats]")
    print(f"  - Total concepts: {len(rows)}")
    print(f"  - Unique forms found: {len(forms)}")
    print(f"  - Forms: {', '.join(sorted(forms)[:10])}{'...' if len(forms) > 10 else ''}")
    
    print("\n" + "=" * 60)
    print("Done!")
    print("=" * 60)

if __name__ == "__main__":
    main()
