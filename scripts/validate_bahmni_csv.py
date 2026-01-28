#!/usr/bin/env python3
"""
Validation Script for Bahmni CSV Generation
Prevents common issues encountered during ANC form conversion
"""

import pandas as pd
import json
import sys
import os

def load_existing_bahmni_concepts():
    """Load existing Bahmni concepts to check for conflicts"""
    concepts_file = "scripts/aws/bahmni-metadata/concepts/concepts.json"
    if os.path.exists(concepts_file):
        with open(concepts_file, 'r') as f:
            bahmni_concepts = json.load(f)
        
        existing_names = set()
        for concept in bahmni_concepts:
            if isinstance(concept.get('name'), str):
                existing_names.add(concept['name'])
        return existing_names
    return set()

def validate_concepts_csv(concepts_file, existing_bahmni_names):
    """Validate concepts.csv for common issues"""
    print(f"🔍 Validating {concepts_file}...")
    
    try:
        df = pd.read_csv(concepts_file, keep_default_na=False)
        issues = []
        
        # Check 1: Duplicate UUIDs
        duplicate_uuids = df[df.duplicated(subset=['uuid'], keep=False)]
        if not duplicate_uuids.empty:
            issues.append(f"❌ Duplicate UUIDs found: {len(duplicate_uuids)} rows")
        
        # Check 2: Duplicate names
        duplicate_names = df[df.duplicated(subset=['name'], keep=False)]
        if not duplicate_names.empty:
            issues.append(f"❌ Duplicate names found: {len(duplicate_names)} rows")
        
        # Check 3: Missing datatypes (handle various null representations)
        df['datatype'] = df['datatype'].astype(str).replace('nan', 'N/A')
        missing_datatypes = df[(df['datatype'] == '') | (df['datatype'] == ' ') | (df['datatype'] == 'nan')]
        if not missing_datatypes.empty:
            issues.append(f"❌ Missing datatypes: {len(missing_datatypes)} rows")
        
        # Check 4: Missing required columns
        required_columns = ['uuid', 'name', 'datatype', 'class']
        missing_columns = [col for col in required_columns if col not in df.columns]
        if missing_columns:
            issues.append(f"❌ Missing required columns: {missing_columns}")
        
        # Check 5: Avni prefix presence
        if 'name' in df.columns:
            non_prefixed = df[~df['name'].str.contains(('Avni - |AVNI - JSS'), na=False)]
            if not non_prefixed.empty:
                issues.append(f"⚠️  Names without proper prefix: {len(non_prefixed)} rows")
        
        # Check 6: Conflicts with existing Bahmni concepts
        conflicts = []
        for name in df['name']:
            base_name = name.replace('Avni - ', '')
            if base_name in existing_bahmni_names:
                conflicts.append(name)
        
        if conflicts:
            issues.append(f"❌ Conflicts with existing Bahmni concepts: {len(conflicts)} names")
            for conflict in conflicts:
                issues.append(f"   - {conflict} (conflicts with '{conflict.replace('Avni - ', '')}')")
        
        # Check 7: CRITICAL - No Concept Sets allowed in concepts.csv
        concept_sets_in_concepts = df[df['class'] == 'ConvSet']
        if not concept_sets_in_concepts.empty:
            issues.append(f"🚨 CRITICAL: Found {len(concept_sets_in_concepts)} concept sets in concepts.csv")
            issues.append("   concepts.csv should ONLY contain individual concepts (class: Misc)")
            issues.append("   Move all concept sets (class: ConvSet) to concept_sets.csv")
        
        if issues:
            print("❌ Issues found:")
            for issue in issues:
                print(f"  {issue}")
            return False
        else:
            print("✅ Concepts CSV validation passed!")
            return True
            
    except Exception as e:
        print(f"❌ Error reading concepts file: {e}")
        return False

def validate_concept_sets_csv(concept_sets_file, existing_bahmni_names):
    """Validate concept_sets.csv for common issues"""
    print(f"🔍 Validating {concept_sets_file}...")
    
    try:
        df = pd.read_csv(concept_sets_file, keep_default_na=False)
        issues = []
        
        # Check 1: Duplicate UUIDs
        duplicate_uuids = df[df.duplicated(subset=['uuid'], keep=False)]
        if not duplicate_uuids.empty:
            issues.append(f"❌ Duplicate UUIDs found: {len(duplicate_uuids)} rows")
        
        # Check 2: Duplicate names (CRITICAL)
        duplicate_names = df[df.duplicated(subset=['name'], keep=False)]
        if not duplicate_names.empty:
            issues.append(f"❌ CRITICAL: Duplicate names found: {len(duplicate_names)} rows")
            print(f"   Duplicate names: {duplicate_names['name'].unique()}")
        
        # Check 3: Missing required columns
        required_columns = ['uuid', 'name', 'class']
        missing_columns = [col for col in required_columns if col not in df.columns]
        if missing_columns:
            issues.append(f"❌ Missing required columns: {missing_columns}")
        
        # Check 4: Avni prefix presence
        if 'name' in df.columns:
            non_prefixed = df[~df['name'].str.startswith('Avni - ', na=False)]
            if not non_prefixed.empty:
                issues.append(f"⚠️  Names without 'Avni - ' prefix: {len(non_prefixed)} rows")
        
        # Check 5: Empty child columns (potential duplicates)
        child_columns = [col for col in df.columns if col.startswith('child.')]
        for col in child_columns:
            empty_children = df[df[col].isna() | (df[col] == '') | (df[col] == ' ')]
            if len(empty_children) == len(df):
                issues.append(f"⚠️  Column {col} is completely empty")
        
        # Check 6: Conflicts with existing Bahmni concepts
        conflicts = []
        for name in df['name']:
            base_name = name.replace('Avni - ', '')
            if base_name in existing_bahmni_names:
                conflicts.append(name)
        
        if conflicts:
            issues.append(f"❌ Conflicts with existing Bahmni concepts: {len(conflicts)} names")
            for conflict in conflicts:
                issues.append(f"   - {conflict} (conflicts with '{conflict.replace('Avni - ', '')}')")
        
        if issues:
            print("❌ Issues found:")
            for issue in issues:
                print(f"  {issue}")
            return False
        else:
            print("✅ Concept Sets CSV validation passed!")
            return True
            
    except Exception as e:
        print(f"❌ Error reading concept sets file: {e}")
        return False

def main():
    """Main validation function"""
    if len(sys.argv) != 3:
        print("Usage: python validate_bahmni_csv.py <concepts.csv> <concept_sets.csv>")
        sys.exit(1)
    
    concepts_file = sys.argv[1]
    concept_sets_file = sys.argv[2]
    
    print("🚀 Bahmni CSV Validation Started")
    print("=" * 50)
    
    # Load existing Bahmni concepts for conflict detection
    print("📋 Loading existing Bahmni concepts...")
    existing_bahmni_names = load_existing_bahmni_concepts()
    print(f"   Found {len(existing_bahmni_names)} existing concepts")
    
    concepts_valid = validate_concepts_csv(concepts_file, existing_bahmni_names)
    concept_sets_valid = validate_concept_sets_csv(concept_sets_file, existing_bahmni_names)
    
    print("=" * 50)
    if concepts_valid and concept_sets_valid:
        print("🎉 All validations passed! Ready for Bahmni upload.")
        sys.exit(0)
    else:
        print("❌ Validation failed! Please fix issues before uploading.")
        sys.exit(1)

if __name__ == "__main__":
    main()
