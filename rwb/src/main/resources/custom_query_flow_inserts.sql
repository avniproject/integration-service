INSERT INTO public.custom_query (id, uuid, name, query, organisation_id, is_voided, version, created_by_id,
                                 last_modified_by_id, created_date_time, last_modified_date_time)
VALUES

-- 1. Work Order Registration (36875)
(DEFAULT, 'a1adbdb4-1301-11f1-ae1c-0248ea85d583', 'Work Order Registration',
'WITH primary_users AS (
    SELECT DISTINCT u.id AS user_id, u.name AS first_name
    FROM public.users u
             JOIN user_group ug ON u.id = ug.user_id AND ug.is_voided = false
             JOIN groups g ON g.id = ug.group_id AND g.is_voided = false
    WHERE g.name = ''Primary Users''
      AND u.disabled_in_cognito = false
      AND u.is_voided = false
      AND u.organisation_id = :org_id
),
synced_users AS (
    SELECT DISTINCT user_id
    FROM sync_telemetry
    WHERE sync_status = ''complete''
)
SELECT pu.user_id, pu.first_name
FROM primary_users pu
JOIN synced_users su ON pu.user_id = su.user_id
WHERE NOT EXISTS (
    SELECT 1 FROM flow_request_queue frq
    JOIN message_receiver mr ON mr.id = frq.message_receiver_id
    WHERE mr.receiver_id = pu.user_id
      AND mr.receiver_type = ''User''
      AND frq.flow_id = :flow_id
      AND frq.is_voided = false AND frq.delivery_status = ''Sent''
);',
:org_id, false, 0, 1, 1, now(), now()),


-- 2. Nudge to register WO (36876)
(DEFAULT, 'd06712b8-1301-11f1-ae1d-0248ea85d583', 'Nudge to register WO',
'WITH primary_users AS (
    SELECT DISTINCT u.id AS user_id, u.name AS first_name
    FROM public.users u
             JOIN user_group ug ON u.id = ug.user_id AND ug.is_voided = false
             JOIN groups g ON g.id = ug.group_id AND g.is_voided = false
    WHERE g.name = ''Primary Users''
      AND u.disabled_in_cognito = false
      AND u.is_voided = false
      AND u.organisation_id = :org_id
),
old_sync AS (
    SELECT user_id
    FROM sync_telemetry
    WHERE sync_status = ''complete''
      AND created_date_time < now() - INTERVAL ''2 DAYS''
),
wo_users AS (
    SELECT DISTINCT created_by_id AS user_id
    FROM individual
    WHERE subject_type_id = (
        SELECT id FROM subject_type
        WHERE name = ''Work Order'' AND organisation_id = :org_id AND is_voided = false
    )
)
SELECT pu.user_id, pu.first_name
FROM primary_users pu
JOIN old_sync os ON pu.user_id = os.user_id
WHERE pu.user_id NOT IN (SELECT user_id FROM wo_users)
  AND NOT EXISTS (
    SELECT 1 FROM flow_request_queue frq
    JOIN message_receiver mr ON mr.id = frq.message_receiver_id
    WHERE mr.receiver_id = pu.user_id
      AND mr.receiver_type = ''User''
      AND frq.flow_id = :flow_id
      AND frq.is_voided = false AND frq.delivery_status = ''Sent''
      AND frq.created_date_time > now() - INTERVAL ''3 DAYS''
);',
:org_id, false, 0, 1, 1, now(), now()),


-- 3. Nudge to Login (36859)
(DEFAULT, '0a61e7cc-1302-11f1-ae1e-0248ea85d583', 'Nudge to Login',
'WITH primary_users AS (
    SELECT DISTINCT u.id AS user_id, u.name AS first_name
    FROM public.users u
             JOIN user_group ug ON u.id = ug.user_id AND ug.is_voided = false
             JOIN groups g ON g.id = ug.group_id AND g.is_voided = false
    WHERE g.name = ''Primary Users''
      AND u.disabled_in_cognito = false
      AND u.is_voided = false
      AND u.organisation_id = :org_id
),
no_sync_users AS (
    SELECT pu.user_id
    FROM primary_users pu
    WHERE pu.user_id NOT IN (
        SELECT user_id FROM sync_telemetry WHERE sync_status = ''complete''
    )
)
SELECT pu.user_id, pu.first_name
FROM primary_users pu
JOIN no_sync_users ns ON pu.user_id = ns.user_id
WHERE pu.user_id IN (
    SELECT id FROM users
    WHERE last_activated_date_time < now() - INTERVAL ''2 DAYS''
)
AND NOT EXISTS (
    SELECT 1 FROM flow_request_queue frq
    JOIN message_receiver mr ON mr.id = frq.message_receiver_id
    WHERE mr.receiver_id = pu.user_id
      AND mr.receiver_type = ''User''
      AND frq.flow_id = :flow_id
      AND frq.is_voided = false AND frq.delivery_status = ''Sent''
      AND frq.created_date_time > now() - INTERVAL ''3 DAYS''
);',
:org_id, false, 0, 1, 1, now(), now()),


-- 4. Successful WO registration (36877)
(DEFAULT, '2656ff9e-1302-11f1-ae1f-0248ea85d583', 'Successful WO registration',
'WITH primary_users AS (
    SELECT DISTINCT u.id AS user_id, u.name AS first_name
    FROM public.users u
             JOIN user_group ug ON u.id = ug.user_id AND ug.is_voided = false
             JOIN groups g ON g.id = ug.group_id AND g.is_voided = false
    WHERE g.name = ''Primary Users''
      AND u.disabled_in_cognito = false
      AND u.is_voided = false
      AND u.organisation_id = :org_id
),
wo_users AS (
    SELECT DISTINCT created_by_id AS user_id
    FROM individual
    WHERE subject_type_id = (
        SELECT id FROM subject_type
        WHERE name = ''Work Order'' AND organisation_id = :org_id AND is_voided = false
    )
)
SELECT pu.user_id, pu.first_name
FROM primary_users pu
JOIN wo_users wo ON pu.user_id = wo.user_id
WHERE NOT EXISTS (
    SELECT 1 FROM flow_request_queue frq
    JOIN message_receiver mr ON mr.id = frq.message_receiver_id
    WHERE mr.receiver_id = pu.user_id
      AND mr.receiver_type = ''User''
      AND frq.flow_id = :flow_id
      AND frq.is_voided = false AND frq.delivery_status = ''Sent''
);',
:org_id, false, 0, 1, 1, now(), now()),


-- 5. Daily Recording (36878)
(DEFAULT, '5b6a83fe-1302-11f1-ae20-0248ea85d583', 'Daily Recording',
'WITH primary_users AS (
    SELECT DISTINCT u.id AS user_id, u.name AS first_name
    FROM public.users u
             JOIN user_group ug ON u.id = ug.user_id AND ug.is_voided = false
             JOIN groups g ON g.id = ug.group_id AND g.is_voided = false
    WHERE g.name = ''Primary Users''
      AND u.disabled_in_cognito = false
      AND u.is_voided = false 
      AND u.organisation_id = :org_id
  ),
entities AS (
    SELECT u.id AS user_id,
           COUNT(DISTINCT CASE WHEN st.name = ''Farmer'' THEN i.id END) AS farmers,
           COUNT(DISTINCT CASE WHEN st.name = ''Excavating Machine'' THEN i.id END) AS machines,
           COUNT(DISTINCT CASE WHEN st.name = ''Gram Panchayat'' THEN i.id END) AS gps
    FROM individual i
             JOIN subject_type st ON st.id = i.subject_type_id
             JOIN users u ON u.id = i.created_by_id
    WHERE i.organisation_id = :org_id
      AND i.is_voided = false
    GROUP BY u.id
)
SELECT pu.user_id, pu.first_name
FROM primary_users pu
JOIN entities e ON pu.user_id = e.user_id
WHERE (e.farmers > 0 OR e.gps > 0)
  AND e.machines > 0
  
  --  NEW CHECK: ensure message not already sent
AND NOT EXISTS (
    SELECT 1
    FROM flow_request_queue frq
    JOIN message_receiver mr ON mr.id = frq.message_receiver_id
    WHERE mr.receiver_id = pu.user_id
      AND mr.receiver_type = ''User''
      AND frq.flow_id = :flow_id
      AND frq.is_voided = false AND frq.delivery_status = ''Sent''
);',
:org_id, false, 0, 1, 1, now(), now()),


-- 6. Nudge for Endline (36879)
(DEFAULT, '654ff854-1302-11f1-ae21-0248ea85d583', 'Nudge for Endline',
'WITH primary_users AS (
    SELECT DISTINCT u.id AS user_id, u.name AS first_name
    FROM public.users u
             JOIN user_group ug ON u.id = ug.user_id AND ug.is_voided = false
             JOIN groups g ON g.id = ug.group_id AND g.is_voided = false
    WHERE g.name = ''Primary Users''
      AND u.disabled_in_cognito = false
      AND u.is_voided = false
      AND u.organisation_id = :org_id
),
recent_activity AS (
    SELECT DISTINCT created_by_id AS user_id
    FROM encounter
    WHERE encounter_type_id IN (
        SELECT id FROM encounter_type
        WHERE name IN (''Work order daily Recording - Machine'', ''Work order daily Recording - Farmer'')
          AND organisation_id = :org_id
          AND is_voided = false
    )
      AND encounter_date_time > now() - INTERVAL ''5 DAYS''
),
entities AS (
    SELECT u.id AS user_id,
           COUNT(*) FILTER (WHERE st.name IN (''Farmer'', ''Gram Panchayat'')) AS farmers_or_gp,
           COUNT(*) FILTER (WHERE st.name = ''Excavating Machine'') AS machines
    FROM individual i
             JOIN subject_type st ON st.id = i.subject_type_id
             JOIN users u ON u.id = i.created_by_id
    WHERE i.organisation_id = :org_id
      AND i.is_voided = false
    GROUP BY u.id
)
SELECT pu.user_id, pu.first_name
FROM primary_users pu
JOIN entities e ON pu.user_id = e.user_id
WHERE (e.farmers_or_gp > 0 AND e.machines > 0)
  AND pu.user_id NOT IN (SELECT user_id FROM recent_activity)
  AND NOT EXISTS (
    SELECT 1 FROM flow_request_queue frq
    JOIN message_receiver mr ON mr.id = frq.message_receiver_id
    WHERE mr.receiver_id = pu.user_id
      AND mr.receiver_type = ''User''
      AND frq.flow_id = :flow_id
      AND frq.is_voided = false AND frq.delivery_status = ''Sent''
      AND frq.created_date_time > now() - INTERVAL ''3 DAYS''
);',
:org_id, false, 0, 1, 1, now(), now()),


-- 7. Nudge for Work Order Endline (36880)
(DEFAULT, '6ce74b44-1302-11f1-ae22-0248ea85d583', 'Nudge for Work Order Endline',
'WITH primary_users AS (
    SELECT DISTINCT u.id AS user_id, u.name AS first_name
    FROM public.users u
             JOIN user_group ug ON u.id = ug.user_id AND ug.is_voided = false
             JOIN groups g ON g.id = ug.group_id AND g.is_voided = false
    WHERE g.name = ''Primary Users''
      AND u.disabled_in_cognito = false
      AND u.is_voided = false
      AND u.organisation_id = :org_id
),
entity_counts AS (
    SELECT u.id AS user_id,
           COUNT(DISTINCT CASE WHEN st.name = ''Farmer'' THEN i.id END) AS farmers,
           COUNT(DISTINCT CASE WHEN st.name = ''Excavating Machine'' THEN i.id END) AS machines,
           COUNT(DISTINCT CASE WHEN st.name = ''Gram Panchayat'' THEN i.id END) AS gps
    FROM individual i
             JOIN subject_type st ON st.id = i.subject_type_id
             JOIN users u ON u.id = i.created_by_id
    WHERE i.organisation_id = :org_id
      AND i.is_voided = false
    GROUP BY u.id
),
endline_counts AS (
    SELECT u.id AS user_id,
           COUNT(DISTINCT CASE WHEN et.name = ''Farmer Endline'' THEN e.individual_id END) AS farmer_endlines,
           COUNT(DISTINCT CASE WHEN et.name = ''Excavating Machine Endline'' THEN e.individual_id END) AS machine_endlines,
           COUNT(DISTINCT CASE WHEN et.name = ''Gram Panchayat Endline'' THEN e.individual_id END) AS gp_endlines
    FROM encounter e
             JOIN encounter_type et ON et.id = e.encounter_type_id
             JOIN users u ON u.id = e.created_by_id
    WHERE e.organisation_id = :org_id
      AND e.is_voided = false
    GROUP BY u.id
),
wo_endline AS (
    SELECT DISTINCT created_by_id AS user_id
    FROM encounter
    WHERE encounter_type_id = (
        SELECT id FROM encounter_type
        WHERE name = ''Work order endline'' AND organisation_id = :org_id AND is_voided = false
    )
)
SELECT pu.user_id, pu.first_name
FROM primary_users pu
JOIN entity_counts ec ON pu.user_id = ec.user_id
LEFT JOIN endline_counts el ON pu.user_id = el.user_id
WHERE ec.farmers = COALESCE(el.farmer_endlines, 0)
  AND ec.machines = COALESCE(el.machine_endlines, 0)
  AND (ec.gps = 0 OR ec.gps = COALESCE(el.gp_endlines, 0))
  AND pu.user_id NOT IN (SELECT user_id FROM wo_endline)
  AND NOT EXISTS (
    SELECT 1 FROM flow_request_queue frq
    JOIN message_receiver mr ON mr.id = frq.message_receiver_id
    WHERE mr.receiver_id = pu.user_id
      AND mr.receiver_type = ''User''
      AND frq.flow_id = :flow_id
      AND frq.is_voided = false AND frq.delivery_status = ''Sent''
      AND frq.created_date_time > now() - INTERVAL ''3 DAYS''
);',
:org_id, false, 0, 1, 1, now(), now());
