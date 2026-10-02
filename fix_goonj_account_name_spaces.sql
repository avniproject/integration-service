-- =====================================================================
-- Goonj: Account Name extra spaces
--
--   Concept: "Account  name"  (NOTE: TWO spaces in the concept name)
--            uuid 2978117c-a297-4171-99c6-23c3522ca0f8
--
--   !! DO NOT RENAME THE CONCEPT !!
--   The concept name itself contains a double space, and is hardcoded as
--   a string literal in ActivityConstants.java:13 and
--   DistributionConstants.java:15, looked up by name via
--   subject.getObservation(). "Tidying" it during a whitespace cleanup is
--   a natural mistake to make -- it would make that lookup return null and
--   silently stop Activity/Distribution sending the account to Salesforce.
--
-- ---------------------------------------------------------------------
--   THE DECISION  (02-Oct-2026)
-- ---------------------------------------------------------------------
--   We fix ONLY user sync_settings. We do NOT touch subject values.
--
--   Why: 91 of the 95 padded subject values belong to entities that are
--   written FROM Salesforce on every sync (Section 1 has the code
--   evidence). Cleaning them in Avni is reverted the next time the record
--   is touched in SF -- and because user settings would by then hold the
--   clean value, the subject silently stops reaching the user's phone.
--   The cleanup would cause the very bug it was meant to fix.
--
--   Salesforce owns those values. Avni owns user sync_settings. So we
--   make the settings match reality instead of the other way round.
--
-- ---------------------------------------------------------------------
--   WHAT THE PROBLEM ACTUALLY IS
-- ---------------------------------------------------------------------
--   Extra spaces only break anything when the two sides DISAGREE. Sync
--   matches a user's syncConcept values against the subject's
--   sync_concept_N_value by exact string comparison.
--
--     subject value   | user setting    | state
--     ----------------+-----------------+---------------------------------
--     "HDFC Bank "    | "HDFC Bank "    | fine -- matches, LEAVE ALONE
--     "Axis Bank"     | "Axis Bank  "   | BROKEN -- this is what we fix
--     "ICICI Bank "   | "ICICI Bank"    | broken, but subject is SF-owned;
--                                       | fix = ADD the space, not remove
--     "Orphan Bank  " | (none)          | nobody syncs it; cosmetic
--
--   So "strip all the spaces" is the wrong instinct. A padded user value
--   that matches a padded subject is CORRECT and must not be touched.
--
-- ---------------------------------------------------------------------
--   OBSERVATIONS FROM THE INVESTIGATION
-- ---------------------------------------------------------------------
--   * 95 subject records carry padded account names (92 on 03-Sep, 95 on
--     21-Sep) -- the set is still growing, so this is not a fixed backlog.
--   * Ownership split: Inventory Item 43, Demand 26, Dispatch 22 are all
--     Salesforce-written. Activity 3 and Distribution 1 are Avni-authored.
--   * All five subject types share ONE concept, "Account  name".
--   * Mapping metadata (integration DB), confirmed 21-Sep-2026:
--       Demand         | AccountName      | Account  name | data_type_hint null
--       Dispatch       | AccountName      | Account  name | data_type_hint null
--       Inventory Item | FromWhichAccount | Account  name | data_type_hint null
--     data_type_hint null means populateObservations() writes the SF
--     string verbatim -- free text, no coded-answer lookup, no trim.
--   * Inventory Item reads a DIFFERENT SF field (FromWhichAccount). If
--     Goonj ever cleans at source, that is a second field on a second
--     object -- easy to miss.
--   * Sync matching uses individual.sync_concept_1_value /
--     sync_concept_2_value, not the observations JSON. These queries use
--     those columns: more correct, and far cheaper (indexed).
--
-- ---------------------------------------------------------------------
--   SCOPE OF THIS SCRIPT
-- ---------------------------------------------------------------------
--   Section 1  code evidence for the decision          (reference only)
--   Section 2  FETCH   -- what would change, and why   (read-only)
--   Section 3  UPDATE  -- apply it                     (writes)
--   Section 4  VERIFY  -- confirm the result           (read-only)
--
--   NOT in scope, deliberately:
--     * subject / observation values (Salesforce owns them)
--     * the "ADD the space" direction -- a padded SF subject with a clean
--       user setting. Real breakage, but the fix is the opposite of a
--       trim and needs a per-case decision. Section 4 lists them.
--     * renaming the concept. Never.
-- =====================================================================

set role goonj;


-- #####################################################################
-- SECTION 1 -- CODE EVIDENCE: why subject values are not being fixed
-- #####################################################################
--
--   Verified against this branch on 02-Oct-2026.
--
--   1. Direction. Demand, Dispatch and Inventory are INBOUND from
--      Salesforce -- their workers live in worker/goonj/:
--        DemandWorker.java, DispatchWorker.java, InventoryWorker.java
--      Activity and Distribution are OUTBOUND, in worker/avni/.
--
--   2. AccountName is not filtered out on the way in:
--        Dispatch.java:40-41   Ignored_Fields lists ACCOUNT_ID and
--                              ACCOUNT_CODE but NOT ACCOUNT_NAME
--        Demand.java:22-23     every non-core SF field becomes an obs
--        Inventory.java:28-31  only SourceOfMaterial is ignored
--
--   3. The value is written verbatim. BaseGoonjService
--      .populateObservations(), the dataTypeHint == null branch:
--        observationHolder.addObservation(mapping.getAvniValue(),
--                                         goonjEntity.getValue(obsField));
--      No btrim, no whitespace collapse, anywhere in that path.
--
--   4. It is rewritten on EVERY update, not just on create:
--        DemandEventWorker.java:52     avniSubjectRepository.create(...)
--        DispatchEventWorker.java:59   avniSubjectRepository.create(...)
--        InventoryEventWorker.java:68  avniSubjectRepository.create(...)
--
--   Conclusion: any Avni-side edit to those observation values survives
--   only until Salesforce next touches the record. A durable fix would
--   need either Goonj cleaning Account.Name AND FromWhichAccount at
--   source, or a trim added at ingestion in populateObservations().
--   Neither is in scope here.
-- #####################################################################


-- #####################################################################
-- SECTION 2 -- FETCH: the user settings that need fixing
-- #####################################################################
--   Read-only. Run this first and keep the output -- it is the record of
--   what Section 3 will change.
--
--   verdict column:
--     SAFE TO TRIM  -- every subject holds the clean spelling. Trimming
--                      gains the user those subjects, loses nothing.
--     PARTIAL       -- subjects hold both spellings. Trimming gains the
--                      clean ones; the padded ones stay unmatched. Still
--                      a strict improvement, but not a complete fix.
--     DO NOT TRIM   -- the user's padded value ALREADY matches subjects
--                      exactly. Trimming would LOSE them. Section 3
--                      excludes these; they are shown so you can see them.
-- #####################################################################
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


-- #####################################################################
-- SECTION 3 -- UPDATE: apply the fix
-- #####################################################################
--   WRITES DATA. Run Section 2 first and keep its output.
--
--   What it does: for each affected user, replaces the padded value in
--   syncConcept1Values / syncConcept2Values with the clean spelling the
--   subjects actually hold. It is a value-by-value mapping, NOT a blanket
--   trim -- values that are correctly padded are left exactly as they are.
--
--   Safety properties:
--     * Skips any value that already matches subjects exactly
--       (the "DO NOT TRIM" verdict) -- coalesce(e.cnt,0) = 0 below.
--     * Preserves every other key in each sync setting object
--       (subjectTypeUUID, syncConcept1, syncConcept2, ...).
--     * jsonb_agg(DISTINCT ...) so that if trimming produces a value the
--       user already had, it collapses rather than duplicating.
--     * Users with null / non-array sync_settings are skipped, not errored.
--     * Backs up every row it touches first, inside the same transaction.
--
--   NOTE: changing sync_settings forces a FULL re-sync on that user's
--   device. Run it outside field hours.
-- #####################################################################

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

-- Expected: the row count above should equal the number of DISTINCT
-- usernames in the Section 2 output, excluding any that only had
-- "DO NOT TRIM" rows. If it does not, ROLLBACK and investigate.

COMMIT;   -- or ROLLBACK;

-- Rollback after commit, if needed:
--   UPDATE public.users u
--   SET sync_settings = b.old_sync_settings
--   FROM public.bkp_users_sync_settings_acctname b
--   WHERE u.id = b.user_id;


-- #####################################################################
-- SECTION 4 -- VERIFY
-- #####################################################################

-- 4a. Re-run SECTION 2. Expect: no SAFE TO TRIM and no PARTIAL rows left.
--     Any remaining rows should all be "DO NOT TRIM", which is correct --
--     those were deliberately skipped.

-- 4b. Nothing was lost: every user should still have the same number of
--     sync setting entries as before.
SELECT b.username,
       jsonb_array_length(b.old_sync_settings->'subjectTypeSyncSettings') AS before_count,
       jsonb_array_length(u.sync_settings->'subjectTypeSyncSettings')     AS after_count
FROM public.bkp_users_sync_settings_acctname b
JOIN public.users u ON u.id = b.user_id
WHERE jsonb_array_length(b.old_sync_settings->'subjectTypeSyncSettings')
   <> jsonb_array_length(u.sync_settings->'subjectTypeSyncSettings');
-- expect 0 rows

-- 4c. Show exactly what changed, per user.
SELECT b.username,
       b.old_sync_settings->'subjectTypeSyncSettings' AS before,
       u.sync_settings->'subjectTypeSyncSettings'     AS after
FROM public.bkp_users_sync_settings_acctname b
JOIN public.users u ON u.id = b.user_id
WHERE b.old_sync_settings IS DISTINCT FROM u.sync_settings
ORDER BY b.username;

-- 4d. OUT OF SCOPE, FOR AWARENESS: the opposite direction.
--     A user's value is clean but the subjects are padded -- so the user
--     is NOT receiving those subjects. The subject side is Salesforce-
--     owned and cannot be trimmed, so the fix here would be to ADD the
--     space to the user's setting. Not done automatically; each one needs
--     a look. Raise with Goonj if the counts are material.
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


-- #####################################################################
-- RUNBOOK
-- #####################################################################
--   1. PRERELEASE
--      a. Run SECTION 2. Save the output.
--      b. Run SECTION 3 inside the transaction. Check the UPDATE count
--         against the Section 2 output before COMMIT.
--      c. Run SECTION 4a-4c. 4b must return 0 rows.
--      d. Log in as one affected user on the Avni client, force a full
--         sync, confirm the expected subjects now appear.
--
--   2. PRODUCTION -- same order, outside field hours.
--
--   3. AFTERWARDS
--      a. Re-run SECTION 2 after a Goonj sync cycle. New rows mean
--         Salesforce is still emitting padded names and new users are
--         being configured against them -- at that point raise the
--         ingestion trim or the SF-side cleanup as a separate change.
--      b. Review the SECTION 4d list with Goonj.
--
--   REMINDER: never rename the "Account  name" concept (file header).
-- #####################################################################
