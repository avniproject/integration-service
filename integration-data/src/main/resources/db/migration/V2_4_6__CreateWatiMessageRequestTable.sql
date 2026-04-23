CREATE TABLE wati_message_request (
    id                    SERIAL PRIMARY KEY,
    uuid                  VARCHAR(255) NOT NULL,
    integration_system_id BIGINT NOT NULL REFERENCES integration_system (id),
    flow_name             VARCHAR(255) NOT NULL,
    entity_id             VARCHAR(500) NOT NULL,
    entity_type           VARCHAR(100) NOT NULL,
    phone_number          VARCHAR(20)  NOT NULL,
    template_name         VARCHAR(255) NOT NULL,
    parameters            JSONB,
    locale                VARCHAR(20),
    status                VARCHAR(50)  NOT NULL DEFAULT 'Pending',
    attempt_count         INTEGER      NOT NULL DEFAULT 0,
    last_attempt_time     TIMESTAMP,
    next_retry_time       TIMESTAMP,
    wati_message_id       VARCHAR(500),
    wati_status           VARCHAR(100),
    error_message         VARCHAR(2000),
    created_date_time     TIMESTAMP    NOT NULL DEFAULT NOW(),
    is_voided             BOOLEAN      NOT NULL DEFAULT FALSE,
    version               INTEGER      NOT NULL DEFAULT 0
);

CREATE INDEX idx_wati_msg_req_status ON wati_message_request (status, integration_system_id);
CREATE INDEX idx_wati_msg_req_entity ON wati_message_request (entity_id, flow_name);
CREATE INDEX idx_wati_msg_req_retry ON wati_message_request (next_retry_time) WHERE status = 'Failed';
