-- =====================================================================
-- Goonj: remove extra spaces from Account Name values
--   Concept: Account Name (uuid 2978117c-a297-4171-99c6-23c3522ca0f8)
--   Fixes: individual.observations, individual.sync_concept_1_value,
--          audit.last_modified_date_time (same statement),
--          users.sync_settings -> subjectTypeSyncSettings[*].syncConcept1Values
--   Cleaning rule: btrim + collapse internal runs of whitespace to one space
-- =====================================================================

set role goonj;

BEGIN;

-- ---------------------------------------------------------------------
-- STEP 1 (pre-check): bad values and their counts
-- ---------------------------------------------------------------------
SELECT '[' || (i.observations ->> '2978117c-a297-4171-99c6-23c3522ca0f8') || ']' AS bad_value,
       count(*) AS record_count
FROM public.individual i
WHERE i.observations ? '2978117c-a297-4171-99c6-23c3522ca0f8'
  AND (i.observations ->> '2978117c-a297-4171-99c6-23c3522ca0f8')
      <> regexp_replace(btrim(i.observations ->> '2978117c-a297-4171-99c6-23c3522ca0f8'), '\s+', ' ', 'g')
GROUP BY bad_value
ORDER BY bad_value;

SELECT '[' || v.value || ']' AS bad_user_sync_value, count(*) AS user_count
FROM public.users u
CROSS JOIN LATERAL jsonb_array_elements(u.sync_settings->'subjectTypeSyncSettings') AS s(sts)
CROSS JOIN LATERAL jsonb_array_elements_text(s.sts->'syncConcept1Values') AS v(value)
WHERE v.value <> regexp_replace(btrim(v.value), '\s+', ' ', 'g')
GROUP BY v.value
ORDER BY v.value;

-- ---------------------------------------------------------------------
-- STEP 2: fix individual (observations + sync column) and bump audit,
--         all in ONE statement via data-modifying CTE
-- ---------------------------------------------------------------------
WITH fixed AS (
    UPDATE public.individual i
    SET observations = jsonb_set(
            i.observations,
            '{2978117c-a297-4171-99c6-23c3522ca0f8}',
            to_jsonb(regexp_replace(btrim(i.observations ->> '2978117c-a297-4171-99c6-23c3522ca0f8'), '\s+', ' ', 'g'))),
        sync_concept_1_value = regexp_replace(btrim(i.sync_concept_1_value), '\s+', ' ', 'g')
    WHERE i.observations ? '2978117c-a297-4171-99c6-23c3522ca0f8'
      AND (
            (i.observations ->> '2978117c-a297-4171-99c6-23c3522ca0f8')
                <> regexp_replace(btrim(i.observations ->> '2978117c-a297-4171-99c6-23c3522ca0f8'), '\s+', ' ', 'g')
            OR i.sync_concept_1_value
                <> regexp_replace(btrim(i.sync_concept_1_value), '\s+', ' ', 'g')
          )
    RETURNING i.id, i.audit_id
)
UPDATE public.audit a
SET last_modified_date_time = now()
FROM fixed f
WHERE a.id = f.audit_id;

-- ---------------------------------------------------------------------
-- STEP 3: fix users' sync_settings (every subjectTypeSyncSettings entry)
-- ---------------------------------------------------------------------
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

-- ---------------------------------------------------------------------
-- STEP 4: verify — every query below must return 0
-- ---------------------------------------------------------------------
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

-- ---------------------------------------------------------------------
-- STEP 5: commit only if all three verify counts are 0
-- ---------------------------------------------------------------------
COMMIT;
-- ROLLBACK;  -- use instead of COMMIT if verification fails
