#!/usr/bin/env python3
"""
Standard Prefix Script for Non-ANC Forms
Applies "AVNI - " prefix to all forms except ANC (which uses "AVNI - JSS")
"""

import pandas as pd
import sys
import os

def apply_standard_prefix(form_name, concepts_file, concept_sets_file):
    """Apply standard AVNI - prefix to form files"""
    
    print(f"🔄 Applying standard prefix to {form_name}...")
    
    # Load concepts file
    concepts_df = pd.read_csv(concepts_file, keep_default_na=False)
    
    # Update concept names
    def update_concept_name(name):
        if not name.startswith('AVNI - '):
            return f'AVNI - {name}'
        return name
    
    concepts_df['name'] = concepts_df['name'].apply(update_concept_name)
    concepts_df['shortname'] = concepts_df['shortname'].apply(update_concept_name)
    
    # Load concept sets file
    concept_sets_df = pd.read_csv(concept_sets_file, keep_default_na=False)
    
    # Update concept set names (except ANC which uses JSS)
    def update_concept_set_name(name):
        if 'ANC' in name:
            # Keep ANC-specific naming
            return name
        elif not name.startswith('AVNI - '):
            return f'AVNI - {name}'
        return name
    
    concept_sets_df['name'] = concept_sets_df['name'].apply(update_concept_set_name)
    concept_sets_df['shortname'] = concept_sets_df['shortname'].apply(update_concept_set_name)
    
    # Update child references in main form
    main_form_row = concept_sets_df.iloc[0].copy()
    child_columns = [col for col in concept_sets_df.columns if col.startswith('child.')]
    
    # Create mapping for updated concept set names
    section_mapping = dict(zip(concept_sets_df['name'], concept_sets_df['name']))
    
    # Update child references
    for col in child_columns:
        child_name = main_form_row[col]
        if child_name and child_name.strip():
            # Find updated name for this child
            for section_name in section_mapping:
                if child_name in section_name or section_name in child_name:
                    main_form_row[col] = section_mapping[section_name]
                    break
    
    concept_sets_df.iloc[0] = main_form_row
    
    # Save updated files
    concepts_df.to_csv(concepts_file, index=False)
    concept_sets_df.to_csv(concept_sets_file, index=False)
    
    print(f"✅ Applied AVNI - prefix to {form_name}")
    print(f"✅ Updated {len(concepts_df)} concepts")
    print(f"✅ Updated {len(concept_sets_df)} concept sets")
    print(f"✅ Files ready for Bahmni upload")

def main():
    if len(sys.argv) != 4:
        print("Usage: python apply_standard_prefix.py <form_name> <concepts.csv> <concept_sets.csv>")
        print("Example: python apply_standard_prefix.py 'General Visit' CSV_dumps/General_concepts.csv CSV_dumps/General_concept_sets.csv")
        sys.exit(1)
    
    form_name = sys.argv[1]
    concepts_file = sys.argv[2]
    concept_sets_file = sys.argv[3]
    
    if not os.path.exists(concepts_file):
        print(f"❌ Concepts file not found: {concepts_file}")
        sys.exit(1)
    
    if not os.path.exists(concept_sets_file):
        print(f"❌ Concept sets file not found: {concept_sets_file}")
        sys.exit(1)
    
    apply_standard_prefix(form_name, concepts_file, concept_sets_file)

if __name__ == "__main__":
    main()
