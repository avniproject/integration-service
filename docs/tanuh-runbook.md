# Tanuh high-risk model: runbook

How to run the Tanuh module of the integration service for Tanuh's UAT and production organisations. Built in integration-service#130, #131 and #132, for avni-product#1910.

## 1. What runs

Each Tanuh organisation has two jobs on the integration service.

- **The main job**, every 15 minutes, reads the Oral Screenings that are new or edited since its last run. It sends each one that has photos to the high-risk model, which for now is a stand-in. It then picks the case's review group and writes five hidden values back onto the screening: the model's result, status, version and run time, and the review group. A screening with no photo is marked "Not scored" without a model call.
- **The retry job**, five minutes after each main run, tries again every screening that is waiting after a failure.

While the model is down, no new case with photos reaches the physician. Cases without a photo still arrive, marked "Not scored", so the physician's list can look normal; the red retry check is the sign. Cases with photos wait and none is lost. The stand-in never scores on an organisation set up as production.

## 2. Setting an organisation up

UAT goes first, on the staging integration service (int-staging). Production follows only once the epic's list under "Before production" holds, on int-prod.

1. Check the implementation team's configuration is live on the organisation: the six hidden questions, the job user in the Integration group only, and the visit type named exactly "Oral Screening".
2. Create the organisation's two health checks (section 4).
3. Copy the organisation's script, `docs/tanuh-uat-setup.sql` or `docs/tanuh-production-setup.sql`, to a folder outside this repository. Put the job user's name and password into the copy, run the copy on that service's database just before the restart, then delete the copy. Never type the password into the file in the repository: one commit would publish it. The first run reads screenings changed after that moment. Running the script again changes nothing.
   - If review booking has already stopped on the organisation, start from the moment it stopped instead: the script's `TIMESTAMP` option, in UTC. On production this happens when the configuration promotion carries UAT's Oral Screening form after UAT's booking stop. A screening referred between then and the restart booked no review, and a start at the restart would never read it, so it would never reach the physician. Ask the implementation team when the promotion landed.
4. Restart the integration service. For production, see section 3.
5. After the first run with a new screening, confirm it was scored (section 8). If the visit type has any other name on that organisation, the job reads nothing and its check stays green, so this is the only sign. Then confirm that a scored screening in any group other than Closed appears in the physician's pending list. If none does, the webapp is not reading the values the job writes, and no case reaches the physician.

## 3. Deploying to production

int-prod is deployed through CircleCI's production approval step: `PRODUCTION_approve`, then `PRODUCTION_deploy` to int-prod.avniproject.org. A deploy restarts every organisation's integrations on that server, so pick the moment.

## 4. Health checks

Each organisation has two checks on healthchecks.io, named after its integration system: `tanuh_uat` and `tanuh_uat-error` for UAT, `tanuh_prod` and `tanuh_prod-error` for production. Production's checks are its own.

- Set the period to 15 minutes and the grace to 10 minutes.
- Create them before the first run. A ping to a check that does not exist is dropped without a trace.
- A hung run shows only as a late ping. The service's connection to Avni has no working timeout, so one stuck call stops that organisation's job until the service is restarted.

## 5. Reading a red check

- **Main check down:** the run stopped with an error. Usually it is the sign-in, the visit list, a missing cursor row, or a cursor in the future (section 10). The error-reporting service (Bugsnag) has the details.
- **Retry check down:** at least one retry failed in that run, usually because of the model or a missing permission. Section 8 shows why each screening waits. The next run that clears every retry turns the check green. If it stays red while new screenings are being scored, see section 9.
- A failure in one organisation leaves the other organisation's checks as they were.

## 6. Changing the sample rate or the stand-in's mode

Settings are rows in `integration_system_config` for the organisation's integration system. They are read when the service starts, so every change needs a restart.

- `safety_sample_rate`: the share of cleared cases sent to a physician anyway. 0.05 is 5 in 100.
- `model_stub_mode`:
  - `by_encounter`, the default, gives each screening a fixed answer of its own.
  - `fixed` gives every screening the answer in `model_stub_fixed_result`, which must be `High Risk`, `Low Risk` or `Non Suspicious`.
  - `fail` makes every model call fail, to test the retry path.

```sql
UPDATE integration_system_config SET value = '0.10'
WHERE key = 'safety_sample_rate'
  AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'tanuh_uat');
```

## 7. Stopping an organisation

Set both schedule rows to `-` and restart. Voiding the integration system row does not stop it.

```sql
UPDATE integration_system_config SET value = '-'
WHERE key IN ('main.scheduled.job.cron', 'error.scheduled.job.cron')
  AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'tanuh_uat');
```

To start it again, set them back to `0 */15 * * * ?` and `0 5/15 * * * ?` and restart. Screenings changed while it was stopped are read on its first run.

## 8. Finding why a screening waits

Use the service's error records page, or this query on the service's database:

```sql
SELECT r.entity_id AS screening, t.name AS reason, l.error_msg, l.logged_at
FROM error_record r
JOIN error_record_log l ON l.error_record_id = r.id
JOIN error_type t ON t.id = l.error_type_id
WHERE r.integration_system_id = (SELECT id FROM integration_system WHERE name = 'tanuh_uat')
ORDER BY l.logged_at DESC;
```

- `HighRiskModelCallFailed`: the model did not answer. The retry job tries again on every run.
- `ScreeningProcessingFailed`: something else failed, such as reading the patient or a photo.

The main job logs each screening it scores, for example `Screening <id> scored Low Risk, group Low Risk`. Its read position moves forward as it reads:

```sql
SELECT read_upto_date_time AS reads_after_utc
FROM integrating_entity_status
WHERE entity_type = 'TanuhOralScreening'
  AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'tanuh_uat');
```

## 9. A screening that keeps failing

A retry check that stays red while new screenings are being scored usually means one screening the retries cannot fix, such as one whose photo file is missing or whose patient the job's user cannot read. While that check stays red, a new model outage on the organisation raises no fresh alert, so deal with it soon.

1. Run the query in section 8 a few hours apart. A screening still listed with the same reason while others clear is one the retries cannot fix. Its message says why.
2. If the cause can be fixed, such as a missing permission, fix it. The next retry run scores the screening and the check turns green.
3. If it cannot be fixed, the case has to reach the physician by hand, because a screening without the model's values never appears in the physician's list. Tell Tanuh's team which screening it is. The physician opens it at `/case/<screening uuid>` on the physician webapp (https://uat-tanuh.avniproject.org on UAT, https://tanuh.avniproject.org on production) and records the review there.
4. Then stop retrying it. On the service's error records page, switch on "processing disabled" for its record, or run the query below. The retry job skips it from its next run and the check turns green. While the record exists, the main job keeps leaving the screening alone, even after a worker edits it. Switching the setting off puts it back in the retries.

   ```sql
   UPDATE error_record SET processing_disabled = true
   WHERE entity_id = '<screening>'
     AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'tanuh_uat');
   ```

Whether such a case should instead be marked "Not scored" after some number of failed tries, so it reaches the physician's list on its own, is an open question with Tanuh.

## 10. Resetting the read position

The main job fails with "cursor is in the future" when its read position is more than a day ahead, because then it would never read anything. To set it back, or to read a past period again:

1. Stop the organisation (section 7).
2. Set the position to the last good time, in UTC. Indian time would be five and a half hours off.

   ```sql
   UPDATE integrating_entity_status SET read_upto_date_time = TIMESTAMP '2026-10-20 04:30:00'
   WHERE entity_type = 'TanuhOralScreening'
     AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'tanuh_uat');
   ```

3. Start it again. A screening the job already scored, and nobody has changed since, is skipped, so reading a period twice does not score anything twice.

## 11. Things that lose waiting screenings or leave data behind

- **The metadata migrator's Avni-to-Bahmni clean-up** must not run on a database with Tanuh organisations. It resets every read position on the database to 1900, so Tanuh's next run would score every screening the organisation has ever had. It also deletes every waiting screening's record, so those screenings are never retried.
- **A crash in the middle of a photo download** can leave that photo in the service's temporary folder (`/tmp`). After a crash, delete the files named `tanuh-photo-*` there.
- **A bulk update that gives many screenings the same time** is read correctly but more slowly, because each such time is read on its own. Spread the times where the update allows, as Avni's own migrations do with `id * interval '1 millisecond'`.

## 12. Rolling back

A build from before the Tanuh module cannot load the integration list on the admin page while rows of type `tanuh` exist, because it does not know that type. Roll forward rather than back. If a rollback is unavoidable, expect that page to fail until the current build is back.

## 13. The scheduler threads

Twenty threads run every module's jobs on the service (`avni.int.scheduler.thread.pool.size`). A long Tanuh run holds one of them, so many long runs at once can delay other modules.
