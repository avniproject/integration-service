# Deployment Runbook — JSS Bahmni ↔ Avni Integration

Step-by-step order for setting up the integration on an environment. Do the steps in order; each has a check.
Prerelease first; production follows the same steps (differences are marked **Prod**).

> The JSS work lives on branch `jss_ganiyari_dev` and is **never merged into `main`** (`main` is merged *into* this branch to stay current). Deploy from this branch.

---

## Who / where

| System | Prerelease | Production |
|---|---|---|
| Bahmni | https://jss-bahmni-prerelease.avniproject.org (`ssh jss-bahmni-prerelease`) | JSS's Bahmni (network access to be agreed with JSS IT) |
| Avni | https://prerelease.avniproject.org, org **JSSCP_UAT_Org** | https://app.avniproject.org, JSSCP production org |
| Integration service + DB (avni-int) | `int-prerelease` | `int-prod` |

---

## Step 1 — Bahmni: "Avni Subject UUID" person attribute (once per Bahmni instance)

Holds the Avni UUID of a linked patient. Prerelease: already exists (created 2026-04-23).

| Field | Value |
|---|---|
| Name | `avni_subject_uuid` |
| Description | Avni Subject UUID |
| Format | `java.lang.String` (Text) |
| Searchable | No |
| UUID | `ceddfccf-7913-4a63-b60e-d3114f867e4b` (keep the same everywhere) |

Run `JSS Bahmni integration/bahmni-config/avni-subject-uuid-attribute.sql` on the Bahmni DB (checks first, creates only if missing).
Do **not** add it to the registration screen config — only the integration writes it.

**Check:** the script's last query returns one row.

## Step 2 — Bahmni: "View in Avni" link on the patient dashboard

Follow `JSS Bahmni integration/bahmni-config/README.md`: append the `avniLink` directive to `customDisplayControl/js/customControl.js`, copy `views/avniLink.html`, add the `avniLink` section to `clinical/dashboard.json`. Back up the two edited files first.
**Prod:** `avniWebAppUrl` = `https://app.avniproject.org`; commit the same change to JSS's config repo (`jss-config`).

**Check:** hard refresh (Ctrl+Shift+R) a linked patient's dashboard → "Avni" section with "View this person in Avni".

## Step 3 — Avni: metadata bundle and users

1. **Prod only:** rebuild the bundle from **production** Bahmni data (skill `jss-bahmni-form-sync`, Phases 1–3: forms in use, multi-select, answers used — read-only queries on the production Bahmni DB).
2. Check the bundle against the org (skill lesson 39: no type changes, no name clashes).
3. Upload `JSS Bahmni integration/avni_org_setup/jss-avni-bundle_<org>.zip` (App Designer → Upload → Metadata Zip).
4. Create the integration user in the org (read individuals/encounters, create encounters).
5. Give doctors/testers who will click "View in Avni" an Avni web login in the org.
6. The "Individual ID Update" form's Bahmni ID field has the validation regex `^(GAN|SEM|SIV|BAM|EME)[0-9]+$`.

**Check:** open any individual → a "Bahmni - Diabetes Intake Template" visit is **not** offered under unplanned visits (eligibility rule); the forms exist in App Designer.

## Step 4 — Integration DB (avni-int): back up and clean up

1. Stop the integration service.
2. Back up avni-int: `pg_dump -Fc <avni-int db> > avni-int-before-jss-$(date +%F).dump`
3. **Prerelease only** (it ran the old trial migrations; production never did):
   `psql <avni-int db> -f integration-data/src/main/resources/db/onetime/JSS_ResetOldTrialMappings_prerelease.sql`
   It must run **before** the new code starts — it refuses to run if `V2_5_0` is already applied.

**Check:** the script prints how many old mapping rows and history rows it removed.

## Step 5 — Integration service: settings and deploy

Environment variables on the integration server (credentials never go into the repo):

| Variable | Value |
|---|---|
| `OPENMRS_BASE_URL` | Bahmni base URL |
| `OPENMRS_USER` / `OPENMRS_PASSWORD` | Bahmni API user for the integration |
| `BAHMNI_AVNI_API_URL` | `https://prerelease.avniproject.org:443` / **Prod** `https://app.avniproject.org:443` |
| `BAHMNI_AVNI_API_USER` / `BAHMNI_AVNI_API_PASSWORD` | Avni integration user (Step 3.4) |
| `BAHMNI_INT_APP_TASKS` | `JssIdUpdateLink,BahmniEncounter,BahmniLabResult,BahmniErrorRecords` *(link job: being built)* |
| `BAHMNI_SCHEDULE_CRON` | How often the jobs run, e.g. every 15 minutes |

Deploy branch `jss_ganiyari_dev`. On start, the integration service applies the JSS migrations automatically: `V2_5_0` (form/answer mappings and settings), `V2_5_1` (link-error types), `V2_5_2` (link job bookmark).

**Check:** run `JSS Bahmni integration/deploy/avni-int-checks.sql` (read-only) — migrations applied, ~2,900 mappings, settings, 4 error types, bookmark at 1900-01-01.

## Step 6 — First runs

| What to watch | Where |
|---|---|
| ID Updates being linked; bookmark moving forward | Check 6 in `avni-int-checks.sql`; service logs |
| Avni UUID written on Bahmni patients | "View in Avni" link appears on their dashboard |
| Bahmni visits arriving in Avni as "Bahmni - …" visits | Avni web app, linked individuals |
| ID problems for JSSCP | Metabase report (`reports/bahmni-link-errors.sql` on avni-int) |
| Sync errors for the Avni team | Check 7 in `avni-int-checks.sql`; integration admin app error log |

The first link run processes **every** existing ID Update and copies each linked person's past Bahmni visits — expect it to take a while.

## Rollback

1. Stop the integration service.
2. Restore avni-int from the Step 4 backup (`pg_restore`).
3. Deploy the previous build.
4. Bahmni dashboard link: restore the two backed-up config files. Avni metadata stays (harmless); unwanted synced visits can be voided via the Avni API (`DELETE /api/encounter/{id}`).
