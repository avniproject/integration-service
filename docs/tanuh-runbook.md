# Tanuh high-risk model: runbook

How to run the Tanuh module of the integration service for Tanuh's UAT and production organisations. Built in integration-service#130, #131 and #132, for avni-product#1910.

## 1. What runs

Each Tanuh organisation has two jobs on the integration service.

- **The main job**, every 15 minutes, reads the Oral Screenings that are new or edited since its last run. For each one it:
  1. reads the screening's patient from Avni and sends the patient's registration, then the screening, to the data quality service (section 14), and reads the screening's verdict;
  2. writes the QC verdict back onto the screening: `QC Pass` or `QC Fail`, and the time of the check. If a field worker's edit reached Avni while the service was answering, nothing is written and the edited version is checked on the next run;
  3. if the screening passed QC and has photos, sends its uuid and its photos' S3 addresses to Tanuh's model service (section 17), without waiting for a result, and writes `OCSHR Call Made` `Yes` on it once the service answers for the case, whether it takes it, refuses it, or already holds it with other photos: the call was made (decided 17 Nov 2026). A call that fails or gets no answer writes nothing, and the same request goes again. A screening that failed QC, has no photo, or has a photo outside Avni's S3 storage is not sent; it gets `No` in the same write as its verdict. A screening with more than 13 photos sends its first 13 (section 17). The model service scores on its own time and writes its result to Avni itself, through Avni's API, as `high_risk_avni_user`; that write is not this job's concern.
- **The data quality check is off for now** (`dqs_enabled` `false`, decided 17 Nov 2026). Steps 1 and 2 are skipped: the data quality service is not asked and no verdict is written. Every screening with a photo Tanuh's model can read is sent while `OCSHR Call Made` is empty, and `OCSHR Call Made` is all the job writes: `Yes` once Tanuh's service answers for the screening, `No` when it has no photo the model can read. Switching the check on is in section 14.
- **The retry job**, five minutes after each main run, tries again every screening that is waiting after a failure. A screening whose verdict was written but whose hand-over failed is only handed over again, if it is still marked `QC Pass`; any other is tried again from step 1.

Whether a screening passes QC is decided by this service, not by the data quality service: the data quality service returns each rule's outcome, and a screening fails QC when the service could not read it, or when a rule named in `qc_fail_rules` (section 14) failed it. Any other failed rule is logged and does not fail the screening, and neither does a rule that could not decide or did not apply. A screening that fails QC is not sent to the model service, and neither is one that passes without any photo, as there is nothing for the model to look at. Both get `OCSHR Call Made` `No`. A screening with a photo Avni will not sign, because it is outside the organisation's media storage, fails QC without the data quality service being asked, and gets `No` in the same write (section 17).

A screening goes to Tanuh's model service once at most. It is sent only while it has no answer, and once it is `Yes` or `No` it is never sent again, even after an edit: a `No` given for missing photos stays when photos are added later. The job keeps each answer in its own database as well as on the screening (section 18), because a phone's edit can clear `OCSHR Call Made`; an answer an edit cleared is written back on the next run.

While the data quality service or the model service is down, the screenings wait and none is lost; the red retry check is the sign. A failed or slow send writes nothing, so a screening is never recorded as not sent because the service was down.

## 2. Setting an organisation up

UAT goes first, then production, and both run on the production integration service (int-prod). Tanuh's model service accepts only the AWS role `avni_integration_service`, and only int-prod runs as that role (section 17). Production follows only once the epic's list under "Before production" holds.

1. Check the implementation team's configuration is live on the organisation: the QC questions the job writes while the data quality check is on (`QC status` with its answers `QC Pass` and `QC Fail`, and `QC check time`, or whatever names section 15 maps; not needed while it is off), the `OCSHR Call Made` question with its answers `Yes` and `No` (section 16), the job user in the Integration group only, and the visit type named exactly "Oral Screening".
2. Create the organisation's two health checks (section 4).
3. If the organisation's job also runs on another integration service, stop it there first (section 7): two services would check the same screenings as the same job user. UAT's ran on int-staging before 17 Nov 2026; check that one.
4. Copy the organisation's script, `docs/tanuh-uat-setup.sql` or `docs/tanuh-production-setup.sql`, to a folder outside this repository. Put the job user's name and password, the data quality service's address and the model service's values (section 17) into the copy, run the copy on that service's database just before the restart, then delete the copy. Never type the password into the file in the repository: one commit would publish it. The first run reads screenings changed after that moment. Running the script again changes nothing.
   - If review booking has already stopped on the organisation, start from the moment it stopped instead: the script's `TIMESTAMP` option, in UTC. On production this happens when the configuration promotion carries UAT's Oral Screening form after UAT's booking stop. A screening referred between then and the restart was never sent on, and a start at the restart would never read it. Ask the implementation team when the promotion landed.
5. Import the organisation's question and answer names from `docs/tanuh-mapping-bundle` (section 15). Without them every job fails.
6. While the data quality check is on, let the job user into the data quality service (section 14, "Letting the job user in"). While it is off, skip this step.
7. Restart the integration service. For production, see section 3. Both scripts set `int_env` to `prod`, which must match the service's `AVNI_INT_ENV`; on a mismatch the service skips the organisation and logs "Environment mismatch detected".
8. After the first run with a new screening, open it and confirm it carries a QC verdict (section 8). If the visit type has any other name on that organisation than its `oral_screening` mapping row, the job reads nothing and its check stays green, so this is the only sign.

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
- **A Bugsnag warning with both checks green:** a screening that needs a person. Each kind is its own group, with the organisation's integration system as its context: `tanuh:HELD_WITH_OTHER_PHOTOS`, a screening Tanuh already holds with other photos, which are the ones it scores, marked `Yes` (section 17); `tanuh:REFUSED`, one Tanuh's service refused and will not score, marked `Yes` because the call was made (section 17); `tanuh:PHOTO_OUTSIDE_STORAGE`, one with a photo Avni will not sign, now marked `QC Fail` or `No` (section 17). Neither check turns red for these, so Bugsnag is the only sign. The job never sends the first two again, so take each one's screening uuid to Tanuh's team. Several refusals or refused photos in a row usually mean a setup fault, such as photos in a bucket Tanuh does not read.
- A failure in one organisation leaves the other organisation's checks as they were.

## 6. Settings

Settings are rows in `integration_system_config` for the organisation's integration system. They are read when the service starts, so every change needs a restart.

- `high_risk_avni_user`: the Avni user Tanuh's model service writes its results as (`highrisk@tanuh_uat` on UAT). The job writes `OCSHR Call Made` after a hand-over only if that user or the job user changed the screening last, because Tanuh's result can land before the job's write. A screening that user changed last is not checked again. Set it before switching on: without it, each result Tanuh writes looks like a worker's edit, so the screening goes to the data quality service again. It is not sent to the model again (section 18).
- `ocshr_api_url`, `ocshr_credential_config`, `ocshr_service_account` and `ocshr_audience`: section 17.
- `dqs_enabled`, `dqs_api_url`, `dqs_tenant` and `qc_fail_rules`: section 14.

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
- `HighRiskModelCallFailed`: the screening's verdict is written, but its hand-over failed: the model service did not answer, answered with an error, or refused the sign-in. One cause stops the send before any call: Avni failing to sign a photo for any reason other than refusing it, such as Avni being down. A photo Avni refuses with a 403 is not a wait: the screening is marked `No` (section 17). Nothing was written to `OCSHR Call Made`. The retry job hands it over again on every run, every 15 minutes, without checking it a second time. A field worker's edit in the meantime makes it a new version, which is checked in full. A 401 after one token renewal, a 403 and a 422 about the request's format are retried the same way on purpose: each means a setup or contract fault on one side, the red retry check is the signal, and nothing is lost while it is fixed.
  - If Tanuh's team confirms they hold the case, write `OCSHR Call Made` `Yes` on the screening in Avni and delete the record. The job's next check of the screening takes that `Yes` into its own record (section 18), so the screening is not sent again.
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
3. If it cannot be fixed, tell Tanuh's team which screening it is and why it waits. With `DataQualityCallFailed` or `ScreeningProcessingFailed` it has no QC verdict; with `HighRiskModelCallFailed` it is marked `QC Pass` but was not sent to the model service. Either way it needs looking at by hand.
4. Then stop retrying it. On the service's error records page, switch on "processing disabled" for its record, or run the query below. The retry job skips it from its next run and the check turns green. While the record exists, the main job keeps leaving the screening alone, even after a worker edits it. Switching the setting off puts it back in the retries.

   A disabled `HighRiskModelCallFailed` record also leaves the job's record of the send with no answer (section 18). Switch it back on once the cause is fixed, so the retry settles it; or, if Tanuh's team confirm they hold the case, write `Yes` on the screening as in section 8.

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
- **An edit made on a phone that had not received the job's answers.** When a phone saves an edit, Avni replaces every answer on the screening with the phone's copy, so the QC verdict, `OCSHR Call Made` and any result Tanuh's service wrote are cleared. The job checks the edit as a new version and writes its verdict, but it does not send the screening again: it writes `OCSHR Call Made` back from its own record (section 18). A send whose answer was lost is repeated with its first photos, whatever the edit changed. A result Tanuh wrote is not put back by this job, and Tanuh's guide does not say whether its service writes one again, so a result an edit cleared may stay cleared until Tanuh's team writes it again.
- **A bulk update that gives many screenings the same time** is read correctly but more slowly, because each such time is read on its own. Spread the times where the update allows, as Avni's own migrations do with `id * interval '1 millisecond'`.

## 12. Rolling back

A build from before the Tanuh module cannot load the integration list on the admin page while rows of type `tanuh` exist, because it does not know that type. Roll forward rather than back. If a rollback is unavoidable, expect that page to fail until the current build is back.

## 13. The scheduler threads

Twenty threads run every module's jobs on the service (`avni.int.scheduler.thread.pool.size`). A long Tanuh run holds one of them, so many long runs at once can delay other modules.

## 14. The data quality service

The data quality service (DQS, repository `data-quality-service`) checks each screening's data against Samvit's rulepack. It runs on its own server, reachable over the internet, one per environment: the staging service for Tanuh UAT, the production service for Tanuh production. Its own runbook is `deploy/aws/README.md` in that repository.

**Off for now.** Both setup scripts set `dqs_enabled` to `false` (decided 17 Nov 2026), so the job does not call this service and writes no QC verdict (section 1). Only `false` turns the check off; with no row, or any other value, it is on. To switch it on: set `dqs_api_url`, let the job user in (below), add the QC questions to the organisation (section 2, step 1), set `dqs_enabled` to `true`, and restart. Screenings already marked keep their `OCSHR Call Made` and are not sent again.

### What is sent

Each Oral Screening the main job reads (section 1) is posted as one record to `POST <dqs_api_url>/v1/<dqs_tenant>/records/oral_screening`. An edited screening is sent again under the same record id, and the service replaces its copy. The record carries the screening's uuid as `record_id`, the patient's uuid as `beneficiary_id`, the username of whoever created it as `worker_id`, the LGD code of the patient's village as `village_id`, the visit time, the device's entry time, the time this version reached Avni as `received_at`, the screening's answers, counts of how the worker's and the on-device model's photo verdicts agree, and `image_urls`: a link to each photo, in the form's order, signed by Avni just before each post, after the job user's sign-in, so a record sent again after a refused token carries fresh links. A signed link lasts two minutes, so the service must fetch the photos while it handles the record; a link it keeps is useless after that. The data quality service does not yet read `image_urls`: until it does, it answers each record with a warning that the field is not declared and was ignored, which does not affect the verdict. A photo Avni refuses to sign, because it is outside the organisation's media storage, fails QC without the screening being sent, and gets `OCSHR Call Made` `No` (section 17). Any other failure to sign fails the screening as `ScreeningProcessingFailed` (section 8), and the retry job sends it again. The field mapping is in `tanuh/src/main/java/org/avni_integration_service/tanuh/dqs/OralScreeningRecord.java`.

Just before it, the job posts the screening's patient to `POST <dqs_api_url>/v1/<dqs_tenant>/records/registration`, so the rules that compare a screening with the person, such as a visit dated before the registration, find the registration there. The patient is read from Avni's `/api/subject/<uuid>` only to build this record; patients are not read on a schedule of their own. The record carries the patient's uuid as both `record_id` and `beneficiary_id`, the username of whoever registered them as `worker_id`, the village's LGD code, the registration date as `registered_on`, the screening's visit time, entry time and arrival in Avni as `visit_at`, `recorded_at` and `received_at` (the record is the patient as they stood at that screening), the registration's GPS fix, the age on the registration date, sex, date of birth, the full name and the `Full Address` answer. The service hashes the name and drops the address on arrival, keeping only whether one was given. The registration is sent with every screening and replaces the service's copy, so an edit to a patient reaches the service with that patient's next screening. The field mapping is in `tanuh/src/main/java/org/avni_integration_service/tanuh/dqs/RegistrationRecord.java`.

The service answers with the rule outcomes. The job reads only whether the screening could be scored and how many rules failed (section 1); the rest is logged when a screening fails QC. The registration's own outcomes are not read: it is sent only so that the screening's rules can use it.

The village's LGD code is a location attribute: a Numeric concept named by the `lgd_code` mapping row (`LGD code`), on the organisation's Location form, with a value on each village. The service finds the village, its taluk and its district by that code, against the LGD village layer it was published with. A patient whose village has no code is sent without a `village_id`; the service then cannot place the record, so the rule on GPS outside the assigned area is not evaluated and the record joins no district's peer group.

What is not sent: other visit types, a patient with no screening, the photographs themselves (only links to them), and deletions. A screening deleted in Avni after it was sent stays in the service.

### Settings

- `dqs_enabled`: `false` skips the data quality check (above); with no row, or any other value, the check runs.
- `dqs_api_url`: the service's address, for example `https://13.233.1.2` (the staging service answers on its Elastic IP with a certificate for it). It must be https, because the job user's sign-in token goes with every record; only an address on the integration service's own machine (`localhost`, `127.0.0.1`) may be plain http. With no value, or a value that is not https, every screening waits with `DataQualityCallFailed`.
- `dqs_tenant`: the tenant key in the service's URLs, `samvit`.
- `qc_fail_rules`: the rule ids, comma-separated, whose failure makes a screening `QC Fail`, for example `ST-001,QC-030`. The setup scripts set that list on each organisation, so the verdict never rests on a value built into the code; an organisation set up before the row existed gets it by running its setup script again, which adds only missing rows, and a restart. Running the script again does not change a row that exists: to change the list on an organisation already set up, update its `qc_fail_rules` row and restart. The same list is also the default when the row is absent, empty or holds no rule id (such as ` ` or `,`): the checks sheet's blocking checks among the rules Samvit runs on an oral screening (no visit time or one in the future, and unusable images). SM-003, no usable image, was in the list until Samvit stopped running it; QC-030 fails every screening it would have failed. A rule failed on the screening but not listed is logged as `Screening <id> passed QC; failed rules not in qc_fail_rules: [...]`. A failed QC is logged with the listed rules that failed it. Rules on the registration record never decide the verdict. Ids are matched exactly, case included. A listed rule the service gave no outcome for on a screening is logged as a warning, `Screening <id>: qc_fail_rules lists [...], which the data quality service gave no outcome for`: the id is misspelt, or the tenant's published rules no longer run it, and either way it can never fail a screening. Correct the row.

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
- `ran no rule on it`: the service answered 200 with no rule outcome at all, which would pass every screening. The tenant's published rulepack has no rules for that record type; publish one that does. Nothing is written; the screening is sent again on the next retry.

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

A 200 means it was imported. Check the 37 rows on the admin app's mapping metadata screens. The newest is `ocshr_call_made`, the `OCSHR Call Made` question the job writes after each hand-over.

The bundle's one mapping type, `Concept`, is voided on purpose. When the service starts, the Bahmni module looks up the mapping type named `Concept` across every integration, so a second live one stops the whole service from starting, for every organisation on that server. On 18 Nov 2026 a live Tanuh `Concept` type kept int-prerelease restarting until it was voided, and int-prod carried the same until it was voided before any restart. The Tanuh job reads its rows by the type's name, voided or not. A server whose Tanuh `Concept` type is live needs `UPDATE mapping_type SET is_voided = true WHERE name = 'Concept' AND integration_system_id = (SELECT id FROM integration_system WHERE name = '<organisation>');` before its next restart.

Importing the repository's bundle again puts every row back to the repository's name, undoing edits made on the organisation.

**Changing a name.** Edit the row in the integration admin app's mapping metadata screens, working in the organisation's integration system. When Tanuh renames something for every organisation, change it in `docs/tanuh-mapping-bundle/mappingMetadata.json` too, so new organisations and the tests get the new name.

**Carrying UAT's edits to production.** Export UAT's bundle (`GET /int/export` while working in `tanuh_uat`), delete `integrationSystemConfigs.json` and `errorTypes.json` from the zip, and import the rest while working in `tanuh_prod`. Left in, the first would copy UAT's settings, including its Avni address, environment and job user, into production, and the second would add a second copy of each error type. The export leaves out each row's note (`about`); production's notes stay as they were.

- The visit type and question names must match Avni exactly. Answers are matched without regard to case.
- The legacy photo names must keep `%d` where the photo's number goes.
- To retire a row, void it. A voided row is not read.

**Rows the job cannot trust** fail every job of that organisation before any work. Every fault is named at once in the log and the error-reporting service: a missing key, a misspelt or doubled key, a blank name, or a legacy photo name without `%d`. Both checks go red. The other organisation is unaffected. Fix the rows, and the next run proceeds.

**A name that is valid but wrong** is not caught. The job reads a question that is never answered and sends nothing for it, or writes the verdict under a question the form does not have. Avni accepts a wrongly named coded answer without an error, since it resolves answers by name across the organisation. After a change, open one checked screening and one record in the data quality service and check the values arrived.

## 16. Upgrading an organisation set up by an earlier version

This covers an organisation set up before the data quality service, which scored screenings itself, and one running the stand-in model. To move it to this version:

1. Check the organisation has the `OCSHR Call Made` question, with its answers `Yes` and `No`, on the Oral Screening form. UAT has it. Without it a `QC Pass` verdict still writes, but a `QC Fail` verdict, which now carries `No`, fails with a 500, and a passing screening is posted to Tanuh again on every retry.
2. Before switching on, edit on a phone one screening that holds `OCSHR Call Made`, sync it, and confirm the value is still there. A question hidden by a form rule is cleared by the phone on every edit, and a sent screening would then look unsent.
3. Run the organisation's setup script again (section 2, step 3, with the data quality service's address and the model service's values in the copy). It adds the rows that are missing and changes nothing else. The rows `model_stub_mode`, `safety_sample_rate` and `model_stub_fixed_result` are no longer read and can stay.
4. Deploy, and import the mapping bundle (section 15) straight after. The new build fails every job of the organisation until the `ocshr_call_made` row is there, and the old build fails with it, as "not a key the job uses". So the setup script goes before the deploy, the bundle right after it, and one red run between them is expected.
5. Let the job user into the data quality service (section 14) if it is not already, and restart.
6. The deploy adds the table `tanuh_ocshr_case` (migration `V2_4_10`), empty. Nothing needs doing to it.
7. Screenings checked before this version have no `OCSHR Call Made`. Those marked `QC Pass` are sent after a worker's edit, or once the read position is moved back before them (section 10). Screenings scored before the data quality service keep their model values; the job no longer writes or changes them.

## 17. Tanuh's model service

Tanuh's guide to the service is attached to their Jira story PE-248.

**What is sent.** For each screening that passes QC with photos, `POST <ocshr_api_url>/v1/cases` with `{"case_id": "<screening uuid>", "images": [{"uri": "s3://<bucket>/<key>"}, ...]}`. The photos go in the form's order, each once, with the old flat photo slots read when the groups are empty. Tanuh reads the photos from S3 with a role of their own (`ocshr-image-reader-uat` for UAT, and one each for staging and production), so nothing is downloaded here.

- Before the send, Avni signs each photo's address and the signed link is thrown away. Avni signs only addresses in the organisation's own media storage, and refuses any other with a 403 whose message says the user is "not authorized to access" the address. A screening with such a photo is not sent: it gets `No`, together with `QC Fail` when the refusal comes at the quality check, and is reported to Bugsnag as `tanuh:PHOTO_OUTSIDE_STORAGE` (section 5). Only that refusal counts. A 403 without it, such as one from the firewall in front of Avni, or Avni's own on a server set to hide exception messages, is a failure like any other: the screening waits, as `ScreeningProcessingFailed` at the quality check and as `HighRiskModelCallFailed` at the hand-over.
- Avni's check reads only the first folder of a photo's address, so the job itself takes an address with a `.`, `..` or empty step, or one still holding an escape or a backslash once decoded, as outside storage. Such a screening gets `No` in its verdict's write, as one with a photo outside S3 does.
- Tanuh's service takes 1 to 13 photos per case. A screening with more sends its first 13, in the form's order (decided 17 Nov 2026), and the job logs a warning. A case cannot change once sent: a resend with other photos gets a 409. So if Tanuh raises the limit, screenings already sent keep their 13; raise `MAX_IMAGES` in `OralScreeningWorker` for the ones sent after.

**How the job signs in.** The service accepts only the AWS role `avni_integration_service` in account 118388513628, which int-prod runs as.

1. The job exchanges that role for a Google token, using Tanuh's credential file (`ocshr_credential_config`).
2. It acts as `ocshr_service_account`, default `sa-ocshr-avni@proj-model-hub.iam.gserviceaccount.com`, and asks for an ID token for `ocshr_audience`, default `medgemma-intake`, with its email in it.
3. The token is reused until it expires, and renewed once after a 401.

- The Google library takes AWS credentials from the `AWS_*` environment variables first, then the instance metadata service, and has no profile or container credentials. So the integration service's process must not carry other AWS keys. On 17 Nov 2026 it carried none.
- int-prod requires IMDSv2. Tanuh's credential file names `imdsv2_session_token_url`, which the library uses.
- The file is checked before use. Every sign-in step must go to Google's own addresses, AWS credentials must come from the metadata address, and a service account impersonation in the file, as Google's tool writes it, must name `ocshr_service_account` at Google's IAM credentials address. Anything else is refused, and the screening waits with `HighRiskModelCallFailed`.

**What each answer writes.** `OCSHR Call Made` says whether the call was made (decided 17 Nov 2026): `Yes` once the service answers for the case, `No` when the job does not call. The job never waits for the score.

| The service answers | `OCSHR Call Made` | The screening |
|---|---|---|
| 200 or 202 naming this case | `Yes` | done |
| 422 `INVALID_MANIFEST` | `Yes` | done, though Tanuh will not score it; reported to Bugsnag as `tanuh:REFUSED` (section 5) |
| 409 `CASE_EXISTS_WITH_DIFFERENT_MANIFEST` | `Yes` | done; Tanuh scores the photos it already holds; reported to Bugsnag as `tanuh:HELD_WITH_OTHER_PHOTOS` (section 5) |
| 401 | nothing | the token is renewed once and the call repeated |
| anything else, or no answer | nothing | waits with `HighRiskModelCallFailed` (section 8), and the same request goes again |

The job only ever repeats a screening's first send (section 18), so a 409 means something else sent the same screening, such as the organisation also set up on int-staging.

A redirect is never followed, so the ID token goes only to `ocshr_api_url`. It counts as an answer the job does not know.

**Checking the sign-in by hand.** Tanuh's guide gives a `gcloud` check to run on a machine with the role. A 404 `CASE_NOT_FOUND` for the case `does-not-exist` proves the sign-in works. On 17 Nov 2026 the same two Google calls, made from int-prod by a script with no dependencies, returned that answer.

## 18. The job's record of sends

The table `tanuh_ocshr_case` in the integration service's database holds one row per screening the job sent to Tanuh's model service or marked not sent, per organisation. A phone's edit can clear `OCSHR Call Made` on the screening, so this row, not the screening, decides whether a screening is sent.

- `image_uris`: the photos of the first send, kept before the send is made. A send whose answer is lost is repeated with exactly these, so Tanuh answers it as the same case and an edit's photos never reach the model. Empty for a screening marked not sent without a send.
- `call_made`: `YES` once Tanuh's service answered for the screening, whether it took it, refused it or holds it with other photos; `NO` when it is not sent. Empty while the first send's answer is not known; the retry job settles it, and so does an answer a person writes on the screening (section 8).

```sql
SELECT c.screening_uuid, c.call_made, c.image_uris, c.created_date_time
FROM tanuh_ocshr_case c
WHERE c.integration_system_id = (SELECT id FROM integration_system WHERE name = 'tanuh_uat')
ORDER BY c.created_date_time DESC;
```

Delete a row only to send a screening again on purpose, such as when Tanuh's team asks for a refused case again. Remove `OCSHR Call Made` from the screening in Avni as well, as the job sends only while it is empty. The next run then decides afresh from the screening as it is.
