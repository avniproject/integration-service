#!/bin/bash
# =============================================================================
# Bahmni Metadata Extraction Script
# =============================================================================
# Purpose: Extract Bahmni metadata for analysis, integration, and documentation
# Compatible with MySQL 5.6 (no JSON_ARRAYAGG)
#
# Usage:
#   ./extract-bahmni-metadata.sh [options]
#
# Options:
#   --all           Extract all metadata (default)
#   --forms         Extract forms only
#   --concepts      Extract concepts only
#   --encounters    Extract encounter types only
#   --locations     Extract locations only
#   --identifiers   Extract patient identifier types only
#   --config        Extract jss-config app configuration
#   --output DIR    Output directory (default: ./bahmni-metadata)
#   --help          Show this help message
# =============================================================================

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_header() { echo -e "\n${BLUE}════════════════════════════════════════════════════════════${NC}"; echo -e "${BLUE}  $1${NC}"; echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}\n"; }
log_subheader() { echo -e "\n${CYAN}── $1 ──${NC}\n"; }

# =============================================================================
# Configuration
# =============================================================================
PRERELEASE_HOST="jss-bahmni-prerelease.avniproject.org"
SSH_KEY="$HOME/.ssh/openchs-infra.pem"
SSH_USER="ubuntu"
DB_CONTAINER="bahmni-docker-openmrsdb-1"
DB_USER="openmrs_admin"
DB_PASSWORD="OpenMRS_JSS2024"
DB_NAME="openmrs"
JSS_CONFIG_PATH="/home/ubuntu/bahmni-docker/jss-config"

OUTPUT_DIR="./bahmni-metadata"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Extraction flags
EXTRACT_ALL=true
EXTRACT_FORMS=false
EXTRACT_CONCEPTS=false
EXTRACT_ENCOUNTERS=false
EXTRACT_LOCATIONS=false
EXTRACT_IDENTIFIERS=false
EXTRACT_CONFIG=false

# =============================================================================
# Helper Functions
# =============================================================================

# Convert TSV output to JSON array
tsv_to_json() {
  local input_file="$1"
  local output_file="$2"
  
  # Read header line to get column names
  local header=$(head -1 "$input_file")
  local columns=($(echo "$header" | tr '\t' '\n'))
  
  # Build JSON array
  echo "[" > "$output_file"
  local first=true
  tail -n +2 "$input_file" | while IFS=$'\t' read -r line; do
    if [ "$first" = true ]; then
      first=false
    else
      echo "," >> "$output_file"
    fi
    
    # Build JSON object
    echo -n "  {" >> "$output_file"
    local i=0
    local values=($(echo "$line" | tr '\t' '\n'))
    local obj_first=true
    for col in "${columns[@]}"; do
      if [ "$obj_first" = true ]; then
        obj_first=false
      else
        echo -n ", " >> "$output_file"
      fi
      local val="${values[$i]:-}"
      # Escape quotes and handle NULL
      val=$(echo "$val" | sed 's/"/\\"/g')
      if [ "$val" = "NULL" ] || [ -z "$val" ]; then
        echo -n "\"$col\": null" >> "$output_file"
      elif [[ "$val" =~ ^[0-9]+$ ]]; then
        echo -n "\"$col\": $val" >> "$output_file"
      else
        echo -n "\"$col\": \"$val\"" >> "$output_file"
      fi
      ((i++))
    done
    echo -n "}" >> "$output_file"
  done
  echo "" >> "$output_file"
  echo "]" >> "$output_file"
}

# Run query and save as JSON using Python for reliable conversion
run_query_json() {
  local query="$1"
  local output_file="$2"
  local temp_file="${output_file}.tsv"
  
  # Run query with headers
  ssh -i "$SSH_KEY" "$SSH_USER@$PRERELEASE_HOST" \
    "docker exec $DB_CONTAINER mysql -u $DB_USER -p$DB_PASSWORD $DB_NAME -e \"$query\"" \
    2>/dev/null > "$temp_file"
  
  # Convert to JSON using Python with latin-1 encoding for special characters
  python3 << EOF
import csv
import json
import sys

try:
    with open('$temp_file', 'r', encoding='latin-1') as f:
        reader = csv.DictReader(f, delimiter='\t')
        rows = []
        for row in reader:
            clean_row = {}
            for k, v in row.items():
                if v == 'NULL' or v == '' or v is None:
                    clean_row[k] = None
                elif v.isdigit():
                    clean_row[k] = int(v)
                else:
                    try:
                        clean_row[k] = float(v)
                    except:
                        clean_row[k] = v
            rows.append(clean_row)
        
        with open('$output_file', 'w', encoding='utf-8') as out:
            json.dump(rows, out, indent=2, ensure_ascii=False)
        print(len(rows))
except Exception as e:
    print(f"Error: {e}", file=sys.stderr)
    with open('$output_file', 'w') as out:
        json.dump([], out)
    print(0)
EOF
  
  rm -f "$temp_file"
}

# =============================================================================
# Parse Arguments
# =============================================================================

while [[ $# -gt 0 ]]; do
  case $1 in
    --all) EXTRACT_ALL=true; shift ;;
    --forms) EXTRACT_ALL=false; EXTRACT_FORMS=true; shift ;;
    --concepts) EXTRACT_ALL=false; EXTRACT_CONCEPTS=true; shift ;;
    --encounters) EXTRACT_ALL=false; EXTRACT_ENCOUNTERS=true; shift ;;
    --locations) EXTRACT_ALL=false; EXTRACT_LOCATIONS=true; shift ;;
    --identifiers) EXTRACT_ALL=false; EXTRACT_IDENTIFIERS=true; shift ;;
    --config) EXTRACT_ALL=false; EXTRACT_CONFIG=true; shift ;;
    --output) OUTPUT_DIR="$2"; shift 2 ;;
    --help) head -20 "$0" | tail -15; exit 0 ;;
    *) log_error "Unknown option: $1"; exit 1 ;;
  esac
done

if [ "$EXTRACT_ALL" = true ]; then
  EXTRACT_FORMS=true
  EXTRACT_CONCEPTS=true
  EXTRACT_ENCOUNTERS=true
  EXTRACT_LOCATIONS=true
  EXTRACT_IDENTIFIERS=true
  EXTRACT_CONFIG=true
fi

# =============================================================================
# Setup
# =============================================================================

log_header "Bahmni Metadata Extraction"
log_info "Target: $PRERELEASE_HOST"
log_info "Output: $OUTPUT_DIR"
log_info "Timestamp: $TIMESTAMP"

mkdir -p "$OUTPUT_DIR"/{forms,concepts,encounter_types,locations,patient_identifiers,app_config}

# Test connections
log_info "Testing SSH connection..."
ssh -i "$SSH_KEY" -o ConnectTimeout=10 "$SSH_USER@$PRERELEASE_HOST" "echo OK" > /dev/null 2>&1 || { log_error "SSH failed"; exit 1; }
log_info "SSH connection successful"

log_info "Testing database connection..."
ssh -i "$SSH_KEY" "$SSH_USER@$PRERELEASE_HOST" \
  "docker exec $DB_CONTAINER mysql -u $DB_USER -p$DB_PASSWORD $DB_NAME -e 'SELECT 1'" > /dev/null 2>&1 || { log_error "DB failed"; exit 1; }
log_info "Database connection successful"

# =============================================================================
# Extract Forms
# =============================================================================

if [ "$EXTRACT_FORMS" = true ]; then
  log_header "Extracting Forms"
  
  log_subheader "Forms Definition"
  COUNT=$(run_query_json "SELECT form_id, name, uuid, version, encounter_type as encounter_type_id, published, retired FROM form WHERE retired = 0" "$OUTPUT_DIR/forms/forms.json")
  log_info "Forms extracted: $COUNT records"
  
  log_subheader "Form Fields"
  COUNT=$(run_query_json "SELECT ff.form_field_id, ff.form_id, ff.field_id, ff.field_number, ff.field_part, ff.page_number, ff.parent_form_field, ff.min_occurs, ff.max_occurs, ff.required, ff.sort_weight FROM form_field ff JOIN form f ON ff.form_id = f.form_id WHERE f.retired = 0" "$OUTPUT_DIR/forms/form_fields.json")
  log_info "Form fields extracted: $COUNT records"
  
  log_subheader "Observation Templates (ConvSet Concepts)"
  COUNT=$(run_query_json "SELECT c.concept_id, cn.name, c.uuid, cc.name as class, cd.name as datatype, c.is_set FROM concept c JOIN concept_name cn ON c.concept_id = cn.concept_id JOIN concept_class cc ON c.class_id = cc.concept_class_id JOIN concept_datatype cd ON c.datatype_id = cd.concept_datatype_id WHERE cc.name = 'ConvSet' AND cn.locale = 'en' AND cn.concept_name_type = 'FULLY_SPECIFIED' AND c.retired = 0" "$OUTPUT_DIR/forms/observation_templates.json")
  log_info "Observation templates extracted: $COUNT records"
fi

# =============================================================================
# Extract Concepts
# =============================================================================

if [ "$EXTRACT_CONCEPTS" = true ]; then
  log_header "Extracting Concepts"
  
  log_subheader "All Concepts"
  COUNT=$(run_query_json "SELECT c.concept_id, cn.name, c.uuid, cc.name as class, cd.name as datatype, c.is_set FROM concept c JOIN concept_name cn ON c.concept_id = cn.concept_id JOIN concept_class cc ON c.class_id = cc.concept_class_id JOIN concept_datatype cd ON c.datatype_id = cd.concept_datatype_id WHERE cn.locale = 'en' AND cn.concept_name_type = 'FULLY_SPECIFIED' AND c.retired = 0" "$OUTPUT_DIR/concepts/concepts.json")
  log_info "Concepts extracted: $COUNT records"
  
  log_subheader "Concept Sets (Parent-Child)"
  COUNT=$(run_query_json "SELECT cs.concept_set_id, cs.concept_id, cs.concept_set as parent_concept_id, cs.sort_weight, pcn.name as parent_name, ccn.name as child_name FROM concept_set cs JOIN concept pc ON cs.concept_set = pc.concept_id JOIN concept_name pcn ON pc.concept_id = pcn.concept_id AND pcn.locale = 'en' AND pcn.concept_name_type = 'FULLY_SPECIFIED' JOIN concept cc ON cs.concept_id = cc.concept_id JOIN concept_name ccn ON cc.concept_id = ccn.concept_id AND ccn.locale = 'en' AND ccn.concept_name_type = 'FULLY_SPECIFIED' WHERE pc.retired = 0 AND cc.retired = 0" "$OUTPUT_DIR/concepts/concept_sets.json")
  log_info "Concept sets extracted: $COUNT records"
  
  log_subheader "Concept Classes"
  COUNT=$(run_query_json "SELECT concept_class_id, name, uuid, description FROM concept_class WHERE retired = 0" "$OUTPUT_DIR/concepts/concept_classes.json")
  log_info "Concept classes extracted: $COUNT records"
  
  log_subheader "Concept Datatypes"
  COUNT=$(run_query_json "SELECT concept_datatype_id, name, uuid, description FROM concept_datatype WHERE retired = 0" "$OUTPUT_DIR/concepts/concept_datatypes.json")
  log_info "Concept datatypes extracted: $COUNT records"
  
  log_subheader "Concept Answers (Coded Values)"
  COUNT=$(run_query_json "SELECT ca.concept_answer_id, ca.concept_id, ca.answer_concept, ca.sort_weight, qcn.name as question_name, acn.name as answer_name FROM concept_answer ca JOIN concept qc ON ca.concept_id = qc.concept_id JOIN concept_name qcn ON qc.concept_id = qcn.concept_id AND qcn.locale = 'en' AND qcn.concept_name_type = 'FULLY_SPECIFIED' JOIN concept ac ON ca.answer_concept = ac.concept_id JOIN concept_name acn ON ac.concept_id = acn.concept_id AND acn.locale = 'en' AND acn.concept_name_type = 'FULLY_SPECIFIED' WHERE qc.retired = 0 AND ac.retired = 0" "$OUTPUT_DIR/concepts/concept_answers.json")
  log_info "Concept answers extracted: $COUNT records"
  
  log_subheader "Concept Numeric (Ranges)"
  COUNT=$(run_query_json "SELECT cn2.concept_id, cn.name, cn2.hi_absolute, cn2.hi_critical, cn2.hi_normal, cn2.low_absolute, cn2.low_critical, cn2.low_normal, cn2.units, cn2.precise FROM concept_numeric cn2 JOIN concept c ON cn2.concept_id = c.concept_id JOIN concept_name cn ON c.concept_id = cn.concept_id AND cn.locale = 'en' AND cn.concept_name_type = 'FULLY_SPECIFIED' WHERE c.retired = 0" "$OUTPUT_DIR/concepts/concept_numeric.json")
  log_info "Concept numeric ranges extracted: $COUNT records"
fi

# =============================================================================
# Extract Encounter Types
# =============================================================================

if [ "$EXTRACT_ENCOUNTERS" = true ]; then
  log_header "Extracting Encounter Types"
  
  COUNT=$(run_query_json "SELECT encounter_type_id, name, uuid, description FROM encounter_type WHERE retired = 0" "$OUTPUT_DIR/encounter_types/encounter_types.json")
  log_info "Encounter types extracted: $COUNT records"
  
  log_subheader "Visit Types"
  COUNT=$(run_query_json "SELECT visit_type_id, name, uuid, description FROM visit_type WHERE retired = 0" "$OUTPUT_DIR/encounter_types/visit_types.json")
  log_info "Visit types extracted: $COUNT records"
fi

# =============================================================================
# Extract Locations
# =============================================================================

if [ "$EXTRACT_LOCATIONS" = true ]; then
  log_header "Extracting Locations"
  
  COUNT=$(run_query_json "SELECT location_id, name, uuid, description, parent_location, address1, address2, city_village, state_province, country, postal_code FROM location WHERE retired = 0" "$OUTPUT_DIR/locations/locations.json")
  log_info "Locations extracted: $COUNT records"
  
  log_subheader "Location Tags"
  COUNT=$(run_query_json "SELECT location_tag_id, name, uuid, description FROM location_tag WHERE retired = 0" "$OUTPUT_DIR/locations/location_tags.json")
  log_info "Location tags extracted: $COUNT records"
  
  log_subheader "Location Tag Mappings"
  COUNT=$(run_query_json "SELECT ltm.location_id, l.name as location_name, ltm.location_tag_id, lt.name as tag_name FROM location_tag_map ltm JOIN location l ON ltm.location_id = l.location_id JOIN location_tag lt ON ltm.location_tag_id = lt.location_tag_id WHERE l.retired = 0 AND lt.retired = 0" "$OUTPUT_DIR/locations/location_tag_mappings.json")
  log_info "Location tag mappings extracted: $COUNT records"
fi

# =============================================================================
# Extract Patient Identifier Types
# =============================================================================

if [ "$EXTRACT_IDENTIFIERS" = true ]; then
  log_header "Extracting Patient Identifier Types"
  
  COUNT=$(run_query_json "SELECT patient_identifier_type_id, name, uuid, description, format, format_description, required, check_digit, validator FROM patient_identifier_type WHERE retired = 0" "$OUTPUT_DIR/patient_identifiers/identifier_types.json")
  log_info "Patient identifier types extracted: $COUNT records"
  
  log_subheader "Person Attribute Types"
  COUNT=$(run_query_json "SELECT person_attribute_type_id, name, uuid, description, format, searchable, sort_weight FROM person_attribute_type WHERE retired = 0" "$OUTPUT_DIR/patient_identifiers/person_attribute_types.json")
  log_info "Person attribute types extracted: $COUNT records"
fi

# =============================================================================
# Extract App Configuration (jss-config)
# =============================================================================

if [ "$EXTRACT_CONFIG" = true ]; then
  log_header "Extracting App Configuration (jss-config)"
  
  log_subheader "Clinical App Config"
  mkdir -p "$OUTPUT_DIR/app_config/clinical"
  scp -i "$SSH_KEY" "$SSH_USER@$PRERELEASE_HOST:$JSS_CONFIG_PATH/openmrs/apps/clinical/*.json" "$OUTPUT_DIR/app_config/clinical/" 2>/dev/null || log_warn "Could not copy clinical JSON"
  scp -i "$SSH_KEY" "$SSH_USER@$PRERELEASE_HOST:$JSS_CONFIG_PATH/openmrs/apps/clinical/*.js" "$OUTPUT_DIR/app_config/clinical/" 2>/dev/null || true
  log_info "Clinical config extracted"
  
  log_subheader "Registration App Config"
  mkdir -p "$OUTPUT_DIR/app_config/registration"
  scp -i "$SSH_KEY" "$SSH_USER@$PRERELEASE_HOST:$JSS_CONFIG_PATH/openmrs/apps/registration/*.json" "$OUTPUT_DIR/app_config/registration/" 2>/dev/null || log_warn "Could not copy registration JSON"
  log_info "Registration config extracted"
  
  log_subheader "Order Templates"
  mkdir -p "$OUTPUT_DIR/app_config/ordertemplates"
  scp -i "$SSH_KEY" "$SSH_USER@$PRERELEASE_HOST:$JSS_CONFIG_PATH/openmrs/ordertemplates/*" "$OUTPUT_DIR/app_config/ordertemplates/" 2>/dev/null || log_warn "Could not copy order templates"
  log_info "Order templates extracted"
  
  log_subheader "Reports Config"
  mkdir -p "$OUTPUT_DIR/app_config/reports"
  scp -r -i "$SSH_KEY" "$SSH_USER@$PRERELEASE_HOST:$JSS_CONFIG_PATH/openmrs/apps/reports/*" "$OUTPUT_DIR/app_config/reports/" 2>/dev/null || log_warn "Could not copy reports"
  log_info "Reports config extracted"
  
  log_subheader "Home App Config"
  mkdir -p "$OUTPUT_DIR/app_config/home"
  scp -i "$SSH_KEY" "$SSH_USER@$PRERELEASE_HOST:$JSS_CONFIG_PATH/openmrs/apps/home/*.json" "$OUTPUT_DIR/app_config/home/" 2>/dev/null || log_warn "Could not copy home config"
  log_info "Home config extracted"
  
  log_subheader "ADT Config"
  mkdir -p "$OUTPUT_DIR/app_config/adt"
  scp -i "$SSH_KEY" "$SSH_USER@$PRERELEASE_HOST:$JSS_CONFIG_PATH/openmrs/apps/adt/*.json" "$OUTPUT_DIR/app_config/adt/" 2>/dev/null || log_warn "Could not copy ADT config"
  log_info "ADT config extracted"
fi

# =============================================================================
# Generate Extraction Report
# =============================================================================

log_header "Generating Extraction Report"

# Count records
FORMS_COUNT=$(jq 'length' "$OUTPUT_DIR/forms/forms.json" 2>/dev/null || echo "0")
OBS_TEMPLATES_COUNT=$(jq 'length' "$OUTPUT_DIR/forms/observation_templates.json" 2>/dev/null || echo "0")
CONCEPTS_COUNT=$(jq 'length' "$OUTPUT_DIR/concepts/concepts.json" 2>/dev/null || echo "0")
CONCEPT_SETS_COUNT=$(jq 'length' "$OUTPUT_DIR/concepts/concept_sets.json" 2>/dev/null || echo "0")
CONCEPT_ANSWERS_COUNT=$(jq 'length' "$OUTPUT_DIR/concepts/concept_answers.json" 2>/dev/null || echo "0")
ENCOUNTER_TYPES_COUNT=$(jq 'length' "$OUTPUT_DIR/encounter_types/encounter_types.json" 2>/dev/null || echo "0")
VISIT_TYPES_COUNT=$(jq 'length' "$OUTPUT_DIR/encounter_types/visit_types.json" 2>/dev/null || echo "0")
LOCATIONS_COUNT=$(jq 'length' "$OUTPUT_DIR/locations/locations.json" 2>/dev/null || echo "0")
IDENTIFIER_TYPES_COUNT=$(jq 'length' "$OUTPUT_DIR/patient_identifiers/identifier_types.json" 2>/dev/null || echo "0")
PERSON_ATTR_TYPES_COUNT=$(jq 'length' "$OUTPUT_DIR/patient_identifiers/person_attribute_types.json" 2>/dev/null || echo "0")

cat > "$OUTPUT_DIR/EXTRACTION_REPORT.md" << EOF
# Bahmni Metadata Extraction Report

**Generated:** $(date)
**Timestamp:** $TIMESTAMP
**Source:** $PRERELEASE_HOST

## Summary

| Category | Count |
|----------|-------|
| Forms | $FORMS_COUNT |
| Observation Templates | $OBS_TEMPLATES_COUNT |
| Concepts | $CONCEPTS_COUNT |
| Concept Sets | $CONCEPT_SETS_COUNT |
| Concept Answers | $CONCEPT_ANSWERS_COUNT |
| Encounter Types | $ENCOUNTER_TYPES_COUNT |
| Visit Types | $VISIT_TYPES_COUNT |
| Locations | $LOCATIONS_COUNT |
| Patient Identifier Types | $IDENTIFIER_TYPES_COUNT |
| Person Attribute Types | $PERSON_ATTR_TYPES_COUNT |

## Files Generated

### Forms
- \`forms/forms.json\` - Form definitions
- \`forms/form_fields.json\` - Form field mappings
- \`forms/observation_templates.json\` - Observation templates (ConvSet concepts)

### Concepts
- \`concepts/concepts.json\` - All concepts with class and datatype
- \`concepts/concept_sets.json\` - Parent-child concept relationships
- \`concepts/concept_classes.json\` - Concept class definitions
- \`concepts/concept_datatypes.json\` - Concept datatype definitions
- \`concepts/concept_answers.json\` - Coded answer options
- \`concepts/concept_numeric.json\` - Numeric ranges and units

### Encounter Types
- \`encounter_types/encounter_types.json\` - Encounter type definitions
- \`encounter_types/visit_types.json\` - Visit type definitions

### Locations
- \`locations/locations.json\` - Location hierarchy
- \`locations/location_tags.json\` - Location tag definitions
- \`locations/location_tag_mappings.json\` - Location-tag associations

### Patient Identifiers
- \`patient_identifiers/identifier_types.json\` - Patient identifier type definitions
- \`patient_identifiers/person_attribute_types.json\` - Person attribute type definitions

### App Configuration (jss-config)
- \`app_config/clinical/\` - Clinical app configuration
- \`app_config/registration/\` - Registration app configuration
- \`app_config/ordertemplates/\` - Order templates
- \`app_config/reports/\` - Report definitions
- \`app_config/home/\` - Home app configuration
- \`app_config/adt/\` - ADT configuration

## Usage Examples

### View Forms
\`\`\`bash
jq '.[] | {name, uuid, encounter_type_id}' forms/forms.json
\`\`\`

### View Concepts by Class
\`\`\`bash
jq '.[] | select(.class == "Test") | {name, uuid}' concepts/concepts.json
\`\`\`

### View Encounter Types
\`\`\`bash
jq '.[] | {name, uuid, description}' encounter_types/encounter_types.json
\`\`\`

### View Concept Hierarchy
\`\`\`bash
jq '.[] | {parent: .parent_name, child: .child_name}' concepts/concept_sets.json
\`\`\`

## Integration with Avni

### Key Mappings Required

1. **Patient Identifier** → Avni Subject ID (GAN ID)
2. **Encounter Types** → Avni Encounter Types
3. **Concepts** → Avni Concepts
4. **Locations** → Avni Catchments

---
**Report Generated:** $(date)
EOF

log_info "Extraction report saved"

# =============================================================================
# Final Summary
# =============================================================================

log_header "Extraction Complete"

echo -e "${GREEN}Summary:${NC}"
echo "  Forms: $FORMS_COUNT"
echo "  Observation Templates: $OBS_TEMPLATES_COUNT"
echo "  Concepts: $CONCEPTS_COUNT"
echo "  Concept Sets: $CONCEPT_SETS_COUNT"
echo "  Encounter Types: $ENCOUNTER_TYPES_COUNT"
echo "  Locations: $LOCATIONS_COUNT"
echo "  Patient Identifier Types: $IDENTIFIER_TYPES_COUNT"
echo ""
echo -e "${GREEN}Output Directory:${NC} $OUTPUT_DIR"
echo ""
echo -e "${GREEN}Next:${NC} Review metadata and create Avni integration mappings"
