-- =====================================================
-- ANC Clinic Visit Missing Answer Mappings
-- Adds answer mappings for answers that were missing from initial configuration
-- =====================================================

-- Section 1: Missing answers for Oedema question
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES
('6f511b7a-00d4-45a1-ba45-8b1ce608b014', 'No oedema', NULL, 1, (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = 1), (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = 1 LIMIT 1), uuid_generate_v4(), false),
('f9f1945f-eb75-4ddb-b354-919afc5f53c4', 'Vulval', NULL, 1, (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = 1), (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = 1 LIMIT 1), uuid_generate_v4(), false),
('4b228992-6d31-4e7a-bbcb-e319ffe672f8', 'Face', NULL, 1, (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = 1), (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = 1 LIMIT 1), uuid_generate_v4(), false),
('5d0462d9-1c5c-42bc-a3e3-8c75cf6b6986', 'Pedal', NULL, 1, (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = 1), (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = 1 LIMIT 1), uuid_generate_v4(), false),
('21eae23f-f925-4c69-bb1e-ce330aeb4b02', 'Entire body', NULL, 1, (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = 1), (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = 1 LIMIT 1), uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Section 2: Missing "None" answer (used by New complaint and Abdomen check questions)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('8b77f487-c885-4b54-8925-153733913b10', 'None', NULL, 1, (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = 1), (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = 1 LIMIT 1), uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;
