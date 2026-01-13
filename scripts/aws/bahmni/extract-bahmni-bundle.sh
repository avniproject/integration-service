#!/bin/bash
# Extract Bahmni Bundle via OpenMRS REST API
# This script exports Bahmni configuration as a bundle that can be processed by integration service

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_header() { echo -e "\n${BLUE}=== $1 ===${NC}\n"; }

# Configuration
PRERELEASE_HOST="jss-bahmni-prerelease.avniproject.org"
OPENMRS_URL="https://$PRERELEASE_HOST/openmrs"
OPENMRS_USER="admin"
OPENMRS_PASSWORD="Morkhade123"
OUTPUT_DIR="./bahmni-bundles"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Test API connectivity first
test_api_connection() {
  log_info "Testing API connection..."
  RESPONSE=$(curl -s -w "\n%{http_code}" -k -u "$OPENMRS_USER:$OPENMRS_PASSWORD" \
    "$OPENMRS_URL/ws/rest/v1/session")
  
  HTTP_CODE=$(echo "$RESPONSE" | tail -1)
  BODY=$(echo "$RESPONSE" | sed '$d')
  
  if [ "$HTTP_CODE" != "200" ]; then
    log_error "API connection failed with HTTP $HTTP_CODE"
    log_error "Response: $BODY"
    log_error "Please verify:"
    log_error "  1. Bahmni is running: make bahmni-status"
    log_error "  2. Credentials are correct in .env.prerelease"
    log_error "  3. Network connectivity to $PRERELEASE_HOST"
    exit 1
  fi
  
  log_info "API connection successful"
}

# Create output directory
mkdir -p "$OUTPUT_DIR"

log_header "Bahmni Bundle Extraction"
log_info "Target: $PRERELEASE_HOST"
log_info "Output Directory: $OUTPUT_DIR"

# Test API connection first
test_api_connection

# ============================================================================
# 1. Extract All Patients as Bundle
# ============================================================================

log_header "1. Extracting Patients Bundle"

log_info "Fetching all patients..."
RESPONSE=$(curl -s -w "\n%{http_code}" -k -u "$OPENMRS_USER:$OPENMRS_PASSWORD" \
  "$OPENMRS_URL/ws/rest/v1/patient?v=full&limit=1000")

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" != "200" ]; then
  log_error "Failed to fetch patients (HTTP $HTTP_CODE)"
  log_error "Response: $BODY"
  exit 1
fi

echo "$BODY" | jq '.' > "$OUTPUT_DIR/patients-bundle_$TIMESTAMP.json"

PATIENT_COUNT=$(jq '.results | length' "$OUTPUT_DIR/patients-bundle_$TIMESTAMP.json")
log_info "Extracted $PATIENT_COUNT patients"

# ============================================================================
# 2. Extract All Encounters as Bundle
# ============================================================================

log_header "2. Extracting Encounters Bundle"

log_info "Fetching all encounters..."
RESPONSE=$(curl -s -w "\n%{http_code}" -k -u "$OPENMRS_USER:$OPENMRS_PASSWORD" \
  "$OPENMRS_URL/ws/rest/v1/encounter?v=full&limit=1000")
HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$d')
if [ "$HTTP_CODE" = "200" ]; then
  echo "$BODY" | jq '.' > "$OUTPUT_DIR/encounters-bundle_$TIMESTAMP.json"
  ENCOUNTER_COUNT=$(jq '.results | length' "$OUTPUT_DIR/encounters-bundle_$TIMESTAMP.json")
  log_info "Extracted $ENCOUNTER_COUNT encounters"
else
  log_warn "Failed to fetch encounters (HTTP $HTTP_CODE)"
  ENCOUNTER_COUNT=0
fi

# ============================================================================
# 3. Extract All Observations as Bundle
# ============================================================================

log_header "3. Extracting Observations Bundle"

log_info "Fetching all observations..."
RESPONSE=$(curl -s -w "\n%{http_code}" -k -u "$OPENMRS_USER:$OPENMRS_PASSWORD" \
  "$OPENMRS_URL/ws/rest/v1/obs?v=full&limit=1000")
HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$d')
if [ "$HTTP_CODE" = "200" ]; then
  echo "$BODY" | jq '.' > "$OUTPUT_DIR/observations-bundle_$TIMESTAMP.json"
  OBS_COUNT=$(jq '.results | length' "$OUTPUT_DIR/observations-bundle_$TIMESTAMP.json")
  log_info "Extracted $OBS_COUNT observations"
else
  log_warn "Failed to fetch observations (HTTP $HTTP_CODE)"
  OBS_COUNT=0
fi

# ============================================================================
# 4. Extract All Diagnoses as Bundle
# ============================================================================

log_header "4. Extracting Diagnoses Bundle"

log_info "Fetching all diagnoses..."
RESPONSE=$(curl -s -w "\n%{http_code}" -k -u "$OPENMRS_USER:$OPENMRS_PASSWORD" \
  "$OPENMRS_URL/ws/rest/v1/diagnosis?v=full&limit=1000")
HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$d')
if [ "$HTTP_CODE" = "200" ]; then
  echo "$BODY" | jq '.' > "$OUTPUT_DIR/diagnoses-bundle_$TIMESTAMP.json"
  DIAG_COUNT=$(jq '.results | length' "$OUTPUT_DIR/diagnoses-bundle_$TIMESTAMP.json")
  log_info "Extracted $DIAG_COUNT diagnoses"
else
  log_warn "Failed to fetch diagnoses (HTTP $HTTP_CODE)"
  DIAG_COUNT=0
fi

# ============================================================================
# 5. Extract All Drug Orders as Bundle
# ============================================================================

log_header "5. Extracting Drug Orders Bundle"

log_info "Fetching all drug orders..."
RESPONSE=$(curl -s -w "\n%{http_code}" -k -u "$OPENMRS_USER:$OPENMRS_PASSWORD" \
  "$OPENMRS_URL/ws/rest/v1/drugorder?v=full&limit=1000")
HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$d')
if [ "$HTTP_CODE" = "200" ]; then
  echo "$BODY" | jq '.' > "$OUTPUT_DIR/drug-orders-bundle_$TIMESTAMP.json"
  DRUG_COUNT=$(jq '.results | length' "$OUTPUT_DIR/drug-orders-bundle_$TIMESTAMP.json")
  log_info "Extracted $DRUG_COUNT drug orders"
else
  log_warn "Failed to fetch drug orders (HTTP $HTTP_CODE)"
  DRUG_COUNT=0
fi

# ============================================================================
# 6. Extract Metadata (Forms, Encounter Types, Concepts)
# ============================================================================

log_header "6. Extracting Metadata Bundle"

log_info "Fetching encounter types..."
RESPONSE=$(curl -s -w "\n%{http_code}" -k -u "$OPENMRS_USER:$OPENMRS_PASSWORD" \
  "$OPENMRS_URL/ws/rest/v1/encountertype?v=full&limit=100")
HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$d')
[ "$HTTP_CODE" = "200" ] && echo "$BODY" | jq '.' > "$OUTPUT_DIR/metadata-encounter-types_$TIMESTAMP.json"

log_info "Fetching forms..."
RESPONSE=$(curl -s -w "\n%{http_code}" -k -u "$OPENMRS_USER:$OPENMRS_PASSWORD" \
  "$OPENMRS_URL/ws/rest/v1/form?v=full&limit=200")
HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$d')
[ "$HTTP_CODE" = "200" ] && echo "$BODY" | jq '.' > "$OUTPUT_DIR/metadata-forms_$TIMESTAMP.json"

log_info "Fetching concepts..."
RESPONSE=$(curl -s -w "\n%{http_code}" -k -u "$OPENMRS_USER:$OPENMRS_PASSWORD" \
  "$OPENMRS_URL/ws/rest/v1/concept?v=full&limit=500")
HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$d')
[ "$HTTP_CODE" = "200" ] && echo "$BODY" | jq '.' > "$OUTPUT_DIR/metadata-concepts_$TIMESTAMP.json"

# ============================================================================
# 7. Create Consolidated Bundle (FHIR-like structure)
# ============================================================================

log_header "7. Creating Consolidated Bundle"

cat > "$OUTPUT_DIR/bahmni-consolidated-bundle_$TIMESTAMP.json" << 'BUNDLE_EOF'
{
  "resourceType": "Bundle",
  "type": "collection",
  "timestamp": "TIMESTAMP_PLACEHOLDER",
  "entry": [
    {
      "resource": "PATIENTS_PLACEHOLDER"
    },
    {
      "resource": "ENCOUNTERS_PLACEHOLDER"
    },
    {
      "resource": "OBSERVATIONS_PLACEHOLDER"
    },
    {
      "resource": "DIAGNOSES_PLACEHOLDER"
    },
    {
      "resource": "DRUG_ORDERS_PLACEHOLDER"
    },
    {
      "resource": "METADATA_PLACEHOLDER"
    }
  ]
}
BUNDLE_EOF

# Replace placeholders with actual data
TIMESTAMP_ISO=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

jq --arg ts "$TIMESTAMP_ISO" \
   --slurpfile patients "$OUTPUT_DIR/patients-bundle_$TIMESTAMP.json" \
   --slurpfile encounters "$OUTPUT_DIR/encounters-bundle_$TIMESTAMP.json" \
   --slurpfile observations "$OUTPUT_DIR/observations-bundle_$TIMESTAMP.json" \
   --slurpfile diagnoses "$OUTPUT_DIR/diagnoses-bundle_$TIMESTAMP.json" \
   --slurpfile drugs "$OUTPUT_DIR/drug-orders-bundle_$TIMESTAMP.json" \
   '.timestamp = $ts |
    .entry[0].resource = $patients[0] |
    .entry[1].resource = $encounters[0] |
    .entry[2].resource = $observations[0] |
    .entry[3].resource = $diagnoses[0] |
    .entry[4].resource = $drugs[0]' \
   "$OUTPUT_DIR/bahmni-consolidated-bundle_$TIMESTAMP.json" \
   > "$OUTPUT_DIR/bahmni-consolidated-bundle-final_$TIMESTAMP.json"

log_info "Consolidated bundle created"

# ============================================================================
# 8. Create Processing Instructions
# ============================================================================

log_header "8. Creating Bundle Processing Instructions"

cat > "$OUTPUT_DIR/BUNDLE-PROCESSING_$TIMESTAMP.md" << EOF
# Bahmni Bundle Processing Instructions

**Generated:** $(date)
**Bundle Timestamp:** $TIMESTAMP

## Bundle Contents

### Data Bundles
- \`patients-bundle_$TIMESTAMP.json\` - All patients ($PATIENT_COUNT records)
- \`encounters-bundle_$TIMESTAMP.json\` - All encounters ($ENCOUNTER_COUNT records)
- \`observations-bundle_$TIMESTAMP.json\` - All observations ($OBS_COUNT records)
- \`diagnoses-bundle_$TIMESTAMP.json\` - All diagnoses ($DIAG_COUNT records)
- \`drug-orders-bundle_$TIMESTAMP.json\` - All drug orders ($DRUG_COUNT records)

### Metadata Bundles
- \`metadata-encounter-types_$TIMESTAMP.json\` - Encounter type definitions
- \`metadata-forms_$TIMESTAMP.json\` - Form definitions
- \`metadata-concepts_$TIMESTAMP.json\` - Concept definitions

### Consolidated Bundle
- \`bahmni-consolidated-bundle-final_$TIMESTAMP.json\` - All data in single FHIR-like bundle

## Processing Steps

### Step 1: Validate Bundle Structure
\`\`\`bash
# Check bundle integrity
jq '.' bahmni-consolidated-bundle-final_$TIMESTAMP.json > /dev/null && echo "Bundle is valid JSON"

# Count records in each section
jq '.entry[0].resource.results | length' bahmni-consolidated-bundle-final_$TIMESTAMP.json
jq '.entry[1].resource.results | length' bahmni-consolidated-bundle-final_$TIMESTAMP.json
\`\`\`

### Step 2: Transform to Avni Format
Use integration service to transform Bahmni bundle to Avni format:

\`\`\`bash
# Copy bundle to integration service
scp -i ~/.ssh/openchs-infra.pem \\
  bahmni-consolidated-bundle-final_$TIMESTAMP.json \\
  ubuntu@jss-bahmni-prerelease.avniproject.org:/tmp/

# Process via integration service API
curl -X POST http://localhost:8081/api/bahmni/bundle/process \\
  -H "Content-Type: application/json" \\
  -d @bahmni-consolidated-bundle-final_$TIMESTAMP.json
\`\`\`

### Step 3: Map Bahmni Data to Avni Concepts
- Map Bahmni encounter types to Avni encounter types
- Map Bahmni concepts to Avni concepts
- Map Bahmni patients to Avni subjects (using GAN ID)
- Map Bahmni observations to Avni observations

### Step 4: Sync to Avni
\`\`\`bash
# Send transformed data to Avni API
curl -X POST https://prerelease.avniproject.org/api/bundle/import \\
  -H "Authorization: Bearer [TOKEN]" \\
  -H "Content-Type: application/json" \\
  -d @bahmni-transformed-bundle.json
\`\`\`

## Integration Service Processing

The integration service should:

1. **Parse Bahmni Bundle**
   - Extract patients, encounters, observations, diagnoses, drug orders
   - Validate data structure

2. **Apply Mappings**
   - Use concept mappings from configuration
   - Map encounter types
   - Map patient identifiers (GAN ID)

3. **Transform to Avni Format**
   - Convert Bahmni observations to Avni observations
   - Convert Bahmni encounters to Avni encounters
   - Create Avni subjects from Bahmni patients

4. **Validate Transformed Data**
   - Check required fields
   - Validate references
   - Check data types

5. **Sync to Avni**
   - Send via Avni REST API
   - Handle conflicts and duplicates
   - Log sync results

## Bundle Structure (FHIR-like)

\`\`\`json
{
  "resourceType": "Bundle",
  "type": "collection",
  "timestamp": "2026-01-02T...",
  "entry": [
    {
      "resource": {
        "resourceType": "Patients",
        "results": [...]
      }
    },
    {
      "resource": {
        "resourceType": "Encounters",
        "results": [...]
      }
    },
    ...
  ]
}
\`\`\`

## Key Fields for Mapping

### Patient → Avni Subject
- \`patient.identifiers\` → Find GAN ID
- \`patient.person.names\` → Subject name
- \`patient.person.attributes\` → Additional identifiers
- \`patient.person.birthdate\` → Date of birth

### Encounter → Avni Encounter
- \`encounter.encounterType\` → Map to Avni encounter type
- \`encounter.encounterDatetime\` → Encounter date
- \`encounter.location\` → Location/facility
- \`encounter.provider\` → Provider information

### Observation → Avni Observation
- \`observation.concept\` → Map to Avni concept
- \`observation.value\` → Observation value
- \`observation.obsDatetime\` → Observation date
- \`observation.encounter\` → Link to encounter

### Diagnosis → Avni Diagnosis
- \`diagnosis.diagnosis\` → Map to Avni concept
- \`diagnosis.diagnosisDateTime\` → Diagnosis date
- \`diagnosis.certainty\` → Certainty level
- \`diagnosis.encounter\` → Link to encounter

## Next Steps

1. Review bundle contents
2. Validate data quality
3. Configure mappings in integration service
4. Test transformation with sample data
5. Implement full sync process
6. Monitor sync results

---
**Generated:** $(date)
EOF

log_info "Processing instructions saved to: BUNDLE-PROCESSING_$TIMESTAMP.md"

# ============================================================================
# 9. Summary
# ============================================================================

log_header "Bundle Extraction Complete"

log_info "Summary:"
log_info "  Patients: $PATIENT_COUNT"
log_info "  Encounters: $ENCOUNTER_COUNT"
log_info "  Observations: $OBS_COUNT"
log_info "  Diagnoses: $DIAG_COUNT"
log_info "  Drug Orders: $DRUG_COUNT"
log_info ""
log_info "Output Directory: $OUTPUT_DIR"
log_info ""
log_info "Key Files:"
log_info "  1. bahmni-consolidated-bundle-final_$TIMESTAMP.json - Complete bundle"
log_info "  2. BUNDLE-PROCESSING_$TIMESTAMP.md - Processing instructions"
log_info ""
log_info "Next: Use bundle with integration service for Avni sync"
