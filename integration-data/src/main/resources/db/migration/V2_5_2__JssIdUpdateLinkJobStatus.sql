-- JSS Ganiyari: bookmark for the scheduled "ID Update link" job.
-- The job reads Avni "Individual ID Update" visits saved/edited after read_upto_date_time, links them, then moves the bookmark.
-- Starts at 1900-01-01 so the first run picks up every existing ID Update.
INSERT INTO integrating_entity_status (entity_type, read_upto_date_time, integration_system_id, uuid, is_voided)
SELECT 'JssIdUpdate', '1900-01-01 00:00:00', (SELECT id FROM integration_system WHERE name = 'bahmni'), uuid_generate_v4(), false
WHERE NOT EXISTS (
    SELECT 1 FROM integrating_entity_status
    WHERE entity_type = 'JssIdUpdate' AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'bahmni')
);
