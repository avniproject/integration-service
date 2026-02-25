-- =====================================================
-- ADD: Diabetes Intake Individual Form UUIDs
-- =====================================================
-- Debug output revealed that Diabetes Intake encounters contain 4 forms
-- with Bahmni internal form UUIDs that are NOT in the mappings.
-- Without these mappings, getSplitEncounters() returns empty list!
--
-- Form UUIDs found in actual Diabetes encounters:
-- 1. 7d78b771-11ef-11e5-ac28-ef489a2b0ab1
-- 2. 854434f4-1666-11e4-9b1f-a53a324dedbc
-- 3. 3ccfba5b-82b6-43c3-939b-449f228b66d1
-- 4. 7d0e085f-11ef-11e5-ac28-ef489a2b0ab1 (duplicate)
--
-- All map to the same Avni encounter type: Bahmni - Diabetes Intake Template

-- Form 1: Diabetes Intake Initial Assessment
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
SELECT
    '7d78b771-11ef-11e5-ac28-ef489a2b0ab1'::text as int_system_value,
    'Bahmni - Diabetes Intake Template'::text as avni_value,
    NULL as data_type_hint,
    (SELECT id FROM integration_system WHERE name = 'bahmni') as integration_system_id,
    (SELECT id FROM mapping_group WHERE name = 'GeneralEncounter' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1) as mapping_group_id,
    (SELECT id FROM mapping_type WHERE name = 'EncounterType' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1) as mapping_type_id,
    uuid_generate_v4() as uuid,
    false as is_voided
WHERE NOT EXISTS (
    SELECT 1 FROM mapping_metadata
    WHERE int_system_value = '7d78b771-11ef-11e5-ac28-ef489a2b0ab1'
      AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')
);

-- Form 2
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
SELECT
    '854434f4-1666-11e4-9b1f-a53a324dedbc'::text as int_system_value,
    'Bahmni - Diabetes Intake Template'::text as avni_value,
    NULL as data_type_hint,
    (SELECT id FROM integration_system WHERE name = 'bahmni') as integration_system_id,
    (SELECT id FROM mapping_group WHERE name = 'GeneralEncounter' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1) as mapping_group_id,
    (SELECT id FROM mapping_type WHERE name = 'EncounterType' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1) as mapping_type_id,
    uuid_generate_v4() as uuid,
    false as is_voided
WHERE NOT EXISTS (
    SELECT 1 FROM mapping_metadata
    WHERE int_system_value = '854434f4-1666-11e4-9b1f-a53a324dedbc'
      AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')
);

-- Form 3
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
SELECT
    '3ccfba5b-82b6-43c3-939b-449f228b66d1'::text as int_system_value,
    'Bahmni - Diabetes Intake Template'::text as avni_value,
    NULL as data_type_hint,
    (SELECT id FROM integration_system WHERE name = 'bahmni') as integration_system_id,
    (SELECT id FROM mapping_group WHERE name = 'GeneralEncounter' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1) as mapping_group_id,
    (SELECT id FROM mapping_type WHERE name = 'EncounterType' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1) as mapping_type_id,
    uuid_generate_v4() as uuid,
    false as is_voided
WHERE NOT EXISTS (
    SELECT 1 FROM mapping_metadata
    WHERE int_system_value = '3ccfba5b-82b6-43c3-939b-449f228b66d1'
      AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')
);

-- Form 4 (duplicate UUID - same as Form 1)
-- No need to add duplicate
