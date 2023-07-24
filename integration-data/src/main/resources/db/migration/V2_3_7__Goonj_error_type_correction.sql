UPDATE error_type SET name = 'UnclassifiedError' WHERE name = 'HttpClientErrorException';

DELETE FROM error_type where name = 'NoDemandWithId';

INSERT INTO error_type (name, integration_system_id, comparison_operator, comparison_value)
VALUES ('NoDemandWithId', 2, NULL, NULL);
