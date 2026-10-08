-- V2_4_36: Fix EncounterType mappings based on actual Avni state (JSSCP.zip export 2026-05-18)
-- 1. Void Normal Procedure EncounterType mapping — form is voided in Avni
-- 2. Add EncounterType mappings for all active Bahmni forms in Avni missing their routing
--    Concept-level mappings already exist in V2_4_35 for all forms below.
-- Note: Bahmni Patient Registration excluded — its form has no clinical fields (only Bahmni Entity UUID).

-- ============================================================
-- 1. Void Normal Procedure EncounterType mapping (form voided in Avni)
-- ============================================================
UPDATE mapping_metadata
SET is_voided = true
WHERE int_system_value = '67e29147-9bcd-11e3-927e-8840ab96f0f1'
  AND mapping_type_id = (SELECT id FROM mapping_type WHERE name = 'EncounterType'
      AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1)
  AND is_voided = false;

-- ============================================================
-- 2. Add missing EncounterType mappings
-- ============================================================

-- Bahmni - Menstrual History (ConceptSet UUID: dfb5e5a4-d4b2-40f5-9c74-3ba953decf3a)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
SELECT 'dfb5e5a4-d4b2-40f5-9c74-3ba953decf3a', 'Bahmni - Menstrual History', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'GeneralEncounter' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'EncounterType' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false
WHERE NOT EXISTS (SELECT 1 FROM mapping_metadata WHERE int_system_value = 'dfb5e5a4-d4b2-40f5-9c74-3ba953decf3a' AND is_voided = false);

-- Bahmni - RMRCT, If treatment taken (ConceptSet UUID: 44d1882c-19c2-4377-b4cd-7a32100a1169)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
SELECT '44d1882c-19c2-4377-b4cd-7a32100a1169', 'Bahmni - RMRCT, If treatment taken', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'GeneralEncounter' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'EncounterType' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false
WHERE NOT EXISTS (SELECT 1 FROM mapping_metadata WHERE int_system_value = '44d1882c-19c2-4377-b4cd-7a32100a1169' AND is_voided = false);

-- Bahmni - RMRCT, Microbiology test requirement (ConceptSet UUID: 1996d54e-f05f-413f-8af0-b867bc391c9e)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
SELECT '1996d54e-f05f-413f-8af0-b867bc391c9e', 'Bahmni - RMRCT, Microbiology test requirement', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'GeneralEncounter' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'EncounterType' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false
WHERE NOT EXISTS (SELECT 1 FROM mapping_metadata WHERE int_system_value = '1996d54e-f05f-413f-8af0-b867bc391c9e' AND is_voided = false);

-- Bahmni - RMRCT, Standardized History (ConceptSet UUID: 21808d2a-15a8-4def-a968-d31f657dcb92)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
SELECT '21808d2a-15a8-4def-a968-d31f657dcb92', 'Bahmni - RMRCT, Standardized History', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'GeneralEncounter' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'EncounterType' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false
WHERE NOT EXISTS (SELECT 1 FROM mapping_metadata WHERE int_system_value = '21808d2a-15a8-4def-a968-d31f657dcb92' AND is_voided = false);

-- Bahmni - RNTCP Form (ConceptSet UUID: 2f6a0932-1fc0-4edb-bbfd-ac57d40b4cfd)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
SELECT '2f6a0932-1fc0-4edb-bbfd-ac57d40b4cfd', 'Bahmni - RNTCP Form', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'GeneralEncounter' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'EncounterType' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false
WHERE NOT EXISTS (SELECT 1 FROM mapping_metadata WHERE int_system_value = '2f6a0932-1fc0-4edb-bbfd-ac57d40b4cfd' AND is_voided = false);

-- Bahmni - OPD Followup Non attendance Record Template (ConceptSet UUID: 502f835d-a3ad-477e-9981-0a9fa10bd032)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
SELECT '502f835d-a3ad-477e-9981-0a9fa10bd032', 'Bahmni - OPD Followup Non attendance Record Template', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'GeneralEncounter' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'EncounterType' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false
WHERE NOT EXISTS (SELECT 1 FROM mapping_metadata WHERE int_system_value = '502f835d-a3ad-477e-9981-0a9fa10bd032' AND is_voided = false);

-- Bahmni - Referral Form, Doctors Name (ConceptSet UUID: 142d6b21-defd-4b03-9196-bebacdac363e)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
SELECT '142d6b21-defd-4b03-9196-bebacdac363e', 'Bahmni - Referral Form, Doctors Name', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'GeneralEncounter' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'EncounterType' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false
WHERE NOT EXISTS (SELECT 1 FROM mapping_metadata WHERE int_system_value = '142d6b21-defd-4b03-9196-bebacdac363e' AND is_voided = false);

-- Bahmni - Smart card, blocked package details (ConceptSet UUID: 819beadb-8071-43d7-9d60-78eedb9887f0)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
SELECT '819beadb-8071-43d7-9d60-78eedb9887f0', 'Bahmni - Smart card, blocked package details', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'GeneralEncounter' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'EncounterType' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false
WHERE NOT EXISTS (SELECT 1 FROM mapping_metadata WHERE int_system_value = '819beadb-8071-43d7-9d60-78eedb9887f0' AND is_voided = false);
