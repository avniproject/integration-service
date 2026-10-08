-- V2_4_38: Add missing concept mappings found during sync testing (2026-05-18) — batch 2

-- "PreProliferative Diabetic Retinopathy" — answer concept for Diabetic Retinopathy (Eye Exam)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
SELECT '7ef5a585-49d5-4641-9f1e-a3c93a62b471', 'Bahmni - PreProliferative Diabetic Retinopathy', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false
WHERE NOT EXISTS (SELECT 1 FROM mapping_metadata WHERE int_system_value = '7ef5a585-49d5-4641-9f1e-a3c93a62b471' AND is_voided = false);

-- "Diabetes, Normal" — answer concept for Diabetes question
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
SELECT 'b1ddb87d-8930-43ef-a1cc-1430cea57005', 'Bahmni - Diabetes, Normal', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false
WHERE NOT EXISTS (SELECT 1 FROM mapping_metadata WHERE int_system_value = 'b1ddb87d-8930-43ef-a1cc-1430cea57005' AND is_voided = false);
