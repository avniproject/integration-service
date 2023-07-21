update integration_system set system_type = name where name = 'Goonj';
UPDATE public.error_type
SET comparison_operator = 'null'::varchar(255),
    comparison_value    = 'null'::text
WHERE name in ('NoImplementationInventoryWithId', 'UnclassifiedError', 'NoDemandWithId')
  and integration_system_id = 2;

UPDATE public.error_type
SET comparison_operator = '2'::varchar(255),
    comparison_value    = 'Individual not found with UUID ''.*'' or External ID ''.*'''::text
WHERE name = 'NoSubjectWithId'
  and integration_system_id = 2;
