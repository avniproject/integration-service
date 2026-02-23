#!/bin/bash

# Script to fetch Bahmni concept and its answer UUIDs
# Usage: ./get-bahmni-concept-answers.sh <concept-uuid>

CONCEPT_UUID="${1:-2a5a3b4d-80c4-4d05-8585-e16966ff0c3e}"  # Whether Amala given UUID
BAHMNI_URL="${BAHMNI_URL:-https://jss-bahmni-prerelease.avniproject.org}"

echo "Fetching concept details from Bahmni..."
echo "URL: $BAHMNI_URL/openmrs/ws/rest/v1/concept/$CONCEPT_UUID?v=full"
echo ""

curl -s -u "admin:$BAHMNI_PASSWORD" \
  "$BAHMNI_URL/openmrs/ws/rest/v1/concept/$CONCEPT_UUID?v=full" \
  | jq '.answers[] | {name, uuid}'

echo ""
echo "Copy the UUIDs above for creating answer mappings"
