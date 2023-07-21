UPDATE public.error_type
SET comparison_operator = null::varchar(255),
    comparison_value    = null::text
WHERE name in ('NoImplementationInventoryWithId', 'UnclassifiedError', 'NoDemandWithId')
  and integration_system_id = 2;
