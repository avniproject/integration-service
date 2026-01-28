# JSS Avni-Bahmni Integration - Scoping Overview

## Project Scope
Integration between JSS Avni and Bahmni systems for individual/patient level entities.

## Data Sources
- **Avni Metadata**: `/scripts/aws/avni-metadata/` directory
- **Bahmni Metadata**: `/scripts/aws/bahmni-metadata/` directory

## Filtering Criteria Applied
1. **Individual/Patient Level Only**: Excluded group, household, village, SHG, and other non-individual subject types
2. **Active Entities Only**: Excluded voided (`"voided": true`) and retired (`"retired": 1`) entities
3. **Clinical Data Focus**: Excluded configuration-only metadata (dashboards, reports, roles, etc.)

## Entity Classification Rules
- **Registration**: Individual/Patient registration and program enrolment forms
- **Visit**: Routine visits, follow-ups, clinic visits (general encounters)
- **Program Encounter**: Encounters recorded as part of specific programs
- **Encounter**: Other clinical encounters not covered above

## Integration Identifier
- **Avni Bahmni JSS ID**: A shared identifier added to both Avni and Bahmni systems with the same ID value. This serves as the primary key for mapping and integrating records between the two systems.

## Process Documentation
For detailed implementation steps, see:
- **Process Documentation**: `/docs/Process-Documentation/`
  - `01-Avni-Forms-Extraction-Process.md` - SQL queries and data extraction
  - `02-ANC-to-Bahmni-Conversion-Process.md` - Practical conversion example
  - `03-Bahmni-CSV-Structure-Guide.md` - Bahmni format requirements
  - `04-Complete-Workflow-Summary.md` - End-to-end process
  - `05-Bahmni-to-Avni-Conversion-Process.md` - Bahmni to Avni conversion

## Entity Lists
- **Avni Entities**: See `/docs/JSS-Avni-Bahmni-Integration-Scoping.md` for complete entity lists
- **Bahmni Entities**: See `/docs/Bahmni-Forms-Concepts-Mapping.xlsx` for comprehensive mapping
