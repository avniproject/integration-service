# Bahmni CSV Structure Guide

## Overview
Required structure and format for Bahmni concepts and concept sets CSV files.

## concepts.csv Format
```csv
uuid,name,description,class,shortname,datatype,units,High Normal,Low Normal,synonym.1,answer.1,answer.2,answer.3,answer.4,reference-term-source,reference-term-code,reference-term-relationship
```

### Column Definitions
- **uuid**: Unique identifier for each concept (required)
- **name**: Concept display name (required)
- **description**: Concept description (optional)
- **class**: Concept class (ConvSet for sets, Misc for individuals)
- **shortname**: Short display name (required)
- **datatype**: Data type (Numeric, Date, Text, Coded, N/A)
- **units**: Measurement units (optional)
- **High Normal/Low Normal**: Reference ranges (optional)
- **synonym.1**: Alternative names (optional)
- **answer.1-4**: Answer options for coded concepts (optional)
- **reference-term-***: Terminology mappings (optional)

### Critical Rules
1. **No duplicate UUIDs** - Each UUID must be unique
2. **No duplicate concept names** - Each name must be unique across entire file
3. **Empty datatypes** must be filled with "N/A"
4. **Form/Section concepts** must have class="ConvSet" and datatype="N/A"
5. **Question concepts** must have class="Misc" and actual datatype
6. **Answer concepts** must have class="Misc" and datatype="N/A"

## concept_sets.csv Format
```csv
uuid,name,description,class,shortname,child.1,child.2,child.3,child.4,child.5,child.6,child.7,child.8,child.9,child.10,reference-term-source,reference-term-code,reference-term-relationship
```

### Column Definitions
- **uuid**: Unique identifier for relationship (required)
- **name**: Parent concept name (required)
- **description**: Relationship description (required)
- **class**: Always "Concept Details" (required)
- **shortname**: Short display name (required)
- **child.1-10**: Child concept names (up to 10 per row)
- **reference-term-***: Terminology mappings (optional)

### Critical Rules
1. **No duplicate concepts** - Each concept appears only once across all child columns
2. **Proper hierarchy** - Parent-child relationships must be logical
3. **Maximum children** - Maximum 10 child columns per row
4. **Empty children** - Unused child columns left blank
5. **Generate relationship UUIDs** for each row

## Upload Process
1. **Upload concepts.csv first** - All concepts and relationships
2. **Upload concept_sets.csv second** - Hierarchical structure
3. **Verify successful import** - Check form appears in Bahmni
4. **Test functionality** - Validate form behavior

## Reference Files
- **Extraction Process**: See `01-Avni-Forms-Extraction-Process.md` for SQL queries to get data
- **ANC Example**: See `02-ANC-to-Bahmni-Conversion-Process.md` for practical implementation
- **Complete Workflow**: See `04-Complete-Workflow-Summary.md` for end-to-end process
