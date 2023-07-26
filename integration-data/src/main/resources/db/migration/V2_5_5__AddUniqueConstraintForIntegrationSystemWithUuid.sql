ALTER TABLE users
    ADD UNIQUE (working_integration_system_id, uuid);

ALTER TABLE error_record
    ADD UNIQUE (integration_system_id, uuid);

ALTER TABLE integrating_entity_status
    ADD UNIQUE (integration_system_id, uuid);

ALTER TABLE mapping_metadata
    ADD UNIQUE (integration_system_id, uuid);

ALTER TABLE integration_system
    ADD UNIQUE (uuid);

ALTER TABLE mapping_group
    ADD UNIQUE (integration_system_id, uuid);

ALTER TABLE mapping_type
    ADD UNIQUE (integration_system_id, uuid);

ALTER TABLE error_type
    ADD UNIQUE (integration_system_id, uuid);
