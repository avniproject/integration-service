-- Fix DrugOrderEncounterType avni_value: the correct Avni encounter type name is
-- 'Bahmni - Medication Encounter' (pre-existing), not 'Bahmni - Medication' (created by bundle upload)
UPDATE mapping_metadata SET avni_value = 'Bahmni - Medication Encounter'
WHERE avni_value = 'Bahmni - Medication'
  AND mapping_type_id = (SELECT id FROM mapping_type WHERE name = 'DrugOrderEncounterType'
      AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni'));
