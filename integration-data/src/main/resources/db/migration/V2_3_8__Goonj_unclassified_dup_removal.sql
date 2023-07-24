DELETE FROM error_type where name = 'UnclassifiedError' and integration_system_id = 2;

INSERT INTO error_type (name, integration_system_id, comparison_operator, comparison_value)
VALUES ('UnclassifiedError', 2, NULL, NULL);
