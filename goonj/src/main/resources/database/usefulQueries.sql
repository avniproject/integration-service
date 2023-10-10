--  set role avni_int;

-- Sql query to Fetch type of errors that have to be classified
select
    count(*),
    case
        when (et.follow_up_step = '0') then 'Process'
        when (et.follow_up_step = '1') then 'Terminal'
        when (et.follow_up_step = '2') then 'Internal'
        when (et.follow_up_step = '3') then 'External'
        END,
    et.name,
    erl.error_msg
from error_record er
         join error_record_log erl on er.id = erl.error_record_id
         join error_type et on erl.error_type_id = et.id
where et.follow_up_step <> '1'
  and er.integration_system_id = 2
group by 2,3,4
;
