-- =====================================================
-- Complete ANC Program Sync Configuration
-- Configured for JSS Ganiyari environment
-- =====================================================

-- SECTION 1: CONSTANTS
INSERT INTO constants (key, value, uuid, is_voided)
VALUES ('IntegrationBahmniLocation', '58638451-1102-4846-8462-503e0ddd792f', uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

INSERT INTO constants (key, value, uuid, is_voided)
VALUES ('IntegrationBahmniVisitType', '2ee11869-f426-44a5-9766-1fb195a1c56f', uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

INSERT INTO constants (key, value, uuid, is_voided)
VALUES ('IntegrationBahmniProvider', 'apiuser', uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

INSERT INTO constants (key, value, uuid, is_voided)
VALUES ('IntegrationBahmniEncounterRole', 'Provider', uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

INSERT INTO constants (key, value, uuid, is_voided)
VALUES ('IntegrationBahmniIdentifierType', 'b46af68a-c79a-11e2-b284-107d46e7b2c5', uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

INSERT INTO constants (key, value, uuid, is_voided)
VALUES ('BahmniIdentifierPrefix', 'GAN', uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

INSERT INTO constants (key, value, uuid, is_voided)
VALUES ('IntegrationAvniSubjectType', 'Individual', uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- SECTION 2: Ensure mapping groups exist (idempotent)
INSERT INTO mapping_group (name, integration_system_id, uuid, is_voided)
SELECT 'Common', 1, uuid_generate_v4(), false
WHERE NOT EXISTS (SELECT 1 FROM mapping_group WHERE name = 'Common' AND integration_system_id = 1);

INSERT INTO mapping_group (name, integration_system_id, uuid, is_voided)
SELECT 'ProgramEnrolment', 1, uuid_generate_v4(), false
WHERE NOT EXISTS (SELECT 1 FROM mapping_group WHERE name = 'ProgramEnrolment' AND integration_system_id = 1);

-- SECTION 3: Ensure mapping types exist (idempotent)
INSERT INTO mapping_type (name, integration_system_id, uuid, is_voided)
SELECT 'AvniEventDate_Concept', 1, uuid_generate_v4(), false
WHERE NOT EXISTS (SELECT 1 FROM mapping_type WHERE name = 'AvniEventDate_Concept' AND integration_system_id = 1);

INSERT INTO mapping_type (name, integration_system_id, uuid, is_voided)
SELECT 'AvniUUID_VisitAttributeType', 1, uuid_generate_v4(), false
WHERE NOT EXISTS (SELECT 1 FROM mapping_type WHERE name = 'AvniUUID_VisitAttributeType' AND integration_system_id = 1);

INSERT INTO mapping_type (name, integration_system_id, uuid, is_voided)
SELECT 'AvniEventDate_VisitAttributeType', 1, uuid_generate_v4(), false
WHERE NOT EXISTS (SELECT 1 FROM mapping_type WHERE name = 'AvniEventDate_VisitAttributeType' AND integration_system_id = 1);

INSERT INTO mapping_type (name, integration_system_id, uuid, is_voided)
SELECT 'CommunityEnrolment_VisitType', 1, uuid_generate_v4(), false
WHERE NOT EXISTS (SELECT 1 FROM mapping_type WHERE name = 'CommunityEnrolment_VisitType' AND integration_system_id = 1);

-- SECTION 4: Create core mappings (Common Group)
-- Note: Get current IDs dynamically to handle duplicate groups
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES (
    '3c474750-312b-4d79-b449-6e486ae7f34b',
    'Avni Entity UUID',
    NULL,
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Common' AND integration_system_id = 1),
    (SELECT id FROM mapping_type WHERE name = 'AvniUUID_Concept' AND integration_system_id = 1),
    uuid_generate_v4(),
    false
)
ON CONFLICT DO NOTHING;

-- AvniEventDate_Concept mapping
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES (
    '94d9e354-8fe7-487d-9262-7807f76eb18c',
    'Avni Event Date',
    NULL,
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Common' AND integration_system_id = 1),
    (SELECT id FROM mapping_type WHERE name = 'AvniEventDate_Concept' AND integration_system_id = 1),
    uuid_generate_v4(),
    false
)
ON CONFLICT DO NOTHING;

-- AvniUUID_VisitAttributeType mapping
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES (
    '7e9ed688-f2c1-46f2-b904-bc528dee335a',
    'Avni UUID',
    NULL,
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Common' AND integration_system_id = 1),
    (SELECT id FROM mapping_type WHERE name = 'AvniUUID_VisitAttributeType' AND integration_system_id = 1),
    uuid_generate_v4(),
    false
)
ON CONFLICT DO NOTHING;

-- AvniEventDate_VisitAttributeType mapping
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES (
    '7c24d353-718c-460f-8afc-3967461c8a01',
    'Avni Event Date',
    NULL,
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'Common' AND integration_system_id = 1),
    (SELECT id FROM mapping_type WHERE name = 'AvniEventDate_VisitAttributeType' AND integration_system_id = 1),
    uuid_generate_v4(),
    false
)
ON CONFLICT DO NOTHING;

-- SECTION 5: Program Enrolment Mappings
-- CommunityEnrolment_VisitType: Maps program names to visit type UUID
-- Note: Both "ANC" and "Pregnancy" (actual Avni program name) map to same visit type
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES (
    '2ee11869-f426-44a5-9766-1fb195a1c56f',
    'ANC',
    NULL,
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'ProgramEnrolment' AND integration_system_id = 1),
    (SELECT id FROM mapping_type WHERE name = 'CommunityEnrolment_VisitType' AND integration_system_id = 1),
    uuid_generate_v4(),
    false
)
ON CONFLICT DO NOTHING;

-- Pregnancy program mapping (actual program name in Avni for ANC Clinic Visit encounters)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES (
    '2ee11869-f426-44a5-9766-1fb195a1c56f',
    'Pregnancy',
    NULL,
    1,
    (SELECT MIN(id) FROM mapping_group WHERE name = 'ProgramEnrolment' AND integration_system_id = 1),
    (SELECT id FROM mapping_type WHERE name = 'CommunityEnrolment_VisitType' AND integration_system_id = 1),
    uuid_generate_v4(),
    false
)
ON CONFLICT DO NOTHING;

-- =====================================================
-- CONFIGURATION COMPLETE
-- =====================================================
