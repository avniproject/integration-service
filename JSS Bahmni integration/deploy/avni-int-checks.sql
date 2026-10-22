-- JSS Ganiyari: checks on the integration DB (avni-int) after deploying the JSS branch.
-- Read-only. Run on prerelease (and later production) avni-int.

-- 1. JSS migrations applied (expect V2_5_0, V2_5_1, V2_5_2 with success = true)
SELECT script, success, installed_on FROM flyway_schema_history WHERE script LIKE 'V2_5_%' ORDER BY installed_rank;

-- 2. Old trial migrations gone from history (prerelease: expect 0 after the clean-up script)
SELECT count(*) AS old_jss_migrations FROM flyway_schema_history WHERE script ~ '^V2_4_(1[1-9]|2[0-9]|3[0-9]|5|6|7|8)__(Bahmni|ANC|Diabetes|Fix|Add|Final|Aggressive|Cleanup|AllTests|Drug|Revert|AllForm|Missing|Avni)';

-- 3. Bahmni mappings loaded (expect ~2,900: forms + questions + answers)
SELECT t.name AS mapping_type, count(*) FROM mapping_metadata m JOIN mapping_type t ON t.id = m.mapping_type_id
WHERE m.integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni') AND NOT m.is_voided
GROUP BY t.name ORDER BY 2 DESC;

-- 4. Settings (expect GAN prefix, identifier type, Individual, attribute type, 9 visit types)
SELECT key, value FROM constants
WHERE key IN ('BahmniIdentifierPrefix', 'IntegrationBahmniIdentifierType', 'IntegrationAvniSubjectType', 'AvniSubjectUuidBahmniAttributeTypeUuid', 'OutpatientVisitTypes')
ORDER BY key, value;

-- 5. Link-error types (expect 4)
SELECT name FROM error_type WHERE integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')
AND name IN ('BahmniPatientNotFound', 'MultipleBahmniPatientsWithId', 'BahmniIdOnMultiplePeople', 'BahmniPatientLinkedToAnotherPerson');

-- 6. Link job bookmark (1900-01-01 before the first run, then moves forward)
SELECT entity_type, read_upto_date_time FROM integrating_entity_status WHERE entity_type = 'JssIdUpdate';

-- 7. After runs: open ID problems (same as the Metabase report) and sync errors by type
SELECT et.name AS error_type, count(*) FROM error_record r
JOIN LATERAL (SELECT error_type_id FROM error_record_log WHERE error_record_id = r.id ORDER BY logged_at DESC LIMIT 1) l ON TRUE
JOIN error_type et ON et.id = l.error_type_id
WHERE r.integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')
GROUP BY et.name ORDER BY 2 DESC;
