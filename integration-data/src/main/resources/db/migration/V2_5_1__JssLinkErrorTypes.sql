-- JSS Ganiyari: error types for problems with the Bahmni ID entered on the Avni "Individual ID Update" form.
-- Recorded against the Avni individual; listed in the link-errors report for the JSSCP team to fix.
INSERT INTO error_type (name, integration_system_id, uuid, is_voided, follow_up_step)
SELECT v.name, (SELECT id FROM integration_system WHERE name = 'bahmni'), uuid_generate_v4(), false, '0'
FROM (VALUES
    ('BahmniPatientNotFound'),
    ('MultipleBahmniPatientsWithId'),
    ('BahmniIdOnMultiplePeople'),
    ('BahmniPatientLinkedToAnotherPerson')
) AS v(name)
WHERE NOT EXISTS (
    SELECT 1 FROM error_type e
    WHERE e.name = v.name AND e.integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')
);
