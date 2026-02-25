-- =====================================================
-- FINAL CLEANUP: Remove all duplicate mapping_metadata entries
-- =====================================================
-- Root cause: Multiple mapping_metadata entries for the same mapping_type
-- This causes findByMappingTypeAndIsVoidedFalse() to return 2+ results instead of 1
-- Solution: Keep only the highest ID entry for each mapping_type+is_voided pair

-- Delete all but the highest ID for each mapping_type+is_voided combination
DELETE FROM mapping_metadata m1
WHERE id < (
    SELECT MAX(id)
    FROM mapping_metadata m2
    WHERE m2.mapping_type_id = m1.mapping_type_id
    AND m2.is_voided = m1.is_voided
);
