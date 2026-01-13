ALTER TABLE integration_system_config
    ADD COLUMN IF NOT EXISTS uuid CHARACTER VARYING(255),
    ADD COLUMN IF NOT EXISTS is_voided boolean DEFAULT false,
    ADD COLUMN IF NOT EXISTS is_secret boolean DEFAULT false;
UPDATE integration_system_config SET uuid = uuid_generate_v4() WHERE uuid IS NULL;
ALTER TABLE integration_system_config
    ALTER COLUMN uuid SET NOT NULL;

ALTER TABLE integration_system_config
    ADD UNIQUE (integration_system_id, uuid);