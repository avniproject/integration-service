DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM integration_system WHERE name = 'wati') THEN
    INSERT INTO public.error_type (id, name, integration_system_id, comparison_operator, comparison_value, uuid, is_voided, follow_up_step)
    VALUES
      (DEFAULT, 'BadRequest', (SELECT id FROM integration_system WHERE name = 'wati'), null, null, uuid_generate_v4(), false, '2'),
      (DEFAULT, 'BadConfiguration', (SELECT id FROM integration_system WHERE name = 'wati'), null, null, uuid_generate_v4(), false, '2'),
      (DEFAULT, 'RuntimeError', (SELECT id FROM integration_system WHERE name = 'wati'), null, null, uuid_generate_v4(), false, '2'),
      (DEFAULT, 'Success', (SELECT id FROM integration_system WHERE name = 'wati'), null, null, uuid_generate_v4(), false, '2');
  END IF;
END $$;
