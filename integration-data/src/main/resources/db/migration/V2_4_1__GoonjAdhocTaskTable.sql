CREATE TABLE goonj_adhoc_task
(
    id                   SERIAL PRIMARY KEY,
    uuid                 CHARACTER VARYING(255) NOT NULL,
    integration_system_id int not null references integration_system (id),
    is_voided            BOOLEAN,
    integration_task   CHARACTER VARYING(255) NOT NULL,
    task_config          JSONB,
    trigger_date_time    TIMESTAMP              NOT NULL,
    cut_off_date_time    TIMESTAMP              NOT NULL,
    status               CHARACTER VARYING(255) NOT NULL,
    created_date_time      TIMESTAMP              NOT NULL,
    last_modified_date_time TIMESTAMP              NOT NULL
);
