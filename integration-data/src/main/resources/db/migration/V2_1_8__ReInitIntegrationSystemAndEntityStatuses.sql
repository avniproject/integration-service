-- Add unique constraints required
ALTER TABLE integration_system
    ADD CONSTRAINT unique_system_name UNIQUE (name);

ALTER TABLE integrating_entity_status
    ADD CONSTRAINT entity_type_for_integration_system UNIQUE (entity_type, integration_system_id);

-- Init integration_system
insert into integration_system (id, uuid, name, system_type)
values (2, uuid_generate_v4(), 'Goonj', 'Goonj'),
       (8, uuid_generate_v4(), 'power', 'power'),
       (3, uuid_generate_v4(), 'Amrit', 'Amrit'),
       (10, uuid_generate_v4(), 'lahi', 'lahi')
ON CONFLICT DO NOTHING;

-- Init integrating_entity_status
INSERT INTO integrating_entity_status (id, entity_type, read_upto_numeric, read_upto_date_time, integration_system_id, uuid)
VALUES (DEFAULT, 'Inventory', NULL, now(), (select id from integration_system where name = 'Goonj'), uuid_generate_v4()),
       (DEFAULT, 'Demand', NULL, now(), (select id from integration_system where name = 'Goonj'), uuid_generate_v4()),
       (DEFAULT, 'Dispatch receipt', NULL, now(), (select id from integration_system where name = 'Goonj'), uuid_generate_v4()),
       (DEFAULT, 'Activity', NULL, now(), (select id from integration_system where name = 'Goonj'), uuid_generate_v4()),
       (DEFAULT, 'Distribution', NULL, now(), (select id from integration_system where name = 'Goonj'), uuid_generate_v4()),
       (DEFAULT, 'Dispatch', NULL, now(), (select id from integration_system where name = 'Goonj'), uuid_generate_v4())
on conflict (entity_type, integration_system_id) do nothing;

INSERT INTO integrating_entity_status (id, entity_type, read_upto_numeric, read_upto_date_time, integration_system_id, uuid)
VALUES (DEFAULT, 'Beneficiary', 0, now(), (select id from integration_system where name = 'Amrit'), uuid_generate_v4()),
       (DEFAULT, 'BeneficiaryScan', 0, now(), (select id from integration_system where name = 'Amrit'), uuid_generate_v4()),
       (DEFAULT, 'BornBirth', 0, now(), (select id from integration_system where name = 'Amrit'), uuid_generate_v4()),
       (DEFAULT, 'CBAC', 0, now(), (select id from integration_system where name = 'Amrit'), uuid_generate_v4()),
       (DEFAULT, 'Household', 0, now(), (select id from integration_system where name = 'Amrit'), uuid_generate_v4())
on conflict (entity_type, integration_system_id) do nothing;

INSERT INTO integrating_entity_status (id, entity_type, read_upto_numeric, read_upto_date_time, integration_system_id, uuid)
VALUES (DEFAULT, 'Call Details', 0, now(), (select id from integration_system where name = 'power'), uuid_generate_v4()),
       (DEFAULT, 'Call Details::01141236600', 0, now(), (select id from integration_system where name = 'power'), uuid_generate_v4())
on conflict (entity_type, integration_system_id) do nothing;

INSERT INTO integrating_entity_status (id, entity_type, read_upto_numeric, read_upto_date_time, integration_system_id, uuid)
VALUES (DEFAULT, 'Student', 0, now(), (select id from integration_system where name = 'lahi'), uuid_generate_v4())
on conflict (entity_type, integration_system_id) do nothing;