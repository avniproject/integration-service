-- =====================================================
-- ANC Clinic Visit Mapping Configuration
-- Avni Program Encounter: ANC Clinic Visit
-- Bahmni Encounter Type: Pregnancy- ANC Clinic Visit [A]
-- Generated: 2025-02-03
-- =====================================================

-- Cleanup: Remove duplicate mapping groups to avoid NonUniqueResultException
DELETE FROM mapping_group
WHERE (name IN ('Observation', 'ProgramEncounter') AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni'))
AND id NOT IN (
    SELECT MIN(id) FROM mapping_group
    WHERE name IN ('Observation', 'ProgramEncounter') AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')
    GROUP BY name, integration_system_id
);

-- Cleanup: Remove duplicate mapping types
DELETE FROM mapping_type
WHERE (name IN ('Concept', 'CommunityProgramEncounter_EncounterType', 'CommunityProgramEncounter_BahmniForm') AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni'))
AND id NOT IN (
    SELECT MIN(id) FROM mapping_type
    WHERE name IN ('Concept', 'CommunityProgramEncounter_EncounterType', 'CommunityProgramEncounter_BahmniForm') AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')
    GROUP BY name, integration_system_id
);

-- Section 1: Ensure Mapping Groups Exist
-- Observation and ProgramEncounter groups
INSERT INTO mapping_group (name, integration_system_id, uuid, is_voided)
SELECT 'Observation', 1, uuid_generate_v4(), false
WHERE NOT EXISTS (SELECT 1 FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni'));

INSERT INTO mapping_group (name, integration_system_id, uuid, is_voided)
SELECT 'ProgramEncounter', 1, uuid_generate_v4(), false
WHERE NOT EXISTS (SELECT 1 FROM mapping_group WHERE name = 'ProgramEncounter' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni'));

-- Section 2: Ensure Mapping Types Exist
INSERT INTO mapping_type (name, integration_system_id, uuid, is_voided)
SELECT 'Concept', 1, uuid_generate_v4(), false
WHERE NOT EXISTS (SELECT 1 FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni'));

INSERT INTO mapping_type (name, integration_system_id, uuid, is_voided)
SELECT 'CommunityProgramEncounter_EncounterType', 1, uuid_generate_v4(), false
WHERE NOT EXISTS (SELECT 1 FROM mapping_type WHERE name = 'CommunityProgramEncounter_EncounterType' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni'));

INSERT INTO mapping_type (name, integration_system_id, uuid, is_voided)
SELECT 'CommunityProgramEncounter_BahmniForm', 1, uuid_generate_v4(), false
WHERE NOT EXISTS (SELECT 1 FROM mapping_type WHERE name = 'CommunityProgramEncounter_BahmniForm' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni'));

-- Section 3: Program Encounter Type Mapping
-- -----------------------------------------------------
-- Maps "ANC Clinic Visit" (Avni) to "Pregnancy- ANC Clinic Visit [A]" (Bahmni)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES (
    '2d79e469-88e1-4bd8-9f39-743109962db8',
    'ANC Clinic Visit',
    NULL,
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'ProgramEncounter' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'CommunityProgramEncounter_EncounterType' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(),
    false
);

-- Section 4: Program Encounter Form Mapping
-- -----------------------------------------------------
-- Maps ANC Clinic Visit form to Bahmni concept-set "Avni - JSS ANC Clinic Visit"
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES (
    '29a946e8-9153-474d-86e7-a0b3d26474c5',
    'ANC Clinic Visit',
    NULL,
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'ProgramEncounter' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'CommunityProgramEncounter_BahmniForm' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(),
    false
);

-- Section 5: Observation/Concept Mappings
-- -----------------------------------------------------
-- MappingGroup: Observation, MappingType: Concept

-- 1. Name of ANC Clinic -> Avni - Name of ANC Clinic
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('7dd50376-7ba1-441c-aa0c-7ff5ce0468e2', 'Name of ANC Clinic', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 2. Height -> Avni - Height
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('23bcad9f-ec16-46ec-92f5-e144411e5dec', 'Height', 'Numeric',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 3. Weight -> Avni - Weight at diagnosis
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('8d947379-7a1d-48b2-8760-88fff6add987', 'Weight', 'Numeric',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 4. BMI -> Avni - ANC - BMI
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('a205563d-0ac2-4955-93ac-e2e7adebb56e', 'BMI', 'Numeric',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 5. Is mosquito net given? -> Avni - Is mosquito net given
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('b7be4ddc-14ee-4caf-ab38-e1c87d088688', 'Is mosquito net given?', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 6. Is Safe delivery kit given? -> Avni - Is safe delivery kit given
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('04eecc2b-93eb-49d4-83a4-6629442711ea', 'Is Safe delivery kit given?', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 7. Blood Pressure (systolic) -> Avni - Blood Pressure (systolic)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('6874d48e-8c2f-4009-992c-1d3ca1678cc6', 'Blood Pressure (systolic)', 'Numeric',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 8. Blood Pressure (Diastolic) -> Avni - Blood Pressure (Diastolic)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('da871f6c-cef0-4191-b307-6751b31ac9ec', 'Blood Pressure (Diastolic)', 'Numeric',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 9. New complaint -> Avni - New complaint
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('74599453-6fbd-4f8d-bf7f-34faa3c10eb9', 'New complaint', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 10. Specify the other complaint -> Avni - Other complaint
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('dc0c10ca-c151-4c5c-aedc-2b8040dbea52', 'Specify the other complaint', 'Text',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 11. Whether mother found with pregnancy induced disease -> Avni - Mother found with pregnancy induced disease
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('2a3a4f97-0ca7-4498-b7ec-37225ba3d4c5', 'Whether mother found with pregnancy induced disease', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 12. Oedema -> Avni - Oedema
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('95dd3094-6c99-4622-8614-bf5d33a509e4', 'Oedema', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 13. Abdomen check -> Avni - Abdomen check
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('2a15dc0b-d6a0-4670-b109-4013789cb403', 'Abdomen check', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 14. Current gestational age -> Avni - Current gestational age
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('9b087651-34e8-4391-aa08-8db73f55d7e6', 'Current gestational age', 'Numeric',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 15. Gestational age -> Avni - Gestational age
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('5edc0b6a-aaa2-4499-a673-3db8f0b056e1', 'Gestational age', 'Numeric',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 16. Fundle Height -> Avni - Fundle Height
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('f5ff8848-798a-4b0f-bcaf-33f2d4528f37', 'Fundle Height', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 17. Position -> Avni - Position
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('69a95145-505b-497a-9fc9-61fcc5d2ff59', 'Position', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 18. FHS -> Avni - FHS
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('7c6d3fc6-6a9f-4b44-beef-8c2200da5281', 'FHS', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 19. FHS number -> Avni - FHS number
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('532ae011-4380-4ff5-b7c7-7d163e396221', 'FHS number', 'Numeric',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 20. Foetus movement -> Avni - Foetus movement
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('31651632-0acb-4ee5-a0f3-1628bbed456c', 'Foetus movement', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 21. Position of baby 2 -> Avni - Position of baby 2
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('1887a0e8-81d4-477c-a3d7-103a929d7e7b', 'Position of baby 2', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 22. FHS of baby 2 -> Avni - FHS of baby 2
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('3c83c5d7-8c02-49d1-bde4-9e74cd5c8b31', 'FHS of baby 2', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 23. FHS number of baby 2 -> Avni - FHS number of baby 2
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('6dd83097-9b6d-44fc-b98f-f4819f40ed05', 'FHS number of baby 2', 'Numeric',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 24. Foetal movement baby 2 -> Avni - Foetal movement baby 2
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('4bd77a68-14cc-4dc2-b283-eff91894ed34', 'Foetal movement baby 2', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 25. Breast examination -> Avni - Breast examination
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('7259b0fa-c8d1-4e04-8d13-7dbc05f0169b', 'Breast examination', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 26. High risk condition in this pregnancy? -> Avni - High risk condition in this pregnancy?
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('93679880-83f5-4877-bfc4-e525421c7e52', 'High risk condition in this pregnancy?', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 27. High risk condition in this pregnancy -> Avni - High risk condition in this pregnancy
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('b8724e63-db1d-46fc-8ecc-17ee200bf1a2', 'High risk condition in this pregnancy', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 28. Blood group -> Avni - Blood group
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('c50c8196-01c9-422f-b917-fd2309adb261', 'Blood group', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 29. HIV (Elisa) -> Avni - HIV (Elisa)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('b3e8a85a-c7ca-49b8-9c6f-8bd5ee6bfad1', 'HIV (Elisa)', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 30. Hepatitis B -> Hepatitis B Immunoglobulin
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('f874c217-2fed-41c9-a094-ba6519bd537d', 'Hepatitis B', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 31. Sickle prep -> Avni - Sickle prep
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('610db330-fafe-456f-bd58-e062cb6e52e3', 'Sickle prep', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 32. Hb Electrophoresis -> Hb Electrophoresis
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('198d08c6-b742-4b22-97fd-2293472e571e', 'Hb Electrophoresis', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 33. VDRL -> Avni - VDRL
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('cbe884a3-c5f3-441e-900a-6bc76f3cabca', 'VDRL', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 34. Pus Cell -> Avni - Pus Cell
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('0b8bc1f8-43db-4ecb-9677-22709e91681f', 'Pus Cell', 'Numeric',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 35. RBC -> Avni - RBC
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('b59c126f-975b-45e6-8dd6-584dd54e25c9', 'RBC', 'Numeric',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 36. Epithelial cells -> Avni - Epithelial cells
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('0343e35f-afd0-41ce-af93-e69c184b159c', 'Epithelial cells', 'Numeric',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 37. Cast -> Cast
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('a2b6d675-4c70-4f15-a5ad-b8f5273602f9', 'Cast', 'Numeric',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 38. Crystel -> Avni - Crystel
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('14a023d3-bd25-4343-9d93-34d9f88eb4b3', 'Crystel', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 39. Urine Albumin -> Avni - Urine Albumin
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('78fcebd3-17e5-4621-89be-c580fbf13168', 'Urine Albumin', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 40. Urine sugar -> Avni - Urine sugar
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('55ae9e7a-f6ff-4c0b-861c-fd29b6c5c646', 'Urine sugar', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 41. Haemoglobin -> Avni - Hb
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('a240115e-47a2-4244-8f74-d13d20f087df', 'Haemoglobin', 'Numeric',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 42. Malaria parasite -> Avni - Malaria parasite
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('9a89e9d6-f6e4-4d14-8841-34df9ece70a5', 'Malaria parasite', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 43. Random Blood Sugar (RBS) -> Avni - Random Blood Sugar (RBS)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('d6ac43a2-527d-4168-ba7d-2d233add3a6e', 'Random Blood Sugar (RBS)', 'Numeric',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 44. Glucose test (75gm Glucose) -> Avni - Glucose test (75gm Glucose)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('ae2046a4-015c-44e2-9703-01bc3da13202', 'Glucose test (75gm Glucose)', 'Numeric',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 45. Iron & Folic Acid -> Avni - Iron & Folic Acid
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('2c1760f2-c976-46be-b744-fa97c7448dff', 'Iron & Folic Acid', 'Numeric',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 46. Whether Folic acid given? -> Avni - Whether Folic acid given
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('bf1e5598-594c-4444-94e0-9390f5081e41', 'Whether Folic acid given?', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 47. Whether IFA given? -> Avni - Whether IFA given
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('5740f87b-8cc6-4927-88a2-44636e8f396c', 'Whether IFA given?', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 48. Whether Calcium given? -> Avni - Whether Calcium given
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('00de9acc-4ff6-485b-b979-41ff00745d23', 'Whether Calcium given?', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 49. Whether Amala given -> Avni - Whether Amala given
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('2a5a3b4d-80c4-4d05-8585-e16966ff0c3e', 'Whether Amala given', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 50. Does mother require other medicine? -> Avni - Does mother require other medicine?
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('567732c8-0d25-4155-9f97-28eb284c8963', 'Does mother require other medicine?', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 51. TT 1 -> Avni - TT 1
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('ddeb2311-4a90-4a7c-a698-1cd3db4ff0f3', 'TT 1', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 52. TT 2 -> Avni - TT 2
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('858f66e6-1ed3-4c13-9fdf-08f667b092ba', 'TT 2', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 53. Date of next ANC Visit -> Avni - Date of next ANC Visit
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('6e50431c-6cb0-495f-9735-dd431c9970ff', 'Date of next ANC Visit', 'Date',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 54. Date of next ANC Clinic Visit -> Avni - Date of next ANC Visit (alternate name)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('6e50431c-6cb0-495f-9735-dd431c9970ff', 'Date of next ANC Clinic Visit', 'Date',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 55. Referral required for? -> Avni - Referral required for?
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('049cbcbe-d953-49ad-8198-10ae86bc6bff', 'Referral required for?', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 56. Does woman require referral? -> Avni - Does woman require referral?
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('77d122e8-0620-4754-8375-b0cbe329003c', 'Does woman require referral?', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 57. Is referral required -> Avni - Does woman require referral? (alternate name)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('77d122e8-0620-4754-8375-b0cbe329003c', 'Is referral required', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 58. Place of referral -> Place of referral
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('80fccb06-a62f-43e8-92eb-358bdb600079', 'Place of referral', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 59. Other place of referral -> Other place of referral
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('d169efa9-49af-4c84-ae09-b1b7296c62da', 'Other place of referral', 'Text',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 60. Referral reason -> Referral reason, Admission
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('8a56f008-a910-4d6f-b028-a95db330dbf2', 'Referral reason', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 61. Other referral reason -> Avni - Other referral reason
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('e048675e-eb86-41c2-a47b-aecfa9a3bb8c', 'Other referral reason', 'Text',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 62. Referral place -> Avni - Referral place
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('d78183dd-aa99-47fa-b562-f761f5415028', 'Referral place', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 63. Foetus is ok -> Avni - Foetus is ok
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('6618dfd6-5df2-4c85-b916-0679bcb9be03', 'Foetus is ok', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 64. Presentation of baby -> Avni - Presentation of baby
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('e7b5d460-47dd-490b-af0a-a73a19a93a9d', 'Presentation of baby', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 65. Twin baby -> Avni - Twin baby
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('9f55f157-c835-4068-a948-c849073d1d86', 'Twin baby', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 66. Follow USG required -> Avni - Follow USG required
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('96499ee7-ad90-4831-a0bd-1fd765f6f6c0', 'Follow USG required', 'Coded',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 67. Date of next USG -> Avni - Date of next USG
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('71efca55-ad55-4814-a16f-44d714c6ecf5', 'Date of next USG', 'Date',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- 68. Next ANC Date -> Avni - next anc date
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('dd59a4df-f13b-4ead-9089-1dd1129d6b60', 'Next ANC Date', 'Date',
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false);

-- =====================================================
-- End of ANC Clinic Visit Mapping Configuration
-- Total: 2 encounter mappings + 68 observation mappings = 70 mappings
-- =====================================================
