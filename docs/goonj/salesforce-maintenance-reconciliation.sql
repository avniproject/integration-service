-- =====================================================================
-- Goonj <-> Avni : Salesforce maintenance / outage reconciliation
-- =====================================================================
-- Context: during a Salesforce outage the two sync directions behave
-- differently:
--
--   Salesforce -> Avni (Demand, Dispatch, Inventory)
--     The fetch throws, the section is skipped, and the watermark
--     (integrating_entity_status.read_upto_date_time) is NOT advanced.
--     Self-healing on the next main-job cycle. Nothing to do.
--
--   Avni -> Salesforce (Activity, DispatchReceipt, Distribution)
--     The failed POST creates an error_record AND the watermark IS
--     advanced past the record. The main job will therefore never
--     re-fetch it. Recovery depends entirely on AvniGoonjFullErrorJob
--     retrying the error_record -- which it skips if the error type's
--     follow_up_step = Terminal ('1').
--
-- So Q1 is the pre-flight check, Q3 is the one that finds real problems.
--
-- follow_up_step: 0=Process, 1=Terminal, 2=Internal, 3=External
-- Run as: set role avni_int;
-- =====================================================================


-- ---------------------------------------------------------------------
-- Q1. PRE-FLIGHT: are the outage-related error types retryable?
-- ---------------------------------------------------------------------
-- A connection failure classifies as BadGateway (CONTAINS '502 Bad
-- Gateway') or falls back to UnclassifiedError, because
-- goonj_bypass_errors defaults to true.
-- If EITHER shows 'Terminal', records that errored during the outage
-- will NEVER be retried and will need the Q6 watermark rewind.
-- (The sample SQL in usefulQueries.sql shows these as Internal, but
--  prod diverges -- AddressNotFoundError is Terminal in prod. Verify.)

SELECT et.name,
       et.follow_up_step,
       CASE et.follow_up_step
           WHEN '0' THEN 'Process'
           WHEN '1' THEN 'Terminal  <-- WILL NOT AUTO-RETRY'
           WHEN '2' THEN 'Internal'
           WHEN '3' THEN 'External'
       END AS follow_up,
       et.comparison_operator,   -- 0=EQUALS, 1=CONTAINS, 2=MATCHES
       et.comparison_value
FROM error_type et
WHERE et.integration_system_id = (SELECT id FROM integration_system WHERE name = 'Goonj')
  AND et.is_voided = false
ORDER BY (et.follow_up_step = '1') DESC, et.name;


-- ---------------------------------------------------------------------
-- Q2. What errored during the maintenance window, by type
-- ---------------------------------------------------------------------
-- Set the window to cover the outage (generous on both ends).

WITH win AS (
    SELECT timestamp '2026-09-16 00:00:00' AS from_ts,
           timestamp '2026-09-17 00:00:00' AS to_ts
)
SELECT et.name                                   AS error_type,
       CASE et.follow_up_step
           WHEN '0' THEN 'Process' WHEN '1' THEN 'Terminal'
           WHEN '2' THEN 'Internal' WHEN '3' THEN 'External'
       END                                       AS follow_up,
       COALESCE(er.integrating_entity_type, er.avni_entity_type::text) AS entity_type,
       count(*)                                  AS occurrences,
       count(DISTINCT er.id)                     AS distinct_records,
       min(erl.logged_at)                        AS first_seen,
       max(erl.logged_at)                        AS last_seen,
       left(min(erl.error_msg), 160)             AS sample_msg
FROM error_record_log erl
     JOIN error_record er ON er.id = erl.error_record_id
     JOIN error_type   et ON et.id = erl.error_type_id
     CROSS JOIN win
WHERE er.integration_system_id = (SELECT id FROM integration_system WHERE name = 'Goonj')
  AND erl.logged_at BETWEEN win.from_ts AND win.to_ts
GROUP BY 1, 2, 3
ORDER BY occurrences DESC;


-- ---------------------------------------------------------------------
-- Q3. *** THE IMPORTANT ONE *** records still stuck right now
-- ---------------------------------------------------------------------
-- Looks at the LATEST log per error_record (that is what the retry
-- worker keys off) and flags anything that will not self-heal.
-- Expect this to return rows while the outage is ongoing, and to drain
-- to zero within a few error-job cycles afterwards.
-- Anything still listed as 'STUCK' an hour after recovery is real.

WITH latest_log AS (
    SELECT DISTINCT ON (erl.error_record_id)
           erl.error_record_id,
           erl.error_type_id,
           erl.logged_at,
           erl.error_msg
    FROM error_record_log erl
    ORDER BY erl.error_record_id, erl.logged_at DESC
)
SELECT er.id                       AS error_record_id,
       COALESCE(er.integrating_entity_type, er.avni_entity_type::text) AS entity_type,
       er.entity_id,               -- Salesforce 18-char id, not the readable name
       et.name                     AS error_type,
       CASE et.follow_up_step
           WHEN '0' THEN 'Process' WHEN '1' THEN 'Terminal'
           WHEN '2' THEN 'Internal' WHEN '3' THEN 'External'
       END                         AS follow_up,
       ll.logged_at                AS last_errored_at,
       CASE
           WHEN er.processing_disabled      THEN 'STUCK - processing_disabled'
           WHEN et.follow_up_step = '1'     THEN 'STUCK - Terminal, needs manual re-trigger'
           ELSE 'will auto-retry on error job'
       END                         AS disposition,
       left(ll.error_msg, 200)     AS error_msg
FROM error_record er
     JOIN latest_log ll ON ll.error_record_id = er.id
     JOIN error_type et ON et.id = ll.error_type_id
WHERE er.integration_system_id = (SELECT id FROM integration_system WHERE name = 'Goonj')
ORDER BY (er.processing_disabled OR et.follow_up_step = '1') DESC,
         ll.logged_at DESC;


-- ---------------------------------------------------------------------
-- Q4. Watermark health -- is any entity stalled?
-- ---------------------------------------------------------------------
-- After recovery every row should be moving again. A read_upto_date_time
-- stuck hours behind the others points at an entity whose job is still
-- failing. GoonjErrorRecordLog is the health-check marker, not a sync
-- watermark -- ignore it here.

SELECT ies.entity_type,
       ies.read_upto_date_time,
       now() - ies.read_upto_date_time AS lag
FROM integrating_entity_status ies
WHERE ies.integration_system_id = (SELECT id FROM integration_system WHERE name = 'Goonj')
ORDER BY ies.read_upto_date_time;


-- ---------------------------------------------------------------------
-- Q5. Drill-down: full error history for one record
-- ---------------------------------------------------------------------
-- Use when Q3 flags something. :entity_id is the Salesforce 18-char id.

SELECT erl.logged_at,
       et.name AS error_type,
       CASE et.follow_up_step
           WHEN '0' THEN 'Process' WHEN '1' THEN 'Terminal'
           WHEN '2' THEN 'Internal' WHEN '3' THEN 'External'
       END     AS follow_up,
       erl.error_msg,
       erl.error_body
FROM error_record er
     JOIN error_record_log erl ON er.id = erl.error_record_id
     JOIN error_type       et  ON et.id = erl.error_type_id
WHERE er.integration_system_id = (SELECT id FROM integration_system WHERE name = 'Goonj')
  AND er.entity_id = :'entity_id'
ORDER BY erl.logged_at;


-- ---------------------------------------------------------------------
-- Q6. FIX: rewind the watermark to force a re-fetch
-- ---------------------------------------------------------------------
-- Only needed for records Q3 marks STUCK. Replay is safe: the Avni
-- subject POST upserts by External ID, so re-processing is idempotent.
-- Rewind to just before the earliest stuck record's LastUpdatedDateTime.
-- Check the current value with Q4 first, and note it down so you can
-- put it back if needed.
--
-- UPDATE integrating_entity_status
-- SET    read_upto_date_time = timestamp '2026-09-16 00:00:00'
-- WHERE  entity_type = 'Dispatch'   -- see Q4 for the exact spelling
--   AND  integration_system_id = (SELECT id FROM integration_system WHERE name = 'Goonj');
--
-- The next main-job cycle re-fetches everything after that point.
-- Alternatives, if a rewind is too broad:
--   (b) ask the SF team to re-save the record (bumps LastModifiedDate)
--   (c) adhoc task: POST /goonj/v1/task
--       {"task":"GoonjDispatch","taskConfig":{"state":...,"account":...},
--        "triggerDateTime":...,"cutOffDateTime":...}
--       (form login at /int/login, port 6013; working integration system must be Goonj)
