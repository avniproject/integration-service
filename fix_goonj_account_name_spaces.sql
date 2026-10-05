-- =====================================================================
-- Goonj: Account Name extra spaces
-- Concept "Account  name" (TWO spaces), 2978117c-a297-4171-99c6-23c3522ca0f8
--
-- DECISION: fix user sync_settings only. Do NOT touch subject values --
-- 91 of 95 are written from Salesforce on every sync, so an Avni-side
-- clean is reverted on the next SF update while user settings keep the
-- clean value, silently cutting the subject off from the user.
--
-- DO NOT RENAME THE CONCEPT. The double space is hardcoded in
-- ActivityConstants.java:13 and DistributionConstants.java:15 and looked
-- up by name -- renaming silently breaks Activity/Distribution -> SF.
--
-- Sync matches sync_concept_N_value by exact string compare, so a padded
-- user value matching a padded subject is CORRECT. Never blanket-trim.
--
-- Rationale and code evidence: docs/goonj/account-name-spaces-plan.md
--
--   Section 1  FETCH   read-only
--   Section 2  UPDATE  writes
--   Section 3  VERIFY  read-only
-- =====================================================================

set role goonj;


-- =====================================================================
-- SECTION 1 -- FETCH: user settings needing a fix. Read-only.
--   SAFE TO TRIM  all subjects clean; gains them, loses nothing
--   PARTIAL       subjects hold both spellings; gains the clean ones
--   DO NOT TRIM   value already matches subjects; trimming would LOSE
--                 them. Section 2 skips these.
-- Save this output -- it is the record of what Section 2 changes.
-- =====================================================================
WITH subj AS (
    SELECT val, sum(cnt) AS cnt FROM (
        SELECT sync_concept_1_value AS val, count(*) AS cnt FROM public.individual
        WHERE sync_concept_1_value IS NOT NULL AND is_voided = false GROUP BY 1
        UNION ALL
        SELECT sync_concept_2_value, count(*) FROM public.individual
        WHERE sync_concept_2_value IS NOT NULL AND is_voided = false GROUP BY 1
    ) x GROUP BY val
),
roll AS (
    SELECT regexp_replace(btrim(val),'\s+',' ','g') AS clean,
           min(val) FILTER (WHERE val = regexp_replace(btrim(val),'\s+',' ','g')) AS clean_spelling,
           coalesce(sum(cnt) FILTER (WHERE val = regexp_replace(btrim(val),'\s+',' ','g')),0) AS rec_clean,
           coalesce(sum(cnt) FILTER (WHERE val <> regexp_replace(btrim(val),'\s+',' ','g')),0) AS rec_padded,
           string_agg(DISTINCT '['||val||']', ' ') AS spellings
    FROM subj GROUP BY 1
),
usr AS (
    SELECT u.id AS user_id, u.username, k.slot, v.value AS val
    FROM public.users u
    CROSS JOIN LATERAL jsonb_array_elements(
        CASE WHEN jsonb_typeof(u.sync_settings->'subjectTypeSyncSettings')='array'
             THEN u.sync_settings->'subjectTypeSyncSettings' ELSE '[]'::jsonb END) AS s(sts)
    CROSS JOIN LATERAL (VALUES ('syncConcept1Values'),('syncConcept2Values')) AS k(slot)
    CROSS JOIN LATERAL jsonb_array_elements_text(
        CASE WHEN jsonb_typeof(s.sts->k.slot)='array' THEN s.sts->k.slot ELSE '[]'::jsonb END) AS v(value)
    WHERE u.is_voided = false
)
SELECT u.username, u.slot AS sync_concept_slot,
       '['||u.val||']'              AS user_setting_now,
       '['||r.clean_spelling||']'   AS change_to,
       coalesce(e.cnt,0)            AS subjects_matching_now,
       r.rec_clean                  AS subjects_gained_after_trim,
       r.rec_padded                 AS subjects_still_padded,
       r.spellings                  AS every_subject_spelling,
       CASE
         WHEN coalesce(e.cnt,0) > 0 THEN 'DO NOT TRIM - value already matches '||e.cnt||' subjects, trimming would lose them'
         WHEN r.rec_padded > 0      THEN 'PARTIAL - gains '||r.rec_clean||', leaves '||r.rec_padded||' padded subjects unmatched'
         ELSE 'SAFE TO TRIM - gains '||r.rec_clean||' subjects'
       END AS verdict
FROM usr u
JOIN roll r ON r.clean = regexp_replace(btrim(u.val),'\s+',' ','g')
LEFT JOIN subj e ON e.val = u.val          -- subjects matching the user's CURRENT value exactly
WHERE u.val <> regexp_replace(btrim(u.val),'\s+',' ','g')
  AND r.clean_spelling IS NOT NULL
  AND u.val <> r.clean_spelling
ORDER BY verdict, u.username, u.val;

-- =====================================================================
-- SECTION 2 -- UPDATE. WRITES DATA. Run Section 1 first.
--   Maps each padded value to the clean spelling the subjects hold.
--   Not a blanket trim. Skips "DO NOT TRIM" rows (coalesce(e.cnt,0)=0).
--   Preserves other keys, dedupes, skips null/non-array sync_settings.
--   Forces a full device re-sync for each user touched -- run outside
--   field hours.
-- =====================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS public.bkp_users_sync_settings_acctname (
    user_id           integer PRIMARY KEY,
    username          text,
    old_sync_settings jsonb,
    backed_up_at      timestamptz DEFAULT now()
);

INSERT INTO public.bkp_users_sync_settings_acctname (user_id, username, old_sync_settings)
SELECT u.id, u.username, u.sync_settings
FROM public.users u
WHERE u.is_voided = false
  AND jsonb_typeof(u.sync_settings->'subjectTypeSyncSettings') = 'array'
ON CONFLICT (user_id) DO NOTHING;

WITH subj AS (
    SELECT val, sum(cnt) AS cnt FROM (
        SELECT sync_concept_1_value AS val, count(*) AS cnt FROM public.individual
        WHERE sync_concept_1_value IS NOT NULL AND is_voided = false GROUP BY 1
        UNION ALL
        SELECT sync_concept_2_value, count(*) FROM public.individual
        WHERE sync_concept_2_value IS NOT NULL AND is_voided = false GROUP BY 1
    ) x GROUP BY val
),
roll AS (
    SELECT regexp_replace(btrim(val),'\s+',' ','g') AS clean,
           min(val) FILTER (WHERE val = regexp_replace(btrim(val),'\s+',' ','g')) AS clean_spelling
    FROM subj GROUP BY 1
),
usr AS (
    SELECT u.id AS user_id, v.value AS val
    FROM public.users u
    CROSS JOIN LATERAL jsonb_array_elements(
        CASE WHEN jsonb_typeof(u.sync_settings->'subjectTypeSyncSettings')='array'
             THEN u.sync_settings->'subjectTypeSyncSettings' ELSE '[]'::jsonb END) AS s(sts)
    CROSS JOIN LATERAL (VALUES ('syncConcept1Values'),('syncConcept2Values')) AS k(slot)
    CROSS JOIN LATERAL jsonb_array_elements_text(
        CASE WHEN jsonb_typeof(s.sts->k.slot)='array' THEN s.sts->k.slot ELSE '[]'::jsonb END) AS v(value)
    WHERE u.is_voided = false
),
fixes AS (
    SELECT DISTINCT u.user_id, u.val AS old_val, r.clean_spelling AS new_val
    FROM usr u
    JOIN roll r ON r.clean = regexp_replace(btrim(u.val),'\s+',' ','g')
    LEFT JOIN subj e ON e.val = u.val
    WHERE u.val <> regexp_replace(btrim(u.val),'\s+',' ','g')
      AND r.clean_spelling IS NOT NULL
      AND u.val <> r.clean_spelling
      AND coalesce(e.cnt,0) = 0          -- SAFETY: never touch a value that already matches subjects
),
rebuilt AS (
    SELECT u.id,
           jsonb_set(u.sync_settings, '{subjectTypeSyncSettings}', r.arr) AS new_settings
    FROM public.users u
    CROSS JOIN LATERAL (
        SELECT jsonb_agg(
                 (SELECT coalesce(jsonb_object_agg(kv.key,
                      CASE WHEN kv.key IN ('syncConcept1Values','syncConcept2Values')
                                AND jsonb_typeof(kv.value)='array'
                           THEN (SELECT coalesce(jsonb_agg(DISTINCT to_jsonb(
                                          coalesce(f.new_val, val))), '[]'::jsonb)
                                 FROM jsonb_array_elements_text(kv.value) AS val
                                 LEFT JOIN fixes f ON f.user_id = u.id AND f.old_val = val)
                           ELSE kv.value END), '{}'::jsonb)
                  FROM jsonb_each(t.sts) AS kv)
                 ORDER BY t.ord) AS arr
        FROM jsonb_array_elements(u.sync_settings->'subjectTypeSyncSettings')
             WITH ORDINALITY AS t(sts, ord)
    ) AS r
    WHERE jsonb_typeof(u.sync_settings->'subjectTypeSyncSettings') = 'array'
      AND EXISTS (SELECT 1 FROM fixes f WHERE f.user_id = u.id)
)
UPDATE public.users u
SET sync_settings = rebuilt.new_settings
FROM rebuilt
WHERE u.id = rebuilt.id;

-- Row count above must equal the distinct usernames in Section 1 that had
-- at least one non-"DO NOT TRIM" row. If not, ROLLBACK.

COMMIT;   -- or ROLLBACK;

-- Rollback after commit:
--   UPDATE public.users u SET sync_settings = b.old_sync_settings
--   FROM public.bkp_users_sync_settings_acctname b WHERE u.id = b.user_id;


-- =====================================================================
-- SECTION 3 -- VERIFY. Read-only.
-- =====================================================================

-- 3a. Re-run Section 1. Only "DO NOT TRIM" rows should remain.

-- 3b. Nothing lost -- expect 0 rows.
SELECT b.username,
       jsonb_array_length(b.old_sync_settings->'subjectTypeSyncSettings') AS before_count,
       jsonb_array_length(u.sync_settings->'subjectTypeSyncSettings')     AS after_count
FROM public.bkp_users_sync_settings_acctname b
JOIN public.users u ON u.id = b.user_id
WHERE jsonb_array_length(b.old_sync_settings->'subjectTypeSyncSettings')
   <> jsonb_array_length(u.sync_settings->'subjectTypeSyncSettings');

-- 3c. OUT OF SCOPE, for awareness: reverse direction. User value clean,
-- subject padded, so those subjects never reach the user. Subject side is
-- SF-owned, so the fix is to ADD the space. Per-case decision.
WITH subj AS (
    SELECT val, sum(cnt) AS cnt FROM (
        SELECT sync_concept_1_value AS val, count(*) AS cnt FROM public.individual
        WHERE sync_concept_1_value IS NOT NULL AND is_voided = false GROUP BY 1
        UNION ALL
        SELECT sync_concept_2_value, count(*) FROM public.individual
        WHERE sync_concept_2_value IS NOT NULL AND is_voided = false GROUP BY 1
    ) x GROUP BY val
),
usr AS (
    SELECT u.username, k.slot, v.value AS val
    FROM public.users u
    CROSS JOIN LATERAL jsonb_array_elements(
        CASE WHEN jsonb_typeof(u.sync_settings->'subjectTypeSyncSettings')='array'
             THEN u.sync_settings->'subjectTypeSyncSettings' ELSE '[]'::jsonb END) AS s(sts)
    CROSS JOIN LATERAL (VALUES ('syncConcept1Values'),('syncConcept2Values')) AS k(slot)
    CROSS JOIN LATERAL jsonb_array_elements_text(
        CASE WHEN jsonb_typeof(s.sts->k.slot)='array' THEN s.sts->k.slot ELSE '[]'::jsonb END) AS v(value)
    WHERE u.is_voided = false
)
SELECT u.username,
       u.slot                AS sync_concept_slot,
       '['||u.val||']'       AS user_setting_clean,
       '['||s.val||']'       AS subject_value_padded,
       s.cnt                 AS subjects_not_reaching_user
FROM usr u
JOIN subj s
  ON regexp_replace(btrim(s.val),'\s+',' ','g') = regexp_replace(btrim(u.val),'\s+',' ','g')
 AND s.val <> u.val
WHERE u.val = regexp_replace(btrim(u.val),'\s+',' ','g')   -- user side clean
  AND s.val <> regexp_replace(btrim(s.val),'\s+',' ','g')  -- subject side padded
  AND NOT EXISTS (SELECT 1 FROM subj e WHERE e.val = u.val)
ORDER BY s.cnt DESC, u.username;

-- =====================================================================
-- RUNBOOK
--   1. Prerelease: Section 1 (save output) -> Section 2 in the
--      transaction, check count before COMMIT -> Section 3 (3b = 0 rows)
--      -> log in as an affected user, force sync, confirm subjects appear.
--   2. Production: same order, outside field hours.
--   3. After one Goonj sync cycle: re-run Section 1. New rows mean SF is
--      still emitting padded names -- raise ingestion trim / SF cleanup
--      as a separate change.
-- =====================================================================
