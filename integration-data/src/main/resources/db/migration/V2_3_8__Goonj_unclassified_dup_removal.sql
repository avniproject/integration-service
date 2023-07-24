update error_record_log erl set error_type_id = (SELECT id FROM error_type where name = 'UnclassifiedError' and integration_system_id = 2 and comparison_operator is null)
where error_type_id in (SELECT id FROM error_type where name = 'UnclassifiedError' and integration_system_id = 2 and comparison_operator is not null);

DELETE FROM error_type where name = 'UnclassifiedError' and integration_system_id = 2 and comparison_operator is not null;
