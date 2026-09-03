-- =====================================================================
-- Goonj: remove extra spaces from Account Name values
--   Concept: Account Name (uuid 2978117c-a297-4171-99c6-23c3522ca0f8)
--   Approach (per card):
--     * entities  -> /bulkSubjectMigration API (one call per bad value)
--     * users     -> sync_settings updated separately via SQL (Step 4)
--   Cleaning rule: btrim + collapse internal runs of whitespace to one space
-- =====================================================================

set role goonj;

-- ---------------------------------------------------------------------
-- STEP 1 (analysis): how many ENTITIES have account_name with space?
-- ---------------------------------------------------------------------
SELECT '[' || (i.observations ->> '2978117c-a297-4171-99c6-23c3522ca0f8') || ']' AS bad_value,
       count(*) AS record_count
FROM public.individual i
WHERE i.observations ? '2978117c-a297-4171-99c6-23c3522ca0f8'
  AND (i.observations ->> '2978117c-a297-4171-99c6-23c3522ca0f8')
      <> regexp_replace(btrim(i.observations ->> '2978117c-a297-4171-99c6-23c3522ca0f8'), '\s+', ' ', 'g')
GROUP BY bad_value
ORDER BY bad_value;
-- 92 records with extra spaces

-- ---------------------------------------------------------------------
-- STEP 2 (analysis): how many USERS have account_name with space?
-- ---------------------------------------------------------------------
SELECT '[' || v.value || ']' AS bad_user_sync_value, count(DISTINCT u.id) AS user_count
FROM public.users u
CROSS JOIN LATERAL jsonb_array_elements(u.sync_settings->'subjectTypeSyncSettings') AS s(sts)
CROSS JOIN LATERAL jsonb_array_elements_text(s.sts->'syncConcept1Values') AS v(value)
WHERE v.value <> regexp_replace(btrim(v.value), '\s+', ' ', 'g')
GROUP BY v.value
ORDER BY v.value;
-- 302 account names with extra spaces

-- ---------------------------------------------------------------------
-- STEP 3 (worklist for /bulkSubjectMigration):
--   One row per bad value = one API request:
--     * subject_ids        -> ids to send in the request
--     * bad_value          -> current (padded) account name
--     * destination_value  -> cleaned value for
--                            destinationSyncConcepts["2978117c-a297-4171-99c6-23c3522ca0f8"]
-- ---------------------------------------------------------------------
SELECT i.observations ->> '2978117c-a297-4171-99c6-23c3522ca0f8'  AS bad_value,
       regexp_replace(btrim(i.observations ->> '2978117c-a297-4171-99c6-23c3522ca0f8'),
                      '\s+', ' ', 'g')                             AS destination_value,
       count(*)                                                    AS subject_count,
       jsonb_agg(i.id ORDER BY i.id)                               AS subject_ids
FROM public.individual i
WHERE i.observations ? '2978117c-a297-4171-99c6-23c3522ca0f8'
  AND (i.observations ->> '2978117c-a297-4171-99c6-23c3522ca0f8')
      <> regexp_replace(btrim(i.observations ->> '2978117c-a297-4171-99c6-23c3522ca0f8'), '\s+', ' ', 'g')
GROUP BY 1, 2
ORDER BY 1;

-- STEP 3b (variant): one row per individual (id + uuid + bad/clean value),
-- for building payloads manually or spot-checking records.
SELECT i.id,
       i.uuid,
       st.name                                                     AS subject_type,
       i.observations ->> '2978117c-a297-4171-99c6-23c3522ca0f8'  AS bad_value,
       regexp_replace(btrim(i.observations ->> '2978117c-a297-4171-99c6-23c3522ca0f8'),
                      '\s+', ' ', 'g')                             AS destination_value
FROM public.individual i
JOIN public.subject_type st ON st.id = i.subject_type_id
WHERE i.observations ? '2978117c-a297-4171-99c6-23c3522ca0f8'
  AND (i.observations ->> '2978117c-a297-4171-99c6-23c3522ca0f8')
      <> regexp_replace(btrim(i.observations ->> '2978117c-a297-4171-99c6-23c3522ca0f8'), '\s+', ' ', 'g')
ORDER BY bad_value, i.id;

-- =====================================================================
-- STEP 4: fix users' sync_settings via SQL
--   (run AFTER the API migrations are done)
-- =====================================================================
BEGIN;

WITH rebuilt AS (
    SELECT u.id,
           jsonb_set(u.sync_settings, '{subjectTypeSyncSettings}', r.arr) AS new_settings
    FROM public.users u
    CROSS JOIN LATERAL (
        SELECT jsonb_agg(
                   CASE
                       WHEN sts ? 'syncConcept1Values' THEN
                           jsonb_set(sts, '{syncConcept1Values}',
                               (SELECT coalesce(
                                           jsonb_agg(to_jsonb(regexp_replace(btrim(val), '\s+', ' ', 'g'))),
                                           '[]'::jsonb)
                                FROM jsonb_array_elements_text(sts->'syncConcept1Values') AS val))
                       ELSE sts
                   END
                   ORDER BY ord) AS arr
        FROM jsonb_array_elements(u.sync_settings->'subjectTypeSyncSettings')
             WITH ORDINALITY AS t(sts, ord)
    ) AS r
    WHERE u.sync_settings->'subjectTypeSyncSettings' IS NOT NULL
)
UPDATE public.users u
SET sync_settings = rebuilt.new_settings
FROM rebuilt
WHERE u.id = rebuilt.id
  AND EXISTS (
      SELECT 1
      FROM jsonb_array_elements(u.sync_settings->'subjectTypeSyncSettings') AS s(sts)
      CROSS JOIN LATERAL jsonb_array_elements_text(s.sts->'syncConcept1Values') AS v(value)
      WHERE v.value <> regexp_replace(btrim(v.value), '\s+', ' ', 'g')
  );

COMMIT;   -- or ROLLBACK;

-- =====================================================================
-- STEP 5: verify — all three must return 0
--   (after both the API migrations and Step 4)
-- =====================================================================
SELECT count(*) AS remaining_bad_observations
FROM public.individual i
WHERE i.observations ? '2978117c-a297-4171-99c6-23c3522ca0f8'
  AND (i.observations ->> '2978117c-a297-4171-99c6-23c3522ca0f8')
      <> regexp_replace(btrim(i.observations ->> '2978117c-a297-4171-99c6-23c3522ca0f8'), '\s+', ' ', 'g');

SELECT count(*) AS remaining_bad_sync_column
FROM public.individual
WHERE sync_concept_1_value IS NOT NULL
  AND sync_concept_1_value <> regexp_replace(btrim(sync_concept_1_value), '\s+', ' ', 'g');

SELECT count(*) AS remaining_bad_user_sync_settings
FROM public.users u
CROSS JOIN LATERAL jsonb_array_elements(u.sync_settings->'subjectTypeSyncSettings') AS s(sts)
CROSS JOIN LATERAL jsonb_array_elements_text(s.sts->'syncConcept1Values') AS v(value)
WHERE v.value <> regexp_replace(btrim(v.value), '\s+', ' ', 'g');
