-- ============================================================
-- Tanuh high-risk model, STAGING (integration-service#134)
-- Target: the PRODUCTION integration service's database (int-prod),
--         which works against Tanuh's staging organisation, tanuh_stg, on app.avniproject.org.
--         Staging runs there for the same reason UAT does: Tanuh's model service accepts only the AWS role
--         avni_integration_service, and only int-prod runs as that role.
--
-- Before running:
--   1. Replace <AVNI_USER> and <AVNI_PASSWORD> with the job user the implementation team created
--      in tanuh_stg. It belongs to the Integration group only.
--   2. Create the organisation's two health checks first, tanuh_stg and tanuh_stg-error (docs/tanuh-runbook.md, "Health checks").
--   3. Run this just before the restart that switches Tanuh on. The first run reads screenings
--      changed after this moment, in UTC whatever the session's time zone.
--
--   4. The data quality check is on, against the staging data quality service: replace <DQS_API_URL> with its address,
--      https://16.4.119.236 at the time of writing (docs/tanuh-runbook.md, "The data quality service"). Before the restart,
--      let the job user into that service ("Letting the job user in"); until then every screening waits with
--      DataQualityCallFailed. The organisation's tenant there is samvit-staging. The organisation needs the QC questions
--      the job writes (docs/tanuh-runbook.md, section 2, step 1).
--   5. Tanuh's model service is not set up for staging yet, so this sets none of its settings (ocshr_api_url,
--      ocshr_credential_config, high_risk_avni_user, avni_organisation_id). A screening that passes QC with photos gets
--      its verdict, then waits with HighRiskModelCallFailed, and the retry check (tanuh_stg-error) stays red. Add the
--      four rows, as in docs/tanuh-uat-setup.sql, once Tanuh gives staging's values, and restart.
--   6. int_env is 'prod' to match int-prod's AVNI_INT_ENV; on a mismatch the service skips the organisation and logs
--      "Environment mismatch detected".
--
-- Every insert is guarded, so running it again changes nothing.
--
-- Afterwards, before the restart: import docs/tanuh-mapping-bundle as this organisation's bundle (docs/tanuh-runbook.md,
-- "Question and answer names"). Without the names every job of the organisation fails.
-- ============================================================

-- Step 0: a database built by migrations can leave this id sequence behind rows inserted with fixed ids.
-- This only ever moves it forward.
SELECT setval('integration_system_id_seq',
              GREATEST((SELECT COALESCE(MAX(id), 1) FROM integration_system),
                       (SELECT last_value FROM integration_system_id_seq)));

-- Step 1: the organisation's integration system. The name is unique across every module and is
-- also the health checks' slug, so it stays lower case with no spaces.
INSERT INTO integration_system (name, system_type, uuid, is_voided)
SELECT 'tanuh_stg', 'tanuh', uuid_generate_v4(), false
WHERE NOT EXISTS (SELECT 1 FROM integration_system WHERE name = 'tanuh_stg');

-- Step 2: settings, read when the service starts. A change needs a restart.
INSERT INTO integration_system_config (integration_system_id, key, value, uuid, is_secret)
SELECT s.id, c.key, c.value, uuid_generate_v4(), c.is_secret
FROM integration_system s
CROSS JOIN (VALUES
    ('avni_api_url', 'https://app.avniproject.org', false),
    ('avni_user', '<AVNI_USER>', false),
    ('avni_password', '<AVNI_PASSWORD>', true),
    ('avni_auth_enabled', 'true', false),
    ('int_env', 'prod', false),
    ('main.scheduled.job.cron', '0 */15 * * * ?', false),
    ('error.scheduled.job.cron', '0 5/15 * * * ?', false),
    ('dqs_enabled', 'true', false),
    ('dqs_api_url', '<DQS_API_URL>', false),
    ('dqs_tenant', 'samvit-staging', false),
    ('qc_fail_rules', 'ST-001,QC-030', false)
) AS c(key, value, is_secret)
WHERE s.name = 'tanuh_stg'
  AND NOT EXISTS (SELECT 1 FROM integration_system_config x WHERE x.integration_system_id = s.id AND x.key = c.key);

-- Step 3: where the first run starts reading. The service reads this column as UTC wall-clock, so it is
-- seeded with timezone('UTC', now()); a plain now() in an Indian-time session would start 5.5 hours late.
-- To start from an earlier moment, replace the expression with TIMESTAMP '<yyyy-mm-dd hh:mi:ss>' in UTC.
INSERT INTO integrating_entity_status (entity_type, read_upto_date_time, integration_system_id, uuid)
SELECT 'TanuhOralScreening', timezone('UTC', now())::timestamp(3), s.id, uuid_generate_v4()
FROM integration_system s
WHERE s.name = 'tanuh_stg'
  AND NOT EXISTS (SELECT 1 FROM integrating_entity_status x WHERE x.integration_system_id = s.id AND x.entity_type = 'TanuhOralScreening');

-- Step 4: the reasons a screening waits. follow_up_step is read as an ordinal: '0' is Process, which the retry job
-- tries again.
INSERT INTO error_type (name, integration_system_id, uuid, follow_up_step, comparison_operator)
SELECT t.name, s.id, uuid_generate_v4(), t.step, NULL
FROM integration_system s
CROSS JOIN (VALUES ('DataQualityCallFailed', '0'), ('HighRiskModelCallFailed', '0'), ('ScreeningProcessingFailed', '0')) AS t(name, step)
WHERE s.name = 'tanuh_stg'
  AND NOT EXISTS (SELECT 1 FROM error_type e WHERE e.integration_system_id = s.id AND e.name = t.name);

-- ============================================================
-- Verification: what this organisation now has
-- ============================================================
SELECT id, name, system_type, is_voided FROM integration_system WHERE name = 'tanuh_stg';

SELECT key, CASE WHEN is_secret THEN '(secret)' ELSE value END AS value
FROM integration_system_config
WHERE integration_system_id = (SELECT id FROM integration_system WHERE name = 'tanuh_stg')
ORDER BY key;

SELECT entity_type, read_upto_date_time AS reads_after_utc
FROM integrating_entity_status
WHERE integration_system_id = (SELECT id FROM integration_system WHERE name = 'tanuh_stg');

SELECT name, follow_up_step, comparison_operator
FROM error_type
WHERE integration_system_id = (SELECT id FROM integration_system WHERE name = 'tanuh_stg')
ORDER BY name;
