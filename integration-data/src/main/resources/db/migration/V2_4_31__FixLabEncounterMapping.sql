-- Fix All_Tests_and_Panels encounter mapping:
-- V2_4_30 incorrectly used EncounterType + ConvSet UUID.
-- Lab results in JSS Bahmni are flat obs on LAB_RESULT encounter type,
-- not grouped under a ConvSet. The correct mapping uses LabEncounterType
-- with the actual LAB_RESULT encounter type UUID.

-- Seed LabEncounterType mapping type
INSERT INTO mapping_type (name, integration_system_id, uuid, is_voided)
SELECT 'LabEncounterType', (SELECT id FROM integration_system WHERE name = 'bahmni'), uuid_generate_v4(), false
WHERE NOT EXISTS (
    SELECT 1 FROM mapping_type WHERE name = 'LabEncounterType'
    AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')
);

-- Void the incorrect EncounterType mapping that used the ConvSet UUID
UPDATE mapping_metadata SET is_voided = true
WHERE int_system_value = 'e4edc5a4-e349-11e3-983a-91270dcbd3bf'
  AND avni_value = 'Bahmni - All_Tests_and_Panels'
  AND mapping_type_id = (SELECT id FROM mapping_type WHERE name = 'EncounterType'
      AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni'));

-- Add correct LabEncounterType mapping using LAB_RESULT encounter type UUID
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
SELECT
    '960469a8-9bc6-11e3-927e-8840ab96f0f1',
    'Bahmni - All_Tests_and_Panels',
    NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'GeneralEncounter'
        AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'LabEncounterType'
        AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(),
    false
WHERE NOT EXISTS (
    SELECT 1 FROM mapping_metadata
    WHERE int_system_value = '960469a8-9bc6-11e3-927e-8840ab96f0f1'
      AND avni_value = 'Bahmni - All_Tests_and_Panels'
      AND is_voided = false
);

-- Add OPD visit type to OutpatientVisitTypes so lab encounters pass isOutpatientEncounter check
INSERT INTO constants (key, value, uuid, is_voided)
SELECT 'OutpatientVisitTypes', 'f6ce7bf9-e349-11e3-983a-91270dcbd3bf', uuid_generate_v4(), false
WHERE NOT EXISTS (
    SELECT 1 FROM constants
    WHERE key = 'OutpatientVisitTypes' AND value = 'f6ce7bf9-e349-11e3-983a-91270dcbd3bf'
);
