-- V2_4_37: Add missing concept mappings found during sync testing (2026-05-18)

-- "Nil" — coded answer concept for Lab Samples / All_Tests_and_Panels
-- NOTE: "Bahmni - Nil" concept must also exist in Avni (upload patch bundle if missing)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
SELECT 'd923b664-cf14-4e6f-98f8-b6cbad47e8ad', 'Bahmni - Nil', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false
WHERE NOT EXISTS (SELECT 1 FROM mapping_metadata WHERE int_system_value = 'd923b664-cf14-4e6f-98f8-b6cbad47e8ad' AND is_voided = false);

-- "Diabetes Complication, Heart" — answer concept for Diabetes Complications
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
SELECT '32687510-5fd7-42f4-913a-4711a6250b01', 'Bahmni - Diabetes Complication, Heart', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false
WHERE NOT EXISTS (SELECT 1 FROM mapping_metadata WHERE int_system_value = '32687510-5fd7-42f4-913a-4711a6250b01' AND is_voided = false);

-- "Diabetic Foot" — answer concept for Diabetes Complications
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
SELECT '10fef182-42d3-4a2d-8aec-fcccd6e854cd', 'Bahmni - Diabetic Foot', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false
WHERE NOT EXISTS (SELECT 1 FROM mapping_metadata WHERE int_system_value = '10fef182-42d3-4a2d-8aec-fcccd6e854cd' AND is_voided = false);
