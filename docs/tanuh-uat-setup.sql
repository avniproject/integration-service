-- ============================================================
-- Tanuh high-risk model, UAT (integration-service#132)
-- Target: the STAGING integration service's database (int-staging),
--         which works against Tanuh's UAT organisation on app.avniproject.org.
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
-- Every insert is guarded, so running it again changes nothing.
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
    ('int_env', 'staging', false),
    ('main.scheduled.job.cron', '0 */15 * * * ?', false),
    ('error.scheduled.job.cron', '0 5/15 * * * ?', false),
    ('safety_sample_rate', '0.05', false),
    ('model_stub_mode', 'by_encounter', false),
    ('model_stub_fixed_result', 'Low Risk', false)
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

-- Step 4: the reasons a screening waits for a retry. follow_up_step '0' is Process (read as an ordinal).
INSERT INTO error_type (name, integration_system_id, uuid, follow_up_step, comparison_operator)
SELECT t.name, s.id, uuid_generate_v4(), '0', NULL
FROM integration_system s
CROSS JOIN (VALUES ('HighRiskModelCallFailed'), ('ScreeningProcessingFailed')) AS t(name)
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
