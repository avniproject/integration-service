UPDATE public.flyway_schema_history
SET checksum = -106739497
WHERE script = 'V2_3_3__Goonj_Clear_all_data.sql' and checksum <> -106739497;

UPDATE public.flyway_schema_history
SET checksum = -854101872
WHERE script = 'V2_3_4__Goonj_Reinit_data.sql' and checksum <> -854101872;

delete from public.flyway_schema_history
where script in ('V1_23__UniqueKeyInOneIntergrationSystem.sql',
                 'V1_22__AddIntegrationSystemConfigTable.sql');