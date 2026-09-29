# Demand registration from Avni — engineering reference

This is the detailed reference for whoever actually builds this feature — every rule with the
reasoning behind it, the full list of test scenarios, and the effort estimates. If you just need
to know what's changing and who's doing what, read `goonj-demand-avni-registration-summary.md`
instead — it has the same step-by-step journey in plain language, and is what to share with the
wider team. This document goes a lot deeper than most people will need.

## What we're building, in one sentence

Today a Demand can only be created in Salesforce, and this service copies it into Avni. Some
partner teams are losing their Salesforce logins, so they need a way to start a Demand in Avni
instead — and once they do, that Demand still has to reach Salesforce, get processed there, and
have its updates travel back.

---

## A few terms, so the rest of this makes sense

**Avni** is the mobile and web app field teams use to collect data. **Salesforce** is Goonj's
system of record for Demand — it's where Demand Codes get generated and where the approval
workflow (Demand Assignment, Demand Validation, Demand Post-Validation) happens. This
integration service sits between the two, moving records back and forth on a timer that runs
every few minutes. It doesn't store anything of its own.

Right now, syncing only ever goes one way for any given kind of record: something reads from
Salesforce and writes into Avni, or reads from Avni and writes into Salesforce, never both for the
same thing. Demand today is a read-from-Salesforce-only case.

When a Demand is copied into Avni, it carries a hidden tag — Salesforce's own ID for that record.
That's how future syncs know "this Avni record and that Salesforce record are the same Demand"
rather than creating a duplicate every time the sync runs. The flip side is the Avni record's own
ID, which we'll send to Salesforce so it can recognise a record it's already seen. Both ideas
matter a lot to how we avoid duplicates, and come up again below.

---

## How things work today

```
   Salesforce                                    Avni
  ┌───────────┐   "any new or changed          ┌───────────┐
  │  Demand    │    Demands since last time?"   │  Demand    │
  │  created   │ ─────────────────────────────► │  record    │
  │  here      │   copy each one into Avni,     │  appears   │
  │            │   tagged with Salesforce's     │  here      │
  │            │   own ID for it                │            │
  └───────────┘                                 └───────────┘
```

Salesforce is asked, on a schedule, "anything new or changed since I last asked?" Every Demand
that comes back becomes a record in Avni, carrying Salesforce's ID. If Salesforce later sends the
same Demand again — say its status changed to Approved — the existing Avni record gets updated
rather than duplicated, because that tag matches.

Nothing goes the other way today. If someone tried to start a Demand in Avni right now, it would
simply sit there — the code that would send it onward to Salesforce doesn't exist; it's a
placeholder that throws an error if it's ever called.

The reassuring part: this "other direction" already exists in this codebase for three other kinds
of records — Distribution, Activity, and Dispatch Receipt. We're not inventing a new pattern here,
just extending an existing one to Demand.

---

## Who registers a Demand, and what they can do

For now this is a **Data POC feature on the Avni webapp**, not a field-team feature — field teams
get this in a later pilot. A Data POC can only **create** a Demand for their own account and
geography, but can **view** every account and geography.

## How the new flow will work

```
   Avni                                          Salesforce
  ┌────────────┐                                ┌────────────┐
  │  Demand    │  not linked yet, and           │  record    │
  │  created   │  submitted → send to SF ─────► │  created,  │
  │  in Avni   │  (carries the Avni source ID,  │  Demand    │
  │  by Data   │   which SF must store          │  Code      │
  │  POC       │   mandatorily on its record)   │  assigned  │
  └────────────┘                                └────────────┘
        ▲    │                                         │
        │    └──── edits keep syncing up ─────────────►│
        │            (same push, until assigned)       │
        │                                               │
        └── Demand Code, then later status ◄────────────┘
            (Assigned, DV, DPV) — all via the
            pull sync that already exists today,
            completely unchanged, matching by
            the Avni source ID if present, else
            Salesforce's own Demand ID
```

A Data POC fills in the Demand form on the Avni webapp. There's no separate approval step — the
Data POC is the only person involved, so a second "verified" tick by the same person would just be
them confirming their own work. **Submitting the completed form is what triggers the first send**
to Salesforce, carrying the Avni record's own source ID. Salesforce creates the record, generates
the Demand Code, and is required to store that Avni source ID against its own record — that's what
makes the eventual match-up work.

**This service never waits for or depends on that push's response to contain anything.** The
Demand Code, and everything else about this Demand, arrives back in Avni through the ordinary
Salesforce-to-Avni pull sync — the one that already exists today, unchanged in mechanism. It simply
picks this Demand up on its next scheduled run, exactly the way it already picks up every
Salesforce-originated Demand. The one new piece of logic sits in how that pull decides which Avni
record to update: by the Avni source ID, if the incoming record carries one, otherwise by
Salesforce's own Demand ID — the same fallback this codebase already uses for records that have
never touched Avni.

That first push isn't the end of the Avni-to-Salesforce direction, though. The Data POC can keep
editing the Demand afterward, and **those edits keep syncing up to Salesforce** — not just once —
for as long as the Demand hasn't been assigned to a Processing Center. Salesforce's upsert
behaviour is what makes this safe: every edit just updates the same linked record.

The moment a Demand is **assigned**, that stops. From then on the Demand belongs entirely to
Salesforce, and status changes — Assigned, Demand Validation, Demand Post-Validation — flow back
down into the same Avni record, using that same existing pull. This lock is enforced at three
separate points, not just one: Avni's own form stops allowing edits; this service stops invoking
the upsert call for that Demand at all; and Salesforce's endpoint should also reject an upsert for
an already-assigned Demand, purely as a backstop for the lag between the two. More on this below.

---

## Where the Demand Code comes from

Every Demand Code looks like `2026/ACC-270688/ASM/383` — four pieces stitched together: the year,
a code for the account, a code for the initiative, and a running number.

Avni simply cannot generate this code, and shouldn't try to. That last number is a counter that
only Salesforce can keep track of — Avni has no way of knowing the previous Demand for that
account was number 382. So the code always gets created in Salesforce and copied back afterwards.

In practice, this means the Demand Code doesn't exist yet at the moment someone registers a
Demand. **The registration form should not show this field at all** — it only appears once the
round trip described above finishes and syncs back. Showing an empty or placeholder code field on
the form itself would raise exactly the "why is this blank / is it broken" confusion that leaving
it off avoids entirely.

What we do have to get right on our side: the account and initiative fields both come from what
the user filled in inside Avni, and they feed directly into the code Salesforce builds. If our
conversion from Avni's fields into Salesforce's fields gets those wrong, Salesforce can't build a
correct code at all.

---

## The rules we've agreed to build around

These answer one underlying question: what stops the same Demand from being created twice, or
being edited in two different places at the same time?

| Rule | What it means |
|---|---|
| The Demand Code only ever comes from Salesforce | Avni can't generate it — see above. |
| Salesforce's new endpoint has to behave as an "upsert," keyed on the Avni record's ID | Updates the existing record if Salesforce has seen this Avni ID before; only creates new if it hasn't. Sending the same Demand twice is harmless. Salesforce may also just ignore a repeat and do nothing. |
| We keep syncing a Demand up to Salesforce until it's assigned | The first push creates it in Salesforce; every edit after that keeps updating the same linked record via upsert. This stops the moment the Demand is assigned to a Processing Center. |
| Once a Demand is assigned, it belongs to Salesforce | No further edits sync up from Avni, from any direction. Corrections from that point happen in Salesforce. |
| Editing in Avni is allowed right up until assignment | Not "until approval" — the precise trigger is assignment to a Processing Center. Edits made before that point do reach Salesforce; see the lock section below for what happens at assignment itself. |
| Duplicate-Demand checking happens in Salesforce | Not in Avni. |
| Registration starts switched off for everyone | Turned on only for the specific groups that need it — same switch controls when the feature goes live. Only Data POCs get it for now, on the webapp; field teams are a later pilot. |
| When the existing pull sync writes a Demand into Avni, it matches by the Avni source ID if the Salesforce record carries one, else by the Salesforce Demand ID | This is pull-side logic, not push-side — the push always carries the Avni source ID by definition. Avni-originated Demands have a source ID to match on; Salesforce-originated ones only ever have the Demand ID, exactly as today. |

Coordinating the actual deployment across Salesforce, this service, and Avni is also part of what
we've agreed, but that's a rollout question rather than a sync-behaviour one — it has its own
section near the end of this document.

### What happens when a Demand is assigned — the three-layer lock

Once a Demand is created in Avni, a Data POC can keep editing it, and those edits keep syncing up
to Salesforce, right up until an internal Goonj team (MMT) assigns the Demand to a Processing
Center. At that instant, editing has to stop everywhere at once — and it's stopped in three places
independently, not just one:

- **Avni's own form** stops allowing edits on that Demand the moment the assigned status syncs
  down.
- **This service** stops invoking the upsert call for that Demand entirely, even if an edit is
  somehow still sitting there waiting to be sent.
- **Salesforce's endpoint** should also reject an upsert call for a Demand that's already assigned.

That third layer exists specifically to cover the lag between the two systems: Salesforce's own
assignment update and Avni's edit-lock don't happen in the same instant, so there's a brief window
where Avni might not yet know a Demand has been assigned. If Salesforce's endpoint also refuses a
late upsert during that gap, no edit can slip through no matter which side is slower to notice.

This is a real change from what an earlier draft of this plan assumed — that edits inside this
window would be silently discarded, with a warning shown to the user. That's no longer the design:
edits made before assignment genuinely reach Salesforce via the ongoing upsert, and only edits
attempted *after* assignment are blocked — and blocked outright, not warned-and-allowed.

Before the very first push — meaning before the Data POC has submitted the Demand at all — editing
is unaffected by any of this; nothing has been sent to Salesforce yet.

### Why the upsert behaviour is doing so much of the work

Because Salesforce recognises a Demand by its Avni-side ID, sending the same Demand twice is
harmless. That one decision quietly covers several situations that would otherwise each need
their own careful handling:

- We successfully push a Demand, but saving its Salesforce ID back onto the Avni record fails.
  The next cycle just pushes it again, Salesforce recognises it, and nothing duplicates.
- A user resubmits after their connection drops. Same underlying Demand, same result — one record.
- We retry after a timeout where we genuinely don't know whether the first attempt succeeded.
  Retrying is always safe.

Without that upsert behaviour, every one of these would risk creating a duplicate Demand with a
duplicate code.

---

## Reconciling the requirement sheet's flow rules

The original requirement sheet lays out a few rules about when a Demand can and can't change.
Here's how each one actually plays out given the decisions above:

| The sheet says | What actually happens |
|---|---|
| Locked after approval — no edits, no line-item, quantity or material changes | The precise trigger is **assignment to a Processing Center**, not approval generically. Before that, edits are allowed and do sync up to Salesforce. Enforced at three layers — Avni's form, this service, and Salesforce's own endpoint — see above. |
| Status changes must show on the dashboard as a Demand moves through approval | Already handled by the existing Salesforce-to-Avni sync. No new code — just needs covering in testing. |
| Warn on duplicate Demands | Moved entirely to Salesforce. Avni performs no check of its own. |
| Resubmitting after a failure must not create a second Demand Code | Guaranteed by Salesforce's upsert behaviour, not by any retry logic on our end. |

### One thing worth being precise about: submitting isn't approving

Submitting the Demand form is a Data POC action, done entirely in Avni, and it's what triggers the
first send to Salesforce. **Demand Status**, by contrast, is set by Salesforce and synced down into
Avni — it's read-only there, changes throughout the Demand's life (including after assignment), and
it's what actually controls the edit lock.

Avni never assigns or approves anything itself — that happens entirely in Salesforce, and Avni
only receives the result. That's what makes the assignment-based lock possible at all: Avni knows a
Demand is locked because the status tells it so, not because of anything a Data POC ticks.

---

## What actually needs to be built

| Piece | What it does | Precedent to build from |
|---|---|---|
| A watcher | Polls Avni every cycle for Demand records that are complete (submitted) and not yet assigned | Same pattern as the existing Distribution watcher |
| A push gate | Pushes on the first complete save, and keeps pushing on every later edit — stops the moment the Demand is assigned | Simple check, nothing new architecturally |
| A field converter | Avni fields → Salesforce fields, including the account and initiative values the Demand Code needs | Distribution's converter does almost exactly this |
| A line-item converter | The form's repeating second section — material type, kit type, quantity, sub-types | Both Dispatch and Distribution already handle repeating line items |
| A Dispatch Address converter | Shapes the Avni address the way Salesforce expects | The reverse direction (reading an address out of Salesforce) already exists |
| The call to Salesforce | Sends the converted data to the new endpoint, on every push, always carrying the Avni source ID | Calling pattern exists; the endpoint itself doesn't — Goonj's Salesforce team has to build it |
| The assignment stop | Once a Demand syncs down as assigned, stop invoking the upsert for it | New logic — the counterpart to the push gate |
| Identifier precedence in the existing pull | The existing Salesforce-to-Avni pull worker (`Demand.java` / `DemandEventWorker`) always keys the Avni `externalId` on Salesforce's own Demand ID today. It needs updating to check for and prefer an Avni source ID first, falling back to the Demand ID only when none is present | This is a change to *existing* pull-side code, not new push-side logic — small, but it's the piece that actually links an Avni-originated Demand to the same record once the pull picks it up |
| A separate progress marker | This direction needs its own "where did I leave off," independent of the existing pull | Small, low-risk addition |
| Dispatch Address subject type | A new Avni subject type — First Name = Dispatch Address Name, District (an "Address" concept typed as **Location**, set to an existing District), an Account concept, and Salesforce's Dispatch Address ID as the externalId link — **this SF ID is what gets sent in the Demand creation request**, not just a linking detail | Follows the same linking pattern as Demand itself; on the Demand form it's referenced via a Single Select, filtered by the Account chosen. Using an existing District (not a new location) means this never hits the address-resolution failures that already affect Demand/Dispatch/Inventory |

The Salesforce endpoint is the biggest external dependency — nothing here can be proven working
until it exists, so that conversation with Goonj should start early. On our side, the identifier
precedence change in the existing pull worker matters most: miss it, and the pull keeps keying
every Demand on Salesforce's own ID as it always has, so an Avni-originated Demand's status updates
never match back to the record the Data POC actually created — creating a duplicate in Avni instead
of updating the original.

**The approach for Dispatch Address is settled: it's its own Avni subject**, linked to Salesforce
the same way Demand is. Every Account (office) has an address associated with it, and the Demand
creator picks one from this subject list when raising a Demand — so Avni needs that list before
the form can work at all. Two options for getting it there:

- **Integration with Salesforce, using Salesforce's own API.** Expected to be a small, infrequent
  load — dispatch addresses don't change often, so this isn't a continuous sync in the way Demand's
  own push is; more an occasional catch-up.
- **Bulk upload into Avni, the way Locations are onboarded today** — sourced from a database export
  rather than a live API call, and critically, that export has to include each record's Salesforce
  ID from the start, so every Dispatch Address is already linked and nothing needs backfilling.

Which of these two Goonj prefers is one open question below.

**The location question is settled too: each Dispatch Address subject gets the District of choice**
— an existing entry in Avni's own location hierarchy, not a new one that has to be created. This
matters because every existing entity in this codebase (Demand, Dispatch, Inventory) requires a
resolvable State and District to create a subject at all — `GoonjEntity.getAddressMap()` throws a
`RuntimeException` before ever calling Avni's API if either is blank, and Avni's own server can
separately reject the subject if that combination isn't in its location hierarchy (this is the
`AddressNotFoundError` already flagged as the largest error category in the existing Goonj sync).
Using an existing District sidesteps both failure modes entirely — there's no location-creation
question to answer, because nothing new is ever being created. (Nothing in this codebase's `avni`
client module creates locations today either way — every existing entity only ever references one
that already exists — so this decision also means we're not relying on a capability that may not
exist.)

---

## Which APIs this actually uses

Everything below is either an API that already exists and gets reused as-is, or a new one that has
to be built. Nothing in between.

| Direction | API | Status | Notes |
|---|---|---|---|
| Avni → this service | `GET /api/subjects` | **Existing** — Avni's own platform API | Already how the watcher pattern works for Distribution, Activity, and Dispatch Receipt. `AvniSubjectRepository.getSubjects()` in this codebase. |
| This service → Avni | `POST /api/subject` | **Existing** — Avni's own platform API | Used by the existing pull sync to write or update a Demand in Avni — `AvniSubjectRepository.create()`. This is the same call, on the same existing pull cycle, that brings the Demand Code back in; nothing new is added here except which identifier it matches on (see the rules above). |
| This service → Avni | `PUT /api/subject/{id}` | **Existing, but not expected to be used here** | `AvniSubjectRepository.update()`. In this codebase today it's used exactly once — by `DispatchEventWorker`, to remove a single line item after an upstream deletion — not a general-purpose update, and not needed for this feature. |

There is **no PATCH method anywhere in this codebase** — `AvniHttpClient` only implements GET,
POST, PUT, and DELETE. Worth stating plainly since it's easy to assume a partial-update verb
exists somewhere given how often "upsert" comes up in this plan; it doesn't, and nothing here needs
one.
| Salesforce → this service | `DemandService/getDemands` | **Existing** — already live today | The current pull-direction endpoint. Keeps working exactly as-is for status updates flowing down (Assigned, DV, DPV) — nothing changes here. `DemandRepository.GET_DEMANDS_PATH` in this codebase. |
| This service → Salesforce | *(no endpoint yet)* | **New — Goonj's Salesforce team has to build this** | The single biggest piece of new work in this whole feature. Needs to accept a Demand, behave as an upsert keyed on the Avni record's ID, and (ideally) reject an update for an already-assigned Demand. Other push-direction entities in this codebase call resource paths like `/web/distribution` and `/web/media` — a Demand endpoint would likely follow that same naming convention, but the exact path and payload shape is Goonj's call. |
| This service → Salesforce | *(depends on the Dispatch Address decision)* | **New, if the Salesforce-integration option is chosen** | If Dispatch Address is onboarded via Salesforce's own API rather than a bulk upload, that needs a new Salesforce endpoint too — most likely another `DemandService/getDemands`-style read endpoint, though expected to be a small, infrequent load rather than a continuous sync. If a bulk upload is chosen instead, **no new API is needed at all** — it's a periodic database-sourced file import, already carrying each record's Salesforce ID, the same mechanism Locations already use. |

On our side, none of this needs a new *Avni* API — the Avni platform already exposes everything
this feature needs. What's missing is entirely on Salesforce's side, plus the new code in this
service that calls the existing Avni APIs in the new direction.

---

## Who's doing what

Two groups are involved. Everything on our side — both the Avni form work and the code in this
service — sits with the same team, but they're genuinely different kinds of work, so it's worth
keeping them distinct below.

**Goonj's Salesforce team needs to:**

- Build the new endpoint that accepts a Demand and behaves as an upsert.
- Generate the Demand Code on that same endpoint, following the year / account / initiative /
  sequence logic.
- Run the duplicate-Demand check on their side.
- Confirm the picklists that are still incomplete — disaster type, target community, and which
  kit types have sub-types.
- Decide whether a repeat update that Salesforce chooses to ignore should still tell us anything.
- Make sure Salesforce's own endpoint rejects an upsert for an already-assigned Demand — the
  backstop layer of the assignment lock.
- Weigh in on how Dispatch Address should sync — full integration, or bulk upload.
- Deploy their endpoint first, before we deploy anything.

**On the Avni side, the form work involves:**

- Creating the Dispatch Address subject type itself — the prerequisite before the Demand form can
  reference it at all.
- Building the Demand form, with all its fields, including the Dispatch Address Single Select
  filtered by the chosen Account.
- Building the line-items section, including the kit sub-types.
- Field validations — dates in the right order, quantities that make sense, fields that only
  appear conditionally.
- The edit rule that keys off the Demand being **assigned**, not "approved," and with no separate
  approval step of its own.
- Access control: Data POCs can create only for their own account and geography, but can view all
  of them — and registration is Data-POC-only for now, on the webapp, with field teams a later
  pilot.
- Making sure registration starts switched off, and can be turned on only for the intended roles.

**And the integration code involves:**

- The watcher and the push gate — pushing on the first complete save, and on every edit after that,
  until assignment.
- The Dispatch Address subject type, and however it gets its data from Salesforce.
- The two converters — one for the Demand's own fields, one for its line items — plus the Dispatch
  Address conversion.
- The call to Salesforce, on every push.
- Updating the existing pull worker to match by Avni source ID first, falling back to Salesforce's
  own Demand ID.
- Stopping the upsert entirely once a Demand syncs down as assigned.
- Deciding which identifier to use, and keeping a separate progress marker for this direction.
- Error handling, so a failed push gets retried rather than silently disappearing.
- Deploying second, after Salesforce, with registration only opened up last.

Nothing here is provably working until Salesforce's endpoint exists, even though most of the Avni
side can be built and tested against a stand-in before that happens. The form work and the
integration code can run alongside each other too — they only really need to meet at testing time,
so the form shouldn't be left until last just because it feels like the smaller piece.

---

## Open questions

**Who's building the Salesforce endpoint, and by when?** This is the biggest blocker — nothing
goes live without it, and it has the longest lead time of anything here, so it's worth raising
first.

**What are the final picklists?** Disaster type currently has no options listed at all. Target
community is only partially filled in. And of the fourteen kit types in the form, only three —
CFW, Marriage Kits, and Vaapsi — have documented sub-types; it's unclear whether the rest have any
at all. Both the form and the field mapping depend on these being settled first, or the work gets
done twice.

**Who can void a Demand, and when?** The requirement sheet allows voiding a duplicate Demand but
bars deleting one once it's assigned — it's not yet clear whether that's a Data POC action, an
MMT/Sanjha-team action, or both, and whether it differs before and after assignment.

**As originally raised in the notes:** *"Dispatch Address - Do we build an Integration workflow for
Dispatch Address sync as well, Or if this doesnt change regularly, we can just do a bulk upload like
we do for Locations?"* No longer blocked on a location question — see below.

### Already settled

**The push's response is never depended on for anything.** This service doesn't wait for or parse
Salesforce's immediate reply to the push for a Demand ID or Code — it only needs the push to
succeed (or fail and be retried). What actually links an Avni-originated Demand to its Salesforce
record is Salesforce mandatorily storing the Avni source ID it was sent, and the existing pull sync
later finding that Demand and matching it back by that same source ID.

This is a real simplification over an earlier draft of this plan, which assumed the round trip
depended on Salesforce's push response carrying a usable ID, and that a response without one would
be a silent failure mode worth designing around. It isn't one: the pull sync doesn't care what the
push responded with, only what Salesforce ends up storing.

**The edit lock keys off assignment to a Processing Center, not approval generically or the Demand
Code appearing** — enforced at three layers (Avni's form, this service, and Salesforce's own
endpoint). See the lock section above.

**Edits sync up to Salesforce continuously, not just once**, for as long as a Demand is unassigned.
This replaces an earlier assumption in this plan that the push was a one-time create — it isn't.

**Who gets access, and to what.** Data POCs only, for now, on the Avni webapp — field teams are a
later pilot, not this phase. A Data POC can create a Demand only for their own account and
geography, but can view every account and geography.

**Each Dispatch Address subject's location is the District of choice** — an existing entry in
Avni's location hierarchy, never a new one. This removes the location-creation question entirely:
there's no need to check whether Avni can create a new location, because nothing new is being
created, and the address-resolution failures already affecting Demand/Dispatch/Inventory don't
apply here.

**There is no separate approval or "verified" step.** A Data POC is the only person involved, so a
second tick by the same person to confirm their own work adds nothing. Submitting the completed
form is what triggers the first send to Salesforce.

---

## Test scenarios

These are the situations that should demonstrably work — or fail safely — before this feature is
considered done.

| # | Scenario | Setup | Action | Expected result |
|---|---|---|---|---|
| 1 | A new Avni Demand successfully reaches Salesforce | A Data POC fills in the Demand form on the Avni webapp | They submit it, and the sync cycle runs | A matching Demand appears in Salesforce with every field correctly mapped |
| 2 | The Demand Code comes back to Avni | The Demand from #1 now exists in Salesforce with a code, carrying the Avni source ID it was sent | The existing pull sync runs on its normal schedule | The same Avni Demand — not a new one — now shows the Demand Code, matched back by its Avni source ID |
| 3 | A status change doesn't create a duplicate | The Demand from #1–2 is already linked | Salesforce moves its status to Approved | The existing Avni record updates in place; the total number of Demands in Avni doesn't increase |
| 4 | A draft Demand isn't sent anywhere | A Data POC has started the Demand form but not yet completed and submitted it | A sync cycle runs | Nothing is sent to Salesforce; the Demand simply stays in draft in Avni |
| 5 | A linked Demand is never pushed again | A Demand already has a Salesforce link | Sync cycles run repeatedly | The push never fires again for that Demand — no repeat calls to Salesforce at all |
| 6 | The pull correctly avoids a duplicate | A Demand pushed from Avni now exists in Salesforce, carrying the Avni source ID | The existing pull sync picks it up | It matches by the Avni source ID and updates the original Avni record — it must not fall back to Salesforce's own Demand ID and create a second one |
| 7 | Resubmitting after a dropped connection is safe | A user submits, loses connection before confirmation, and submits again | Both attempts reach Salesforce | Exactly one Demand and one Demand Code exist in Salesforce |
| 8 | Editing before submission works as expected | A Demand is still being drafted in Avni | A Data POC edits a field and then submits it | The version sent to Salesforce is the edited one, not the original |
| 9 | Edits before assignment keep syncing up | A Demand is already linked and unassigned | A Data POC edits a field, then a sync cycle runs | The edited version reaches Salesforce and updates the same linked record — no duplicate |
| 10 | Editing is blocked the instant a Demand is assigned | The assigned status has reached the device | A Data POC tries to change quantity or material | The edit is blocked by the Avni form |
| 10a | The lock holds even if Avni hasn't caught up yet | A Demand has just been assigned in Salesforce, but that status hasn't synced down to Avni yet | The integration service or Salesforce receives an upsert for that Demand anyway | This service does not invoke the call for an already-assigned Demand; if it somehow did, Salesforce's endpoint rejects it |
| 11 | Duplicate detection happens on Salesforce's side | An existing Salesforce Demand already matches on account, initiative, material, and date | A near-identical Demand is pushed from Avni | Salesforce applies its own duplicate handling; Avni performs no check of its own |
| 12 | Someone without permission can't register a Demand | A user's role hasn't been enabled for Demand registration | The user opens Avni | There's no option to register a Demand at all |
| 13 | A Salesforce outage doesn't lose the Demand | Salesforce's endpoint returns an error or times out | A push is attempted | The Demand isn't lost — it's recorded as a retryable error, using the same handling already used elsewhere, and gets retried on a later cycle |
| 14 | The pull sync uses the right identifier for each Demand | One Demand started in Avni and carries an Avni source ID on its Salesforce record; another started in Salesforce and has never had one | The pull sync processes both | The Avni-originated one is matched and updated by its Avni source ID; the Salesforce-originated one falls back to being matched by Salesforce's own Demand ID, exactly as it does today |
| 15 | Demands created directly in Salesforce still work exactly as before | A Demand is created in Salesforce with no Avni involvement | A sync cycle runs | It appears in Avni exactly as it does today — nothing about the existing path regresses |
| 16 | Partners who keep their Salesforce access are unaffected | A partner still has Salesforce access | They create a Demand there as usual | Nothing changes for them |
| 17 | An unmapped value fails clearly | A picklist value chosen in Avni has no equivalent set up in Salesforce | A Demand is submitted using it | It fails with a clear "no mapping found" message rather than silently sending the wrong value |
| 18 | The two sync directions don't interfere with each other | Both directions have pending work in the same cycle | The job runs | Each direction processes its own records fully, without disturbing the other's progress marker |
| 19 | The Demand Code is formatted correctly | An Avni-originated Demand has completed its round trip | The code is inspected | It follows year / account code / initiative code / running number, and the number is unique per account |
| 20 | The dashboard reflects the Demand's progress | A Demand moves through its approval stages in Salesforce | A sync runs after each change | Avni's dashboard shows the correct current status each time, with the Demand Code visible |
| 21 | A Data POC can only create for their own scope | A Data POC's account and geography are set up in Avni | They open the Demand form | Only their own account is available to create against, but they can still view Demands for other accounts and geographies |
| 22 | Dispatch Address is scoped to the chosen Account | A Demand form has an Account selected | The Data POC opens the Dispatch Address field | Only addresses belonging to that Account are offered, sourced from the Dispatch Address subject type |

---

## The order this needs to happen in

Getting this sequence wrong means either Demands pile up in Avni with nowhere to go, or Salesforce
starts receiving calls it isn't ready to handle.

**First, agree.** Settle the open questions above with Goonj — the Salesforce endpoint has the
longest lead time of anything here, so start that conversation immediately. Get the final
picklists sorted too, since building the form or the field mapping ahead of that just means
redoing the work later. Decide how Dispatch Address syncs before building the form, since the form
can't offer a real address list without it.

**Then build, on both sides at once.** Goonj's Salesforce team builds the endpoint. We build the Avni
form — with registration deliberately left switched off — and we build the integration code
alongside it. All three can happen in parallel, and our integration code can be tested against a
stand-in without waiting for Goonj's endpoint to exist.

**Then test, in stages.** Start against a stand-in, before Goonj's real endpoint is ready, for
everything that doesn't need Salesforce itself — the draft case, permissions and account scoping,
editing before submission, and unmapped values. Once the real endpoint exists on a
staging Salesforce org, run the rest — the happy path, the Demand Code coming back, duplicate
protection, resubmission, edits continuing to sync before assignment, the lock at assignment
holding on all three layers, outage handling, identifier precedence, and the code format itself.
Separately, confirm nothing about the existing Salesforce-to-Avni path has broken. Finally, walk
the full loop end to end on staging before anything touches production.

**Then deploy, strictly in order.** Salesforce goes first. Our service goes second — and at this
point, nothing actually happens yet, because registration is still switched off for everyone.
Then wait a day or two, deliberately, to confirm both deployments are stable before any real data
starts moving. Only then does registration get switched on for the intended groups — that's the
actual moment this feature goes live.

**Then watch.** Confirm the first real Demands make it all the way through — created in Avni,
pushed to Salesforce, the code coming back, and status updates flowing down afterward.

---

## A rough sense of effort

These figures cover only the Avni side of this — the form and configuration work, plus the
integration code. Goonj's own Salesforce work isn't estimated here at all, and it sits squarely on
the critical path; nothing goes live without it. Treat these numbers as a planning aid, not a
commitment — they're sized against comparable work already sitting in this codebase.

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

Split by entity rather than by kind of work, since Dispatch Address turned out to be substantial
enough on its own to track separately — a new subject type, its own field mapping, and its own
sync decision, alongside the Demand work itself.

**Analysis and solutioning (~1)** covers what's left to close out after this plan — settling the
open questions with Goonj, confirming the sync method for Dispatch Address, and any remaining design
decisions before implementation starts. The bulk of solutioning is already behind this plan, not
ahead of it.

**Avni form & config** is split Demand (~1) and Dispatch Address (~1). Demand's form is the larger
build in absolute terms — all its fields, the line-items section, the edit rule, access scoping —
but Dispatch Address's own form/config work (its subject type definition, the District-of-choice
location, the Single Select on the Demand form filtered by Account) is real enough to warrant its
own day rather than being absorbed into Demand's number.

**Integration service** is likewise split Demand (~2) and Dispatch Address (~2). Demand's piece
covers the watcher, the field and line-item converters, the call to Salesforce, the identifier-
precedence change in the existing pull worker, and the assignment-triggered stop. Dispatch
Address's piece covers however it gets its data from
Salesforce — the specific size still depends on the onboarding option chosen (see below), but the
location question that used to make this unpredictable is resolved (District of choice, an existing
Avni location — no location-creation risk to price in).

**UAT (~2) and Deployment (~1)** are tighter than earlier passes assumed, on the view that a well
specified plan reduces rework risk substantially — most of the "wait, what should happen here?"
churn has already happened in this conversation, not left for implementation or testing to surface.
Worth keeping in mind that UAT still depends on three teams outside our control synchronising
(Goonj's Salesforce team, Sanjha, MMT) — if that coordination doesn't go smoothly, UAT is the line
most likely to need more room, not the build lines.

Development itself depends on when Salesforce's endpoint becomes available, and when the form and
other details are settled enough to actually start building.
