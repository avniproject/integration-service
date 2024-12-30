INSERT INTO public.error_type (id, name, integration_system_id, comparison_operator, comparison_value, uuid, is_voided,
                               follow_up_step)
VALUES
    (DEFAULT, 'BadRequest'::varchar(250), (select id from integration_system where name = 'rwb'), null::varchar(255),
     null::text,
     uuid_generate_v4(), false::boolean, '2'::varchar(255)),
    (DEFAULT, 'BadConfiguration'::varchar(250), (select id from integration_system where name = 'rwb'), null::varchar(255),
     null::text,
     uuid_generate_v4(), false::boolean, '2'::varchar(255)),
    (DEFAULT, 'RuntimeError'::varchar(250), (select id from integration_system where name = 'rwb'), null::varchar(255),
     null::text,
     uuid_generate_v4(), false::boolean, '2'::varchar(255)),
    (DEFAULT, 'Success'::varchar(250), (select id from integration_system where name = 'rwb'), null::varchar(255),
     null::text,
     uuid_generate_v4(), false::boolean, '2'::varchar(255));