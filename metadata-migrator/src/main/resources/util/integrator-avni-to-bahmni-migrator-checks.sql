-- 1.1 Check concepts migrated properly
-- 1.1 *******START**************************
-- 1.1 Avni
select data_type, count(*)
from concept c
where 1 = 1
  and name not like '%[H]'
  and name not like '%[HP]'
  and name not like '%[Bahmni]'
group by data_type;

-- 1.1 Integration
select data_type_hint, count(*)
from mapping_metadata
where 1 = 1
  and mapping_group_name = 'Observation'
  and mapping_name = 'Concept'
  and avni_value not like '%[H]'
  and avni_value not like '%[HP]'
  and avni_value not like '%[Bahmni]'
  and avni_value not in ('Registration date', 'First name', 'Last name', 'Date of birth', 'Gender')
group by data_type_hint;
-- 1.1 *******END**************************

-- 1.2 Check forms migrated properly
-- 1.2 Check count of concepts grouped by a form
-- 1.2 Bahmni
select concept_set, cn.name, count(*)
from concept_set
         join concept_name cn on concept_set.concept_set = cn.concept_id
where cn.concept_name_type = 'FULLY_SPECIFIED'
  and concept_set.creator = 78
group by concept_set.concept_set, cn.name
order by 3 desc, cn.name;

-- 1.2 Avni
select form_type, st_name, p_name, et_name, form_name, coalesce(count_wo_dups, q_count)
from (
         with dup_counts as (
             select f.name as form_name, count(*) as dup_count
             from form_element fe
                      join concept c on fe.concept_id = c.id
                      join form_element_group feg on fe.form_element_group_id = feg.id
                      join form f on feg.form_id = f.id
             group by f.name, c.id
             having count(*) > 1)
         select q.*, (q_count - dup_count) + 1 as count_wo_dups
         from (
                  select form_type,
                         st.name  as st_name,
                         p.name   as p_name,
                         et.name  as et_name,
                         f.name   as form_name,
                         count(*) as q_count
                  from form_element fe
                           join form_element_group feg on fe.form_element_group_id = feg.id
                           join form f on feg.form_id = f.id
                           join form_mapping fm on f.id = fm.form_id
                           left join encounter_type et on et.id = observations_type_entity_id
                           left join program p on p.id = entity_id
                           left join subject_type st on fm.subject_type_id = st.id
                           join concept c on fe.concept_id = c.id
                  where fm.is_voided = false
                    and f.form_type in
                        ('IndividualProfile', 'ProgramEnrolment', 'ProgramEncounter', 'Encounter', 'ProgramExit')
                  group by form_type, st.name, p.name, et.name, f.name
                  order by count(*) desc) as q
                  left join dup_counts on dup_counts.form_name = q.form_name) form_counts
order by 6 desc, p_name, form_name;
