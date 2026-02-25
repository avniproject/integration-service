-- =====================================================
-- Add Bahmni Entity UUID Concept Mapping
-- =====================================================
-- This maps the Bahmni Entity UUID concept which is required
-- for the integration to track and link Avni encounters to Bahmni encounters

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
SELECT
    'a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d'::text as int_system_value,
    'Bahmni Entity UUID'::text as avni_value,
    NULL as data_type_hint,
    (SELECT id FROM integration_system WHERE name = 'bahmni') as integration_system_id,
    (SELECT id FROM mapping_group WHERE name = 'Common' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1) as mapping_group_id,
    (SELECT id FROM mapping_type WHERE name = 'AvniUUID_Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1) as mapping_type_id,
    uuid_generate_v4() as uuid,
    false as is_voided
WHERE NOT EXISTS (
    SELECT 1 FROM mapping_metadata
    WHERE int_system_value = 'a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d'
    AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')
);
