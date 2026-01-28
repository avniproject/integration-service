-- Insert Avni Bahmni JSS ID concept
INSERT INTO concept (uuid, name, data_type, is_voided, organisation_id, version, created_date_time, last_modified_date_time, created_by_id, last_modified_by_id, active)
VALUES (
    'abdac5eb-dded-4da9-b59e-4d285690a8c4',
    'Avni Bahmni JSS ID',
    'Text',
    false,
    (SELECT id FROM organisation WHERE name = 'JSS'),  -- Replace with actual org name or ID
    0,
    now(),
    now(),
    (SELECT id FROM users WHERE username = 'admin@jss'),  -- Replace with actual admin user
    (SELECT id FROM users WHERE username = 'admin@jss'),  -- Replace with actual admin user
    true
);
