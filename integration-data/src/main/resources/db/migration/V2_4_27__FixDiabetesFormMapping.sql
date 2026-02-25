-- =====================================================
-- FIX: Diabetes Intake Form Mapping for getSplitEncounters()
-- =====================================================
-- Root Cause: getSplitEncounters() filters forms using metaData::hasBahmniConceptSet()
-- which loads all mappings with mapping_type = 'EncounterType'
--
-- The form UUID 60619143-5b49-4c10-92f4-0d080cd10b8a must be in this list
-- for the sync to work. If it's voided or missing, getSplitEncounters() returns empty.
--
-- Solution: Ensure clean mapping entry (no duplicates, not voided)

-- Step 1: Remove any voided versions
DELETE FROM mapping_metadata
WHERE int_system_value = '60619143-5b49-4c10-92f4-0d080cd10b8a'
  AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')
  AND (is_voided = true OR mapping_type_id IS NULL);

-- Step 2: Remove duplicates, keeping only the first (lowest ID)
WITH duplicates AS (
    SELECT m.id,
           ROW_NUMBER() OVER (
               PARTITION BY m.int_system_value, m.mapping_type_id, m.is_voided
               ORDER BY m.id ASC
           ) as rn
    FROM mapping_metadata m
    WHERE m.int_system_value = '60619143-5b49-4c10-92f4-0d080cd10b8a'
      AND m.integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')
)
DELETE FROM mapping_metadata
WHERE id IN (
    SELECT id FROM duplicates WHERE rn > 1
);

-- Step 3: Ensure correct mapping exists
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
SELECT
    '60619143-5b49-4c10-92f4-0d080cd10b8a'::text as int_system_value,
    'Bahmni - Diabetes Intake Template'::text as avni_value,
    NULL as data_type_hint,
    (SELECT id FROM integration_system WHERE name = 'bahmni') as integration_system_id,
    (SELECT id FROM mapping_group WHERE name = 'GeneralEncounter' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1) as mapping_group_id,
    (SELECT id FROM mapping_type WHERE name = 'EncounterType' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1) as mapping_type_id,
    uuid_generate_v4() as uuid,
    false as is_voided
WHERE NOT EXISTS (
    SELECT 1 FROM mapping_metadata
    WHERE int_system_value = '60619143-5b49-4c10-92f4-0d080cd10b8a'
      AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')
      AND is_voided = false
      AND mapping_type_id = (SELECT id FROM mapping_type WHERE name = 'EncounterType' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1)
);
