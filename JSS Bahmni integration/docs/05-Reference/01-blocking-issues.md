# BLOCKING ISSUE: "Whether Amala given" Answer Mappings Missing

## Status
🔴 **BLOCKING** - ANC Clinic Visit sync cannot proceed until this is fixed

## Problem Summary
The ANC Clinic Visit sync fails with:
```
ERROR: Don't know how to handle ZZ for concept: Avni - Whether Amala given
```

**Root Cause**: The `Whether Amala given` coded question has mappings for the question itself, but **MISSING answer mappings** for the "Yes" and "No" answer options.

## How This Causes "ZZ" Error

1. **Avni sends**: `"Whether Amala given": "Yes"`
2. **Mapper tries to find**: Answer mapping for "Yes"
3. **No mapping found**: `getMappingForAvniValue("Yes")` returns null
4. **Null value sent to Bahmni**: Encounter submission with invalid answer
5. **Bahmni rejects**: "Don't know how to handle ZZ" (Bahmni's error for invalid coded value)

## Resolution Steps

### Step 1: Get Bahmni Answer UUIDs

Run this curl command to fetch the answer options from Bahmni:

```bash
# Set your Bahmni password
export BAHMNI_PASSWORD="your_password_here"

# Query the concept
curl -s -u admin:$BAHMNI_PASSWORD \
  "https://jss-bahmni-prerelease.avniproject.org/openmrs/ws/rest/v1/concept/2a5a3b4d-80c4-4d05-8585-e16966ff0c3e?v=full" \
  | jq '.answers[] | {name, uuid}'
```

This will output something like:
```json
{
  "name": "Avni - Yes",
  "uuid": "57e20de7-10de-4391-b7ce-87b2f40d19a2"
}
{
  "name": "Avni - No",
  "uuid": "f88da2e8-6ab4-44b5-b762-233485cd25f9"
}
```

### Step 2: Update the SQL Mappings Template

Edit `/integration-data/src/main/resources/db/onetime/ANCClinicVisitWhetherAmalaMappings.sql` and replace:
- `<BAHMNI_YES_UUID>` with the UUID for "Yes" answer
- `<BAHMNI_NO_UUID>` with the UUID for "No" answer

Example (with hypothetical UUIDs):
```sql
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('57e20de7-10de-4391-b7ce-87b2f40d19a2', 'Yes', NULL,  -- BAHMNI YES UUID
    ...);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('f88da2e8-6ab4-44b5-b762-233485cd25f9', 'No', NULL,   -- BAHMNI NO UUID
    ...);
```

### Step 3: Execute the SQL

Run the updated SQL migration:

```bash
# Using psql if you have local access
psql -U postgres -h localhost integration_service_db \
  -f integration-data/src/main/resources/db/onetime/ANCClinicVisitWhetherAmalaMappings.sql

# Or through Flyway (automatic on next application startup)
# The file is in the db/onetime directory so Flyway will apply it
```

### Step 4: Re-run the Test

```bash
cd /Users/nupoorkhandelwal/Avni/integration-service

JAVA_HOME=/Users/nupoorkhandelwal/Library/Java/JavaVirtualMachines/ms-17.0.16/Contents/Home \
./gradlew :bahmni:test --tests "ProgramEncounterWorkerExternalTest.debugANCClinicVisitSync" \
-Dspring.profiles.active=jss_ganiyari -i
```

## Verification

After applying the mappings, verify in the database:

```sql
-- Check the new mappings exist
SELECT avni_value, int_system_value
FROM mapping_metadata
WHERE avni_value IN ('Yes', 'No')
  AND mapping_group_id = (SELECT id FROM mapping_group WHERE name = 'Observation')
  AND mapping_type_id = (SELECT id FROM mapping_type WHERE name = 'Concept');
```

Should show:
- `Yes` → Bahmni UUID for Yes answer
- `No` → Bahmni UUID for No answer

## Related Files

- **Template SQL**: `integration-data/src/main/resources/db/onetime/ANCClinicVisitWhetherAmalaMappings.sql`
- **Mapper code**: `bahmni/src/main/java/org/avni_integration_service/bahmni/mapper/avni/BahmniModuleObservationMapper.java` (line 156)
- **Form definition**: `scripts/aws/avni-metadata/forms/ANC Clinic Visit.json` (contains answer names)

## Additional Questions

**Q: Are there other coded questions missing answer mappings?**

A: Unknown. The ANC answer mappings file (`ANCClinicVisitAnswerMappings.sql`) is 1251 lines and may be incomplete. Use this pattern to check other coded questions:

```bash
# Check which coded questions have no answer mappings
grep "data_type_hint = 'Coded'" ANCClinicVisitMappings.sql | \
  grep -o "'[^']*'" | head -1 | \
  xargs grep -f - ANCClinicVisitAnswerMappings.sql
```

**Q: Why wasn't this caught earlier?**

A: The answer mappings file may have been created from a template that didn't include all coded questions, or "Whether Amala given" was added to the form later without corresponding answer mappings.

**Q: Can I just use the Avni answer UUIDs instead of Bahmni UUIDs?**

A: No. The mapping must use Bahmni's answer UUIDs because:
1. The mapper sends the Bahmni value (`int_system_value`) to the Bahmni API
2. Bahmni API expects its own answer UUIDs, not Avni's
3. Using Avni UUIDs will cause "Answer concept not found" errors
