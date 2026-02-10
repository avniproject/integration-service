-- =====================================================
-- Subject to Bahmni Mapping Configuration
-- Avni Subject: Individual
-- Bahmni Encounter Type: Patient Registration [A]
-- =====================================================

-- Section 1: Ensure Mapping Groups Exist
-- -----------------------------------------------------
INSERT INTO mapping_group (name, integration_system_id, uuid, is_voided)
SELECT 'PatientSubject', id, uuid_generate_v4(), false
FROM integration_system WHERE name = 'bahmni'
ON CONFLICT DO NOTHING;

INSERT INTO mapping_group (name, integration_system_id, uuid, is_voided)
SELECT 'Observation', id, uuid_generate_v4(), false
FROM integration_system WHERE name = 'bahmni'
ON CONFLICT DO NOTHING;

-- Section 2: Ensure Mapping Types Exist
-- -----------------------------------------------------
INSERT INTO mapping_type (name, mapping_group_id, integration_system_id, uuid, is_voided)
SELECT 'Subject_EncounterType',
    (SELECT id FROM mapping_group WHERE name = 'PatientSubject' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    id, uuid_generate_v4(), false
FROM integration_system WHERE name = 'bahmni'
ON CONFLICT DO NOTHING;

INSERT INTO mapping_type (name, mapping_group_id, integration_system_id, uuid, is_voided)
SELECT 'Subject_BahmniForm',
    (SELECT id FROM mapping_group WHERE name = 'PatientSubject' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    id, uuid_generate_v4(), false
FROM integration_system WHERE name = 'bahmni'
ON CONFLICT DO NOTHING;

INSERT INTO mapping_type (name, mapping_group_id, integration_system_id, uuid, is_voided)
SELECT 'PatientIdentifier_Concept',
    (SELECT id FROM mapping_group WHERE name = 'PatientSubject' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    id, uuid_generate_v4(), false
FROM integration_system WHERE name = 'bahmni'
ON CONFLICT DO NOTHING;

INSERT INTO mapping_type (name, integration_system_id, uuid, is_voided)
SELECT 'Concept', id, uuid_generate_v4(), false
FROM integration_system WHERE name = 'bahmni'
ON CONFLICT DO NOTHING;

-- Section 3: Subject Encounter Type Mapping
-- -----------------------------------------------------
-- Maps Individual (Avni) to "Patient Registration [A]" (Bahmni)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES (
    'b805a574-f22a-4a6f-95f8-6ff24c4a8d59',
    'Individual',
    NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'PatientSubject' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Subject_EncounterType' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(),
    false
);

-- Section 4: Subject Form Mapping
-- -----------------------------------------------------
-- Maps Individual form to Bahmni concept-set (Avni - JSS Registration Form)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES (
    '4b23dc0e-3ae0-4d41-8546-35797063e123',
    'Individual',
    NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'PatientSubject' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Subject_BahmniForm' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(),
    false
);

-- Section 5: Patient Identifier Mapping
-- -----------------------------------------------------
-- Maps Avni Bahmni JSS ID to Bahmni Patient Identifier Type
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES (
    '15c84573-8294-4e93-8d34-1028848eadca',
    'Avni Bahmni JSS ID',
    NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'PatientSubject' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'PatientIdentifier_Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(),
    false
);

-- Section 6: Observation/Concept Mappings for Registration
-- -----------------------------------------------------
-- MappingGroup: Observation, MappingType: Concept

-- 1. Birth Order
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('821ba930-505c-4fd3-9f24-66b60ed45bac', 'Birth Order', 'Numeric',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 2. Father's Name
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('9e6983b8-06ef-4648-b360-6684100b1be1', 'Father''s Name', 'Text',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 3. Father's Occupation
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('bf564151-63f9-4176-917f-f37de34b9bae', 'Father''s Occupation', 'Coded',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 4. Mother's Name
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('74a554d8-5b87-4d27-9ae5-272ab326608f', 'Mother''s Name', 'Text',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 5. Mother's Occupation
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('ea760e4f-c12f-490b-9865-9c6e4510ce64', 'Mother''s Occupation', 'Coded',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 6. Father's Education Level
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('b1001c4d-0449-464a-947f-a04c4fdcc651', 'Father''s Education Level', 'Coded',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 7. Mother's Education Level
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('d98aae1a-ce33-4e51-b031-66e13bc0ba11', 'Mother''s Education Level', 'Coded',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 8. Caste Category
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('9ad4b520-4e33-4b1b-a056-37ae6418988f', 'Caste Category', 'Coded',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 9. Sub Caste
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('047877ac-dba7-4acf-8c77-97c979c2fc26', 'Sub Caste', 'Coded',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 10. Other Sub Caste
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('ae7d54e9-fac0-4898-b334-87664bd055d2', 'Other Sub Caste', 'Text',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 11. Religion
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('b2c60cb8-983c-4e0e-a90d-4b21e87e10bd', 'Religion', 'Coded',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 12. Aadhaar ID
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('681fce2b-ea38-4651-a0b8-2cddd307ade7', 'Aadhaar ID', 'Numeric',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 13. Contact Number
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('0a725832-b21c-4151-b017-7e6af770ba54', 'Contact Number', 'Text',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 14. Date of Marriage
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('9d958124-09bb-466c-a4b4-db8d285def1f', 'Date of Marriage', 'Date',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 15. Education
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('673d65bd-6dc4-4aac-8e1e-1ee355ac081b', 'Education', 'Coded',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 16. Occupation
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('20ef261a-f110-4eaa-a592-2a1eeb0bf061', 'Occupation', 'Coded',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 17. Other Occupation
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('4c429211-634e-4c2b-9a31-3f0a395f8f8d', 'Other Occupation', 'Text',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 18. Marital Status
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('9e995ea6-a5f7-410f-adc2-2d2ce6d5e19b', 'Marital Status', 'Coded',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 19. Father/Husband's Name
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('ecdf3c54-2808-494d-87be-8fb744d5c3bc', 'Father/Husband''s Name', 'Text',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 20. First Name (Hindi)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('de490ab6-5c24-4de5-9f95-fe78be1b0c11', 'First Name (Hindi)', 'Text',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 21. Last Name (Hindi)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('9131372e-6e9b-4d07-b088-d7e961c61f76', 'Last Name (Hindi)', 'Text',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 22. Individual Id
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('8033840e-a347-474d-a6ad-861ebffcec00', 'Individual Id', 'Text',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 23. Relation to Head of Household
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('eaee156e-8ef3-4148-a80c-a466cd059ae3', 'Relation to Head of Household', 'Coded',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 24. Whether Any Disability
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('bab107f6-fc0e-4be7-ab71-658a92d72f35', 'Whether Any Disability', 'Coded',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 25. Type of Disability
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('7061c675-c2ba-4016-886d-eeb432548378', 'Type of Disability', 'Coded',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 26. Status of Individual
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('d333f2a2-717e-478f-acbc-173bc7374d66', 'Status of Individual', 'Coded',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 27. Electricity in House
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('e23ef639-5d54-46bc-811c-ee1886bce81f', 'Electricity in House', 'Coded',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 28. Smart Card (Insurance)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('2a445ac8-56e7-4eda-8756-0a9c4fa9a77b', 'Smart Card (Insurance)', 'Coded',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 29. Is Sterilization Done
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('852f4e54-4969-4724-94e0-cddef0ac1f66', 'Is Sterilization Done', 'Coded',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 30. Non Programme Village Name
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('1c710642-4f37-4f47-9df9-393127eaafc9', 'Non Programme Village Name', 'Text',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 31. Ration Card
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('86fc3018-8eeb-4a58-a9d9-a40fff839305', 'Ration Card', 'Coded',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 32. Land Possession
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('b984ad33-05d8-4621-adf3-152e72a0db1b', 'Land Possession', 'Coded',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 33. Land Area
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('430ebb19-831d-470d-80eb-7969814f13e4', 'Land Area', 'Numeric',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 34. Property
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('aa88dba4-4f5d-4d35-9dc1-2390969cc5f3', 'Property', 'Coded',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 35. Other Property
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('32609e0f-f3c8-4dcb-af7c-5e8a96e8e89d', 'Other Property', 'Text',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- 36. Type of Residence
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('c5d2673b-0f5c-48bf-93e4-f1a1ae820732', 'Type of Residence', 'Coded',
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- Section 7: Answer Concept Mappings for Coded fields
-- -----------------------------------------------------

-- Yes/No answers
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('57e20de7-10de-4391-b7ce-87b2f40d19a2', 'Yes', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('f88da2e8-6ab4-44b5-b762-233485cd25f9', 'No', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- Occupation answers
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('1e24aee4-fb61-4d60-b61a-cdde17e4545f', 'Business', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('c483c8de-1e93-40e7-9e9a-dbe1c7e705a1', 'Don''t Know', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('d06f8595-d126-4b6b-8305-ef7633892d75', 'Farming', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('27f77972-71d6-466c-94a3-214b4b4a23f6', 'Job', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('8ee4ddf3-34a4-4ec7-ac47-08498e8ef4a8', 'Labour', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('798b7942-cf58-4d4e-a529-5972942e0a7e', 'Other', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('d7d45e44-8ce9-42d4-a98e-78d11c58d899', 'Housework', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- Caste Category answers
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('9b3a3dca-b93a-4b67-975f-5f6f1a815cea', 'General', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('8aa7b5ea-5d35-4a08-af33-0345af903e2f', 'OBC', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('0b78a94a-c652-4d59-ad16-c9138db05e29', 'SC', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('8d1c99b2-fbed-4407-9f6a-040b9a5731b2', 'ST', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- Religion answers
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('a1a0ecc3-2a85-43c4-b75a-3544a1ada0bf', 'Christian', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('6dbd4a89-d040-4ed3-bf1d-23bafdb1553e', 'Hindu', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('e8bb7929-2357-49ff-8c98-28b338f2c6d8', 'Jain', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('fd46c9f7-783a-4cc0-b6b3-489aeb28f26c', 'Muslim', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('2f6e7ba7-15dc-4af3-b4d7-e971e6733525', 'Sikh', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- Marital Status answers
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('ba48e389-23df-43e4-89a6-85f392d4312e', 'Currently Married', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('1bc8befa-49b7-4a70-8203-8ec5d0660783', 'Divorced', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('c219f819-db31-4a13-a895-fe67155e284b', 'Remarried', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('568a29bd-40fc-4261-bb85-9f9366b9be1b', 'Separated', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('4a4cc0eb-25c0-47b8-b9dd-de2dbefc1425', 'Unmarried', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('9329d91a-385b-4633-a788-c10ccb7d1143', 'Widow(er)', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- Education answers
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('9ac76826-0daa-4b90-9a9c-e269ac168005', 'Education 1-5', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('d3ff5645-4c84-47f4-9f35-6d0242cd727c', 'Education 11-12', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('0b2b6bb5-7eb8-4b14-bc68-662fade3aca4', 'Education 6-7', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('6b941c49-349f-40f6-a4ae-bdd00f45fbba', 'Education 8-10', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('ff7c5cd7-fe19-4a64-b8cb-700b4ad7a58f', 'Graduation', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('45b68efb-635d-48ae-9fdf-64f0ea3f46db', 'Illiterate', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('9fa90cc8-0dc0-43ee-820f-39f8fe3ca1ec', 'Literate Without Schooling', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('7efbfba4-66d1-4510-9024-aaaedf439392', 'NA', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('4c9f0571-a3ce-4026-854e-a28b388b0c0f', 'Post Graduation', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- Occupation (detailed) answers
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('1100010c-b1fe-4d14-a2ec-c46a8af3cbb1', 'Daily Wage Labourer', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('f021800c-b3f5-4c70-b90a-dcbb83227b55', 'Farmer', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('fd6a3706-f1f3-4f86-96a3-80b6866612a2', 'Government Job', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('0bef903a-20e5-4efa-940a-37d419f68f7c', 'Private Job', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('2a5dc992-7a1e-4fb2-9a9d-3093f7c9fca3', 'Professional/Technical', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('4a13e345-7d13-4649-a370-1722a596d0fe', 'Shopkeeper', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('84000e44-81ea-425f-8c88-9e4b111a5570', 'Skilled Manual', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('fc63152f-457d-4392-aa80-87e6fd19f20a', 'Unemployed', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- Ration Card answers
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('78b4a024-82af-42fe-bf58-6da696073af9', 'Antyodaya', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('3eac5822-f5fe-421d-8cdd-e89bef20baca', 'APL', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('906f6286-515b-4ff5-8065-0fcedef003b9', 'BPL', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- Type of Residence answers
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('7f731647-b8f8-4d5b-be2d-8d3ebd9cba05', 'Aadha Kacha Pakka', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('65b3e4db-3c4e-4a56-9a2f-c07e9888e710', 'Kaccha', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('bc43ce86-7cd4-474c-bced-8a5af0a4cb87', 'Pakka', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- Property answers
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('03c61006-8dc8-449e-9486-ecfba3e84f99', '2 Wheeler', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('ad8156fd-c3ac-461c-8611-a5cf4cfbcc7e', '4 Wheeler', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('12ee51fa-d8a5-4c8d-b88a-3a9c2f0dfaf1', 'Cycle', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('3b4d35f0-ef97-4954-8ee2-69d5db4894e6', 'Fridge', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('94099e11-6e9e-4c9e-9a68-83dee9d6042a', 'Generator', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('8b77f487-c885-4b54-8925-153733913b10', 'None', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('345821de-b302-4df3-9754-c555dfa530a3', 'Tractor', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- Status of Individual answers
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('a8a31361-df8c-4b6e-8075-639f436d0b3f', 'Birth Status', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('c125eec0-2bc4-4add-b1d0-7531e5ffa9c8', 'Death Status', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('4d5e83b5-ddc3-4668-930f-96da0cce7763', 'Female Marriage', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('285a38f0-52a1-4021-90cc-3ba8cbe88371', 'In Migrant', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('e57e1f8d-0a6c-4470-b692-3dc82cce0dfc', 'Out Migrant', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('cc8a0f5e-be37-4e6a-8f49-41c9f7d2f839', 'Resident', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- Type of Disability answers
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('6609670d-cd0e-414d-afde-af1d491d8b97', 'Arthritis', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('33f8f979-d573-4aed-808f-e241dc53179f', 'Back Pain', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('33abbad1-4e96-4e80-951d-832607accfc5', 'Cerebral Palsy', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('d15dcefe-df9a-4bdc-9775-118bc57990ed', 'Hearing Impairement', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('03e03f5d-4975-4d64-82fe-4c825cf1852c', 'Locomotor', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('2c8705d4-b7f5-4d08-a515-cd8dbd4207d8', 'Mental Illness', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('7fb6744e-24c7-461f-9091-6c66156ebea0', 'Mental Retardation', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('c48b441b-5b7e-4c8c-9c6d-b91c3a7941fe', 'Paralysis', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('5b5fbb3d-1fe7-4711-bfce-77bbc465b832', 'Post Burn', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('17b4f868-4a61-4316-9338-4c458b0e45b5', 'Post Fracture', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('9bcbb18e-b680-45bf-a6b5-d9d47e7cca1b', 'Post Operative', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('301ec1f5-9d8e-4fbf-8f2b-f3c883457622', 'Speech Impairement', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('18da1ae2-ec99-40dc-af1f-f6f86e5fc399', 'Vision Impairement', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

-- Sub Caste answers
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('2b90b234-8a63-4d4c-92d0-6389d8b1fb78', 'Baiga', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('7335b4a6-fd22-4061-ba4f-54c5f3d1bfbe', 'Gond', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('0bebd451-d11f-404e-9207-6bd189eb83cf', 'Oraon', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('0e03a56d-dfca-4365-b10b-d1bc8390ff87', 'Patel', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('38e6aaf2-d6cd-4a7b-9087-4bdc3df31ece', 'Satnami', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);

INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('65b5abfa-9229-48bf-8ed7-40e715840302', 'Yadav', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')),
    uuid_generate_v4(), false);
