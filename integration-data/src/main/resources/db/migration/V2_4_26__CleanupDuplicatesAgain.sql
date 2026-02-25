-- =====================================================
-- RE-RUN: Clean up duplicate mapping_metadata entries
-- =====================================================
-- The previous migration may have created new duplicates
-- Using CTE to identify exact duplicates and keep only one per mapping_type+is_voided

WITH duplicates AS (
    SELECT m.id, m.mapping_type_id, m.is_voided,
           ROW_NUMBER() OVER (PARTITION BY m.mapping_type_id, m.is_voided ORDER BY m.id DESC) as rn
    FROM mapping_metadata m
)
DELETE FROM mapping_metadata
WHERE id IN (
    SELECT id FROM duplicates WHERE rn > 1
);
