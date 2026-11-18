-- ============================================================
-- Tanuh high-risk model, UAT (integration-service#132, integration-service#134)
-- Target: the PRODUCTION integration service's database (int-prod),
--         which works against Tanuh's UAT organisation on app.avniproject.org.
--         UAT runs there because Tanuh's model service accepts only the AWS role avni_integration_service,
--         and only int-prod runs as that role (decided 17 Nov 2026).
--
-- Before running:
--   1. Replace <AVNI_USER> and <AVNI_PASSWORD> with the job user the implementation team created
--      in Tanuh UAT. It belongs to the Integration group only.
--   2. Create the organisation's two health checks first (docs/tanuh-runbook.md, "Health checks").
--   3. Run this just before the restart that switches Tanuh on. The first run reads screenings
--      changed after this moment, in UTC whatever the session's time zone.
--   4. If review booking has already stopped on UAT, start from the moment it stopped instead (Step 3).
--      A screening referred in between booked no review and would otherwise never reach the physician.
--
--   5. The data quality check is off for now (dqs_enabled 'false', decided 17 Nov 2026): leave dqs_api_url empty. To switch
--      it on later, set dqs_api_url and dqs_enabled 'true' (docs/tanuh-runbook.md, "The data quality service").
--   6. Replace <OCSHR_API_URL> with the model service's address from Tanuh's guide on PE-248, and
--      <OCSHR_CREDENTIAL_CONFIG_JSON> with Tanuh's credential file, its JSON pasted whole; it holds no secret.
--      Replace <HIGH_RISK_AVNI_USER> with the Avni user Tanuh's service writes its results as: highrisk@tanuh_uat.
--   7. If tanuh_uat still runs on int-staging, stop it there first (docs/tanuh-runbook.md, "Stopping an organisation"),
--      or both services check the same screenings. int_env is 'prod' to match int-prod's AVNI_INT_ENV; on a mismatch
--      the service skips the organisation and logs "Environment mismatch detected".
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
SELECT 'tanuh_uat', 'tanuh', uuid_generate_v4(), false
WHERE NOT EXISTS (SELECT 1 FROM integration_system WHERE name = 'tanuh_uat');

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
    ('ocshr_api_url', '<OCSHR_API_URL>', false),
    ('ocshr_credential_config', '<OCSHR_CREDENTIAL_CONFIG_JSON>', false),
    ('high_risk_avni_user', '<HIGH_RISK_AVNI_USER>', false),
    ('dqs_enabled', 'false', false),
    ('dqs_api_url', '', false),
    ('dqs_tenant', 'samvit', false),
    ('qc_fail_rules', 'ST-001,QC-030', false)
) AS c(key, value, is_secret)
WHERE s.name = 'tanuh_uat'
  AND NOT EXISTS (SELECT 1 FROM integration_system_config x WHERE x.integration_system_id = s.id AND x.key = c.key);

-- Step 3: where the first run starts reading. The service reads this column as UTC wall-clock, so it is
-- seeded with timezone('UTC', now()); a plain now() in an Indian-time session would start 5.5 hours late.
-- To start from an earlier moment, such as when review booking stopped (see the header), replace the expression
-- with TIMESTAMP '<yyyy-mm-dd hh:mi:ss>' in UTC.
INSERT INTO integrating_entity_status (entity_type, read_upto_date_time, integration_system_id, uuid)
SELECT 'TanuhOralScreening', timezone('UTC', now())::timestamp(3), s.id, uuid_generate_v4()
FROM integration_system s
WHERE s.name = 'tanuh_uat'
  AND NOT EXISTS (SELECT 1 FROM integrating_entity_status x WHERE x.integration_system_id = s.id AND x.entity_type = 'TanuhOralScreening');

-- Step 4: the reasons a screening waits. follow_up_step is read as an ordinal: '0' is Process, which the retry job
-- tries again.
INSERT INTO error_type (name, integration_system_id, uuid, follow_up_step, comparison_operator)
SELECT t.name, s.id, uuid_generate_v4(), t.step, NULL
FROM integration_system s
CROSS JOIN (VALUES ('DataQualityCallFailed', '0'), ('HighRiskModelCallFailed', '0'), ('ScreeningProcessingFailed', '0')) AS t(name, step)
WHERE s.name = 'tanuh_uat'
  AND NOT EXISTS (SELECT 1 FROM error_type e WHERE e.integration_system_id = s.id AND e.name = t.name);

-- ============================================================
-- Verification: what this organisation now has
-- ============================================================
SELECT id, name, system_type, is_voided FROM integration_system WHERE name = 'tanuh_uat';

SELECT key, CASE WHEN is_secret THEN '(secret)' ELSE value END AS value
FROM integration_system_config
WHERE integration_system_id = (SELECT id FROM integration_system WHERE name = 'tanuh_uat')
ORDER BY key;

SELECT entity_type, read_upto_date_time AS reads_after_utc
FROM integrating_entity_status
WHERE integration_system_id = (SELECT id FROM integration_system WHERE name = 'tanuh_uat');

SELECT name, follow_up_step, comparison_operator
FROM error_type
WHERE integration_system_id = (SELECT id FROM integration_system WHERE name = 'tanuh_uat')
ORDER BY name;
