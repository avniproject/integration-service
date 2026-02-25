-- =====================================================
-- Fix Bahmni Provider ID and Encounter Role UUID
-- Updates constants with correct Bahmni values
-- =====================================================

-- Update IntegrationBahmniProvider with correct provider ID (numeric ID from Bahmni provider table)
UPDATE constants
SET value = '258'
WHERE key = 'IntegrationBahmniProvider';

-- Update IntegrationBahmniEncounterRole with correct role UUID
UPDATE constants
SET value = '8d94f280-c2cc-11de-8d13-0010c6dffd0f'
WHERE key = 'IntegrationBahmniEncounterRole';
