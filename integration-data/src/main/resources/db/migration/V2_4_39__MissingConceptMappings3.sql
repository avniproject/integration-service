-- V2_4_39: Add missing answer concept mappings for Diabetes Intake Template (2026-05-18)

-- "Neuropathy" — answer for Diabetes, Foot Exam
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
SELECT 'f5310242-25db-492d-a93f-473b5d2602de', 'Bahmni - Neuropathy', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false
WHERE NOT EXISTS (SELECT 1 FROM mapping_metadata WHERE int_system_value = 'f5310242-25db-492d-a93f-473b5d2602de' AND is_voided = false);

-- "Diabetes, Abnormal" — answer for Diabetes, Peripheral Pulses
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
SELECT 'da46b45c-aa7b-4858-975f-7827cd831108', 'Bahmni - Diabetes, Abnormal', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false
WHERE NOT EXISTS (SELECT 1 FROM mapping_metadata WHERE int_system_value = 'da46b45c-aa7b-4858-975f-7827cd831108' AND is_voided = false);

-- "PVD" (Peripheral Vascular Disease) — answer for Diabetes, Foot Exam
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
SELECT 'a3d1e582-dec1-4921-bf13-10f23e37d4e8', 'Bahmni - PVD', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false
WHERE NOT EXISTS (SELECT 1 FROM mapping_metadata WHERE int_system_value = 'a3d1e582-dec1-4921-bf13-10f23e37d4e8' AND is_voided = false);

-- "Excessive Urine" — answer for Diabetes, Complaint
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
SELECT '59201706-fe05-4451-9da6-c173267d7291', 'Bahmni - Excessive Urine', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false
WHERE NOT EXISTS (SELECT 1 FROM mapping_metadata WHERE int_system_value = '59201706-fe05-4451-9da6-c173267d7291' AND is_voided = false);
