-- ============================================================
-- DIL Wati Integration — Staging Setup
-- Target: Staging Integration Service → Prod Avni (UAT org)
--
-- TODOs before running:
--   1. Replace pumpOperatorUat with actual DIL UAT org name
--   2. Replace <AVNI_USER> and <AVNI_PASSWORD> with DIL admin credentials on prod Avni
--   3. Replace <WATI_API_URL> and <WATI_API_KEY> with DIL Wati credentials
--   4. Replace <WEEKLY_SURVEY_TEMPLATE_EN/TE/OR> with approved weekly_survey template names
--
-- Run Section A on: staging avni_int DB
-- Run Section B on: prod Avni DB (UAT org)
-- ============================================================


-- ============================================================
-- SECTION A: Run on staging avni_int DB
-- ============================================================

-- Step 1: Create integration system entry
INSERT INTO public.integration_system (name, system_type, uuid, is_voided)
VALUES ('pumpOperatorUat', 'wati', uuid_generate_v4(), false);

-- Step 2: Global config
INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'avni_api_url', 'https://app.avniproject.org', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'avni_user', 'apiuser@po_uat', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'avni_password', '<AVNI_PASSWORD>', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'avni_auth_enabled', 'true', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'wati_api_url', '<WATI_API_URL>', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'wati_api_key', '<WATI_API_KEY>', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'int_env', 'staging', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

-- 5am IST (server is UTC)
INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'main.scheduled.job.cron', '30 23 * * *', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

-- 6:30am IST
INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'error.scheduled.job.cron', '0 1 * * *', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

-- Step 3: Flow 1 — weekly_survey (Pump Operator group, with incentives)
INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey.enabled', 'true', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey.custom_query', 'dil_weekly_survey_scheduled_today', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey.template_name', 'weekly_survey_reminder_v2', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey.template_name.te_IN', 'weekly_survey_reminder_v2_te', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey.template_name.od_IN', 'weekly_survey_reminder_v3_or', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey.template_params', 'name,amount', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey.cooldown_days', '6', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey.max_retries', '3', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey.retry_interval_hours', '24', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey.failure_report_channel', 'error_record', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

-- Step 4: Flow 2 — biweekly_payment (runs on 13th and 28th only)
INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.biweekly_payment.enabled', 'true', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.biweekly_payment.custom_query', 'dil_biweekly_payment_summary', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.biweekly_payment.template_name', 'biweekly_payment_summary_chlorine_refill', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.biweekly_payment.template_name.te_IN', 'biweekly_payment_summary_chlorine_refill_te', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.biweekly_payment.template_name.od_IN', 'biweekly_payment_summary_chlorine_refill_or', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.biweekly_payment.template_params', 'name,total_submissions,approved_count,payment_amount,rejected_count', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.biweekly_payment.cooldown_days', '10', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.biweekly_payment.max_retries', '3', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.biweekly_payment.retry_interval_hours', '24', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.biweekly_payment.failure_report_channel', 'error_record', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

-- Step 5: Flow 3 — weekly_survey_rnd (Pump Operator R&D group, no incentives)
INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey_rnd.enabled', 'true', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey_rnd.custom_query', 'dil_weekly_survey_rnd_scheduled_today', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey_rnd.template_name', 'weekly_survey_reminder_control_wo_incentives_v2', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey_rnd.template_name.te_IN', 'weekly_survey_reminder_control_wo_incentives_v2_te', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey_rnd.template_name.od_IN', 'weekly_survey_reminder_control_wo_incentives_v2_or', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey_rnd.template_params', 'name', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey_rnd.cooldown_days', '6', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey_rnd.max_retries', '3', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey_rnd.retry_interval_hours', '24', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

INSERT INTO integration_system_config (integration_system_id, key, value, uuid)
SELECT id, 'flow.weekly_survey_rnd.failure_report_channel', 'error_record', uuid_generate_v4()
FROM integration_system WHERE name = 'pumpOperatorUat';

-- Step 6: Error type for permanent message failures (required by WatiErrorService; without it,
-- permanent failures are silently dropped). Idempotent so re-running provisioning is safe.
INSERT INTO error_type (name, follow_up_step, uuid, is_voided, integration_system_id)
SELECT 'WatiMessagePermanentFailure', 'Terminal', uuid_generate_v4(), false, s.id
FROM integration_system s
WHERE s.name = 'pumpOperatorUat'
  AND NOT EXISTS (
        SELECT 1 FROM error_type et
        WHERE et.integration_system_id = s.id
          AND et.name = 'WatiMessagePermanentFailure'
  );


-- ============================================================
-- SECTION B: Run on prod Avni DB (UAT org)
-- ============================================================

-- Custom Query 1: weekly_survey — daily reminder for Pump Operators (with incentives)
-- Payment rate concept UUIDs (from PaymentRateChart / Payment Rate encounter):
--   Effective Date : 94d61f77-3da7-4533-b34b-055fc009e1c2
--   Odisha Rate    : a808a7a3-83ff-4404-a319-c0cd9eff4ecd  (od_IN)
--   AP Rate        : 94483301-2899-4ba6-85f9-8233041015d5  (te_IN)
INSERT INTO public.custom_query (uuid, name, query, organisation_id, is_voided, version,
                                 created_by_id, last_modified_by_id, created_date_time, last_modified_date_time)
VALUES (
    uuid_generate_v4(),
    'dil_weekly_survey_scheduled_today',
    'WITH rate_lookup AS (
     SELECT
         observations->>''a808a7a3-83ff-4404-a319-c0cd9eff4ecd'' AS od_rate,
         observations->>''94483301-2899-4ba6-85f9-8233041015d5'' AS ap_rate
     FROM encounter e_pr
     JOIN encounter_type et_pr ON et_pr.id = e_pr.encounter_type_id
     WHERE et_pr.name           = ''Payment Rate''
       AND e_pr.organisation_id = :org_id
       AND e_pr.is_voided       = false
     ORDER BY (e_pr.observations->>''94d61f77-3da7-4533-b34b-055fc009e1c2'')::timestamptz DESC
     LIMIT 1
 )
 SELECT DISTINCT
     u.phone_number                       AS phone_number,
     u.settings->>''locale''              AS locale,
     e.uuid                               AS entity_id,
     u.name                               AS name,
     CASE u.settings->>''locale''
         WHEN ''od_IN'' THEN COALESCE((SELECT od_rate FROM rate_lookup), ''300'')
         WHEN ''te_IN'' THEN COALESCE((SELECT ap_rate FROM rate_lookup), ''500'')
         ELSE                COALESCE((SELECT ap_rate FROM rate_lookup), ''500'')
     END                                  AS amount
 FROM encounter e
          JOIN encounter_type et          ON et.id = e.encounter_type_id           AND et.is_voided = false
          JOIN individual i               ON i.id = e.individual_id                AND i.is_voided = false
          JOIN catchment_address_mapping cam ON cam.addresslevel_id = i.address_id
          JOIN users u                    ON u.catchment_id = cam.catchment_id      AND u.is_voided = false
          JOIN user_group ug              ON u.id = ug.user_id                      AND ug.is_voided = false
          JOIN groups g                   ON g.id = ug.group_id                     AND g.is_voided = false
     WHERE et.name                  = ''Self-report Survey''
       AND e.organisation_id        = :org_id
       AND e.is_voided               = false
       AND g.name                   = ''Pump Operator''
       AND u.disabled_in_cognito    = false
       AND e.encounter_date_time    IS NULL
       AND (e.earliest_visit_date_time AT TIME ZONE ''Asia/Kolkata'')::date = (CURRENT_TIMESTAMP AT TIME ZONE ''Asia/Kolkata'')::date',
    (SELECT id FROM organisation WHERE name = 'Pump Operator DIL UAT'), false, 0, 1, 1, now(), now()
);

-- Custom Query 2: weekly_survey_rnd — daily reminder for Pump Operator R&D (no incentives)
INSERT INTO public.custom_query (uuid, name, query, organisation_id, is_voided, version,
                                 created_by_id, last_modified_by_id, created_date_time, last_modified_date_time)
VALUES (
    uuid_generate_v4(),
    'dil_weekly_survey_rnd_scheduled_today',
    'SELECT DISTINCT
         u.phone_number                       AS phone_number,
         u.settings->>''locale''              AS locale,
         e.uuid                               AS entity_id,
         u.name                               AS name
     FROM encounter e
              JOIN encounter_type et          ON et.id = e.encounter_type_id           AND et.is_voided = false
              JOIN individual i               ON i.id = e.individual_id                AND i.is_voided = false
              JOIN catchment_address_mapping cam ON cam.addresslevel_id = i.address_id
              JOIN users u                    ON u.catchment_id = cam.catchment_id      AND u.is_voided = false
              JOIN user_group ug              ON u.id = ug.user_id                      AND ug.is_voided = false
              JOIN groups g                   ON g.id = ug.group_id                     AND g.is_voided = false
     WHERE et.name                  = ''Self-report Survey''
       AND e.organisation_id        = :org_id
       AND e.is_voided               = false
       AND g.name                   = ''Pump Operator R&D''
       AND u.disabled_in_cognito    = false
       AND e.encounter_date_time    IS NULL
       AND (e.earliest_visit_date_time AT TIME ZONE ''Asia/Kolkata'')::date = (CURRENT_TIMESTAMP AT TIME ZONE ''Asia/Kolkata'')::date',
    (SELECT id FROM organisation WHERE name = 'Pump Operator DIL UAT'), false, 0, 1, 1, now(), now()
);

-- Custom Query 3: biweekly_payment — runs on 13th and 28th only
-- On 13th: covers 28th of last month to today
-- On 28th: covers 13th of this month to today
-- Payment rate: read from PaymentRateChart / Payment Rate encounter; falls back to 500 if not found.
-- Concept UUIDs: Odisha Rate a808a7a3-…-c0cd9eff4ecd, AP Rate 94483301-…-8233041015d5
INSERT INTO public.custom_query (uuid, name, query, organisation_id, is_voided, version,
                                 created_by_id, last_modified_by_id, created_date_time, last_modified_date_time)
VALUES (
    uuid_generate_v4(),
    'dil_biweekly_payment_summary',
    'WITH period AS (
    SELECT
        CASE
            WHEN EXTRACT(DAY FROM (CURRENT_TIMESTAMP AT TIME ZONE ''Asia/Kolkata'')::date) = 13
                THEN (DATE_TRUNC(''month'', (CURRENT_TIMESTAMP AT TIME ZONE ''Asia/Kolkata'')::date) - INTERVAL ''1 month'')::date + 27
            ELSE DATE_TRUNC(''month'', (CURRENT_TIMESTAMP AT TIME ZONE ''Asia/Kolkata'')::date)::date + 12
        END::timestamp AS period_start
),
latest_payment_rate AS (
    SELECT observations
    FROM encounter e_pr
    JOIN encounter_type et_pr ON et_pr.id = e_pr.encounter_type_id
    CROSS JOIN period p
    WHERE et_pr.name           = ''Payment Rate''
      AND e_pr.organisation_id = :org_id
      AND e_pr.is_voided       = false
      AND (e_pr.observations->>''94d61f77-3da7-4533-b34b-055fc009e1c2'')::timestamptz <= p.period_start
    ORDER BY (e_pr.observations->>''94d61f77-3da7-4533-b34b-055fc009e1c2'')::timestamptz DESC
    LIMIT 1
),
payment_rates AS (
    SELECT ''od_IN''::text AS locale,
           COALESCE((SELECT (observations->>''a808a7a3-83ff-4404-a319-c0cd9eff4ecd'')::numeric FROM latest_payment_rate), 500) AS rate
    UNION ALL
    SELECT ''te_IN''::text AS locale,
           COALESCE((SELECT (observations->>''94483301-2899-4ba6-85f9-8233041015d5'')::numeric FROM latest_payment_rate), 500) AS rate
),
eligible_users AS (
    SELECT DISTINCT u.id AS user_id, u.phone_number, u.name, u.settings ->> ''locale'' AS locale
    FROM users u
        JOIN user_group ug ON u.id = ug.user_id AND ug.is_voided = false
        JOIN groups g      ON g.id = ug.group_id AND g.is_voided = false
    WHERE g.name               = ''Pump Operator''
      AND u.is_voided           = false
      AND u.disabled_in_cognito = false
      AND u.organisation_id     = :org_id
),
latest_approval_per_encounter AS (
    SELECT DISTINCT ON (eas.entity_id)
        eas.entity_id AS encounter_id,
        ast.status    AS status
    FROM entity_approval_status eas
        JOIN approval_status ast ON ast.id = eas.approval_status_id
        JOIN encounter e_scope ON e_scope.id = eas.entity_id
                               AND e_scope.organisation_id = :org_id
    WHERE eas.is_voided   = false
      AND eas.entity_type = ''Encounter''
    ORDER BY eas.entity_id, eas.created_date_time DESC
),
encounter_stats AS (
    SELECT
        CASE WHEN e.last_modified_by_id IN (SELECT eu2.user_id FROM eligible_users eu2)
             THEN e.last_modified_by_id
             ELSE e.created_by_id
        END                                                     AS user_id,
        COUNT(e.id)                                             AS total_submissions,
        COUNT(CASE WHEN lae.status = ''Approved'' THEN 1 END)   AS approved_count,
        COUNT(CASE WHEN lae.status = ''Rejected'' THEN 1 END)   AS rejected_count
    FROM encounter e
        JOIN encounter_type et ON et.id = e.encounter_type_id AND et.is_voided = false
        LEFT JOIN latest_approval_per_encounter lae ON lae.encounter_id = e.id
        CROSS JOIN period p
    WHERE et.name              = ''Self-report Survey''
      AND e.organisation_id    = :org_id
      AND e.is_voided           = false
      AND e.encounter_date_time IS NOT NULL
      AND e.encounter_date_time >= p.period_start
      AND e.encounter_date_time <  ((CURRENT_TIMESTAMP AT TIME ZONE ''Asia/Kolkata'')::date + INTERVAL ''1 day'') AT TIME ZONE ''Asia/Kolkata''
    GROUP BY CASE WHEN e.last_modified_by_id IN (SELECT eu2.user_id FROM eligible_users eu2)
                  THEN e.last_modified_by_id
                  ELSE e.created_by_id
             END
)
SELECT
    eu.phone_number                                            AS phone_number,
    eu.locale                                                  AS locale,
    eu.user_id::TEXT                                           AS entity_id,
    eu.name                                                    AS name,
    es.total_submissions::TEXT                                 AS total_submissions,
    es.approved_count::TEXT                                    AS approved_count,
    (es.approved_count * COALESCE(pr.rate, 500))::TEXT         AS payment_amount,
    es.rejected_count::TEXT                                    AS rejected_count
FROM encounter_stats es
    JOIN eligible_users eu     ON eu.user_id = es.user_id
    LEFT JOIN payment_rates pr ON pr.locale  = eu.locale
WHERE EXTRACT(DAY FROM (CURRENT_TIMESTAMP AT TIME ZONE ''Asia/Kolkata'')::date) IN (13, 28)',
    (SELECT id FROM organisation WHERE name = 'Pump Operator DIL UAT'), false, 0, 1, 1, now(), now()
);


-- ============================================================
-- SECTION C: Verification (run on staging avni_int DB after setup)
-- ============================================================

-- Confirm integration system and all config rows are present
SELECT key, value FROM integration_system_config
WHERE integration_system_id = (SELECT id FROM integration_system WHERE name = 'pumpOperatorUat')
ORDER BY key;

-- After triggering AvniWatiMainJob manually, check messages were queued
SELECT flow_name, status, COUNT(*) FROM wati_message_request
WHERE integration_system_id = (SELECT id FROM integration_system WHERE name = 'pumpOperatorUat')
GROUP BY flow_name, status;


-- ============================================================
-- SECTION D: Run on prod Avni DB to update existing custom queries
--            for dynamic payment rates (PaymentRateChart encounter)
-- Replace :org_id with the actual organisation id before running.
-- ============================================================

-- Update 1: weekly_survey — replace hardcoded '500' amount with locale-specific rate
UPDATE public.custom_query
SET query = $Q$
WITH rate_lookup AS (
    SELECT
        observations->>'a808a7a3-83ff-4404-a319-c0cd9eff4ecd' AS od_rate,
        observations->>'94483301-2899-4ba6-85f9-8233041015d5' AS ap_rate
    FROM encounter e_pr
    JOIN encounter_type et_pr ON et_pr.id = e_pr.encounter_type_id
    WHERE et_pr.name           = 'Payment Rate'
      AND e_pr.organisation_id = :org_id
      AND e_pr.is_voided       = false
    ORDER BY (e_pr.observations->>'94d61f77-3da7-4533-b34b-055fc009e1c2')::timestamptz DESC
    LIMIT 1
)
SELECT DISTINCT
    u.phone_number                       AS phone_number,
    u.settings->>'locale'                AS locale,
    e.uuid                               AS entity_id,
    u.name                               AS name,
    CASE u.settings->>'locale'
        WHEN 'od_IN' THEN COALESCE((SELECT od_rate FROM rate_lookup), '300')
        WHEN 'te_IN' THEN COALESCE((SELECT ap_rate FROM rate_lookup), '500')
        ELSE               COALESCE((SELECT ap_rate FROM rate_lookup), '500')
    END                                  AS amount
FROM encounter e
         JOIN encounter_type et          ON et.id = e.encounter_type_id           AND et.is_voided = false
         JOIN individual i               ON i.id = e.individual_id                AND i.is_voided = false
         JOIN catchment_address_mapping cam ON cam.addresslevel_id = i.address_id
         JOIN users u                    ON u.catchment_id = cam.catchment_id      AND u.is_voided = false
         JOIN user_group ug              ON u.id = ug.user_id                      AND ug.is_voided = false
         JOIN groups g                   ON g.id = ug.group_id                     AND g.is_voided = false
WHERE et.name                  = 'Self-report Survey'
  AND e.organisation_id        = :org_id
  AND e.is_voided               = false
  AND g.name                   = 'Pump Operator'
  AND u.disabled_in_cognito    = false
  AND e.encounter_date_time    IS NULL
  AND (e.earliest_visit_date_time AT TIME ZONE 'Asia/Kolkata')::date = (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Kolkata')::date
$Q$
WHERE name            = 'dil_weekly_survey_scheduled_today'
  AND organisation_id = (SELECT id FROM organisation WHERE name = 'Pump Operator DIL');


-- Update 2: biweekly_payment — replace hardcoded VALUES rates with PaymentRateChart lookup
UPDATE public.custom_query
SET query = $Q$
WITH period AS (
    SELECT
        CASE
            WHEN EXTRACT(DAY FROM (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Kolkata')::date) = 13
                THEN (DATE_TRUNC('month', (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Kolkata')::date) - INTERVAL '1 month')::date + 27
            ELSE DATE_TRUNC('month', (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Kolkata')::date)::date + 12
        END::timestamp AS period_start
),
latest_payment_rate AS (
    SELECT observations
    FROM encounter e_pr
    JOIN encounter_type et_pr ON et_pr.id = e_pr.encounter_type_id
    CROSS JOIN period p
    WHERE et_pr.name           = 'Payment Rate'
      AND e_pr.organisation_id = :org_id
      AND e_pr.is_voided       = false
      AND (e_pr.observations->>'94d61f77-3da7-4533-b34b-055fc009e1c2')::timestamptz <= p.period_start
    ORDER BY (e_pr.observations->>'94d61f77-3da7-4533-b34b-055fc009e1c2')::timestamptz DESC
    LIMIT 1
),
payment_rates AS (
    SELECT 'od_IN'::text AS locale,
           COALESCE((SELECT (observations->>'a808a7a3-83ff-4404-a319-c0cd9eff4ecd')::numeric FROM latest_payment_rate), 500) AS rate
    UNION ALL
    SELECT 'te_IN'::text AS locale,
           COALESCE((SELECT (observations->>'94483301-2899-4ba6-85f9-8233041015d5')::numeric FROM latest_payment_rate), 500) AS rate
),
eligible_users AS (
    SELECT DISTINCT u.id AS user_id, u.phone_number, u.name, u.settings ->> 'locale' AS locale
    FROM users u
        JOIN user_group ug ON u.id = ug.user_id AND ug.is_voided = false
        JOIN groups g      ON g.id = ug.group_id AND g.is_voided = false
    WHERE g.name               = 'Pump Operator'
      AND u.is_voided           = false
      AND u.disabled_in_cognito = false
      AND u.organisation_id     = :org_id
),
latest_approval_per_encounter AS (
    SELECT DISTINCT ON (eas.entity_id)
        eas.entity_id AS encounter_id,
        ast.status    AS status
    FROM entity_approval_status eas
        JOIN approval_status ast ON ast.id = eas.approval_status_id
        JOIN encounter e_scope ON e_scope.id = eas.entity_id
                               AND e_scope.organisation_id = :org_id
    WHERE eas.is_voided   = false
      AND eas.entity_type = 'Encounter'
    ORDER BY eas.entity_id, eas.created_date_time DESC
),
encounter_stats AS (
    SELECT
        CASE WHEN e.last_modified_by_id IN (SELECT eu2.user_id FROM eligible_users eu2)
             THEN e.last_modified_by_id
             ELSE e.created_by_id
        END                                                    AS user_id,
        COUNT(e.id)                                            AS total_submissions,
        COUNT(CASE WHEN lae.status = 'Approved' THEN 1 END)   AS approved_count,
        COUNT(CASE WHEN lae.status = 'Rejected' THEN 1 END)   AS rejected_count
    FROM encounter e
        JOIN encounter_type et ON et.id = e.encounter_type_id AND et.is_voided = false
        LEFT JOIN latest_approval_per_encounter lae ON lae.encounter_id = e.id
        CROSS JOIN period p
    WHERE et.name              = 'Self-report Survey'
      AND e.organisation_id    = :org_id
      AND e.is_voided           = false
      AND e.encounter_date_time IS NOT NULL
      AND e.encounter_date_time >= p.period_start
      AND e.encounter_date_time <  ((CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Kolkata')::date + INTERVAL '1 day') AT TIME ZONE 'Asia/Kolkata'
    GROUP BY CASE WHEN e.last_modified_by_id IN (SELECT eu2.user_id FROM eligible_users eu2)
                  THEN e.last_modified_by_id
                  ELSE e.created_by_id
             END
)
SELECT
    eu.phone_number                                            AS phone_number,
    eu.locale                                                  AS locale,
    eu.user_id::TEXT                                           AS entity_id,
    eu.name                                                    AS name,
    es.total_submissions::TEXT                                 AS total_submissions,
    es.approved_count::TEXT                                    AS approved_count,
    (es.approved_count * COALESCE(pr.rate, 500))::TEXT         AS payment_amount,
    es.rejected_count::TEXT                                    AS rejected_count
FROM encounter_stats es
    JOIN eligible_users eu     ON eu.user_id = es.user_id
    LEFT JOIN payment_rates pr ON pr.locale  = eu.locale
WHERE EXTRACT(DAY FROM (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Kolkata')::date) IN (13, 28)
$Q$
WHERE name            = 'dil_biweekly_payment_summary'
  AND organisation_id = (SELECT id FROM organisation WHERE name = 'Pump Operator DIL');


-- Verify the updates took effect
SELECT name, LEFT(query, 120) AS query_preview
FROM custom_query
WHERE name IN ('dil_weekly_survey_scheduled_today', 'dil_biweekly_payment_summary')
  AND organisation_id = (SELECT id FROM organisation WHERE name = 'Pump Operator DIL');
