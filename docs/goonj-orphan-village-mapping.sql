-- =====================================================================
-- Goonj: map orphan Activities / Distributions to their Village group
--   Card: Goonj-Data-Tech/work#26
--
-- BACKGROUND
--   Every Activity/Distribution is registered on a Village location and
--   must become a member of that village's Village group subject (via
--   the hidden Group Affiliation concept on the registration forms).
--   Via draft-edit, users could save with a village that doesn't exist,
--   so no group membership was created (the card's "~200" records).
--
-- DIAGNOSIS (queries run on prod, 2026-09-04 — details in STEP 1)
--   Orphan = non-voided Activity/Distribution with no active group
--   membership, excluding 'Other' villages. Found 7,204 orphans, in
--   two buckets:
--     bucket A:   482 (279 Act + 203 Dist) — NO Village group subject
--                 exists at their address. This is the card's actual
--                 draft bug; STEP 3 creates these villages first.
--     bucket B: 6,722 (3,776 Act + 2,946 Dist) — Village exists but the
--                 membership was never created: the hidden GA rule has
--                 been failing silently since ~Sept-Oct 2025, leaking
--                 several hundred orphans per month, still ongoing.
--   Also found ~139 members with duplicate active memberships —
--   residue of the original rollout's duplicate-village issue
--   (optional cleanup in STEP 6).
--
-- PLAN
--   STEP 0  sanity-check the name-based lookups (no hardcoded ids)
--   STEP 1  analysis: orphan counts, monthly trend, bucket split
--   STEP 2  analysis: orphan addresses missing a Village group subject
--   STEP 3  insert missing Village group subjects       (bucket A)
--   STEP 4  insert group_subject memberships            (buckets A + B)
--   STEP 5  verify everything is clean
--   STEP 6  (optional) void duplicate active memberships
--
-- PREREQUISITES / DECISIONS BEFORE RUNNING
--   1. Team sign-off that bucket B is in scope: ~7,200 membership rows
--      + 482 Village subjects will be inserted, not just the card's 200.
--   2. Replace CHANGE_ME with the admin/system username (user id 9820
--      in the reference run) — appears in STEPs 0, 3, 4, 5, 6.
--   3. The leak continues until active users are on app release 15.3
--      with GA visible + mandatory + auto-populated on both forms.
--      => Re-run STEPs 1-5 as a mop-up after that rollout; the script
--         is idempotent (only ever touches current orphans).
--
--   Rules carried over from the original rollout script
--   (avniproject/Goonj/create_and_add_to_village.sql) INCLUDING fixes
--   for the mistakes made back then:
--     * orphan check uses membership_end_date IS NULL (the original used
--       IS NOT NULL and had to void + redo)
--     * 'Other' villages are excluded from BOTH the village-creation and
--       the membership inserts (original created them, then voided)
--     * membership insert picks exactly ONE village per member
--       (DISTINCT ON) so duplicate Village subjects on the same address
--       cannot produce duplicate memberships
-- =====================================================================

set role goonj;

-- ---------------------------------------------------------------------
-- STEP 0: sanity-check the lookups used throughout
--   Cross-check against the reference script's known values:
--     organisation 391, Activity/Distribution subject types 1099/1100,
--     Village subject type 1470, group roles 824/825, admin user 9820.
-- ---------------------------------------------------------------------

-- 0a. subject type names — the script assumes 'Activity', 'Distribution',
--     'Village'; fix the names below everywhere if these differ
SELECT id, name, "type" FROM public.subject_type WHERE is_voided = false ORDER BY id;

-- 0b. resolved ids — every column must be non-null
SELECT
    (SELECT id FROM public.organisation WHERE db_user = 'goonj')                           AS org_id,
    (SELECT id FROM public.subject_type WHERE name = 'Activity'     AND is_voided = false) AS activity_st_id,
    (SELECT id FROM public.subject_type WHERE name = 'Distribution' AND is_voided = false) AS distribution_st_id,
    (SELECT id FROM public.subject_type WHERE name = 'Village'      AND is_voided = false) AS village_st_id,
    (SELECT id FROM public.group_role
     WHERE group_subject_type_id  = (SELECT id FROM public.subject_type WHERE name = 'Village'  AND is_voided = false)
       AND member_subject_type_id = (SELECT id FROM public.subject_type WHERE name = 'Activity' AND is_voided = false)
       AND is_voided = false)                                                              AS activity_role_id,
    (SELECT id FROM public.group_role
     WHERE group_subject_type_id  = (SELECT id FROM public.subject_type WHERE name = 'Village'      AND is_voided = false)
       AND member_subject_type_id = (SELECT id FROM public.subject_type WHERE name = 'Distribution' AND is_voided = false)
       AND is_voided = false)                                                              AS distribution_role_id,
    (SELECT id FROM public.users WHERE username = 'CHANGE_ME')                             AS admin_user_id;

-- 0c. confirm the admin/system user to attribute this fix to
--     (the reference script used user id 9820 — look up its username):
-- SELECT id, username FROM public.users WHERE id = 9820;

-- ---------------------------------------------------------------------
-- STEP 1 (analysis): orphan Activities / Distributions
--   orphan = non-voided subject with NO active group membership
--   (is_voided = false AND membership_end_date IS NULL  <-- corrected
--    predicate; the original script's IS NOT NULL bug lives on as a
--    cautionary tale)
--   'Other' villages are excluded — they legitimately stay unmapped.
-- ---------------------------------------------------------------------

-- 1a. orphan count per subject type
--     [2026-09-04: Activity 4,055 / Distribution 3,149 = 7,204]
SELECT st.name AS subject_type, count(*) AS orphan_count
FROM public.individual i
JOIN public.subject_type st ON st.id = i.subject_type_id
JOIN public.address_level al ON al.id = i.address_id
WHERE st.name IN ('Activity', 'Distribution')
  AND i.is_voided = false
  AND al.title <> 'Other'
  AND NOT EXISTS (SELECT 1 FROM public.group_subject gs
                  WHERE gs.member_subject_id = i.id
                    AND gs.is_voided = false
                    AND gs.membership_end_date IS NULL)
GROUP BY st.name;

-- 1b. overall membership coverage (sanity: membership IS the norm)
--     [2026-09-04: Activity 39,554/44,633; Distribution 23,518/27,382
--      have an active membership — i.e. ~88% coverage]
SELECT st.name, count(*) AS total,
       count(*) FILTER (WHERE EXISTS (SELECT 1 FROM public.group_subject gs
                                      WHERE gs.member_subject_id = i.id
                                        AND gs.is_voided = false
                                        AND gs.membership_end_date IS NULL)) AS with_active_membership
FROM public.individual i
JOIN public.subject_type st ON st.id = i.subject_type_id
WHERE st.name IN ('Activity', 'Distribution') AND i.is_voided = false
GROUP BY st.name;

-- 1c. orphans by month — shows the leak started ~Sept-Oct 2025 and is
--     still ongoing (several hundred/month). Re-check after the 15.3
--     form fix rolls out: new months should drop to ~0.
SELECT date_trunc('month', i.created_date_time) AS month, st.name, count(*)
FROM public.individual i
JOIN public.subject_type st ON st.id = i.subject_type_id
JOIN public.address_level al ON al.id = i.address_id
WHERE st.name IN ('Activity', 'Distribution')
  AND i.is_voided = false
  AND al.title <> 'Other'
  AND NOT EXISTS (SELECT 1 FROM public.group_subject gs
                  WHERE gs.member_subject_id = i.id
                    AND gs.is_voided = false
                    AND gs.membership_end_date IS NULL)
GROUP BY 1, 2 ORDER BY 1;

-- 1d. bucket split: does a Village group subject exist at the address?
--     [2026-09-04: false (bucket A) Act 279 + Dist 203 = 482;
--                  true  (bucket B) Act 3,776 + Dist 2,946 = 6,722]
SELECT st.name,
       EXISTS (SELECT 1 FROM public.individual v
               WHERE v.address_id = i.address_id
                 AND v.subject_type_id = (SELECT id FROM public.subject_type
                                          WHERE name = 'Village' AND is_voided = false)
                 AND v.is_voided = false) AS village_subject_exists,
       count(*)
FROM public.individual i
JOIN public.subject_type st ON st.id = i.subject_type_id
JOIN public.address_level al ON al.id = i.address_id
WHERE st.name IN ('Activity', 'Distribution')
  AND i.is_voided = false
  AND al.title <> 'Other'
  AND NOT EXISTS (SELECT 1 FROM public.group_subject gs
                  WHERE gs.member_subject_id = i.id
                    AND gs.is_voided = false
                    AND gs.membership_end_date IS NULL)
GROUP BY 1, 2;

-- 1e (spot-check): the orphans themselves
SELECT i.id, i.uuid, st.name AS subject_type, al.title AS village,
       i.registration_date, i.created_date_time
FROM public.individual i
JOIN public.subject_type st ON st.id = i.subject_type_id
JOIN public.address_level al ON al.id = i.address_id
WHERE st.name IN ('Activity', 'Distribution')
  AND i.is_voided = false
  AND al.title <> 'Other'
  AND NOT EXISTS (SELECT 1 FROM public.group_subject gs
                  WHERE gs.member_subject_id = i.id
                    AND gs.is_voided = false
                    AND gs.membership_end_date IS NULL)
ORDER BY st.name, al.title, i.id;

-- ---------------------------------------------------------------------
-- STEP 2 (analysis): orphan addresses with NO Village group subject
--   These are bucket A's villages, to be created in STEP 3.
--   [2026-09-04: expect these to cover the 482 bucket-A orphans]
-- ---------------------------------------------------------------------
SELECT DISTINCT i.address_id, al.title AS village
FROM public.individual i
JOIN public.subject_type st ON st.id = i.subject_type_id
JOIN public.address_level al ON al.id = i.address_id
WHERE st.name IN ('Activity', 'Distribution')
  AND i.is_voided = false
  AND al.title <> 'Other'
  AND NOT EXISTS (SELECT 1 FROM public.group_subject gs
                  WHERE gs.member_subject_id = i.id
                    AND gs.is_voided = false
                    AND gs.membership_end_date IS NULL)
  AND NOT EXISTS (SELECT 1 FROM public.individual v
                  WHERE v.address_id = i.address_id
                    AND v.subject_type_id = (SELECT id FROM public.subject_type
                                             WHERE name = 'Village' AND is_voided = false)
                    AND v.is_voided = false)
ORDER BY village;

-- ---------------------------------------------------------------------
-- STEP 3: create the missing Village group subjects (bucket A)
--   Only for addresses found in STEP 2 (i.e. only where an orphan
--   actually needs them); 'Other' excluded; one village per address.
-- ---------------------------------------------------------------------
BEGIN;

INSERT INTO public.individual
(uuid, address_id, observations, version, date_of_birth, date_of_birth_verified, gender_id, registration_date,
 organisation_id, first_name, last_name, is_voided, audit_id, facility_id, registration_location,
 subject_type_id, legacy_id, created_by_id, last_modified_by_id, created_date_time, last_modified_date_time,
 sync_concept_1_value, sync_concept_2_value, profile_picture, middle_name, manual_update_history, sync_disabled,
 sync_disabled_date_time)
SELECT uuid_generate_v4(), temp.address_id, '{}', 0, null, false,
       null, now()::date,
       (SELECT id FROM public.organisation WHERE db_user = 'goonj'),
       temp.title, null, false,
       create_audit(), null, null,
       (SELECT id FROM public.subject_type WHERE name = 'Village' AND is_voided = false), null,
       (SELECT id FROM public.users WHERE username = 'CHANGE_ME'),
       (SELECT id FROM public.users WHERE username = 'CHANGE_ME'),
       temp.custom_timestamp, temp.custom_timestamp, null,
       null, null, null, '#26|Insert villages for orphan activities/distributions',
       false, null
FROM (
    SELECT DISTINCT ON (i.address_id)
           i.address_id, al.title,
           current_timestamp + (random() * 1000 * interval '1 millisecond') AS custom_timestamp
    FROM public.individual i
    JOIN public.subject_type st ON st.id = i.subject_type_id
    JOIN public.address_level al ON al.id = i.address_id
    WHERE st.name IN ('Activity', 'Distribution')
      AND i.is_voided = false
      AND al.title <> 'Other'
      AND NOT EXISTS (SELECT 1 FROM public.group_subject gs
                      WHERE gs.member_subject_id = i.id
                        AND gs.is_voided = false
                        AND gs.membership_end_date IS NULL)
      AND NOT EXISTS (SELECT 1 FROM public.individual v
                      WHERE v.address_id = i.address_id
                        AND v.subject_type_id = (SELECT id FROM public.subject_type
                                                 WHERE name = 'Village' AND is_voided = false)
                        AND v.is_voided = false)
    ORDER BY i.address_id
) temp;
-- row count must equal the STEP 2 count

COMMIT;   -- or ROLLBACK;

-- ---------------------------------------------------------------------
-- STEP 4: create the group_subject memberships (buckets A + B)
--   [2026-09-04: expect ~4,055 Activity + ~3,149 Distribution rows]
--   DISTINCT ON (member) + ORDER BY v.id picks exactly one (the oldest)
--   Village subject per address, so pre-existing duplicate Village
--   subjects cannot create duplicate memberships.
-- ---------------------------------------------------------------------
BEGIN;

-- 4a. Activities -> Village
INSERT INTO public.group_subject
(uuid, group_subject_id, member_subject_id, group_role_id, membership_start_date, membership_end_date, organisation_id,
 audit_id, is_voided, version, created_by_id, last_modified_by_id, created_date_time, last_modified_date_time,
 member_subject_address_id, group_subject_address_id, group_subject_sync_concept_1_value,
 group_subject_sync_concept_2_value, sync_disabled, sync_disabled_date_time)
SELECT uuid_generate_v4(), temp.group_id, temp.member_id,
       (SELECT id FROM public.group_role
        WHERE group_subject_type_id  = (SELECT id FROM public.subject_type WHERE name = 'Village'  AND is_voided = false)
          AND member_subject_type_id = (SELECT id FROM public.subject_type WHERE name = 'Activity' AND is_voided = false)
          AND is_voided = false),
       temp.custom_timestamp, null,
       (SELECT id FROM public.organisation WHERE db_user = 'goonj'),
       create_audit(), false, 0,
       (SELECT id FROM public.users WHERE username = 'CHANGE_ME'),
       (SELECT id FROM public.users WHERE username = 'CHANGE_ME'),
       temp.custom_timestamp, temp.custom_timestamp, temp.address_id, temp.address_id,
       null, null, false, null
FROM (
    SELECT DISTINCT ON (da.id)
           da.id AS member_id, v.id AS group_id, da.address_id,
           current_timestamp + (random() * 1000 * interval '1 millisecond') AS custom_timestamp
    FROM public.individual da
    JOIN public.individual v ON v.address_id = da.address_id
    JOIN public.address_level al ON al.id = da.address_id
    WHERE da.subject_type_id = (SELECT id FROM public.subject_type
                                WHERE name = 'Activity' AND is_voided = false)
      AND da.is_voided = false
      AND v.subject_type_id = (SELECT id FROM public.subject_type
                               WHERE name = 'Village' AND is_voided = false)
      AND v.is_voided = false
      AND al.title <> 'Other'
      AND NOT EXISTS (SELECT 1 FROM public.group_subject gs
                      WHERE gs.member_subject_id = da.id
                        AND gs.is_voided = false
                        AND gs.membership_end_date IS NULL)
    ORDER BY da.id, v.id
) temp;

-- 4b. Distributions -> Village
INSERT INTO public.group_subject
(uuid, group_subject_id, member_subject_id, group_role_id, membership_start_date, membership_end_date, organisation_id,
 audit_id, is_voided, version, created_by_id, last_modified_by_id, created_date_time, last_modified_date_time,
 member_subject_address_id, group_subject_address_id, group_subject_sync_concept_1_value,
 group_subject_sync_concept_2_value, sync_disabled, sync_disabled_date_time)
SELECT uuid_generate_v4(), temp.group_id, temp.member_id,
       (SELECT id FROM public.group_role
        WHERE group_subject_type_id  = (SELECT id FROM public.subject_type WHERE name = 'Village'      AND is_voided = false)
          AND member_subject_type_id = (SELECT id FROM public.subject_type WHERE name = 'Distribution' AND is_voided = false)
          AND is_voided = false),
       temp.custom_timestamp, null,
       (SELECT id FROM public.organisation WHERE db_user = 'goonj'),
       create_audit(), false, 0,
       (SELECT id FROM public.users WHERE username = 'CHANGE_ME'),
       (SELECT id FROM public.users WHERE username = 'CHANGE_ME'),
       temp.custom_timestamp, temp.custom_timestamp, temp.address_id, temp.address_id,
       null, null, false, null
FROM (
    SELECT DISTINCT ON (da.id)
           da.id AS member_id, v.id AS group_id, da.address_id,
           current_timestamp + (random() * 1000 * interval '1 millisecond') AS custom_timestamp
    FROM public.individual da
    JOIN public.individual v ON v.address_id = da.address_id
    JOIN public.address_level al ON al.id = da.address_id
    WHERE da.subject_type_id = (SELECT id FROM public.subject_type
                                WHERE name = 'Distribution' AND is_voided = false)
      AND da.is_voided = false
      AND v.subject_type_id = (SELECT id FROM public.subject_type
                               WHERE name = 'Village' AND is_voided = false)
      AND v.is_voided = false
      AND al.title <> 'Other'
      AND NOT EXISTS (SELECT 1 FROM public.group_subject gs
                      WHERE gs.member_subject_id = da.id
                        AND gs.is_voided = false
                        AND gs.membership_end_date IS NULL)
    ORDER BY da.id, v.id
) temp;

COMMIT;   -- or ROLLBACK;

-- =====================================================================
-- STEP 5: verify — 5a..5d must all return 0
-- =====================================================================

-- 5a. no orphan Activities/Distributions remain (outside 'Other' villages)
SELECT count(*) AS remaining_orphans
FROM public.individual i
JOIN public.subject_type st ON st.id = i.subject_type_id
JOIN public.address_level al ON al.id = i.address_id
WHERE st.name IN ('Activity', 'Distribution')
  AND i.is_voided = false
  AND al.title <> 'Other'
  AND NOT EXISTS (SELECT 1 FROM public.group_subject gs
                  WHERE gs.member_subject_id = i.id
                    AND gs.is_voided = false
                    AND gs.membership_end_date IS NULL);

-- 5b. this run created no Village subjects on 'Other' addresses
SELECT count(*) AS other_villages_created
FROM public.individual i
JOIN public.address_level al ON al.id = i.address_id
WHERE i.manual_update_history = '#26|Insert villages for orphan activities/distributions'
  AND i.is_voided = false
  AND al.title = 'Other';

-- 5c. this run created no memberships into 'Other' villages
SELECT count(*) AS memberships_to_other
FROM public.group_subject gs
JOIN public.address_level al ON al.id = gs.group_subject_address_id
WHERE gs.created_by_id = (SELECT id FROM public.users WHERE username = 'CHANGE_ME')
  AND gs.created_date_time >= current_date
  AND gs.is_voided = false
  AND al.title = 'Other';

-- 5d. no member with more than one active membership
--     [2026-09-04: ~139 pre-existing duplicates from the original
--      rollout — this fix adds none. If team approves, clean them up
--      with STEP 6, after which this must return 0.]
SELECT count(*) AS members_with_duplicate_memberships
FROM (SELECT member_subject_id
      FROM public.group_subject
      WHERE is_voided = false AND membership_end_date IS NULL
      GROUP BY member_subject_id
      HAVING count(*) > 1) dup;

-- 5e (info): what this run inserted
--     [expected: ~482 villages, ~7,204 memberships as of 2026-09-04]
SELECT 'villages created' AS what, count(*)
FROM public.individual
WHERE manual_update_history = '#26|Insert villages for orphan activities/distributions'
  AND is_voided = false
UNION ALL
SELECT 'memberships created', count(*)
FROM public.group_subject
WHERE created_by_id = (SELECT id FROM public.users WHERE username = 'CHANGE_ME')
  AND created_date_time >= current_date
  AND is_voided = false;

-- =====================================================================
-- STEP 6 (OPTIONAL, needs team sign-off): void duplicate active
-- memberships left over from the original rollout (~139 members with
-- more than one active membership; residue of the duplicate-Village
-- issue). Keeps the OLDEST membership per member, voids the rest.
-- Run STEP 5d again afterwards — it must return 0.
-- =====================================================================
-- BEGIN;
--
-- UPDATE public.group_subject
-- SET is_voided = true,
--     last_modified_by_id = (SELECT id FROM public.users WHERE username = 'CHANGE_ME'),
--     last_modified_date_time = current_timestamp + (random() * 1000 * interval '1 millisecond')
-- WHERE id IN (
--     SELECT id FROM (
--         SELECT id, row_number() OVER (PARTITION BY member_subject_id
--                                       ORDER BY created_date_time, id) AS rn
--         FROM public.group_subject
--         WHERE is_voided = false AND membership_end_date IS NULL
--     ) ranked
--     WHERE rn > 1
-- );
--
-- COMMIT;   -- or ROLLBACK;
