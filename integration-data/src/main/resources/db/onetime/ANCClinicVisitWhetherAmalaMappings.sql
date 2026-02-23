-- =====================================================
-- ANC Clinic Visit - "Whether Amala given" Answer Mappings
-- Required: Bahmni answer UUIDs for Yes and No
-- Created: 2026-02-22
-- =====================================================

-- IMPORTANT: Get Bahmni answer UUIDs by running:
-- ./get-bahmni-concept-answers.sh
-- or query manually:
-- curl -s -u admin:password "https://jss-bahmni-prerelease.avniproject.org/openmrs/ws/rest/v1/concept/2a5a3b4d-80c4-4d05-8585-e16966ff0c3e?v=full" | jq '.answers[] | {name, uuid}'

-- Answer: Whether Amala given - Yes (Avni - Yes)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('57e20de7-10de-4391-b7ce-87b2f40d19a2', 'Yes', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;

-- Answer: Whether Amala given - No (Avni - No)
INSERT INTO mapping_metadata (int_system_value, avni_value, data_type_hint, integration_system_id, mapping_group_id, mapping_type_id, uuid, is_voided)
VALUES ('f88da2e8-6ab4-44b5-b762-233485cd25f9', 'No', NULL,
    (SELECT id FROM integration_system WHERE name = 'bahmni'),
    (SELECT id FROM mapping_group WHERE name = 'Observation' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    (SELECT id FROM mapping_type WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') LIMIT 1),
    uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;
