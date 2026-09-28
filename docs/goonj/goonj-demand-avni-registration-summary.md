# Demand registration from Avni — summary

**In one line:** A Data POC creates the Demand in Avni, edits keep syncing to Salesforce until it's
assigned to a Processing Center, and from that point Salesforce owns it.

For now: **Data POC only, Avni webapp only** — field teams come later. A Data POC creates only for
their own account/geography, but can view all.

## The journey, at a glance

Legend: **[EXISTS]** = works today. **[NEW]** = Goonj must build. **[PENDING]** = approach not
decided.

```
  0. Dispatch Address must already be in Avni        [Approach settled: it will be its
     before a Data POC can pick one for a Demand       own Avni subject, same linking
              │                                         pattern as Demand.
              │                                         PENDING — what location to give
              │                                         each one. Avni requires a real,
              │                                         resolvable location to create
              │                                         any subject at all — so if a
              │                                         dispatch address doesn't map to
              │                                         one already in Avni, we need a
              │                                         way to add it, and it's unclear
              │                                         if Avni even has an API for that.]
              ▼
  1. Data POC fills in and submits the Demand form
              │        (Avni webapp)
              ▼
       integration service asks Avni: "anything new or edited?"
       [EXISTS — Avni's own API, already used for other record types]
              │
              ▼
  2. Sent to Salesforce for the first time
              │
              │  ──►  calls Salesforce's "receive a Demand" API
              │       [NEW — Goonj has to build this; doesn't exist today]
              ▼
  3. Salesforce creates the record, checks for duplicates,
     generates the Demand Code
              │
              │  ◄──  replies with the Demand Code + a Salesforce reference number
              ▼
       integration service saves the reply onto the Avni record
       [EXISTS — the same "create or update" call Demand's own sync into
        Avni already uses]
              ▼
  4. Demand Code appears in Avni
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
- **Dispatch Address will be its own Avni subject** — but what location to give it isn't settled.
  Avni needs a real, resolvable location to create any subject; if a dispatch address doesn't
  already map to one, we need a way to create it, and it's unclear whether Avni even has an API
  for that (step 0).

## Who's building what

| Goonj's Salesforce team | The Avni team |
|---|---|
| Build the new "receive a Demand" endpoint (step 2) — this doesn't exist today | Build the Demand form, including Dispatch Address selection (step 1) |
| Make it recognise repeat edits as the same Demand, not new ones (steps 3, 5) | Build the "keep editing until assigned" sync (steps 4–5) |
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
| 2 | **Does Salesforce return the Demand Code in the response, or only on a later sync?** | Goonj / SF | Decides if the code shows after one sync or two |
| 3 | **What are the final picklists?** Disaster Type has no options; Target Community is partial; only 3 of 14 Kit Types (CFW, Marriage Kits, Vaapsi) have documented sub-types | Goonj | 🚩 Blocker — form and field mapping both depend on these |
| 4 | **Who can void a Demand, and when?** Duplicates can be voided; deletion is barred once assigned | Both | Unclear if this is a Data POC action, Goonj-internal, or both |
| 5 | **What location does each Dispatch Address subject get, and can Avni even create a new location if one doesn't already exist?** | Goonj / SF | 🚩 Blocker for the "full sync" path — Avni requires a resolvable location to create any subject at all (step 0) |
| 6 | As raised in the notes: *"Do we build an Integration workflow for Dispatch Address sync as well, Or if this doesnt change regularly, we can just do a bulk upload like we do for Locations?"* | Goonj / SF | Either way, question 5 above has to be answered first |

**Already settled:**
- Salesforce returns the Demand ID on every call, even repeats — Avni uses it to update the
  existing record, never creates a new one.
- Edit lock triggers on **assignment to a Processing Center**, not approval — enforced at three
  layers: Avni's form, this service, Salesforce's endpoint.

## Rough estimate

Avni-side effort only — Goonj's Salesforce work isn't included, and sits on the critical path.

| Work | Person-days |
|---|---|
| Avni form & config | ~3 |
| Integration service | ~5 |
| QA, UAT and go-live | ~5 |
| **Total** | **~13** |

Excludes the 2–3 days already spent on solutioning. End-to-end cost: **~15–16 days**.

**Treat ~13 as optimistic, not expected** — assumes the cheap Dispatch Address path (bulk upload;
full sync adds 1–1.5 days) and a smooth UAT. QA/UAT depends on three external teams coordinating
(Goonj's SF team, Sanjha, MMT), which good specs alone won't speed up. Full reasoning: tech doc.

## Where the detail lives

Interested in the tech detail? See `goonj-demand-avni-registration-tech.md`.
