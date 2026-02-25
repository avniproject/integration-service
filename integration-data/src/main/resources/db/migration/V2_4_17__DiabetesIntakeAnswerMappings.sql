-- =====================================================
-- Diabetes Intake Template - Answer Concept Mappings
-- These mappings are needed for Coded observations
-- to translate Bahmni answer UUIDs to Avni answer names
-- Created: 2026-02-25
-- =====================================================

-- STEP 1: Ensure data_type_hint is 'Coded' for coded concepts
-- (Required for proper observation type conversion)
UPDATE mapping_metadata
SET data_type_hint = 'Coded'
WHERE avni_value IN (
    'Bahmni - Diabetes, Complications',
    'Bahmni - Diabetes, Eye Exam',
    'Bahmni - Diabetes, Foot Exam',
    'Bahmni - Diabetes, Peripheral Pulses',
    'Bahmni - Diabetes, Skin fold Thickness',
    'Bahmni - Diabetes, Sensory Charting Assessment',
    'Bahmni - Diabetes, Complaint'
)
AND mapping_group_id = (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1)
AND data_type_hint != 'Coded';

-- STEP 2: Add Answer Concept Mappings
-- MappingGroup: Observation, MappingType: Concept
-- int_system_value = Bahmni answer UUID
-- avni_value = Avni answer name (must match what's configured in Avni form)

-- Answer: Diabetes Complication, Heart
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('32687510-5fd7-42f4-913a-4711a6250b01', 'Bahmni - Diabetes Complication, Heart', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: PreProliferative Diabetic Retinopathy
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('7ef5a585-49d5-4641-9f1e-a3c93a62b471', 'Bahmni - PreProliferative Diabetic Retinopathy', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Neuropathy
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('f5310242-25db-492d-a93f-473b5d2602de', 'Bahmni - Neuropathy', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Diabetes, Abnormal
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('da46b45c-aa7b-4858-975f-7827cd831108', 'Bahmni - Diabetes, Abnormal', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Diabetes, Normal
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('b1ddb87d-8930-43ef-a1cc-1430cea57005', 'Bahmni - Diabetes, Normal', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Weakness
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('fc5a1852-3383-4824-8d0e-51d20d9f7bc1', 'Bahmni - Weakness', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- =====================================================
-- End of Answer Concept Mappings
-- Total: 6 unique answer mappings
-- =====================================================
