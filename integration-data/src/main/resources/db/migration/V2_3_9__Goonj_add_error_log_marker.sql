INSERT INTO integrating_entity_status (entity_type, read_upto_numeric, read_upto_date_time,
                                       integration_system_id, uuid, is_voided)
select 'GoonjErrorRecordLog'::varchar(100),
       null::integer,
       now(),
       2::integer,
       '21e59aa6-2779-4df9-947b-398f1048b8ad'::varchar(255),
       false::boolean
WHERE NOT EXISTS(
        SELECT entity_type
        FROM integrating_entity_status
        WHERE entity_type = 'GoonjErrorRecordLog' and integration_system_id = 2
    );



