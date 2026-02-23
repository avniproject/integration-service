-- =====================================================
-- ANC Clinic Visit - Answer Concept Mappings
-- These mappings translate Avni answer concept UUIDs to Avni answer names
-- for Coded observations in the ANC Clinic Visit form
-- Created: 2026-02-22
-- NOTE: Both Avni and Bahmni use the same answer concept UUIDs
-- =====================================================

-- Answer: 1+
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('d791406f-86bd-47ef-8c23-22accc521558', '1+', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: 2+
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('ba904c8d-502d-4979-bc99-cfa077678279', '2+', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: 3+
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('63513531-d5a4-47c1-a4ef-a02063b43871', '3+', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: 4+
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('b133d9ce-1661-420f-b73a-392300959b4e', '4+', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: A+
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('89d2a461-f458-4565-81df-4efabb333bd6', 'A+', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: A-
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('63b128d5-b29d-4759-b242-de24247681fc', 'A-', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: AB+
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('db83359e-4530-4a79-9981-36bdf03ffc5d', 'AB+', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: AB-
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('d9674584-51ca-40d1-86be-3f65f6b2f84c', 'AB-', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: AS
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('48c7a76c-13f8-4f1f-b09e-d4e00461e3c6', 'AS', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Abnormal crystel
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('cb4a9364-ac2f-41ac-af30-f60f1d50f48d', 'Abnormal crystel', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Achanakmar
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('1ac4611a-a6b2-4e5f-b78d-6021fcc34714', 'Achanakmar', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Actual data entry/min
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('b4f90265-4c84-492f-8ee5-8358dcb54966', 'Actual data entry/min', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Admission at Referral centre
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('dad7f494-99ec-42ce-b50b-07a7450be30c', 'Admission at Referral centre', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Age is greater than 30 years
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('56c6c67f-48b8-43a6-95ff-424e7c625645', 'Age is greater than 30 years', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Already Taken in Gov. Facility
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('50a3f54b-32aa-42b6-9d74-ee8a5f341d69', 'Already Taken in Gov. Facility', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Antepartum haemorrahoge
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('7756e71d-6802-4b82-8e74-36a7721b69dc', 'Antepartum haemorrahoge', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: As it is
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('64238ece-a3d6-4bfe-b666-a2d09b9360d9', 'As it is', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Audible
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('d67f3971-30cf-4293-83f7-f0f10f5f843d', 'Audible', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: B+
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('a2b9a6ca-9266-45b3-a842-fa5f7e9849ff', 'B+', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: B-
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('f781b597-449c-4636-8b52-e9cd1e495003', 'B-', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Bamhani
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('aedf7360-4ef2-437f-908e-cc3d819ccf7b', 'Bamhani', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Barar
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('cd54ed53-89e6-4129-9331-30bc35745809', 'Barar', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Bijara
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('cb77720c-0a5f-45dc-a7a4-03d2acf35d87', 'Bijara', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Bleeding
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('0aeb893c-34ba-49f0-bd6f-0696fce74458', 'Bleeding', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Bleeding PV
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('38efe56c-ba52-44b3-bca8-1d88032e3585', 'Bleeding PV', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Bloody nipple discharge
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('64b4da6a-2a29-45ff-b0b6-634fd5c89ffb', 'Bloody nipple discharge', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Blured vision
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('6fabfd13-9908-4a10-9bb7-39da1d674165', 'Blured vision', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Boiraha
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('940ccd9d-3c8c-4795-9e65-52120544298d', 'Boiraha', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Breach
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('37006041-5287-4913-b409-0ff8d0065557', 'Breach', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Breach presentation
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('d9338fbe-e943-41de-ae3b-911ef3a9a649', 'Breach presentation', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Breast lump
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('4ccdcc36-6fdf-4e94-bcda-f441a5124bb0', 'Breast lump', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Breathing difficulty
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('a72d4345-9ff2-410b-9c2f-ed61e4ea464b', 'Breathing difficulty', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Burning micturition
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('aa43a1cd-faa2-45cc-be26-1a19b0bc9580', 'Burning micturition', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Chuiha
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('d97cd90f-5642-449b-b637-bb9f0d980730', 'Chuiha', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Community Health Centre
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('9699d4fd-a67f-4f45-8d3c-df491f27db60', 'Community Health Centre', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Consultation with specialist at referral centre
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('f1ecf77c-122f-4462-8bc8-812013c80fa3', 'Consultation with specialist at referral centre', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Decreased
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('dd14086c-77ca-47d0-9593-d1a1bbfde905', 'Decreased', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Delivery/LACS
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('7d0d495a-e733-4ea8-93ca-c57bf42c6ee9', 'Delivery/LACS', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Diabetes
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('1928469b-a981-4579-beb7-ae47b972e671', 'Diabetes', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: District Hospital
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('602e1362-0c44-49d8-ac94-42a741c6273b', 'District Hospital', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Entire body
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('21eae23f-f925-4c69-bb1e-ce330aeb4b02', 'Entire body', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Face
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('4b228992-6d31-4e7a-bbcb-e319ffe672f8', 'Face', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Fetus growth retardation
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('f0103541-35c5-4440-842e-9f929242a9b1', 'Fetus growth retardation', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Fetus is not moving
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('7f358e8b-08d2-4243-bb0c-dda19c4997ae', 'Fetus is not moving', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Fever
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('232ae0c9-533f-48cf-8439-79aa03e23e6b', 'Fever', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Foetus is ok
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('6618dfd6-5df2-4c85-b916-0679bcb9be03', 'Foetus is ok', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Fundle Height doesn't correspond to the gestational week
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('f86f75fa-9464-44b7-958b-5f3ce04b4f87', 'Avni - Fundle Height doesn''t correspond to the gestational week', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Ganiyari
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('41f067f3-48a0-4f95-aee5-dcee95a8b9de', 'Ganiyari', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Ghameri
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('c776b982-48d1-40a4-9dea-033fbd809649', 'Ghameri', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Gravida/pregnancies more than 5
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('4c8e75b7-28c3-4dbd-b09f-df3b6f57218b', 'Gravida/pregnancies more than 5', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: HIV (Elisa) is positive
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('ac1a0235-9881-4a50-aa85-f1d43443d5a8', 'HIV (Elisa) is positive', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Headache
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('44683953-68ed-4498-81e8-924879c59117', 'Headache', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Heart disease
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('c8a0b118-3326-4948-9fb5-529f6f5d2b96', 'Heart disease', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: High Blood Sugar
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('fb6afb2e-02ee-473b-82a5-f4cfb45eda80', 'High Blood Sugar', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: High Glucose Level
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('c94fbd91-b122-4c85-878c-1e4ecae1d003', 'High Glucose Level', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Hydroamniosis
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('2720cec8-db49-49e5-a1d7-9e1fb45d136d', 'Hydroamniosis', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Hydroamniosis/Twins
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('1f8d2a21-3f34-478b-9a19-2002205c4051', 'Hydroamniosis/Twins', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Hypertension
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('e7e56956-effe-4cc7-85a6-f726b6bf1ffb', 'Hypertension', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Increased
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('f9001edc-0f9a-4eb3-949c-3238ee8adecb', 'Increased', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Infection
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('e668ceed-92ee-4b2f-8714-72838f11264b', 'Infection', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: JSS Ganiyari hospital
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('b03ca67b-390c-4223-9084-577ac737100a', 'JSS Ganiyari hospital', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: JSS subcenter
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('b704eaf7-3cfb-4016-a884-456e1f0b7e62', 'JSS subcenter', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Jaundice
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('49adc061-6cac-4d92-9faa-7d3279bb77f4', 'Jaundice', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Karhikachar
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('24afb860-4fc0-4068-ba8b-452f2748ee0e', 'Karhikachar', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Karka
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('8610a1b7-4482-443e-a9e2-33feed1e04b0', 'Karka', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Leprosy
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('8f50e18a-f14e-4128-82e3-19624cedba47', 'Leprosy', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Low Haemoglobin
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('b45aaa32-e4f1-4fc0-9d78-2dd8e8365dd4', 'Low Haemoglobin', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Manpur
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('b998c308-45b8-4bde-8e24-cc0f4a9a8e96', 'Manpur', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Mastitis
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('bc33afcc-d57a-4925-bd07-fef33097cf47', 'Mastitis', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Medical college
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('3ab10f93-7267-4469-b81d-1f3d6c6bdc99', 'Medical college', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: NA
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('7efbfba4-66d1-4510-9024-aaaedf439392', 'NA', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Negative
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('0fe2ae63-332a-40bf-82c7-371ef7b66494', 'Negative', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Nevsa & Karpiha
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('c784e3ed-fd4f-47dc-80ee-03d517836d2a', 'Nevsa & Karpiha', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Nil
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('7f2382b9-e15b-4ff0-a5a6-acb88470100c', 'Nil', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: No
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('f88da2e8-6ab4-44b5-b762-233485cd25f9', 'No', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: No ANC clinic
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('48101a2e-4799-4bd6-98f2-afd653bc893d', 'No ANC clinic', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: No foetal movement
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('99a2d857-20e0-4bcf-acc8-2f0be2c70663', 'No foetal movement', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: No need
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('cda516ab-5d80-45d8-b300-582f7cfb87eb', 'No need', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: No oedema
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('6f511b7a-00d4-45a1-ba45-8b1ce608b014', 'No oedema', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: None
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('8b77f487-c885-4b54-8925-153733913b10', 'None', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Normal crystel
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('d2e7fe4e-3a7f-435d-a973-e114bb849a56', 'Normal crystel', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Normal nipple
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('cbc8d904-4309-4b27-86a7-025654fda538', 'Normal nipple', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Not audible/Can't locate
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('05580ea9-fb5c-4f8f-aeae-8c120950de92', 'Avni - Not audible/Can''t locate', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Not delivered even after 14 days of EDD
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('ca4ac889-a095-4770-bbd8-1c3041781fb5', 'Not delivered even after 14 days of EDD', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Not done
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('5826df5f-d771-4559-b2be-2f1391805076', 'Not done', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: O+
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('0e8d5617-3d7f-4585-adee-4b439088bbfe', 'O+', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: O-
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('2fdc8bab-d738-4362-86fc-27b14a0ab7ba', 'O-', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Other
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('798b7942-cf58-4d4e-a529-5972942e0a7e', 'Other', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Pain in abdomen
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('cb8653a9-3248-440c-aef1-17683e49233b', 'Pain in abdomen', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Pedal
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('5d0462d9-1c5c-42bc-a3e3-8c75cf6b6986', 'Pedal', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Piparkhuti
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('9b0954ff-73db-4286-940c-d9355d6a352c', 'Piparkhuti', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Positive
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('6f6458f7-ce0b-46c2-bd03-0fa30943d75d', 'Positive', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Pregnancy induced diabetes
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('6d5601e5-4ddd-4263-97f8-c0c033b1a714', 'Pregnancy induced diabetes', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Previous C-section scar
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('d097c766-9539-4bf8-9efd-e6ebc77e0295', 'Previous C-section scar', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Raised BP
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('1f09b442-8de2-450c-9dcd-eab08e5b69fe', 'Raised BP', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Retracted nipple
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('7a6870a8-62f1-4751-8fd1-ef63715876e2', 'Retracted nipple', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Rh negative blood group
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('78127122-066c-4917-80f3-2682830c590c', 'Rh negative blood group', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: SHW
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('7ac0d9a2-ff82-41e6-9f38-134904d476f9', 'SHW', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: SS
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('1f43668b-d1fd-41ef-afa3-38c265d467ac', 'SS', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Seizure
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('cbc1b5b1-e4f9-4c0e-abe6-cc03362ff08d', 'Seizure', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Semaria-1
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('6b44eae3-0290-40ea-b60e-677bc0cbbc1f', 'Semaria-1', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Semaria-2
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('d3637bbf-badf-4ff8-bc10-3b184e97cadd', 'Semaria-2', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Shivtarai
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('85c10514-22a5-4909-aa52-2c3d533ce4d5', 'Shivtarai', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Short stature
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('f9fc9347-ca4e-4f92-b393-6fa2f5f6d19b', 'Short stature', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Sickle cell disease
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('77ac9c58-afaa-42ba-88d2-992bd0db63d7', 'Sickle cell disease', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Sickle prep
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('610db330-fafe-456f-bd58-e062cb6e52e3', 'Sickle prep', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Sonography
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('1a893a89-4984-43e1-85cc-b64fb582824c', 'Sonography', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Sub Center
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('31a0f7f1-7b23-4436-ae0a-0a982c11077c', 'Sub Center', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Swelling all over body
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('e2fbcb43-c533-40bc-b36d-5850c17d4a04', 'Swelling all over body', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Swelling on face/vulva/abdomen/whole body
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('19003a06-5195-49ef-9f4f-52462fe6f17c', 'Swelling on face/vulva/abdomen/whole body', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: TB
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('7943642f-c96f-4de6-a470-b9cb0b45feb0', 'TB', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Trace
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('ae92d882-9ca4-448b-9cbf-f7459548bd55', 'Trace', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Transverse
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('901014fd-60bb-4843-a319-c3d58e2bdb19', 'Transverse', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Transverse lie
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('06314b86-e90b-4ad5-aa52-6597270816af', 'Transverse lie', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Twins
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('192cad34-767c-43a0-8b04-0443ca87a6a0', 'Twins', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Urine Albumin Present
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('f4bc4dd5-c028-49d2-a1df-818aeb9e17cc', 'Urine Albumin Present', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: VDRL is positive
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('1f70c8c2-8789-42ad-be5e-db94bd3d38e4', 'VDRL is positive', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: VHW
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('355143bf-b812-4ec8-93e6-6795fe4f7dc9', 'VHW', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Vertex
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('d49d3369-74aa-4d45-a1f0-6578453fddb6', 'Vertex', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Vomiting
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('9ad19f3b-3a8c-4e7e-81d8-12f796763291', 'Vomiting', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Vulval
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('f9f1945f-eb75-4ddb-b354-919afc5f53c4', 'Vulval', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Weight is less than 35 kg
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('56e1dbb0-f4ff-42bf-9610-d56993d8e5b8', 'Weight is less than 35 kg', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Yes
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('57e20de7-10de-4391-b7ce-87b2f40d19a2', 'Yes', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: infec
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('8293fa19-4fd6-4942-bc93-032bc3d39e68', 'infec', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Numeric range mappings for Fundle Height and other numeric fields
-- Answer: 12-14
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('be02d807-e70a-43a4-bb5e-dbd8e250f2c9', '12-14', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: 14-16
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('6fa2d5e5-b709-4690-a141-4d86d0b7c7ab', '14-16', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: 16-18
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('b58302a2-1050-454b-81f4-aad5e8ab3854', '16-18', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: 18-20
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('18fa41fe-6719-49a2-b0df-7527fbcce6bf', '18-20', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: 20-22
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('18c1a185-b33c-4dc8-816b-18365744bd13', '20-22', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: 22-24
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('f04d5228-54b6-4c48-a412-b0dbb6abd477', '22-24', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: 24-26
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('2cef8d3a-4699-46ab-8e3e-ff1d758b5b23', '24-26', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: 26-28
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('0c722806-f54c-40f6-aa6a-1475815dc498', '26-28', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: 28-30
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('230f8227-0d49-49f8-9052-0c5df27215a4', '28-30', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: 30-32
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('f91ae1aa-7cda-4b75-9979-dd8c141ed405', '30-32', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: 32-34
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('72c464db-93c7-431f-8ca2-75a4ff081482', '32-34', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: 34-36
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('e0451d57-3185-4ab6-9fff-f3801523f9ba', '34-36', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: 36-38
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('7340c96d-80cb-430e-b89d-ebb664293767', '36-38', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: 38-40
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('a7442dd5-a548-4ac9-a758-167afb326b0c', '38-40', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

