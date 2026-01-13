WITH primary_users as (
    select distinct u.id user_id, u.name first_name, catchment_id
    from public.users u
             join user_group ug on u.id = ug.user_id and ug.is_voided = false
             join groups g on g.id = ug.group_id and g.is_voided = false
    where g.name = 'Primary Users'
        and u.disabled_in_cognito = false
        and u.last_activated_date_time is null OR u.last_activated_date_time < now() - INTERVAL '3 DAYS'
        and u.is_voided = false
),
     work_orders as (
         select i.id wo_id, i.address_id, organisation_id
         from individual i
         where i.is_voided = false
           and i.subject_type_id = (select id
                                    from subject_type
                                    where name = 'Work Order'
                                      and organisation_id = (select id from organisation where db_user = :org_db_user)
                                      and not subject_type.is_voided)
     ),
     closed_work_orders as (select wo.wo_id, wo.address_id
                            from work_orders wo
                                     join encounter e on wo.wo_id = e.individual_id
                            where e.encounter_type_id =
                                  (select id
                                   from encounter_type
                                   where name = 'Work order endline'
                                     and organisation_id = (select id from organisation where db_user = :org_db_user)
                                     and not encounter_type.is_voided)
                              and (e.is_voided is null or e.is_voided = false)
                              and wo.organisation_id = (select id from organisation where db_user = :org_db_user)
                            group by 1, 2
                            having count(e.id) = 1),
     catchments_without_work_orders_or_atleast_one_open_work_order as (
         select c.id
         from catchment c
                  join virtual_catchment_address_mapping_table cam on cam.catchment_id = c.id
                  left join work_orders wo on wo.address_id = cam.addresslevel_id
                  left join closed_work_orders cwo on cwo.wo_id = wo.wo_id
         where c.is_voided = false
         group by 1
         having count(wo.wo_id) = null
             OR count( wo.wo_id) > count( cwo.wo_id)
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
from primary_users pu
         join catchments_without_work_orders_or_atleast_one_open_work_order cat on pu.catchment_id = cat.id
where user_id not in (select cuid
                      from active_user_ids where cuid is not null
                      union
                      select muid
                      from active_user_ids where muid is not null);