-- Drug Order (Medication/Prescription) sync: Bahmni → Avni
-- Consultation encounters with drug orders are synced as "Bahmni - Medication" general encounters in Avni.
-- All drug orders are joined into a single text observation on the Avni encounter.

-- Seed DrugOrderEncounterType mapping type
INSERT INTO mapping_type (name, integration_system_id, uuid, is_voided)
SELECT 'DrugOrderEncounterType', (SELECT id FROM integration_system WHERE name = 'bahmni'), uuid_generate_v4(), false
WHERE NOT EXISTS (
    SELECT 1 FROM mapping_type WHERE name = 'DrugOrderEncounterType'
    AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')
);

-- Seed DrugOrderConcept mapping type
INSERT INTO mapping_type (name, integration_system_id, uuid, is_voided)
SELECT 'DrugOrderConcept', (SELECT id FROM integration_system WHERE name = 'bahmni'), uuid_generate_v4(), false
WHERE NOT EXISTS (
    SELECT 1 FROM mapping_type WHERE name = 'DrugOrderConcept'
    AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')
);

-- Map DrugOrderEncounterType: int_system_value is the Bahmni Consultation encounter type UUID
-- avni_value is the Avni encounter type name that will receive all prescription encounters
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
SELECT
    'da7a4fe0-0a6a-11e3-939c-8c50edb4be99',
    'Bahmni - Medication',
    NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'GeneralEncounter'
        AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'DrugOrderEncounterType'
        AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(),
    false
WHERE NOT EXISTS (
    SELECT 1 FROM mapping_metadata
    WHERE avni_value = 'Bahmni - Medication'
      AND mapping_type_id = (SELECT id FROM mapping_type WHERE name = 'DrugOrderEncounterType'
          AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1)
      AND is_voided = false
);

-- Map DrugOrderConcept: the Avni concept that stores the formatted drug list text
-- int_system_value is a placeholder (drug orders are not obs in Bahmni, they come from the orders array)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
SELECT
    'drug-orders-text',
    'Bahmni - Medications',
    NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'GeneralEncounter'
        AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'DrugOrderConcept'
        AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(),
    false
WHERE NOT EXISTS (
    SELECT 1 FROM mapping_metadata
    WHERE avni_value = 'Bahmni - Medications'
      AND mapping_type_id = (SELECT id FROM mapping_type WHERE name = 'DrugOrderConcept'
          AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1)
      AND is_voided = false
);
