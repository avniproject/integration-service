-- Revert V2_4_33: 'Bahmni - Medication' is the correct Avni encounter type
-- (the one with form fields for drug orders). 'Bahmni - Medication Encounter' is wrong.
UPDATE mapping_metadata SET avni_value = 'Bahmni - Medication'
WHERE avni_value = 'Bahmni - Medication Encounter'
  AND mapping_type_id = (SELECT id FROM mapping_type WHERE name = 'DrugOrderEncounterType'
      AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni'));
