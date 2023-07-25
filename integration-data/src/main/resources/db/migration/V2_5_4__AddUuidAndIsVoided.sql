ALTER TABLE constants
    ADD COLUMN IF NOT EXISTS uuid CHARACTER VARYING(255), ADD COLUMN IF NOT EXISTS is_voided boolean DEFAULT false;
UPDATE constants SET uuid = uuid_generate_v4() WHERE uuid IS NULL;
ALTER TABLE constants
    ALTER COLUMN uuid SET NOT NULL;

ALTER TABLE ignored_integrating_concept
    ADD COLUMN IF NOT EXISTS uuid CHARACTER VARYING(255), ADD COLUMN IF NOT EXISTS is_voided boolean DEFAULT false;
UPDATE ignored_integrating_concept SET uuid = uuid_generate_v4() WHERE uuid IS NULL;
ALTER TABLE ignored_integrating_concept
    ALTER COLUMN uuid SET NOT NULL;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS uuid CHARACTER VARYING(255), ADD COLUMN IF NOT EXISTS is_voided boolean DEFAULT false;
UPDATE users SET uuid = uuid_generate_v4() WHERE uuid IS NULL;
ALTER TABLE users
    ALTER COLUMN uuid SET NOT NULL;

ALTER TABLE error_record
    ADD COLUMN IF NOT EXISTS uuid CHARACTER VARYING(255), ADD COLUMN IF NOT EXISTS is_voided boolean DEFAULT false;
UPDATE error_record SET uuid = uuid_generate_v4() WHERE uuid IS NULL;
ALTER TABLE error_record
    ALTER COLUMN uuid SET NOT NULL;

ALTER TABLE error_record_log
    ADD COLUMN IF NOT EXISTS uuid CHARACTER VARYING(255), ADD COLUMN IF NOT EXISTS is_voided boolean DEFAULT false;
UPDATE error_record_log SET uuid = uuid_generate_v4() WHERE uuid IS NULL;
ALTER TABLE error_record_log
    ALTER COLUMN uuid SET NOT NULL;

ALTER TABLE integrating_entity_status
    ADD COLUMN IF NOT EXISTS uuid CHARACTER VARYING(255), ADD COLUMN IF NOT EXISTS is_voided boolean DEFAULT false;
UPDATE integrating_entity_status SET uuid = uuid_generate_v4() WHERE uuid IS NULL;
ALTER TABLE integrating_entity_status
    ALTER COLUMN uuid SET NOT NULL;

ALTER TABLE mapping_metadata
    ADD COLUMN IF NOT EXISTS uuid CHARACTER VARYING(255), ADD COLUMN IF NOT EXISTS is_voided boolean DEFAULT false;
UPDATE mapping_metadata SET uuid = uuid_generate_v4() WHERE uuid IS NULL;
ALTER TABLE mapping_metadata
    ALTER COLUMN uuid SET NOT NULL;

ALTER TABLE integration_system
    ADD COLUMN IF NOT EXISTS uuid CHARACTER VARYING(255), ADD COLUMN IF NOT EXISTS is_voided boolean DEFAULT false;
UPDATE integration_system SET uuid = uuid_generate_v4() WHERE uuid IS NULL;
ALTER TABLE integration_system
    ALTER COLUMN uuid SET NOT NULL;

ALTER TABLE mapping_group
    ADD COLUMN IF NOT EXISTS uuid CHARACTER VARYING(255), ADD COLUMN IF NOT EXISTS is_voided boolean DEFAULT false;
UPDATE mapping_group SET uuid = uuid_generate_v4() WHERE uuid IS NULL;
ALTER TABLE mapping_group
    ALTER COLUMN uuid SET NOT NULL;

ALTER TABLE mapping_type
    ADD COLUMN IF NOT EXISTS uuid CHARACTER VARYING(255), ADD COLUMN IF NOT EXISTS is_voided boolean DEFAULT false;
UPDATE mapping_type SET uuid = uuid_generate_v4() WHERE uuid IS NULL;
ALTER TABLE mapping_type
    ALTER COLUMN uuid SET NOT NULL;

ALTER TABLE error_type
    ADD COLUMN IF NOT EXISTS uuid CHARACTER VARYING(255), ADD COLUMN IF NOT EXISTS is_voided boolean DEFAULT false;
UPDATE error_type SET uuid = uuid_generate_v4() WHERE uuid IS NULL;
ALTER TABLE error_type
    ALTER COLUMN uuid SET NOT NULL;