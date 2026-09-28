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
  │  in Avni   │                                │  Demand    │
  │  by Data   │  ID + Demand Code come back,   │  Code      │
  │  POC       │ ◄── saved onto same record ─── │  assigned  │
  └────────────┘                                └────────────┘
        ▲    │                                         │
        │    └──── edits keep syncing up ─────────────►│
        │            until the Demand is assigned      │
        │       status changes flow down always        │
        └──── (Assigned, DV, DPV) ◄──────────────────────┘
                (reuses the sync that already exists today)
```

A Data POC fills in the Demand form on the Avni webapp. There's no separate approval step — the
Data POC is the only person involved, so a second "verified" tick by the same person would just be
them confirming their own work. **Submitting the completed form is what triggers the first send**
to Salesforce. Salesforce creates the record, generates the Demand Code, and hands back its own ID
for that record. We save that ID onto the Avni record.

That first exchange isn't the end of the Avni-to-Salesforce direction, though. The Data POC can
keep editing the Demand afterward, and **those edits keep syncing up to Salesforce** — not just
once — for as long as the Demand hasn't been assigned to a Processing Center. Salesforce's upsert
behaviour is what makes this safe: every edit just updates the same linked record.

The moment a Demand is **assigned**, that stops. From then on the Demand belongs entirely to
Salesforce, and status changes — Assigned, Demand Validation, Demand Post-Validation — flow back
down into the same Avni record, using the sync that already exists today. This lock is enforced at
three separate points, not just one: Avni's own form stops allowing edits; this service stops
invoking the upsert call for that Demand at all; and Salesforce's endpoint should also reject an
upsert for an already-assigned Demand, purely as a backstop for the lag between the two. More on
this below.

Sending the Demand, getting the ID back, and saving it are meant to happen as one exchange. That's
not confirmed yet, though — it's possible Salesforce only returns the ID on a later sync instead
of in its immediate response, which would mean the Demand Code takes an extra cycle to show up.
That's one of the open questions further down, and it's worth settling before anyone starts
building the piece of code that talks to Salesforce.

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
| Identify a Demand by its Avni ID if we have one, else by the Salesforce Demand ID | Avni-originated Demands always have the former; Salesforce-originated ones only have the latter. |

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
| The call to Salesforce | Sends the converted data to the new endpoint, on every push | Calling pattern exists; the endpoint itself doesn't — Goonj's Salesforce team has to build it |
| ID write-back | Saves Salesforce's ID onto the Avni record on the first push, linking it | Genuinely new — no existing example in this codebase |
| The assignment stop | Once a Demand syncs down as assigned, stop invoking the upsert for it | New logic — the counterpart to the push gate |
| Identifier precedence | Avni ID if present, else the Salesforce Demand ID | New logic |
| A separate progress marker | This direction needs its own "where did I leave off," independent of the existing pull | Small, low-risk addition |
| Dispatch Address subject type | A new Avni subject type — First Name = Dispatch Address Name, an "Address" concept typed as **Location**, an Account Name concept, and Salesforce's Dispatch Address ID as the externalId link | Follows the same linking pattern as Demand itself; on the Demand form it's referenced via a Single Select, filtered by the Account chosen — the Location typing lives on the Dispatch Address subject's own Address field, not on the Demand form directly |

The Salesforce endpoint is the biggest external dependency — nothing here can be proven working
until it exists, so that conversation with Goonj should start early. On our side, the ID
write-back matters most: miss it, and at best we harmlessly re-check a Demand every cycle; at
worst, the next status update from Salesforce has nothing to match against and creates a duplicate
record in Avni.

**The approach for Dispatch Address is settled: it's its own Avni subject**, linked to Salesforce
the same way Demand is. Every Account (office) has an address associated with it, and the Demand
creator picks one from this subject list when raising a Demand — so Avni needs that list before
the form can work at all. Whether it arrives via a proper ongoing integration workflow (like Demand
itself) or a periodic bulk upload — the way Locations are handled today, on the assumption dispatch
addresses don't change often — is one open question below.

**What's not settled, and matters more: what location each Dispatch Address subject actually
gets.** Every existing entity in this codebase (Demand, Dispatch, Inventory) requires a resolvable
State and District to create a subject at all — `GoonjEntity.getAddressMap()` throws a
`RuntimeException` before ever calling Avni's API if either is blank, and Avni's own server can
separately reject the subject if that State/District combination isn't in its location hierarchy
(this is the `AddressNotFoundError` already flagged as the largest error category in the existing
Goonj sync). If a dispatch address's real-world location doesn't already exist in Avni, someone
needs to add it — and **it's unclear whether Avni exposes an API to create a new location under a
given location type at all.** Nothing in this codebase's `avni` client module does this today; every
existing entity only ever references a location that already exists, never creates one. This needs
checking against Avni's own platform team or documentation, not something answerable from this
repo.

---

## Which APIs this actually uses

Everything below is either an API that already exists and gets reused as-is, or a new one that has
to be built. Nothing in between.

| Direction | API | Status | Notes |
|---|---|---|---|
| Avni → this service | `GET /api/subjects` | **Existing** — Avni's own platform API | Already how the watcher pattern works for Distribution, Activity, and Dispatch Receipt. `AvniSubjectRepository.getSubjects()` in this codebase. |
| This service → Avni | `POST /api/subject` | **Existing** — Avni's own platform API | Used to write the Demand into Avni in the first place; `AvniSubjectRepository.create()`. Also how Salesforce's ID and Demand Code get saved back onto the record. |
| This service → Avni | `PUT /api/subject/{id}` | **Existing, but not currently expected to be used here** | `AvniSubjectRepository.update()`. In this codebase today it's used exactly once — by `DispatchEventWorker`, to remove a single line item after an upstream deletion — not by Distribution or Activity, and not as a general-purpose update. The ID write-back for Demand is expected to use the same `create()`/POST path above, since that's already an upsert. |

There is **no PATCH method anywhere in this codebase** — `AvniHttpClient` only implements GET,
POST, PUT, and DELETE. Worth stating plainly since it's easy to assume a partial-update verb
exists somewhere given how often "upsert" comes up in this plan; it doesn't, and nothing here needs
one.
| Salesforce → this service | `DemandService/getDemands` | **Existing** — already live today | The current pull-direction endpoint. Keeps working exactly as-is for status updates flowing down (Assigned, DV, DPV) — nothing changes here. `DemandRepository.GET_DEMANDS_PATH` in this codebase. |
| This service → Salesforce | *(no endpoint yet)* | **New — Goonj's Salesforce team has to build this** | The single biggest piece of new work in this whole feature. Needs to accept a Demand, behave as an upsert keyed on the Avni record's ID, and (ideally) reject an update for an already-assigned Demand. Other push-direction entities in this codebase call resource paths like `/web/distribution` and `/web/media` — a Demand endpoint would likely follow that same naming convention, but the exact path and payload shape is Goonj's call. |
| This service → Salesforce | *(depends on the Dispatch Address decision)* | **New, if a full sync is chosen** | If Dispatch Address gets its own ongoing sync rather than a bulk upload, that needs a new Salesforce endpoint too — most likely another `DemandService/getDemands`-style read endpoint. If a bulk upload is chosen instead, **no new API is needed at all** — it becomes a periodic file import, the same mechanism Locations already use. |

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
- Confirm what location each Dispatch Address subject should get, and whether Avni can even
  create a new location if the real one doesn't already exist — 🚩 blocks the full-sync path.
- Weigh in on how Dispatch Address should sync — full integration, or bulk upload.
- Deploy their endpoint first, before we deploy anything.

**On the Avni side, the form work involves:**

- Building the Demand form itself, with all its fields, including the Dispatch Address Single
  Select filtered by the chosen Account.
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
- The call to Salesforce, on every push, and writing its ID back on the first one.
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

**Does Salesforce hand back the Demand Code in its immediate response, or only on a later sync?**
This decides whether a user sees their code after one cycle or two, and it needs settling before
the field converter gets built.

**What are the final picklists?** Disaster type currently has no options listed at all. Target
community is only partially filled in. And of the fourteen kit types in the form, only three —
CFW, Marriage Kits, and Vaapsi — have documented sub-types; it's unclear whether the rest have any
at all. Both the form and the field mapping depend on these being settled first, or the work gets
done twice.

**Who can void a Demand, and when?** The requirement sheet allows voiding a duplicate Demand but
bars deleting one once it's assigned — it's not yet clear whether that's a Data POC action, an
MMT/Sanjha-team action, or both, and whether it differs before and after assignment.

**What location does each Dispatch Address subject get, and can Avni even create a new location if
one doesn't already exist?** 🚩 This is the blocker for the "full sync" path specifically. Avni
requires a resolvable location to create any subject at all (confirmed by this codebase's existing
pattern — see above); nothing here creates new Avni locations today, only references existing ones.
Being confirmed with someone on the Avni platform team.

**As originally raised in the notes:** *"Dispatch Address - Do we build an Integration workflow for
Dispatch Address sync as well, Or if this doesnt change regularly, we can just do a bulk upload like
we do for Locations?"* Either way, the location question above has to be answered first.

### Already settled

**Salesforce will return the Demand ID on every call**, including when it recognises a Demand it
already has. On our side, that ID is used to update whichever record already exists — setting it
on a Demand that started in Avni, or updating one that's already linked — never to create a second
record from the response.

It's worth understanding why this particular answer matters so much: if a repeat call from us ever
came back successful but without a usable ID, our side would have no way of knowing anything went
wrong. It would record the push as successful and move on, meaning that Demand would never be
retried, never show up as an error, and never get flagged for anyone to notice. It would simply
sit in Avni without a Demand Code, and Salesforce's own copy would eventually sync down separately
as a second, unrelated-looking record. Guaranteeing the ID comes back every time is what rules
that scenario out entirely.

**The edit lock keys off assignment to a Processing Center, not approval generically or the Demand
Code appearing** — enforced at three layers (Avni's form, this service, and Salesforce's own
endpoint). See the lock section above.

**Edits sync up to Salesforce continuously, not just once**, for as long as a Demand is unassigned.
This replaces an earlier assumption in this plan that the push was a one-time create — it isn't.

**Who gets access, and to what.** Data POCs only, for now, on the Avni webapp — field teams are a
later pilot, not this phase. A Data POC can create a Demand only for their own account and
geography, but can view every account and geography.

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
| 2 | The Demand Code comes back to Avni | The Demand from #1 now exists in Salesforce with a code | The next sync cycle runs | The same Avni Demand — not a new one — now shows the Demand Code and its Salesforce link |
| 3 | A status change doesn't create a duplicate | The Demand from #1–2 is already linked | Salesforce moves its status to Approved | The existing Avni record updates in place; the total number of Demands in Avni doesn't increase |
| 4 | A draft Demand isn't sent anywhere | A Data POC has started the Demand form but not yet completed and submitted it | A sync cycle runs | Nothing is sent to Salesforce; the Demand simply stays in draft in Avni |
| 5 | A linked Demand is never pushed again | A Demand already has a Salesforce link | Sync cycles run repeatedly | The push never fires again for that Demand — no repeat calls to Salesforce at all |
| 6 | A failed write-back recovers cleanly | The push to Salesforce succeeds, but saving the ID back onto Avni fails | The next cycle pushes the same Demand again | Salesforce recognises it and updates the existing record — no second Demand, no second code |
| 7 | Resubmitting after a dropped connection is safe | A user submits, loses connection before confirmation, and submits again | Both attempts reach Salesforce | Exactly one Demand and one Demand Code exist in Salesforce |
| 8 | Editing before submission works as expected | A Demand is still being drafted in Avni | A Data POC edits a field and then submits it | The version sent to Salesforce is the edited one, not the original |
| 9 | Edits before assignment keep syncing up | A Demand is already linked and unassigned | A Data POC edits a field, then a sync cycle runs | The edited version reaches Salesforce and updates the same linked record — no duplicate |
| 10 | Editing is blocked the instant a Demand is assigned | The assigned status has reached the device | A Data POC tries to change quantity or material | The edit is blocked by the Avni form |
| 10a | The lock holds even if Avni hasn't caught up yet | A Demand has just been assigned in Salesforce, but that status hasn't synced down to Avni yet | The integration service or Salesforce receives an upsert for that Demand anyway | This service does not invoke the call for an already-assigned Demand; if it somehow did, Salesforce's endpoint rejects it |
| 11 | Duplicate detection happens on Salesforce's side | An existing Salesforce Demand already matches on account, initiative, material, and date | A near-identical Demand is pushed from Avni | Salesforce applies its own duplicate handling; Avni performs no check of its own |
| 12 | Someone without permission can't register a Demand | A user's role hasn't been enabled for Demand registration | The user opens Avni | There's no option to register a Demand at all |
| 13 | A Salesforce outage doesn't lose the Demand | Salesforce's endpoint returns an error or times out | A push is attempted | The Demand isn't lost — it's recorded as a retryable error, using the same handling already used elsewhere, and gets retried on a later cycle |
| 14 | The right identifier is used in each direction | One Demand started in Avni (has an Avni ID, no Salesforce ID yet); another started in Salesforce (only has a Salesforce ID) | Requests are built for each | The Avni-originated one uses its Avni ID; the Salesforce-originated one uses its Salesforce ID |
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
| Form and configuration | ~3 |
| Integration code | ~5 |
| QA, UAT and go-live | ~5 |
| **Total** | **~13** |

This doesn't include the 2–3 days already spent on solutioning — the discussion that produced this
plan. That's real effort too, but it's a different kind (design, not build-and-test), already
spent, and shouldn't be folded into a forward-looking estimate as if it hasn't happened yet. If
what matters is total cost of this feature start to finish, it's roughly **2–3 already spent, plus
~13 to go** — call it ~15–16 days end to end, not ~13.

**Form and configuration.** The form itself is the largest piece, mostly because of the longer
picklists — kit type, target community, disaster type. The line-items section, validations, the
edit rule, and the access scoping are each smaller pieces on top — and there's one less piece than
originally scoped, since there's no separate approval step to build. Ordinary Avni admin work —
creating users, roles, or locations — isn't part of this estimate; Goonj's own tech team handles
that directly.

**Integration code is back down to ~5.** An earlier pass added 0.5–1 here for the Dispatch Address
subject type, but on reflection that only holds if Goonj chooses a full ongoing sync for it — and
under the cheaper option (a bulk upload, the way Locations work today), setting that up is a
one-time admin/config action, not integration-service code, so it doesn't belong in this bucket at
all. **That's still a real fork, not a resolved one:** if Dispatch Address does end up needing a
proper sync — its own watcher, its own field mapping, its own progress marker, mirroring how Demand
itself works — add 1–1.5 days back here. Question 5 decides which case applies. Beyond that, the
plan being this well specified going in (the rules, the lock behaviour, the exact API shapes) is a
legitimate reason this number can be lean — most of the normal back-and-forth of "wait, what should
happen here?" has already happened, in this conversation, rather than needing to happen during
implementation.

**A second, separate risk sits underneath both branches: the location question isn't costed here at
all.** If Avni turns out to have no API for creating a new location, someone has to add missing
locations by hand before a dispatch address can be created as a subject — that's not integration
code, it's an ongoing operational step, and it could affect the bulk-upload path just as much as the
full-sync one. This needs an answer before either branch of the ~5 vs ~6–6.5 fork above can be
trusted.

**QA, UAT, and go-live stays at ~5 — I'm not comfortable taking this lower, and want to say why
rather than just do it.** The previous pass already compressed this from 8.5 by shrinking the
bug-fix contingency buffer and the UAT-feedback line, the two lines that exist specifically to
absorb what we don't yet know. Squeezing them again means betting on zero problems during UAT.

The reason this doesn't compress the way the build side does: it depends on **three teams outside
our control synchronising** — Goonj's Salesforce team's endpoint, the Sanjha team's validation, and
MMT's assignment step — none of which get faster just because our own design is well specified.
Good specification reduces *our* rework risk; it does nothing for coordination risk across three
separate teams and systems. If this number needs to come down, the honest way to do it is to narrow
what ships in the first release (fewer test scenarios, a smaller pilot group), not to assume UAT
goes cleanly.

Development itself depends on when Salesforce's endpoint becomes available, and when the form and
other details are settled enough to actually start building.

**Not yet included above:** the account/geography access scoping — it came up after this estimate
was sized, and mostly lands in the form/config bucket rather than integration code, but hasn't been
sized on its own.
