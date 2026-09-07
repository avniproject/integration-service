# Map orphan Goonj Activities / Distributions to their Village group

Card: [Goonj-Data-Tech/work#26](https://github.com/Goonj-Data-Tech/work/issues/26)
Executable script: [goonj-orphan-village-mapping.sql](goonj-orphan-village-mapping.sql)
Reference (original rollout): [avniproject/Goonj/create_and_add_to_village.sql](https://github.com/avniproject/Goonj/blob/main/create_and_add_to_village.sql)

## Context / Problem

Every Goonj Activity and Distribution is registered on a Village location and must become a
member of that village's **Village group subject**. Membership is created through a hidden
Group Affiliation (GA) concept on the registration forms, auto-populated from the selected
location.

Via the draft feature, users could change the village after drafting and save — and because the
GA concept is hidden, the "village doesn't exist" error never surfaced. The card estimated
"~200 activities + distributions without village". Draft cannot be disabled (Goonj's field flow
genuinely collects information across multiple sessions), so the fix is:

1. **Prevention (app/config, not this repo):** make GA visible + mandatory + auto-populated on
   both registration forms. Product bug fixed in Avni 15.3; blocked until active users upgrade.
2. **Data fix (this plan):** find the orphans, create any missing Village group subjects, insert
   the memberships via SQL.

## Diagnosis (prod, 2026-09-04)

**Orphan** = non-voided Activity/Distribution with no active group membership
(`group_subject` row with `is_voided = false` and `membership_end_date IS NULL`),
excluding subjects on **'Other'** villages, which legitimately stay unmapped.

Diagnostic queries live in STEP 1 of the script. What they found:

- Membership is the norm: ~88% coverage (Activity 39,554 / 44,633; Distribution 23,518 / 27,382
  have an active membership). The orphan definition is right; the backlog is just far bigger
  than the card's estimate.
- No mis-filtering: there are zero non-voided end-dated memberships, so the orphans genuinely
  have no membership row at all.
- **7,204 orphans**, in two buckets:

| Bucket | Activity | Distribution | Total | Cause |
|---|---|---|---|---|
| A — no Village group subject exists at the address | 279 | 203 | **482** | The card's actual draft bug: saved with a village that doesn't exist |
| B — Village exists, membership never created | 3,776 | 2,946 | **6,722** | The hidden GA rule failing silently |

- The leak is **ongoing, not old backlog**: orphans start ~Sept–Oct 2025 and accumulate at
  several hundred per month through today (103 already in the first days of Sept 2026).
  Something shipped around Sept/Oct 2025 (draft feature / app release / GA rule change) —
  worth confirming with the team.
- Side finding: **~139 members carry duplicate active memberships** — residue of the original
  rollout's duplicate-Village issue (63,211 active membership rows vs 63,072 distinct members).

## Decisions needed before running

1. **Bucket B scope sign-off.** The card was scoped to ~200 records; the script will insert
   ~7,200 membership rows plus 482 Village subjects. Mapping only bucket A would leave 6,722
   known-broken records, so "fix all" is the expected call — but it should be explicit.
2. **Admin user.** Replace `CHANGE_ME` in the script with the admin/system username
   (user id 9820 in the reference run) — one find-and-replace.
3. **Duplicate cleanup (optional STEP 6).** Whether to void the ~139 duplicate memberships in
   the same card.

## Lessons carried over from the original rollout script

The reference script's own history shows three mistakes it had to undo; this script prevents
each from the start:

| Original mistake | Guard in this script |
|---|---|
| Orphan check used `membership_end_date IS NOT NULL` (should be `IS NULL`) — all memberships had to be voided and redone | Correct predicate everywhere |
| Created Village subjects and memberships for 'Other' addresses, voided them afterwards | `title <> 'Other'` filter in every insert |
| Duplicate Village subjects on one address produced 244 duplicate memberships | `DISTINCT ON (member) ... ORDER BY member, village_id` picks exactly one (oldest) village per member |

Other rules: no hardcoded ids — everything resolved by name (`organisation.db_user = 'goonj'`,
subject types by name, group roles by group + member subject type); inserts tagged with
`manual_update_history = '#26|...'` for traceability; timestamps get random-millisecond offsets
so sync ordering stays unique.

## Step-by-step SQL

All statements run under `set role goonj;`. Full runnable version (with comments and expected
counts inline) is in [goonj-orphan-village-mapping.sql](goonj-orphan-village-mapping.sql).

### STEP 0 — sanity-check the lookups (read-only)

Cross-check the resolved ids against the reference script's known values: organisation 391,
Activity/Distribution subject types 1099/1100, Village 1470, group roles 824/825, admin 9820.

```sql
-- 0a. confirm subject type names ('Activity', 'Distribution', 'Village' assumed throughout)
SELECT id, name, "type" FROM public.subject_type WHERE is_voided = false ORDER BY id;

-- 0b. every column must be non-null
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
```

### STEP 1 — analysis: count and characterize the orphans (read-only)

The orphan predicate used everywhere below:

```sql
-- 1a. orphan count per subject type   [2026-09-04: Activity 4,055 / Distribution 3,149]
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
```

The script also has: 1b membership coverage, 1c monthly trend (re-check after 15.3 — new months
should drop to ~0), 1d the bucket A/B split, 1e per-record spot-check.

### STEP 2 — analysis: bucket A's missing villages (read-only)

Orphan addresses where no non-voided Village subject exists — these get created in STEP 3.

```sql
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
```

### STEP 3 — insert the missing Village group subjects (bucket A, write)

One Village `individual` per STEP-2 address, named after the address title, tagged
`'#26|Insert villages for orphan activities/distributions'`. Wrapped in `BEGIN/COMMIT` —
the inserted row count must equal the STEP-2 count before committing.

```sql
INSERT INTO public.individual (uuid, address_id, observations, version, ..., manual_update_history, ...)
SELECT uuid_generate_v4(), temp.address_id, '{}', 0, ...,
       (SELECT id FROM public.subject_type WHERE name = 'Village' AND is_voided = false), ...,
       '#26|Insert villages for orphan activities/distributions', ...
FROM (
    SELECT DISTINCT ON (i.address_id) i.address_id, al.title, ...
    FROM public.individual i
    JOIN public.subject_type st ON st.id = i.subject_type_id
    JOIN public.address_level al ON al.id = i.address_id
    WHERE st.name IN ('Activity', 'Distribution')
      AND i.is_voided = false
      AND al.title <> 'Other'
      AND <orphan predicate>
      AND <no Village subject exists at i.address_id>
    ORDER BY i.address_id
) temp;
```

(Full column list in the .sql file — mirrors the reference script.)

### STEP 4 — insert the memberships (buckets A + B, write)

Two inserts (4a Activities, 4b Distributions), each in `BEGIN/COMMIT`. Expected
~4,055 + ~3,149 rows. Group role resolved per member type; `DISTINCT ON (da.id)` +
`ORDER BY da.id, v.id` guarantees one membership per orphan even where duplicate Village
subjects share an address.

```sql
INSERT INTO public.group_subject (uuid, group_subject_id, member_subject_id, group_role_id, ...)
SELECT uuid_generate_v4(), temp.group_id, temp.member_id,
       (SELECT id FROM public.group_role
        WHERE group_subject_type_id  = <Village subject type>
          AND member_subject_type_id = <Activity|Distribution subject type>
          AND is_voided = false), ...
FROM (
    SELECT DISTINCT ON (da.id) da.id AS member_id, v.id AS group_id, da.address_id, ...
    FROM public.individual da
    JOIN public.individual v ON v.address_id = da.address_id
    JOIN public.address_level al ON al.id = da.address_id
    WHERE da.subject_type_id = <Activity|Distribution subject type>
      AND da.is_voided = false
      AND v.subject_type_id = <Village subject type>
      AND v.is_voided = false
      AND al.title <> 'Other'
      AND <orphan predicate>
    ORDER BY da.id, v.id
) temp;
```

### STEP 5 — verify (read-only)

| Check | Must return |
|---|---|
| 5a remaining orphans (outside 'Other') | 0 |
| 5b Village subjects created on 'Other' addresses by this run | 0 |
| 5c memberships into 'Other' villages by this run | 0 |
| 5d members with >1 active membership | 0 after STEP 6; ~139 pre-existing if STEP 6 skipped |
| 5e info: villages / memberships created by this run | ~482 / ~7,204 (as of 2026-09-04) |

### STEP 6 — optional: void duplicate active memberships (write, needs sign-off)

Keeps the oldest active membership per member, voids the rest (~139 affected members).
Shipped commented-out in the .sql; run STEP 5d again afterwards — must return 0.

```sql
UPDATE public.group_subject
SET is_voided = true, last_modified_by_id = <admin>, last_modified_date_time = <now + random ms>
WHERE id IN (
    SELECT id FROM (
        SELECT id, row_number() OVER (PARTITION BY member_subject_id
                                      ORDER BY created_date_time, id) AS rn
        FROM public.group_subject
        WHERE is_voided = false AND membership_end_date IS NULL
    ) ranked
    WHERE rn > 1
);
```

## After the run

- **Re-run STEPs 1–5 after the 15.3 form fix rolls out** (GA visible + mandatory +
  auto-populated). The leak continues until active users upgrade; the script is idempotent —
  it only ever touches current orphans, so mop-up runs are safe.
- STEP 1c's monthly trend is the health signal: post-15.3 months should show ~0 new orphans.
- Prevention track (app designer, separate from this repo): GA concept visible + mandatory on
  Activity and Distribution registration forms, auto-populate from the selected village.
