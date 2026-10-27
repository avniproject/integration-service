# FVR (Field Visit Report): Avni to Salesforce sync plan

## What we're building

A new FVR form in Avni. When a Field POC submits it, the record is pushed to Salesforce, where the FVR code is generated. It works the same way as Activity sync today.

## Decisions

| Topic | Decision |
|---|---|
| Subject type | FVR is a new Avni subject type |
| Sync direction | Push only, Avni to SF. The FVR code is generated in SF and stays there |
| Edits | Every save is pushed again and updates the same SF record, matched on the Avni ID. That includes Admin edits after submission |
| Account Name | Handled the same as in the Activity form. Many FVRs can share one account |
| Edit lock | Field POCs can't edit after submitting; only Admins can. Enforced in Avni |

## Before we start (prerequisites)

| # | Item | Owner | Blocks |
|---|---|---|---|
| P1 | SF upsert API for FVR, matched on the Avni ID, with the payload contract shared: field names, picklist values, date format | Goonj SF team | Tasks 2–6 |
| P2 | SF confirms the FVR code stays the same when a record is updated | Goonj SF team | Task 6 |
| P3 | FVR subject type and form created in Avni: 24 fields, validations, Account Name as in Activity, up to 3 photos | Avni implementation | Tasks 2–6 |
| P4 | Edit lock in Avni: Field POCs can't edit after submit, Admins can | Avni implementation | Task 6 |

## Tasks

| # | Task | Depends on | Estimate |
|---|---|---|---|
| 1 | Check that the Avni webapp applies the edit lock, not just the mobile app | P4 | 0.1 day |
| 2 | Integration code: FVR entity, field mapping and worker, copied from Activity | P1, P3 | 0.25 day |
| 3 | Database setup: sync tracking entry and error types for FVR | 2 | 0.1 day |
| 4 | Add FVR to the scheduled sync and the manual re-sync job | 2 | 0.05 day |
| 5 | Unit test for the field mapping (multi-select, photos, dates, empty fields) | 2 | 0.1 day |
| 6 | End-to-end test on staging (see checklist below) | 1–5, P2 | 0.5 day |
| 7 | Production rollout and first-sync check | 6 | 0.25 day |
| | **Total (Avni integration side)** | | **about 1.25 days** |

The SF API work and form creation are not included.

## Staging test checklist (task 6)

- [ ] A submitted FVR appears in SF, linked to the right Account, with an FVR code
- [ ] All 24 fields arrive correctly: multi-selects, Yes/No fields, date of visit, photos as links
- [ ] An Admin edit updates the same SF record and the FVR code doesn't change
- [ ] A Field POC can't edit after submitting, in both the mobile app and the webapp
- [ ] Several FVRs for the same account are all created in SF
- [ ] Draft or deleted FVRs are not sent
- [ ] An SF error shows up in the error list and can be re-synced

## Risk

If SF's field names or picklist values don't match the form, testing will take longer. Getting the payload contract in P1 early avoids that.
