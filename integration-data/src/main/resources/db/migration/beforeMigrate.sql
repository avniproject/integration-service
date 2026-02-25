UPDATE public.flyway_schema_history
SET checksum = -106739497
WHERE script = 'V2_3_3__Goonj_Clear_all_data.sql' and checksum <> -106739497;

UPDATE public.flyway_schema_history
SET checksum = -854101872
WHERE script = 'V2_3_4__Goonj_Reinit_data.sql' and checksum <> -854101872;

delete from public.flyway_schema_history
where script in ('V1_23__UniqueKeyInOneIntergrationSystem.sql',
                 'V1_22__AddIntegrationSystemConfigTable.sql');

-- COMPREHENSIVE DUPLICATE CLEANUP: Remove all duplicate mapping_group and mapping_type entries
-- This runs BEFORE migrations to ensure clean state for every test/deployment

-- Step 1: Remove mapping_metadata entries that reference old duplicate mapping_groups (keep only max ID for each name/sys pair)
DELETE FROM mapping_metadata m1
WHERE m1.mapping_group_id IN (
    SELECT mg1.id
    FROM mapping_group mg1
    WHERE EXISTS (
        SELECT 1
        FROM mapping_group mg2
        WHERE mg2.name = mg1.name
        AND mg2.integration_system_id = mg1.integration_system_id
        AND mg2.id > mg1.id
    )
);

-- Step 2: Remove mapping_metadata entries that reference old duplicate mapping_types (keep only max ID for each name/sys pair)
DELETE FROM mapping_metadata m1
WHERE m1.mapping_type_id IN (
    SELECT mt1.id
    FROM mapping_type mt1
    WHERE EXISTS (
        SELECT 1
        FROM mapping_type mt2
        WHERE mt2.name = mt1.name
        AND mt2.integration_system_id = mt1.integration_system_id
        AND mt2.id > mt1.id
    )
);

-- Step 3: Delete old duplicate mapping_group entries (keep only the highest ID for each name/integration_system pair)
DELETE FROM mapping_group mg1
WHERE mg1.id IN (
    SELECT mg1.id
    FROM mapping_group mg1
    WHERE EXISTS (
        SELECT 1
        FROM mapping_group mg2
        WHERE mg2.name = mg1.name
        AND mg2.integration_system_id = mg1.integration_system_id
        AND mg2.id > mg1.id
    )
);

-- Step 4: Delete old duplicate mapping_type entries (keep only the highest ID for each name/integration_system pair)
DELETE FROM mapping_type mt1
WHERE mt1.id IN (
    SELECT mt1.id
    FROM mapping_type mt1
    WHERE EXISTS (
        SELECT 1
        FROM mapping_type mt2
        WHERE mt2.name = mt1.name
        AND mt2.integration_system_id = mt1.integration_system_id
        AND mt2.id > mt1.id
    )
);