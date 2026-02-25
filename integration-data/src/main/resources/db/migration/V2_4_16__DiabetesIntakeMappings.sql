-- =====================================================
-- Diabetes Intake Template Mapping Configuration
-- Avni General Encounter: Bahmni - Diabetes Intake Template
-- Bahmni Encounter Type: Diabetes Intake Template
-- Created: 2026-02-17
-- =====================================================

-- Note: This migration assumes mapping groups/types exist (created by earlier migrations)
-- If they don't exist, subsequent migrations or manual setup may be needed

-- Section 1: Encounter Type Mapping
-- Maps "Bahmni - Diabetes Intake Template" (Avni) to "Diabetes Intake Template" (Bahmni)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES (
    '60619143-5b49-4c10-92f4-0d080cd10b8a',  -- Bahmni Diabetes Intake Template UUID
    'Bahmni - Diabetes Intake Template',      -- Avni encounter type name
    NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'GeneralEncounter' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'EncounterType' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(),
    false
)
ON CONFLICT DO NOTHING;

-- Section 2: Observation/Concept Mappings (13 concepts)
-- MappingGroup: Observation, MappingType: Concept

-- 1. Diabetes, Diagnosed Date
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('1c413fb2-801a-4e84-8fee-8f7d79f0312b', 'Bahmni - Diabetes, Diagnosed Date', 'Date',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- 2. Diabetes, Treatment Stopped Date
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('93cee5e7-b8f6-4ea5-a3e6-b94b6a9c6f30', 'Bahmni - Diabetes, Treatment Stopped Date', 'Date',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- 3. Diabetes, Complaint
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('01badc59-8cd7-4c6e-846b-e1a923c223ee', 'Bahmni - Diabetes, Complaint', 'Coded',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- 4. Diabetes, Other Complaints
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('1b924377-b2a4-454e-88d6-124c939c46b4', 'Bahmni - Diabetes, Other Complaints', 'Text',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- 5. Diabetes, Complications
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('5ec8dd19-2446-4edf-818b-5faffec63427', 'Bahmni - Diabetes, Complications', 'Coded',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- 6. Diabetes, Other Complications
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('a1cb3d0c-cecd-4589-9e3c-d0836bc93194', 'Bahmni - Diabetes, Other Complications', 'Text',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- 7. Diabetes, Peripheral Pulses
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('db466f75-584e-47ef-96a6-ccde4482a1db', 'Bahmni - Diabetes, Peripheral Pulses', 'Coded',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- 8. Diabetes, Sensory Charting Assessment
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('2b40efca-8d5d-4690-a37f-ff4d4ada5102', 'Bahmni - Diabetes, Sensory Charting Assessment', 'Coded',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- 9. Diabetes, Skin fold Thickness
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('a408441f-d028-4a61-9a3b-2ffa1a607e5e', 'Bahmni - Diabetes, Skin fold Thickness', 'Coded',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- 10. Diabetes, Foot Exam
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('5508a7b2-f583-473d-8cf9-f7fc25101ee8', 'Bahmni - Diabetes, Foot Exam', 'Coded',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- 11. Diabetes, Eye Exam
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('77f81858-101d-4726-abe6-b279d81cdbb6', 'Bahmni - Diabetes, Eye Exam', 'Coded',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- 12. Diabetes, Electrocardiography
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('e1aafdd6-29fb-4b1d-b78c-8b1768b98efd', 'Bahmni - Diabetes, Electrocardiography', 'Text',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- 13. Diabetes, X-Ray
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('f2884e7d-0173-4b6c-8108-a4d80b37e713', 'Bahmni - Diabetes, X-Ray', 'Text',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- =====================================================
-- End of Diabetes Intake Template Mapping Configuration
-- Total: 1 encounter mapping + 13 observation mappings = 14 mappings
-- =====================================================
