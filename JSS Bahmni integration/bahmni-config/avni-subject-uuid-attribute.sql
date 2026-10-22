-- JSS Bahmni (OpenMRS MySQL): person attribute type that holds the Avni subject UUID of a linked patient.
-- The integration writes it; the "View in Avni" dashboard link reads it. Run once per Bahmni instance (production at go-live).
-- Prerelease already has it (created 2026-04-23). Keep the same UUID everywhere: the integration (constant
-- AvniSubjectUuidBahmniAttributeTypeUuid) and the dashboard link config refer to it.
--
-- Run on the Bahmni DB, e.g. on the server:
--   cd ~/bahmni-docker && docker compose exec openmrsdb mysql -u<db user> -p openmrs < avni-subject-uuid-attribute.sql

-- 1. Already there? (expect 1 row after step 2)
SELECT person_attribute_type_id, uuid, name, description, format, searchable, retired
FROM person_attribute_type WHERE uuid = 'ceddfccf-7913-4a63-b60e-d3114f867e4b';

-- 2. Create it if missing
INSERT INTO person_attribute_type (name, description, format, searchable, creator, date_created, retired, sort_weight, uuid)
SELECT 'avni_subject_uuid', 'Avni Subject UUID', 'java.lang.String', 0,
       (SELECT user_id FROM users WHERE username = 'superman'), NOW(), 0,
       (SELECT COALESCE(MAX(sort_weight), 0) + 1 FROM person_attribute_type pat), 'ceddfccf-7913-4a63-b60e-d3114f867e4b'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM person_attribute_type WHERE uuid = 'ceddfccf-7913-4a63-b60e-d3114f867e4b');

-- 3. Check again
SELECT uuid, name, description, format, searchable, retired
FROM person_attribute_type WHERE uuid = 'ceddfccf-7913-4a63-b60e-d3114f867e4b';
