-- =====================================================
-- Fix Fundle Height Answer UUID Mapping
-- Corrects the UUID for "14-16" answer in Fundle Height concept
-- The UUID 6fa2d5e5-b709-4690-a141-4d86d0b7c7ab is the correct one in both Avni and Bahmni
-- =====================================================

-- Delete the incorrect mapping and insert the correct one
DELETE FROM mapping_metadata
WHERE avni_value = '14-16'
  AND int_system_value = '9b6e2f4a-1d3c-48e7-a5f9-7c2e5a1d8b3f';

-- Insert the correct mapping for Fundle Height "14-16" answer
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('6fa2d5e5-b709-4690-a141-4d86d0b7c7ab', '14-16', NULL, (SELECT id FROM integration_system WHERE name = 'bahmni'), (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1), (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1), uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;
