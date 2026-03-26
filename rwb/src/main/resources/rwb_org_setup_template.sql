-- ============================================================
-- RWB ORG SETUP TEMPLATE
-- ============================================================
-- Use this template to set up RWB integration for a new org.
-- Replace ALL <PLACEHOLDER> values before running.
--
-- Prerequisites:
--   1. Create an API user in the target Avni org:
--      - Username format: apiuser@<ORG_USERNAME_SUFFIX> (e.g., apiuser@gdgs2627 for db_user gdgs26_27)
--      - Role: "Organisation Administrator"
--      - IMPORTANT: Enable "Generate Token" privilege in Avni Admin for this user
--      - Ensure the password is NOT temporary (login to Avni webapp to confirm)
--   2. Password: Use the SAME password as existing RWB orgs (check integration_system_config
--      table for an existing org's avni_password value). Using a different password will
--      break the Glific flow.
-- ============================================================


-- ============================================================
-- SECTION 1: INTEGRATION DB (avni_int)
-- Connect to the integration service database (avni_int)
-- ============================================================

-- 1a. Create integration system for the org
-- <ORG_DB_USER> = the organisation's database username (e.g., 'rwb_org1')
INSERT INTO public.integration_system (id, name, system_type, uuid, is_voided)
VALUES (DEFAULT, '<ORG_DB_USER>'::varchar(250), 'rwb'::varchar(255), uuid_generate_v4(), false::boolean);

-- 1b. Insert all config values
-- First, get the integration_system_id for the org we just created:
-- SELECT id uuid_generate_v4() FROM integration_system WHERE name = '<ORG_DB_USER>';
-- Use that ID below, or use the subquery approach shown.

-- Required configs
INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'avni_api_url', 'https://app.rwb.avniproject.org/', uuid_generate_v4()
FROM integration_system WHERE name = '<ORG_DB_USER>';

-- API user format: apiuser@<ORG_USERNAME_SUFFIX> (e.g., if ORG_DB_USER is 'gdgs26_27', suffix is 'gdgs2627')
INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'avni_user', 'apiuser@<ORG_USERNAME_SUFFIX>', uuid_generate_v4()
FROM integration_system WHERE name = '<ORG_DB_USER>';

-- IMPORTANT: Use the SAME password as existing RWB orgs. Check an existing org's config:
-- SELECT value FROM integration_system_config WHERE key = 'avni_password' LIMIT 1;
INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'avni_password', '<AVNI_PASSWORD>', uuid_generate_v4()
FROM integration_system WHERE name = '<ORG_DB_USER>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'avni_auth_enabled', 'true', uuid_generate_v4()
FROM integration_system WHERE name = '<ORG_DB_USER>';

-- e.g., '0 */6 * * *' (every 6 hours). Use '-' to disable the job.
INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'main.scheduled.job.cron', '<CRON_EXPRESSION>', uuid_generate_v4()
FROM integration_system WHERE name = '<ORG_DB_USER>';

-- MUST match avni.int.env property (e.g., 'prod', 'staging')
INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'int_env', '<INT_ENV>', uuid_generate_v4()
FROM integration_system WHERE name = '<ORG_DB_USER>';

-- Flow-to-query mappings (7 flows)
-- Values are Glific flow IDs — same for all orgs
INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.Work Order Registration', '36875', uuid_generate_v4()
FROM integration_system WHERE name = '<ORG_DB_USER>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.Nudge to register WO', '36876', uuid_generate_v4()
FROM integration_system WHERE name = '<ORG_DB_USER>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.Nudge to Login', '36859', uuid_generate_v4()
FROM integration_system WHERE name = '<ORG_DB_USER>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.Successful WO registration', '36877', uuid_generate_v4()
FROM integration_system WHERE name = '<ORG_DB_USER>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.Daily Recording', '36878', uuid_generate_v4()
FROM integration_system WHERE name = '<ORG_DB_USER>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.Nudge for Endline', '36879', uuid_generate_v4()
FROM integration_system WHERE name = '<ORG_DB_USER>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.Nudge for Work Order Endline', '36880', uuid_generate_v4()
FROM integration_system WHERE name = '<ORG_DB_USER>';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.Certificate delivery', '32498', uuid_generate_v4()
FROM integration_system WHERE name = '<ORG_DB_USER>';


-- ============================================================
-- SECTION 2: AVNI ORG DB
-- Connect to Avni DB and SET ROLE to the org's dbuser
-- ============================================================

SET ROLE '<ORG_DB_USER>';

-- 2a. Glific external system config
-- <ORG_ID>              = org's ID in Avni DB (SELECT id FROM organisation WHERE db_user = '<ORG_DB_USER>')
-- <GLIFIC_PHONE>        = registered phone number with Glific, e.g., '919876543210'
-- <GLIFIC_BASE_URL>     = Glific API URL, e.g., 'https://api.org1.glific.com'
-- <GLIFIC_PASSWORD>     = Glific API password
-- <GLIFIC_AVNI_SYSTEM_USER> = Same API user: apiuser@<ORG_USERNAME_SUFFIX>
INSERT INTO public.external_system_config
    (id, organisation_id, uuid, is_voided, version, created_by_id, last_modified_by_id,
     created_date_time, last_modified_date_time, system_name, config)
VALUES
    (DEFAULT, <ORG_ID>, uuid_generate_v4(), false, 1, 1, 1, now(), now(), 'Glific',
     '{"phone": "<GLIFIC_PHONE>", "baseUrl": "<GLIFIC_BASE_URL>", "password": "<GLIFIC_PASSWORD>", "avniSystemUser": "<GLIFIC_AVNI_SYSTEM_USER>"}');


-- 2b. Flow-specific custom queries (same for all orgs, only org_id differs)
-- <ORG_ID> = org's ID in Avni DB (SELECT id FROM organisation WHERE db_user = '<ORG_DB_USER>')
INSERT INTO custom_query ( uuid, name, query, organisation_id, is_voided, version, created_by_id,
                                 last_modified_by_id, created_date_time, last_modified_date_time)
VALUES

-- 1. Work Order Registration
( uuid_generate_v4(), 'Work Order Registration',
'WITH primary_users AS (
    SELECT DISTINCT u.id AS user_id, u.name AS first_name
    FROM public.users u
             JOIN user_group ug ON u.id = ug.user_id AND ug.is_voided = false
             JOIN groups g ON g.id = ug.group_id AND g.is_voided = false
    WHERE g.name = ''Primary Users''
      AND u.disabled_in_cognito = false
      AND u.is_voided = false
      AND u.organisation_id = :org_id
),
synced_users AS (
    SELECT DISTINCT user_id
    FROM sync_telemetry
    WHERE sync_status = ''complete''
      AND organisation_id = :org_id
)
SELECT pu.user_id, pu.first_name
FROM primary_users pu
JOIN synced_users su ON pu.user_id = su.user_id
WHERE NOT EXISTS (
    SELECT 1 FROM flow_request_queue frq
    JOIN message_receiver mr ON mr.id = frq.message_receiver_id
    WHERE mr.receiver_id = pu.user_id
      AND mr.receiver_type = ''User''
      AND frq.flow_id = :flow_id
      AND frq.is_voided = false AND frq.delivery_status = ''Sent''
);',
<ORG_ID>, false, 0, 1, 1, now(), now()),


-- 2. Nudge to register WO
( uuid_generate_v4(), 'Nudge to register WO',
'WITH primary_users AS (
    SELECT DISTINCT u.id AS user_id, u.name AS first_name
    FROM public.users u
             JOIN user_group ug ON u.id = ug.user_id AND ug.is_voided = false
             JOIN groups g ON g.id = ug.group_id AND g.is_voided = false
    WHERE g.name = ''Primary Users''
      AND u.disabled_in_cognito = false
      AND u.is_voided = false
      AND u.organisation_id = :org_id
),
old_sync AS (
    SELECT user_id
    FROM sync_telemetry
    WHERE sync_status = ''complete''
      AND organisation_id = :org_id
      AND created_date_time < now() - INTERVAL ''2 DAYS''
),
wo_users AS (
    SELECT DISTINCT created_by_id AS user_id
    FROM individual
    WHERE subject_type_id = (
        SELECT id FROM subject_type
        WHERE name = ''Work Order'' AND organisation_id = :org_id AND is_voided = false
    )
)
SELECT pu.user_id, pu.first_name
FROM primary_users pu
JOIN old_sync os ON pu.user_id = os.user_id
WHERE pu.user_id NOT IN (SELECT user_id FROM wo_users)
  AND NOT EXISTS (
    SELECT 1 FROM flow_request_queue frq
    JOIN message_receiver mr ON mr.id = frq.message_receiver_id
    WHERE mr.receiver_id = pu.user_id
      AND mr.receiver_type = ''User''
      AND frq.flow_id = :flow_id
      AND frq.is_voided = false AND frq.delivery_status = ''Sent''
      AND frq.created_date_time > now() - INTERVAL ''3 DAYS''
);',
<ORG_ID>, false, 0, 1, 1, now(), now()),


-- 3. Nudge to Login
( uuid_generate_v4(), 'Nudge to Login',
'WITH primary_users AS (
    SELECT DISTINCT u.id AS user_id, u.name AS first_name
    FROM public.users u
             JOIN user_group ug ON u.id = ug.user_id AND ug.is_voided = false
             JOIN groups g ON g.id = ug.group_id AND g.is_voided = false
    WHERE g.name = ''Primary Users''
      AND u.disabled_in_cognito = false
      AND u.is_voided = false
      AND u.organisation_id = :org_id
),
no_sync_users AS (
    SELECT pu.user_id
    FROM primary_users pu
    WHERE pu.user_id NOT IN (
        SELECT user_id FROM sync_telemetry WHERE sync_status = ''complete'' AND organisation_id = :org_id
    )
)
SELECT pu.user_id, pu.first_name
FROM primary_users pu
JOIN no_sync_users ns ON pu.user_id = ns.user_id
WHERE pu.user_id IN (
    SELECT id FROM users
    WHERE last_activated_date_time < now() - INTERVAL ''2 DAYS''
)
AND NOT EXISTS (
    SELECT 1 FROM flow_request_queue frq
    JOIN message_receiver mr ON mr.id = frq.message_receiver_id
    WHERE mr.receiver_id = pu.user_id
      AND mr.receiver_type = ''User''
      AND frq.flow_id = :flow_id
      AND frq.is_voided = false AND frq.delivery_status = ''Sent''
      AND frq.created_date_time > now() - INTERVAL ''3 DAYS''
);',
<ORG_ID>, false, 0, 1, 1, now(), now()),


-- 4. Successful WO registration
( uuid_generate_v4(), 'Successful WO registration',
'WITH primary_users AS (
    SELECT DISTINCT u.id AS user_id, u.name AS first_name
    FROM public.users u
             JOIN user_group ug ON u.id = ug.user_id AND ug.is_voided = false
             JOIN groups g ON g.id = ug.group_id AND g.is_voided = false
    WHERE g.name = ''Primary Users''
      AND u.disabled_in_cognito = false
      AND u.is_voided = false
      AND u.organisation_id = :org_id
),
wo_users AS (
    SELECT DISTINCT created_by_id AS user_id
    FROM individual
    WHERE subject_type_id = (
        SELECT id FROM subject_type
        WHERE name = ''Work Order'' AND organisation_id = :org_id AND is_voided = false
    )
)
SELECT pu.user_id, pu.first_name
FROM primary_users pu
JOIN wo_users wo ON pu.user_id = wo.user_id
WHERE NOT EXISTS (
    SELECT 1 FROM flow_request_queue frq
    JOIN message_receiver mr ON mr.id = frq.message_receiver_id
    WHERE mr.receiver_id = pu.user_id
      AND mr.receiver_type = ''User''
      AND frq.flow_id = :flow_id
      AND frq.is_voided = false AND frq.delivery_status = ''Sent''
);',
<ORG_ID>, false, 0, 1, 1, now(), now()),


-- 5. Daily Recording
( uuid_generate_v4(), 'Daily Recording',
'WITH primary_users AS (
    SELECT DISTINCT u.id AS user_id, u.name AS first_name
    FROM public.users u
             JOIN user_group ug ON u.id = ug.user_id AND ug.is_voided = false
             JOIN groups g ON g.id = ug.group_id AND g.is_voided = false
    WHERE g.name = ''Primary Users''
      AND u.disabled_in_cognito = false
      AND u.is_voided = false
      AND u.organisation_id = :org_id
  ),
entities AS (
    SELECT u.id AS user_id,
           COUNT(DISTINCT CASE WHEN st.name = ''Farmer'' THEN i.id END) AS farmers,
           COUNT(DISTINCT CASE WHEN st.name = ''Excavating Machine'' THEN i.id END) AS machines,
           COUNT(DISTINCT CASE WHEN st.name = ''Gram Panchayat'' THEN i.id END) AS gps
    FROM individual i
             JOIN subject_type st ON st.id = i.subject_type_id
             JOIN users u ON u.id = i.created_by_id
    WHERE i.organisation_id = :org_id
      AND i.is_voided = false
    GROUP BY u.id
)
SELECT pu.user_id, pu.first_name
FROM primary_users pu
JOIN entities e ON pu.user_id = e.user_id
WHERE (e.farmers > 0 OR e.gps > 0)
  AND e.machines > 0
  AND NOT EXISTS (
    SELECT 1
    FROM flow_request_queue frq
    JOIN message_receiver mr ON mr.id = frq.message_receiver_id
    WHERE mr.receiver_id = pu.user_id
      AND mr.receiver_type = ''User''
      AND frq.flow_id = :flow_id
      AND frq.is_voided = false AND frq.delivery_status = ''Sent''
);',
<ORG_ID>, false, 0, 1, 1, now(), now()),


-- 6. Nudge for Endline
( uuid_generate_v4(), 'Nudge for Endline',
'WITH primary_users AS (
    SELECT DISTINCT u.id AS user_id, u.name AS first_name
    FROM public.users u
             JOIN user_group ug ON u.id = ug.user_id AND ug.is_voided = false
             JOIN groups g ON g.id = ug.group_id AND g.is_voided = false
    WHERE g.name = ''Primary Users''
      AND u.disabled_in_cognito = false
      AND u.is_voided = false
      AND u.organisation_id = :org_id
),
recent_activity AS (
    SELECT DISTINCT created_by_id AS user_id
    FROM encounter
    WHERE encounter_type_id IN (
        SELECT id FROM encounter_type
        WHERE name IN (''Work order daily Recording - Machine'', ''Work order daily Recording - Farmer'')
          AND organisation_id = :org_id
          AND is_voided = false
    )
      AND encounter_date_time > now() - INTERVAL ''5 DAYS''
),
entities AS (
    SELECT u.id AS user_id,
           COUNT(*) FILTER (WHERE st.name IN (''Farmer'', ''Gram Panchayat'')) AS farmers_or_gp,
           COUNT(*) FILTER (WHERE st.name = ''Excavating Machine'') AS machines
    FROM individual i
             JOIN subject_type st ON st.id = i.subject_type_id
             JOIN users u ON u.id = i.created_by_id
    WHERE i.organisation_id = :org_id
      AND i.is_voided = false
    GROUP BY u.id
)
SELECT pu.user_id, pu.first_name
FROM primary_users pu
JOIN entities e ON pu.user_id = e.user_id
WHERE (e.farmers_or_gp > 0 AND e.machines > 0)
  AND pu.user_id NOT IN (SELECT user_id FROM recent_activity)
  AND NOT EXISTS (
    SELECT 1 FROM flow_request_queue frq
    JOIN message_receiver mr ON mr.id = frq.message_receiver_id
    WHERE mr.receiver_id = pu.user_id
      AND mr.receiver_type = ''User''
      AND frq.flow_id = :flow_id
      AND frq.is_voided = false AND frq.delivery_status = ''Sent''
      AND frq.created_date_time > now() - INTERVAL ''3 DAYS''
);',
<ORG_ID>, false, 0, 1, 1, now(), now()),


-- 7. Nudge for Work Order Endline
( uuid_generate_v4(), 'Nudge for Work Order Endline',
'WITH primary_users AS (
    SELECT DISTINCT u.id AS user_id, u.name AS first_name
    FROM public.users u
             JOIN user_group ug ON u.id = ug.user_id AND ug.is_voided = false
             JOIN groups g ON g.id = ug.group_id AND g.is_voided = false
    WHERE g.name = ''Primary Users''
      AND u.disabled_in_cognito = false
      AND u.is_voided = false
      AND u.organisation_id = :org_id
),
entity_counts AS (
    SELECT u.id AS user_id,
           COUNT(DISTINCT CASE WHEN st.name = ''Farmer'' THEN i.id END) AS farmers,
           COUNT(DISTINCT CASE WHEN st.name = ''Excavating Machine'' THEN i.id END) AS machines,
           COUNT(DISTINCT CASE WHEN st.name = ''Gram Panchayat'' THEN i.id END) AS gps
    FROM individual i
             JOIN subject_type st ON st.id = i.subject_type_id
             JOIN users u ON u.id = i.created_by_id
    WHERE i.organisation_id = :org_id
      AND i.is_voided = false
    GROUP BY u.id
),
endline_counts AS (
    SELECT u.id AS user_id,
           COUNT(DISTINCT CASE WHEN et.name = ''Farmer Endline'' THEN e.individual_id END) AS farmer_endlines,
           COUNT(DISTINCT CASE WHEN et.name = ''Excavating Machine Endline'' THEN e.individual_id END) AS machine_endlines,
           COUNT(DISTINCT CASE WHEN et.name = ''Gram Panchayat Endline'' THEN e.individual_id END) AS gp_endlines
    FROM encounter e
             JOIN encounter_type et ON et.id = e.encounter_type_id
             JOIN users u ON u.id = e.created_by_id
    WHERE e.organisation_id = :org_id
      AND e.is_voided = false
    GROUP BY u.id
),
wo_endline AS (
    SELECT DISTINCT created_by_id AS user_id
    FROM encounter
    WHERE encounter_type_id = (
        SELECT id FROM encounter_type
        WHERE name = ''Work order endline'' AND organisation_id = :org_id AND is_voided = false
    )
)
SELECT pu.user_id, pu.first_name
FROM primary_users pu
JOIN entity_counts ec ON pu.user_id = ec.user_id
LEFT JOIN endline_counts el ON pu.user_id = el.user_id
WHERE ec.farmers > 0 AND ec.machines > 0
  AND ec.farmers = COALESCE(el.farmer_endlines, 0)
  AND ec.machines = COALESCE(el.machine_endlines, 0)
  AND (ec.gps = 0 OR ec.gps = COALESCE(el.gp_endlines, 0))
  AND pu.user_id NOT IN (SELECT user_id FROM wo_endline)
  AND NOT EXISTS (
    SELECT 1 FROM flow_request_queue frq
    JOIN message_receiver mr ON mr.id = frq.message_receiver_id
    WHERE mr.receiver_id = pu.user_id
      AND mr.receiver_type = ''User''
      AND frq.flow_id = :flow_id
      AND frq.is_voided = false AND frq.delivery_status = ''Sent''
      AND frq.created_date_time > now() - INTERVAL ''3 DAYS''
);',
<ORG_ID>, false, 0, 1, 1, now(), now()),


-- 9. Certificate delivery
( uuid_generate_v4(), 'Certificate delivery',
'WITH primary_users AS (
    SELECT DISTINCT u.id AS user_id, u.name AS first_name
    FROM public.users u
             JOIN user_group ug ON u.id = ug.user_id AND ug.is_voided = false
             JOIN groups g ON g.id = ug.group_id AND g.is_voided = false
    WHERE g.name = ''Primary Users''
      AND u.disabled_in_cognito = false
      AND u.is_voided = false
      AND u.organisation_id = :org_id
),
wo_counts AS (
    SELECT i.created_by_id AS user_id,
           COUNT(DISTINCT i.id) AS work_orders
    FROM individual i
             JOIN subject_type st ON st.id = i.subject_type_id
    WHERE st.name = ''Work Order''
      AND i.organisation_id = :org_id
      AND i.is_voided = false
      AND i.created_by_id IN (SELECT user_id FROM primary_users)
    GROUP BY i.created_by_id
),
wo_endline_counts AS (
    SELECT e.created_by_id AS user_id,
           COUNT(DISTINCT e.individual_id) AS wo_endlines
    FROM encounter e
             JOIN encounter_type et ON et.id = e.encounter_type_id
    WHERE et.name = ''Work order endline''
      AND e.organisation_id = :org_id
      AND e.is_voided = false
      AND e.created_by_id IN (SELECT user_id FROM primary_users)
    GROUP BY e.created_by_id
)
SELECT pu.user_id, pu.first_name
FROM primary_users pu
JOIN wo_counts wc ON pu.user_id = wc.user_id
JOIN wo_endline_counts wec ON pu.user_id = wec.user_id
WHERE wc.work_orders > 0
  AND wc.work_orders = wec.wo_endlines
  AND NOT EXISTS (
    SELECT 1 FROM flow_request_queue frq
    JOIN message_receiver mr ON mr.id = frq.message_receiver_id
    WHERE mr.receiver_id = pu.user_id
      AND mr.receiver_type = ''User''
      AND frq.flow_id = :flow_id
      AND frq.is_voided = false AND frq.delivery_status = ''Sent''
);',
<ORG_ID>, false, 0, 1, 1, now(), now());


-- ============================================================
-- SECTION 3: POST-SETUP STEPS (MANUAL)
-- ============================================================
-- 1. Link admin user to integration_system:
--    Go to Integration Admin App → Edit User → Set working_integration_system to the one created above
--
-- 2. Restart integration service to pick up the new integration_system:
--    sudo systemctl restart avni-int-service_appserver.service
--
-- 3. Verify in logs that the RWB job is scheduled:
--    Look for: "RWB [<ORG_DB_USER>]: Main job SCHEDULED with cron: <CRON_EXPRESSION>"
--
-- 4. Verify environment validation passes:
--    If int_env doesn't match avni.int.env, you'll see:
--    "Environment mismatch detected! Current environment: 'X', DB config: 'Y'"
-- ============================================================
