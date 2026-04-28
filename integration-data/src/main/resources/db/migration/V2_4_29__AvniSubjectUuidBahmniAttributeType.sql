-- Constant for the Bahmni person attribute type that stores the Avni subject UUID.
-- Required for the "View on Avni" deep link feature (Phase 1 of JSS Ganiyari integration).
-- The attribute type must already exist in Bahmni (uuid: ceddfccf-7913-4a63-b60e-d3114f867e4b).
INSERT INTO constants (key, value, uuid, is_voided)
SELECT 'AvniSubjectUuidBahmniAttributeTypeUuid', 'ceddfccf-7913-4a63-b60e-d3114f867e4b', uuid_generate_v4(), false
WHERE NOT EXISTS (
    SELECT 1 FROM constants WHERE key = 'AvniSubjectUuidBahmniAttributeTypeUuid'
);
