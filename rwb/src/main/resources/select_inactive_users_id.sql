WITH impacted_users as (
    WITH open_work_orders as (select i.id open_wo_id, i.address_id
                              from individual i
                                       left join encounter e on i.id = e.individual_id
                              where i.is_voided = false
                                and i.subject_type_id = (select id from subject_type where name = 'Work Order' and organisation_id = (select id from organisation where db_user = 'rwbniti') and not subject_type.is_voided)
                                and e.encounter_type_id =
                                    (select id from encounter_type where name = 'Work order endline'  and organisation_id = (select id from organisation where db_user = 'rwbniti') and not encounter_type.is_voided)
                                and (e.is_voided is null or e.is_voided = false)
                                and i.organisation_id = (select id from organisation where db_user = 'rwbniti')
                              group by 1
                              having count(e.id) = 1) -- Use = 1 for testing, < 1 for prod
    select distinct u.id user_id, u.username first_name
    from open_work_orders owo
             join virtual_catchment_address_mapping_table cam on cam.addresslevel_id = owo.address_id
             join users u on u.catchment_id = cam.catchment_id and u.is_voided = false
             join user_group ug on u.id = ug.user_id and ug.is_voided = false
             join groups g on g.id = ug.group_id and g.is_voided = false
    where g.name = 'Primary Users'
),
     active_user_ids as (select (case
                                     when ind.created_date_time > TO_TIMESTAMP(:cutOffDate, 'YYYY-MM-DDTHH24:MI:ss.MS')
                                         then ind.created_by_id end) as cuid,
                                ind.last_modified_by_id              as muid
                         from individual ind
                         where ind.last_modified_date_time > TO_TIMESTAMP(:cutOffDate, 'YYYY-MM-DDTHH24:MI:ss.MS')
                         UNION
                         select (case
                                     when enc.created_date_time > TO_TIMESTAMP(:cutOffDate, 'YYYY-MM-DDTHH24:MI:ss.MS')
                                         then enc.created_by_id end) as cuid,
                                enc.last_modified_by_id              as muid
                         from encounter enc
                         where enc.last_modified_date_time > TO_TIMESTAMP(:cutOffDate, 'YYYY-MM-DDTHH24:MI:ss.MS')
                         order by 1 asc)
select distinct user_id, first_name
from impacted_users iu
where user_id not in (select cuid
                      from active_user_ids
                      union
                      select muid
                      from active_user_ids);