-- The Tanuh job's own record of each screening it sent to Tanuh's high-risk model service, or marked not sent
-- (integration-service#134). It is kept here, not only on the screening, because a phone's edit replaces every answer on
-- the screening, OCSHR Call Made included. A screening with a row here is never sent again with other photos.
--   image_uris: the photos of the first send, as a JSON array, kept so a send whose answer was lost is repeated as it
--               was; empty for a screening marked not sent without a send.
--   call_made:  YES or NO once settled; empty while the first send's answer is not known.
CREATE TABLE IF NOT EXISTS tanuh_ocshr_case (
    id                    SERIAL PRIMARY KEY,
    uuid                  VARCHAR(255) NOT NULL,
    integration_system_id INTEGER      NOT NULL REFERENCES integration_system (id),
    screening_uuid        VARCHAR(255) NOT NULL,
    image_uris            TEXT,
    call_made             VARCHAR(10),
    created_date_time     TIMESTAMP    NOT NULL DEFAULT NOW(),
    is_voided             BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT tanuh_ocshr_case_one_per_screening UNIQUE (integration_system_id, screening_uuid)
);
