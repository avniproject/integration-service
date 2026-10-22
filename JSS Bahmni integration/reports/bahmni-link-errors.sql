-- JSS Ganiyari: Avni individuals whose Bahmni ID could not be linked.
-- Run on the integration database (avni-int), e.g. as a Metabase question for the JSSCP team.
-- One row per individual, latest problem only. A row disappears once the person's ID Update is fixed and they link.
SELECT
    CASE et.name
        WHEN 'BahmniPatientNotFound'              THEN 'Bahmni ID not found in Bahmni'
        WHEN 'MultipleBahmniPatientsWithId'       THEN 'Bahmni ID matches more than one Bahmni patient'
        WHEN 'BahmniIdOnMultiplePeople'           THEN 'Same Bahmni ID entered for more than one person'
        WHEN 'BahmniPatientLinkedToAnotherPerson' THEN 'Bahmni patient already linked to another person'
    END                                   AS problem,
    l.error_body ->> 'bahmniId'           AS bahmni_id_entered,
    l.error_body ->> 'censusId'           AS census_id,
    l.error_body ->> 'name'               AS name,
    l.error_body ->> 'Village'            AS village,
    l.error_body ->> 'Cluster'            AS cluster,
    l.error_msg                           AS what_to_do,
    f.first_seen,
    l.logged_at                           AS last_checked,
    r.entity_id                           AS avni_individual_uuid,
    l.error_body ->> 'idUpdateUuid'       AS id_update_visit_uuid
FROM error_record r
JOIN integration_system s ON s.id = r.integration_system_id AND s.name = 'bahmni'
JOIN LATERAL (
    SELECT * FROM error_record_log x
    WHERE x.error_record_id = r.id AND NOT coalesce(x.is_voided, false)
    ORDER BY x.logged_at DESC LIMIT 1
) l ON TRUE
JOIN error_type et ON et.id = l.error_type_id
JOIN LATERAL (SELECT min(logged_at) AS first_seen FROM error_record_log WHERE error_record_id = r.id) f ON TRUE
WHERE r.avni_entity_type = 'Subject'
  AND NOT coalesce(r.is_voided, false)
  AND et.name IN ('BahmniPatientNotFound', 'MultipleBahmniPatientsWithId', 'BahmniIdOnMultiplePeople', 'BahmniPatientLinkedToAnotherPerson')
ORDER BY problem, cluster, village, name;
