-- =====================================================
-- Add Bahmni Provider UUID constant
-- Separate from provider ID for encounter creation
-- =====================================================

-- Add IntegrationBahmniProviderUUID constant (UUID of the provider, not the numeric ID)
INSERT INTO constants (key, value, uuid, is_voided)
VALUES ('IntegrationBahmniProviderUUID', 'c820353b-a997-4938-847f-1c9a48cc69c2', uuid_generate_v4(), false)
ON CONFLICT DO NOTHING;
