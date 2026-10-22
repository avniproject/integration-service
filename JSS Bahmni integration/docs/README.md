# JSS Ganiyari — Bahmni ↔ Avni Integration

Links JSSCP's field app (Avni) with JSS Ganiyari's hospital system (Bahmni): a person linked through the Avni "Individual ID Update" form gets their hospital visits, lab results and prescriptions copied into Avni, and the hospital gets a link back to their Avni record.

Branch: `jss_ganiyari_dev` · Module: `bahmni/`

---

## Read first

| Doc | What it covers |
|---|---|
| [01-sync-design.md](01-sync-design.md) | **Start here.** What syncs, which IDs link records, the three flows, which jobs run, environments, open questions |
| [02-final-form-status.md](02-final-form-status.md) | Every form on JSS's final list and whether it is built, covered elsewhere, or not built (and why) |
| [03-encounter-sync-code-walkthrough.md](03-encounter-sync-code-walkthrough.md) | Code walk-through of a Bahmni visit becoming Avni visits |
| [04-integration-worker-reference.md](04-integration-worker-reference.md) | All workers in the bahmni module and what each does |
| [05-avni-uuid-bahmni-attribute-sync.md](05-avni-uuid-bahmni-attribute-sync.md) | Writing the Avni ID onto the Bahmni patient ("View on Avni") |
| [06-bahmni-patient-creation.md](06-bahmni-patient-creation.md) | **Planned, after go-live:** creating Bahmni patients for people with no Bahmni ID — gaps, cluster → clinic table, questions for JSS |
| [07-deployment-runbook.md](07-deployment-runbook.md) | **Deploying** to prerelease / production: Bahmni attribute + dashboard link, Avni bundle and users, avni-int backup/clean-up, settings, checks, rollback |

Final form list from JSS: [Google Sheet](https://docs.google.com/spreadsheets/d/1ZRTp0mB2xPG5vlQMetj6Tp35jS9pQS2kBvV1I1tS7f0/edit?gid=2110362113#gid=2110362113)

---

## How to do things

All setup, rebuild, add-a-form and deployment steps live in the Claude Code skill **`jss-bahmni-form-sync`** (`.claude/skills/jss-bahmni-form-sync/SKILL.md`) — including 39 lessons from earlier mistakes. Run it with `/jss-bahmni-form-sync`, or read the file as a checklist.

| Task | Skill section |
|---|---|
| Connect a new JSSCP Avni org for the first time | Phase S |
| Rebuild forms + mappings from the final list | Phases 0–3 |
| Upload to Avni, deploy, verify | Phases 4–5 |
| JSS asks for one more form | "Adding a new form later" |

---

## The working set

| Path | What it is | Edit by hand? |
|---|---|---|
| `JSS Bahmni integration/final_forms.csv` | The final form list with a status per form — **input to the builder** | ✅ Yes |
| `JSS Bahmni integration/CSV dumps/<Source>/` | Bahmni concept exports (`concepts.csv`, `concept_sets.csv`) — source of every form, question and answer | Only by re-exporting from Bahmni |
| `JSS Bahmni integration/CSV dumps/multiselect_concepts.csv` | Questions that allow several answers (Bahmni config + Bahmni data) | ✅ Yes |
| `JSS Bahmni integration/CSV dumps/bahmni_db_answers.csv` | Answers read from the Bahmni DB (defined + used in real visits) — fills gaps in the exports | Only by re-running the Bahmni query |
| `JSS Bahmni integration/avni_org_setup/bundle/` | Template for the one-time Avni setup ("Bahmni Patient Registration") | Rarely |
| `JSS Bahmni integration/avni_org_setup/jss-avni-bundle_<org>.zip` + `_report.md` | **Generated** Avni Metadata Zip to upload, and its report | ❌ Regenerate |
| `JSS Bahmni integration/docs/` | These docs | ✅ Yes |
| `JSS Bahmni integration/bahmni-config/` | "View in Avni" link on the Bahmni patient dashboard (custom display control) - files to add to JSS's Bahmni config, see its README | ✅ Yes |
| `JSS Bahmni integration/reports/bahmni-link-errors.sql` | Metabase query on avni-int: people whose Bahmni ID could not be linked, for JSSCP to fix | ✅ Yes |
| `scripts/build_jss_avni_bundle.py` | Builds the zip, the mapping migration, the report and `02-final-form-status.md` from the files above | — |
| `scripts/build_jss_org_setup_bundle.py` | Builds only the one-time setup zip (normally included in the full zip) | — |
| `integration-data/.../db/migration/V2_5_0__JSSBahmniFormMappings.sql` | **Generated** integration mappings. Never edit once it has run anywhere — add-ons get a new version | ❌ Never |
| `integration-data/.../db/onetime/JSS_ResetOldTrialMappings_prerelease.sql` | One-time clean-up of the old trial mappings on prerelease. Run by hand, before V2_5_0 | — |
| `bahmni/src/test/resources/bahmni-secret.properties.example` | Copy to `bahmni-secret.properties` (git-ignored) to run the live-server tests | — |
| `scripts/aws/` | Bahmni prerelease server (AWS) setup and access | — |
| `JSS Bahmni integration/JSS Bahmni Avni integration Project Tracker - Deliverable Tracker.csv` | Project tracker | ✅ Yes |

Everything else that used to be here (old Avni → Bahmni form sync, ANC/Diabetes mappings, trial bundles, Feb 2026 status reports) was removed in October 2026; it remains in git history (commit `42e1cc07`).
