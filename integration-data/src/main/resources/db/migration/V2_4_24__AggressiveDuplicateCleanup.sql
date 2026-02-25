-- =====================================================
-- AGGRESSIVE CLEANUP: Remove duplicate mapping_metadata entries
-- =====================================================
-- Using CTE to identify exact duplicates and keep only one per mapping_type+is_voided

WITH duplicates AS (
    -- Find all mapping_metadata entries that have duplicates
    SELECT m.id, m.mapping_type_id, m.is_voided,
           ROW_NUMBER() OVER (PARTITION BY m.mapping_type_id, m.is_voided ORDER BY m.id DESC) as rn
    FROM mapping_metadata m
)
DELETE FROM mapping_metadata
WHERE id IN (
    SELECT id FROM duplicates WHERE rn > 1
);
