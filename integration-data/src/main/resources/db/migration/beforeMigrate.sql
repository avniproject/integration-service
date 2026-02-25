UPDATE public.flyway_schema_history
SET checksum = -106739497
WHERE script = 'V2_3_3__Goonj_Clear_all_data.sql' and checksum <> -106739497;

UPDATE public.flyway_schema_history
SET checksum = -854101872
WHERE script = 'V2_3_4__Goonj_Reinit_data.sql' and checksum <> -854101872;

delete from public.flyway_schema_history
where script in ('V1_23__UniqueKeyInOneIntergrationSystem.sql',
                 'V1_22__AddIntegrationSystemConfigTable.sql');

-- Cleanup duplicate mapping_group entries (keep highest ID for each name/integration_system pair)
UPDATE mapping_metadata m1
SET mapping_group_id = (
    SELECT MAX(mg.id) FROM mapping_group mg
    WHERE mg.name = (SELECT name FROM mapping_group WHERE id = m1.mapping_group_id)
    AND mg.integration_system_id = (SELECT integration_system_id FROM mapping_group WHERE id = m1.mapping_group_id)
)
WHERE mapping_group_id IN (
    SELECT id FROM mapping_group mg1
    WHERE id < (
        SELECT MAX(id) FROM mapping_group mg2
        WHERE mg2.name = mg1.name
        AND mg2.integration_system_id = mg1.integration_system_id
    )
);

DELETE FROM mapping_group mg1
WHERE id < (
    SELECT MAX(id) FROM mapping_group mg2
    WHERE mg2.name = mg1.name
    AND mg2.integration_system_id = mg1.integration_system_id
);

-- Cleanup duplicate mapping_type entries (keep highest ID for each name/integration_system pair)
UPDATE mapping_metadata m1
SET mapping_type_id = (
    SELECT MAX(mt.id) FROM mapping_type mt
    WHERE mt.name = (SELECT name FROM mapping_type WHERE id = m1.mapping_type_id)
    AND mt.integration_system_id = (SELECT integration_system_id FROM mapping_type WHERE id = m1.mapping_type_id)
)
WHERE mapping_type_id IN (
    SELECT id FROM mapping_type mt1
    WHERE id < (
        SELECT MAX(id) FROM mapping_type mt2
        WHERE mt2.name = mt1.name
        AND mt2.integration_system_id = mt1.integration_system_id
    )
);

DELETE FROM mapping_type mt1
WHERE id < (
    SELECT MAX(id) FROM mapping_type mt2
    WHERE mt2.name = mt1.name
    AND mt2.integration_system_id = mt1.integration_system_id
);