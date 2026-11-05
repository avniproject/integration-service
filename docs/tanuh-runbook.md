# Tanuh high-risk model: runbook

How to run the Tanuh module of the integration service for Tanuh's UAT and production organisations. Built in integration-service#130, #131 and #132, for avni-product#1910.

## 1. What runs

Each Tanuh organisation has two jobs on the integration service.

- **The main job**, every 15 minutes, reads the Oral Screenings that are new or edited since its last run. For each one it:
  1. reads the screening's patient from Avni and sends the patient's registration, then the screening, to the data quality service (section 14), and reads the screening's verdict;
  2. writes the QC verdict back onto the screening: `QC Pass` or `QC Fail`, and the time of the check. If a field worker's edit reached Avni while the service was answering, nothing is written and the edited version is checked on the next run;
  3. if the screening passed QC and has photos, hands it with its photos to the high-risk model service, without waiting for a result. For now the model service is a stand-in. The real service works on its own time and writes its result to Avni itself, through Avni's API; that write is not this job's concern.
- **The retry job**, five minutes after each main run, tries again every screening that is waiting after a failure. A screening whose verdict was written but whose hand-over failed is only handed over again, if it is still marked `QC Pass`; any other is tried again from step 1.

Whether a screening passes QC is decided by this service, not by the data quality service: the data quality service returns each rule's outcome, and a screening fails QC when the service could not read it, or when a rule named in `qc_fail_rules` (section 14) failed it. Any other failed rule is logged and does not fail the screening, and neither does a rule that could not decide or did not apply. A screening that fails QC is not sent to the model service. A screening that passes without any photo is marked `QC Pass` but not sent either, as there is nothing for the model to look at.

While the data quality service or the model service is down, the screenings wait and none is lost; the red retry check is the sign. The stand-in never takes a screening on an organisation set up as production.

## 2. Setting an organisation up

UAT goes first, on the staging integration service (int-staging). Production follows only once the epic's list under "Before production" holds, on int-prod.

1. Check the implementation team's configuration is live on the organisation: the QC questions the job writes (`QC status` with its answers `QC Pass` and `QC Fail`, and `QC check time`, or whatever names section 15 maps), the job user in the Integration group only, and the visit type named exactly "Oral Screening".
2. Create the organisation's two health checks (section 4).
3. Copy the organisation's script, `docs/tanuh-uat-setup.sql` or `docs/tanuh-production-setup.sql`, to a folder outside this repository. Put the job user's name and password and the data quality service's address into the copy, run the copy on that service's database just before the restart, then delete the copy. Never type the password into the file in the repository: one commit would publish it. The first run reads screenings changed after that moment. Running the script again changes nothing.
   - If review booking has already stopped on the organisation, start from the moment it stopped instead: the script's `TIMESTAMP` option, in UTC. On production this happens when the configuration promotion carries UAT's Oral Screening form after UAT's booking stop. A screening referred between then and the restart was never sent on, and a start at the restart would never read it. Ask the implementation team when the promotion landed.
4. Import the organisation's question and answer names from `docs/tanuh-mapping-bundle` (section 15). Without them every job fails.
5. Let the job user into the data quality service (section 14, "Letting the job user in").
6. Restart the integration service. For production, see section 3.
7. After the first run with a new screening, open it and confirm it carries a QC verdict (section 8). If the visit type has any other name on that organisation than its `oral_screening` mapping row, the job reads nothing and its check stays green, so this is the only sign.

## 3. Deploying to production

int-prod is deployed through CircleCI's production approval step: `PRODUCTION_approve`, then `PRODUCTION_deploy` to int-prod.avniproject.org. A deploy restarts every organisation's integrations on that server, so pick the moment.

## 4. Health checks

Each organisation has two checks on healthchecks.io, named after its integration system: `tanuh_uat` and `tanuh_uat-error` for UAT, `tanuh_prod` and `tanuh_prod-error` for production. Production's checks are its own.

- Set the period to 15 minutes and the grace to 10 minutes.
- Create them before the first run. A ping to a check that does not exist is lost, and the service only logs "Health check ping failed".
- A hung run shows only as a late ping. The service's connection to Avni has no working timeout, so one stuck call stops that organisation's job until the service is restarted.

## 5. Reading a red check

- **Main check down:** the run stopped with an error. Usually it is the sign-in, the visit list, a missing cursor row, a cursor in the future (section 10), or mapping rows the job cannot trust (section 15). The error-reporting service (Bugsnag) has the details. A screening the data quality service or the model service would not take does not stop the run: it waits for a retry.
- **Retry check down:** at least one retry failed in that run, usually because the data quality service or the model service is down, or a permission is missing. Section 8 shows why each screening waits. The next run that clears every retry turns the check green. If it stays red while new screenings are being checked, see section 9.
- A failure in one organisation leaves the other organisation's checks as they were.

## 6. Settings

Settings are rows in `integration_system_config` for the organisation's integration system. They are read when the service starts, so every change needs a restart.

- `model_stub_mode`, for the stand-in model service:
  - `accept`, the default, takes every screening and does nothing with it.
  - `fail` makes every hand-over fail, to test the retry path.
- `high_risk_avni_user`: the Avni user the real model service writes its results as, once it does. A screening that user changed last is not sent again. Without it, each result the model service wrote would send the screening round once more. Set it before the real model service starts writing; it can stay unset while the stand-in runs.
- `dqs_api_url`, `dqs_tenant` and `qc_fail_rules`: section 14.

```sql
UPDATE integration_system_config SET value = 'fail'
WHERE key = 'model_stub_mode'
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

- `DataQualityCallFailed`: the data quality service did not take the screening. The message says why (section 14, "Reading a data quality failure"). The retry job tries again on every run.
- `HighRiskModelCallFailed`: the screening's verdict is written, but its hand-over failed: a photo would not download, or the model service did not take it. The retry job hands it over again on every run, without checking it a second time. A field worker's edit in the meantime makes it a new version, which is checked in full.
- `ScreeningProcessingFailed`: something else failed, such as reading the patient or a photo, or writing the verdict.

The main job logs each verdict it writes, for example `Screening <id>: QC Pass written`, and for a failed screening the rule outcomes and violations that failed it. Its read position moves forward as it reads:

```sql
SELECT read_upto_date_time AS reads_after_utc
FROM integrating_entity_status
WHERE entity_type = 'TanuhOralScreening'
  AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'tanuh_uat');
```

## 9. A screening that keeps failing

A retry check that stays red while new screenings are being checked usually means one screening the retries cannot fix, such as one the data quality service keeps refusing (a 400), one whose photo file is missing, or one whose patient the job's user cannot read. While that check stays red, a new outage on the organisation raises no fresh alert, so deal with it soon.

1. Run the query in section 8 a few hours apart. A screening still listed with the same reason while others clear is one the retries cannot fix. Its message says why.
2. If the cause can be fixed, such as a missing permission, fix it. The next retry run sends the screening and the check turns green.
3. If it cannot be fixed, tell Tanuh's team which screening it is: it has no QC verdict and was not sent to the model service, so it needs looking at by hand.
4. Then stop retrying it. On the service's error records page, switch on "processing disabled" for its record, or run the query below. The retry job skips it from its next run and the check turns green. While the record exists, the main job keeps leaving the screening alone, even after a worker edits it. Switching the setting off puts it back in the retries.

   ```sql
   UPDATE error_record SET processing_disabled = true
   WHERE entity_id = '<screening>'
     AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'tanuh_uat');
   ```

## 10. Resetting the read position

The main job fails with "cursor is in the future" when its read position is more than a day ahead, because then it would never read anything. To set it back, or to read a past period again:

1. Stop the organisation (section 7).
2. Set the position to the last good time, in UTC. Indian time would be five and a half hours off.

   ```sql
   UPDATE integrating_entity_status SET read_upto_date_time = TIMESTAMP '2026-10-20 04:30:00'
   WHERE entity_type = 'TanuhOralScreening'
     AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'tanuh_uat');
   ```

3. Start it again. A screening the job already checked, and nobody has changed since, is skipped, so reading a period twice does not send anything twice.

## 11. Things that lose waiting screenings or leave data behind

- **The metadata migrator's Avni-to-Bahmni clean-up** must not run on a database with Tanuh organisations. It resets every read position on the database to 1900, so Tanuh's next run would send every screening the organisation has ever had. It also deletes every waiting screening's record, so those screenings are never retried.
- **A crash in the middle of a photo download** can leave that photo in the service's temporary folder (`/tmp`). After a crash, delete the files named `tanuh-photo-*` there.
- **A bulk update that gives many screenings the same time** is read correctly but more slowly, because each such time is read on its own. Spread the times where the update allows, as Avni's own migrations do with `id * interval '1 millisecond'`.

## 12. Rolling back

A build from before the Tanuh module cannot load the integration list on the admin page while rows of type `tanuh` exist, because it does not know that type. Roll forward rather than back. If a rollback is unavoidable, expect that page to fail until the current build is back.

## 13. The scheduler threads

Twenty threads run every module's jobs on the service (`avni.int.scheduler.thread.pool.size`). A long Tanuh run holds one of them, so many long runs at once can delay other modules.

## 14. The data quality service

The data quality service (DQS, repository `data-quality-service`) checks each screening's data against Samvit's rulepack. It runs on its own server, reachable over the internet, one per environment: the staging service for Tanuh UAT, the production service for Tanuh production. Its own runbook is `deploy/aws/README.md` in that repository.

### What is sent

Each Oral Screening the main job reads (section 1) is posted as one record to `POST <dqs_api_url>/v1/<dqs_tenant>/records/oral_screening`. An edited screening is sent again under the same record id, and the service replaces its copy. The record carries the screening's uuid as `record_id`, the patient's uuid as `beneficiary_id`, the username of whoever created it as `worker_id`, the LGD code of the patient's village as `village_id`, the visit time, the device's entry time, the time this version reached Avni as `received_at`, the screening's answers, and counts of how the worker's and the on-device model's photo verdicts agree. The field mapping is in `tanuh/src/main/java/org/avni_integration_service/tanuh/dqs/OralScreeningRecord.java`.

Just before it, the job posts the screening's patient to `POST <dqs_api_url>/v1/<dqs_tenant>/records/registration`, so the rules that compare a screening with the person, such as a visit dated before the registration, find the registration there. The patient is read from Avni's `/api/subject/<uuid>` only to build this record; patients are not read on a schedule of their own. The record carries the patient's uuid as both `record_id` and `beneficiary_id`, the username of whoever registered them as `worker_id`, the village's LGD code, the registration date as `registered_on`, the screening's visit time, entry time and arrival in Avni as `visit_at`, `recorded_at` and `received_at` (the record is the patient as they stood at that screening), the registration's GPS fix, the age on the registration date, sex, date of birth, the full name and the `Full Address` answer. The service hashes the name and drops the address on arrival, keeping only whether one was given. The registration is sent with every screening and replaces the service's copy, so an edit to a patient reaches the service with that patient's next screening. The field mapping is in `tanuh/src/main/java/org/avni_integration_service/tanuh/dqs/RegistrationRecord.java`.

The service answers with the rule outcomes. The job reads only whether the screening could be scored and how many rules failed (section 1); the rest is logged when a screening fails QC. The registration's own outcomes are not read: it is sent only so that the screening's rules can use it.

The village's LGD code is a location attribute: a Numeric concept named by the `lgd_code` mapping row (`LGD code`), on the organisation's Location form, with a value on each village. The service finds the village, its taluk and its district by that code, against the LGD village layer it was published with. A patient whose village has no code is sent without a `village_id`; the service then cannot place the record, so the rule on GPS outside the assigned area is not evaluated and the record joins no district's peer group.

What is not sent: other visit types, a patient with no screening, the photographs themselves, and deletions. A screening deleted in Avni after it was sent stays in the service.

### Settings

- `dqs_api_url`: the service's address, for example `https://13.233.1.2` (the staging service answers on its Elastic IP with a certificate for it). It must be https, because the job user's sign-in token goes with every record; only an address on the integration service's own machine (`localhost`, `127.0.0.1`) may be plain http. With no value, or a value that is not https, every screening waits with `DataQualityCallFailed`.
- `dqs_tenant`: the tenant key in the service's URLs, `samvit`.
- `qc_fail_rules`: the rule ids, comma-separated, whose failure makes a screening `QC Fail`, for example `ST-001,SM-003,QC-030`. That is the default when the row is absent or empty: the checks sheet's blocking checks among the rules Samvit runs on an oral screening (no visit time or one in the future, no usable image, unusable images). A rule failed on the screening but not listed is logged as `Screening <id> passed QC; failed rules not in qc_fail_rules: [...]`. A failed QC is logged with the listed rules that failed it. Rules on the registration record never decide the verdict.

### Letting the job user in

The service checks each request's Cognito ID token, the same token the job user's Avni calls carry. The service's tenant authentication file (`tenants/samvit/auth/staging.yaml` or `prod.yaml` in its repository) must name:

- `issuer`: `https://cognito-idp.ap-south-1.amazonaws.com/<user pool id>`, with the pool id from `https://app.avniproject.org/idp-details` (`cognito.poolId`);
- `client_ids`: the `cognito.clientId` from the same page;
- `subjects`: the job user's Cognito `sub`. In the AWS console, Cognito, the pool, Users, the job user: "sub" under its attributes. It is not the username.

Until the file carries those values every screening waits with a 401. UAT's job user and production's are different users, so each environment's file names its own.

### Reading a data quality failure

A screening the service does not take waits with `DataQualityCallFailed` (section 8). The message names the cause:

- `DQS at <host> did not answer`: the service is down or unreachable from the integration service.
- `401`: the token was refused. Usually the authentication file names another pool or client; check `issuer` and `client_ids`.
- `403`: the token is good but the job user is not listed; add its `sub` to `subjects`.
- `404`: `dqs_api_url` or `dqs_tenant` is wrong, or the tenant's published rulepack does not carry `oral_screening` or `registration`. The message names the record type.
- `503`: the service could not fetch Cognito's signing keys.
- A 400: the request body was refused. That is a defect in this service; report it with the screening's uuid.
- `does not say how it was scored`: the service answered 200 with a body that is not its evaluation (a `scored` flag and an outcome for each rule), often a proxy's page in front of it. Nothing is written; the screening is sent again on the next retry.

A screening the service stores but cannot read (it answers 422, for a record that lacks an identity or carries a value it cannot parse) is not a failure: it is marked `QC Fail` whatever `qc_fail_rules` says, and the log names the violations.

## 15. Question and answer names

The job finds the Oral Screening's visit type, questions and answers by name, and writes the QC verdict under names too. The names are not in the code. They are rows in `mapping_metadata`, like the other integrations' mappings, one set per organisation, under the mapping group `Oral Screening` and the mapping type `Concept`:

| Integration system value | Avni value |
|---|---|
| the job's key, such as `qc_status` or `oral_image` | the visit type, question or answer name |

Each job reads them at the start of every run, so an edit applies from the next run with no restart.

**Setting the rows up.** The rows are kept in this repository as a bundle, `docs/tanuh-mapping-bundle`: the files `mappingGroups.json`, `mappingTypes.json` and `mappingMetadata.json` that the service's bundle import reads. Every group, type and row has a fixed uuid, so importing the bundle again updates the same rows, and the same bundle serves UAT and production. Import it once per organisation, after the setup script and before the restart, signed in as an admin user whose working integration system is that organisation:

```
cd docs/tanuh-mapping-bundle && zip /tmp/tanuh-mapping.zip mappingGroups.json mappingTypes.json mappingMetadata.json
curl -c /tmp/int-cookies -d 'email=<admin email>' --data-urlencode 'password=<password>' https://<integration service>/int/login
curl -b /tmp/int-cookies -F 'bundleZip=@/tmp/tanuh-mapping.zip' https://<integration service>/int/import
rm /tmp/int-cookies /tmp/tanuh-mapping.zip
```

A 200 means it was imported. Check the 36 rows on the admin app's mapping metadata screens.

Importing the repository's bundle again puts every row back to the repository's name, undoing edits made on the organisation.

**Changing a name.** Edit the row in the integration admin app's mapping metadata screens, working in the organisation's integration system. When Tanuh renames something for every organisation, change it in `docs/tanuh-mapping-bundle/mappingMetadata.json` too, so new organisations and the tests get the new name.

**Carrying UAT's edits to production.** Export UAT's bundle (`GET /int/export` while working in `tanuh_uat`), delete `integrationSystemConfigs.json` and `errorTypes.json` from the zip, and import the rest while working in `tanuh_prod`. Left in, the first would copy UAT's settings, including its Avni address, environment and job user, into production, and the second would add a second copy of each error type. The export leaves out each row's note (`about`); production's notes stay as they were.

- The visit type and question names must match Avni exactly. Answers are matched without regard to case.
- The legacy photo names must keep `%d` where the photo's number goes.
- To retire a row, void it. A voided row is not read.

**Rows the job cannot trust** fail every job of that organisation before any work. Every fault is named at once in the log and the error-reporting service: a missing key, a misspelt or doubled key, a blank name, or a legacy photo name without `%d`. Both checks go red. The other organisation is unaffected. Fix the rows, and the next run proceeds.

**A name that is valid but wrong** is not caught. The job reads a question that is never answered and sends nothing for it, or writes the verdict under a question the form does not have. Avni accepts a wrongly named coded answer without an error, since it resolves answers by name across the organisation. After a change, open one checked screening and one record in the data quality service and check the values arrived.

## 16. Upgrading an organisation set up by an earlier version

An organisation set up with the earlier script scored screenings itself and wrote the model's values back. To move it to this version:

1. Run the organisation's setup script again (section 2, step 3, with the data quality service's address in the copy). It adds `dqs_api_url`, `dqs_tenant` and the `DataQualityCallFailed` error type, and changes nothing else.
2. Set the stand-in's mode to one this version knows. `by_encounter` and `fixed` are read as `accept`, with a warning at every start.

   ```sql
   UPDATE integration_system_config SET value = 'accept'
   WHERE key = 'model_stub_mode'
     AND integration_system_id = (SELECT id FROM integration_system WHERE name = 'tanuh_uat');
   ```

   The rows `safety_sample_rate` and `model_stub_fixed_result` are no longer read and can stay.
3. Import the mapping bundle (section 15) and let the job user into the data quality service (section 14).
4. Restart. Screenings scored before the upgrade keep their model values; the job no longer writes or changes them.
