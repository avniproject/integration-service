# Demand registration from Avni — summary

**In one line:** A Data POC creates the Demand in Avni, edits keep syncing to Salesforce until it's
assigned to a Processing Center, and from that point Salesforce owns it.

For now: **Data POC only, Avni webapp only** — field teams come later. A Data POC creates only for
their own account/geography, but can view all.

## The journey, at a glance

Legend: **[EXISTS]** = works today. **[NEW]** = Goonj must build. **[PENDING]** = approach not
decided.

```
  0. Dispatch Address must already be in Avni        [NEW — its own Avni subject, same
     before a Data POC can pick one for a Demand       linking pattern as Demand. Location
              │                                         = the District of choice, an
              │                                         existing entry in Avni's own
              │                                         hierarchy — no new location ever
              │                                         needs to be created. Still open:
              │                                         how it syncs in (question 4).]
              ▼
  1. Data POC fills in and submits the Demand form
              │        (Avni webapp)
              ▼
       integration service asks Avni: "anything new or edited?"
       [EXISTS — Avni's own API, already used for other record types]
              │
              ▼
  2. Sent to Salesforce for the first time
              │       (carries the Avni source ID, which
              │        SF must store mandatorily)
              │  ──►  calls Salesforce's "receive a Demand" API
              │       [NEW — Goonj has to build this; doesn't exist today]
              ▼
  3. Salesforce creates the record, checks for duplicates,
     generates the Demand Code
              │
              ▼
  4. Demand Code appears in Avni — via the pull sync that already
     exists today, on its own schedule, matching by Avni source ID
     if present, else Salesforce's own Demand ID
              │
              ▼
  5. Data POC can keep editing the Demand in Avni
              │
              │  ──►  same "receive a Demand" API, called again on every edit —
              │       Salesforce matches it to the same Demand, never creates a
              │       second one [NEW API, same one as step 2]
              │
              │       (this keeps happening until the Demand is assigned)
              ▼
  6. Goonj's team (MMT) assigns the Demand
     to a Processing Center, in Salesforce
              │
              ▼
  7. Editing is locked everywhere, all at once:
       - Avni's form refuses further edits                    [NEW]
       - the integration service stops calling Salesforce     [NEW]
       - Salesforce's own API also refuses a late call         [NEW — same API as step 2,
                                                                  refusing instead of accepting]
              │
              ▼
  8. Goonj's Sanjha team validates the Demand,
     entirely inside Salesforce
              │
              ▼
       integration service asks Salesforce: "any status changes?"
       [EXISTS — the same API that's live and working today]
              │
              ▼
  9. Status updates (Assigned → Validated → Post-Validated)
     flow back down and show up on the Avni dashboard automatically
```

## Three things worth knowing

- **Salesforce's new endpoint must be an "upsert"**, keyed on the Avni record's ID — makes repeat
  pushes safe, prevents duplicate codes.
- **Duplicate checking happens in Salesforce**, not Avni.
- **Dispatch Address will be its own Avni subject**, located by the District of choice — an
  existing Avni location, so no location-creation question to solve (step 0).

## Who's building what

| Goonj's Salesforce team | The Avni team |
|---|---|
| Build the new "receive a Demand" endpoint (step 2) — this doesn't exist today | Create the Dispatch Address subject type, then build the Demand form on top of it, including the Dispatch Address selection (step 1) |
| Make it recognise repeat edits as the same Demand, not new ones (steps 3, 5) | Update the existing pull sync to match on the Avni source ID first (step 4); build the "keep editing until assigned" sync (step 5) |
| Generate the Demand Code (step 3), and run duplicate checking | Build the three-layer lock that fires the moment a Demand is assigned (step 7) |
| Confirm the missing picklists — Disaster Type, Target Community, Kit Type sub-types | Handle errors so a failed send retries instead of getting lost |
| Make Salesforce reject a late edit after assignment, as a backstop (step 7) | Decide, with Goonj, how Dispatch Address gets into Avni (step 0) |
| Weigh in on how Dispatch Address should sync | Keep the account/geography access rules working |
| Deploy their endpoint first, before we deploy anything | Deploy second, after Salesforce; open registration last |

Nothing works end to end until Goonj's endpoint exists — agree on this first.

## Order of work

1. **Agree** — answer the open questions below
2. **Build** — Goonj's endpoint, our form, our integration code; these can run in parallel
3. **Test** — against a stub first, then a real Salesforce staging org, then end to end
4. **Deploy** — Salesforce first, integration second, wait 1–2 days, open registration last
5. **Watch** — confirm the first real Demands complete the full round trip

## Open questions — what we need answered

| # | Question | Owner | Why it matters |
|---|---|---|---|
| 1 | **Who is building the Salesforce upsert endpoint, and by when?** | Goonj / SF | 🚩 Blocker — doesn't exist yet, longest lead time |
| 2 | **What are the final picklists?** Disaster Type has no options; Target Community is partial; only 3 of 14 Kit Types (CFW, Marriage Kits, Vaapsi) have documented sub-types | Goonj | 🚩 Blocker — form and field mapping both depend on these |
| 3 | **Who can void a Demand, and when?** Duplicates can be voided; deletion is barred once assigned | Both | Unclear if this is a Data POC action, Goonj-internal, or both |
| 4 | As raised in the notes: *"Do we build an Integration workflow for Dispatch Address sync as well, Or if this doesnt change regularly, we can just do a bulk upload like we do for Locations?"* | Goonj / SF | Decides the sync build; no longer blocked on the location question |

**Already settled:**
- The push's response is never depended on. What links an Avni-originated Demand to its Salesforce
  record is Salesforce storing the Avni source ID it was sent — the existing pull sync finds it
  later, matching by that same ID, never creating a duplicate.
- Edit lock triggers on **assignment to a Processing Center**, not approval — enforced at three
  layers: Avni's form, this service, Salesforce's endpoint.
- Dispatch Address's location is the **District of choice** — an existing Avni location. No new
  location ever needs to be created, so the earlier location-creation blocker no longer applies.

## Rough estimate

| Work | Person-days |
|---|---|
| Analysis and solutioning | ~1 |
| Avni form & config — Demand | ~1 |
| Avni form & config — Dispatch Address | ~1 |
| Integration service — Demand | ~2 |
| Integration service — Dispatch Address | ~2 |
| UAT | ~2 |
| Deployment | ~1 |
| **Total** | **~10** |

## Where the detail lives

Interested in the tech detail? See `goonj-demand-avni-registration-tech.md`.
