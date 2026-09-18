# Demand registration from Avni — summary

**In one line:** Avni creates the Demand, the integration service pushes it to Salesforce once,
Salesforce gives it a code and owns it from then on.

## Why we're doing this

Salesforce licences are expiring for some Goonj partner teams. Today a Demand can only be created
in Salesforce, so those teams would lose the ability to raise one at all. This lets them start a
Demand in Avni instead.

Partners who keep their Salesforce licences carry on exactly as today — nothing about that flow
changes.

## How it works

```
  Avni                                  Salesforce
  ┌──────────┐  1. verified → push     ┌──────────┐
  │  Demand  │ ──────────────────────► │  Demand  │
  │ created  │                         │ created, │
  │ in Avni  │ ◄────────────────────── │   code   │
  └──────────┘  2. ID + Code back      │ assigned │
        ▲                              └──────────┘
        │   3. status updates later          │
        └────────────────────────────────────┘
```

1. A field team fills in the Demand form in Avni.
2. The Data POC ticks "Verified" — that's what triggers the send to Salesforce.
3. Salesforce creates the record and generates the Demand Code.
4. Salesforce's ID comes back and is saved onto the Avni record. *(Whether this happens in the
   same exchange or on the next sync is still to be confirmed with Goonj — it decides whether the
   Demand Code appears after one cycle or two.)*
5. From then on Salesforce owns it. Status changes (Approved, DA, DV, DPV) flow down into Avni
   automatically, using the sync that already exists today.

## Five things worth knowing

- **The Demand Code always comes from Salesforce.** Avni can't generate it — it needs a per-account
  counter only Salesforce maintains. Users won't see a code immediately; it appears once the round
  trip completes. Field teams need telling, or it looks like a bug.
- **The push happens once.** After Salesforce's ID is saved onto the Avni record, we never push
  that Demand again. Corrections are made in Salesforce from that point.
- **Salesforce's new endpoint must be an "upsert"** — keyed on the Avni record's own ID. That is
  what stops a network retry creating two Demands with two different codes.
- **Editing locks when "Approved" reaches the phone**, per the requirement sheet. There's a window
  before that where edits are allowed but won't reach Salesforce, so the form should warn during it.
- **Duplicate checking happens in Salesforce**, not in Avni.

## Who does what

**Goonj / Salesforce team**

- Build the upsert endpoint that accepts a Demand — this doesn't exist today
- Generate the Demand Code on it, and run duplicate checking
- Confirm the final picklists — Disaster Type, Target Community, and which Kit Types have sub-types

**Avni side**

- Build the Demand form: fields, line items, validations, verification checkbox, edit rule, role gating
- Build the integration: watch Avni, push to Salesforce, save the ID back, handle errors

## Order of work

1. **Agree** — answer the five open questions below
2. **Build** — Goonj's endpoint, our form, our integration code; these can run in parallel
3. **Test** — against a stub first, then a real Salesforce staging org, then end to end
4. **Deploy** — Salesforce first, integration second, wait 1–2 days, open registration last
5. **Watch** — confirm the first real Demands complete the full round trip

## Open questions — what we need answered

| # | Question | Owner | Why it matters |
|---|---|---|---|
| 1 | **Who is building the Salesforce upsert endpoint, and by when?** | Goonj / SF | 🚩 **Blocker.** It doesn't exist today and nothing goes live without it. Longest lead time, so start here |
| 2 | **Does Salesforce return the Demand Code in the response to our push, or only on a later sync?** | Goonj / SF | Decides whether the user sees their Demand Code after one sync cycle or two. Needs settling before we build the converter |
| 3 | **What are the final picklists?** Disaster Type has no options listed at all; Target Community is partial and marked TBD; and only 3 of the 14 Kit Types (CFW, Marriage Kits, Vaapsi) have sub-types documented — do the others have any? | Goonj | 🚩 **Blocker.** Both the Avni form and the field mapping depend on these. Building before they're fixed means doing it twice |
| 4 | **Which user groups get access to Demand registration, and what can each of them do — create, edit, void?** | Both | Registration is off for every group by default, so we need the specific list and the permissions per group before anything can be switched on |

**Already settled:** Salesforce will return the Demand ID on every call, including when it
recognises a Demand it already has. The Avni side uses it to update the record that already
exists — setting the ID on an Avni-created Demand, updating in place for ones already linked —
never creating a new one.

## Rough estimate

Avni-side effort only — **Goonj's Salesforce work is not included**, and that sits on the critical
path.

| Work | Person-days |
|---|---|
| Avni form & config | ~3 |
| Integration service | ~5.5 |
| QA, UAT and go-live | ~8.5 |
| **Total** | **~17** |

Development depends on when the Salesforce endpoint is available, and when the forms and other
details are clear enough to be picked up.

## Where the detail lives

`goonj-demand-avni-registration-tech.md` — the engineering reference, for whoever is actually
building this: every rule with its reasoning, all 20 test scenarios, the field-by-field build
list, and the estimates in depth. Most people won't need it — this page covers what's happening.
