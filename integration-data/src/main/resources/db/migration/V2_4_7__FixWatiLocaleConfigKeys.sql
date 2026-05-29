-- Fix WATI locale config keys to match Avni locale format (te_IN, od_IN) instead of ISO 639-1 short codes (te, or)
UPDATE integration_system_config
SET key = REPLACE(key, '.template_name.te', '.template_name.te_IN')
WHERE key LIKE 'flow.%.template_name.te';

UPDATE integration_system_config
SET key = REPLACE(key, '.template_name.or', '.template_name.od_IN')
WHERE key LIKE 'flow.%.template_name.or';
