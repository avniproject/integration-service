# Avni Forms Extraction Process

## Overview
Documented process for extracting Avni forms data from database and converting to Bahmni-compatible CSV format.

## Data Sources
- **Avni Database**: Direct SQL queries to extract form structures
- **Avni Concept Mapping**: CSV with concept-UUID mappings for coded answers

## SQL Queries Used

### 1. Forms and Form Elements Query
```sql
set role jsscp;

select
    f.name  as FormName,
    f.uuid as FormUUID,
    feg.name as FormElementGroup,
    feg.uuid as FormElementGroupUUID,
    fe.name as "M  FormElement",
    c.name as Concept,
    c.data_type as Datatype,
    c.uuid as ConceptUUID
from form f
         inner join form_element_group feg on feg.form_id = f.id
         inner join form_element fe on fe.form_element_group_id = feg.id
         inner join concept c on fe.concept_id = c.id
         inner join organisation co on co.id = c.organisation_id
         inner join organisation feo on feo.id = fe.organisation_id
         left join concept_answer ca on ca.concept_id = c.id
         left join concept a on ca.answer_concept_id = a.id
         left join non_applicable_form_element rem
                   on rem.form_element_id = fe.id
                       and rem.is_voided <> true
where rem.id is null
  and f.name in
      (
                 -- List of 90+ clinical forms...
      )
and f.is_voided = false
order by
    f.name,
    feg.display_order asc,
    fe.display_order asc;
```

**Purpose**: Extracts all form structures, element groups, individual form elements, concepts, and their UUIDs from Avni database.

**Key Features**:
- Extracts all concepts regardless of datatype (Numeric, Date, Text, Coded)
- Includes comprehensive list of 90+ clinical forms
- Excludes voided/non-applicable elements
- Preserves display order for proper hierarchy
- Joins form → form_element_group → form_element → concept relationships

### 2. Concept Answers Query (Coded Concepts Only)
```sql
set role jsscp;

select
    c.name as concept,
    c.uuid as conceptuuid,
    a.name as conceptanswer,
    a.uuid as conceptansweruuid
from concept_answer ca
         inner join concept c on ca.concept_id = c.id
         inner join concept a on ca.answer_concept_id = a.id
         inner join organisation co on co.id = c.organisation_id
where c.data_type = 'Coded'
  and c.name in (
      -- List of concepts from forms query above
      select distinct c.name
      from form f
               inner join form_element_group feg on feg.form_id = f.id
               inner join form_element fe on fe.form_element_group_id = feg.id
               inner join concept c on fe.concept_id = c.id
               inner join organisation co on co.id = c.organisation_id
               left join non_applicable_form_element rem
                         on rem.form_element_id = fe.id
                             and rem.is_voided <> true
      where rem.id is null
        and f.name in (/* same form list as above */)
        and f.is_voided = false
  )
order by c.name, ca.answer_order;
```

**Purpose**: Extracts concept answers and their UUIDs for all coded concepts from specified forms.

**Key Features**:
- Filters for coded concepts only (c.data_type = 'Coded')
- Provides answer concepts and their UUIDs
- Maintains answer order for proper display
- References same form list as main query

## Output Files Generated
- **Avni forms dump.csv**: Raw extraction from database (4,200+ rows)
- **Avni Concept Mapping.csv**: Concept-UUID mappings for coded answers

## Reference Files
- **ANC Example**: See `02-ANC-to-Bahmni-Conversion-Process.md` for practical conversion example
- **CSV Structure Guide**: See `03-Bahmni-CSV-Structure-Guide.md` for format requirements
- **Complete Workflow**: See `04-Complete-Workflow-Summary.md` for end-to-end process
