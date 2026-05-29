-- WatiErrorService.reportPermanentFailure looks up ErrorType 'WatiMessagePermanentFailure'; without
-- it, every permanent send failure is silently dropped (no ErrorRecord is created).
-- V2_4_5 only seeded BadRequest/BadConfiguration/RuntimeError/Success and was guarded on
-- name = 'wati' (never matches the per-org wati systems, which use system_type = 'wati'), so the
-- permanent-failure type is missing on any migration-seeded deploy.
--
-- Seed it for every existing wati system, idempotently. Wati systems provisioned later (via the
-- per-org setup SQL) seed it as part of provisioning.
INSERT INTO error_type (name, follow_up_step, uuid, is_voided, integration_system_id)
SELECT 'WatiMessagePermanentFailure', 'Terminal', uuid_generate_v4(), false, s.id
FROM integration_system s
WHERE s.system_type = 'wati'
  AND NOT EXISTS (
        SELECT 1 FROM error_type et
        WHERE et.integration_system_id = s.id
          AND et.name = 'WatiMessagePermanentFailure'
  );
