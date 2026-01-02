# Bahmni Metadata Extraction Report

**Generated:** Fri Jan  2 20:08:12 IST 2026
**Timestamp:** 20260102_200734
**Source:** jss-bahmni-prerelease.avniproject.org

## Summary

| Category | Count |
|----------|-------|
| Forms | 3 |
| Observation Templates | 82 |
| Concepts | 10262 |
| Concept Sets | 5329 |
| Concept Answers | 6349 |
| Encounter Types | 10 |
| Visit Types | 7 |
| Locations | 74 |
| Patient Identifier Types | 5 |
| Person Attribute Types | 35 |

## Files Generated

### Forms
- `forms/forms.json` - Form definitions
- `forms/form_fields.json` - Form field mappings
- `forms/observation_templates.json` - Observation templates (ConvSet concepts)

### Concepts
- `concepts/concepts.json` - All concepts with class and datatype
- `concepts/concept_sets.json` - Parent-child concept relationships
- `concepts/concept_classes.json` - Concept class definitions
- `concepts/concept_datatypes.json` - Concept datatype definitions
- `concepts/concept_answers.json` - Coded answer options
- `concepts/concept_numeric.json` - Numeric ranges and units

### Encounter Types
- `encounter_types/encounter_types.json` - Encounter type definitions
- `encounter_types/visit_types.json` - Visit type definitions

### Locations
- `locations/locations.json` - Location hierarchy
- `locations/location_tags.json` - Location tag definitions
- `locations/location_tag_mappings.json` - Location-tag associations

### Patient Identifiers
- `patient_identifiers/identifier_types.json` - Patient identifier type definitions
- `patient_identifiers/person_attribute_types.json` - Person attribute type definitions

### App Configuration (jss-config)
- `app_config/clinical/` - Clinical app configuration
- `app_config/registration/` - Registration app configuration
- `app_config/ordertemplates/` - Order templates
- `app_config/reports/` - Report definitions
- `app_config/home/` - Home app configuration
- `app_config/adt/` - ADT configuration

## Usage Examples

### View Forms
```bash
jq '.[] | {name, uuid, encounter_type_id}' forms/forms.json
```

### View Concepts by Class
```bash
jq '.[] | select(.class == "Test") | {name, uuid}' concepts/concepts.json
```

### View Encounter Types
```bash
jq '.[] | {name, uuid, description}' encounter_types/encounter_types.json
```

### View Concept Hierarchy
```bash
jq '.[] | {parent: .parent_name, child: .child_name}' concepts/concept_sets.json
```

## Integration with Avni

### Key Mappings Required

1. **Patient Identifier** → Avni Subject ID (GAN ID)
2. **Encounter Types** → Avni Encounter Types
3. **Concepts** → Avni Concepts
4. **Locations** → Avni Catchments

---
**Report Generated:** Fri Jan  2 20:08:12 IST 2026
