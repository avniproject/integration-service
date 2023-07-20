DELETE FROM avni_entity_status
WHERE integration_system_id in (select id from integration_system where system_type = 'Goonj');

DELETE FROM error_record_log erl
    USING error_record er
WHERE er.integration_system_id in (select id from integration_system where system_type = 'Goonj') and erl.error_record_id = er.id;

DELETE FROM error_record
WHERE integration_system_id in (select id from integration_system where system_type = 'Goonj');

DELETE FROM error_type
WHERE integration_system_id in (select id from integration_system where system_type = 'Goonj');

DELETE FROM integrating_entity_status
WHERE integration_system_id in (select id from integration_system where system_type = 'Goonj');

DELETE FROM integrating_entity_type
WHERE integration_system_id in (select id from integration_system where system_type = 'Goonj');

DELETE FROM integration_system_config
WHERE integration_system_id in (select id from integration_system where system_type = 'Goonj');

DELETE FROM mapping_metadata
WHERE integration_system_id in (select id from integration_system where system_type = 'Goonj');

DELETE FROM mapping_metadata mm
    USING mapping_type mt
WHERE mt.integration_system_id in (select id from integration_system where system_type = 'Goonj') and mt.id = mm.mapping_type_id;

DELETE FROM mapping_metadata mm
    USING mapping_group mg
WHERE mg.integration_system_id in (select id from integration_system where system_type = 'Goonj') and mg.id = mm.mapping_group_id;

DELETE FROM mapping_type
WHERE integration_system_id in (select id from integration_system where system_type = 'Goonj');

DELETE FROM mapping_group
WHERE integration_system_id in (select id from integration_system where system_type = 'Goonj');
