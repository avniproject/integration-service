# Demand registration from Avni — engineering reference

This is the detailed reference for whoever actually builds this feature — every rule with the
reasoning behind it, the full list of test scenarios, and the effort estimates. If you just need
to know what's changing and who's doing what, read `goonj-demand-avni-registration-summary.md`
instead — this document goes a lot deeper than most people will need.

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

## How the new flow will work

```
   Avni                                          Salesforce
  ┌────────────┐                                ┌────────────┐
  │  Demand    │  not linked yet, and           │  record    │
  │  created   │  verified → send to SF ──────► │  created,  │
  │  in Avni   │                                │  Demand    │
  │  by field  │  ID + Demand Code come back,   │  Code      │
  │  team      │ ◄── saved onto same record ─── │  assigned  │
  └────────────┘                                └────────────┘
        ▲                                              │
        │       status changes later                   │
        └──── (Approved, DA, DV, DPV) ◄─────────────────┘
                (reuses the sync that already exists today)
```

A field team fills in the Demand form in Avni. Once the Data POC ticks "Verified," that's the
signal for this service to send the Demand to Salesforce — but only if it hasn't been sent
already. Salesforce creates the record, generates the Demand Code, and hands back its own ID for
that record. We save that ID onto the Avni record. From that point on, the existing
Salesforce-to-Avni sync just takes over: any status change — Approved, Demand Assignment, Demand
Validation, Demand Post-Validation — flows down into the same Avni record automatically.

Sending the Demand, getting the ID back, and saving it are meant to happen as one exchange rather
than three separate trips. That's not confirmed yet, though — it's possible Salesforce only
returns the ID on a later sync instead of in its immediate response, which would mean the Demand
Code takes an extra cycle to show up. That's one of the open questions further down, and it's
worth settling before anyone starts building the piece of code that talks to Salesforce.

Once that ID has been saved, this service never sends that Demand to Salesforce again. It's a
one-time creation, not an ongoing two-way mirror — after the initial push, everything flows in one
direction only, from Salesforce down into Avni. That's a deliberate choice, and the reasoning for
it is below.

---

## Where the Demand Code comes from

Every Demand Code looks like `2026/ACC-270688/ASM/383` — four pieces stitched together: the year,
a code for the account, a code for the initiative, and a running number.

Avni simply cannot generate this code, and shouldn't try to. That last number is a counter that
only Salesforce can keep track of — Avni has no way of knowing the previous Demand for that
account was number 382. So the code always gets created in Salesforce and copied back afterwards.

In practice, this means someone who registers a Demand in Avni won't see a code right away. It
only appears once the round trip described above finishes. Field teams need to be told this in
advance, or it'll look like something's broken.

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
| We only push a Demand once — when it has no Salesforce ID yet | The moment a Demand carries a Salesforce ID, we stop sending it. Create-only, never an ongoing sync back up. |
| Corrections happen in Salesforce, not Avni | Once a Demand exists there, that's where fixes are made. Avni-side edits don't travel back. |
| Users can keep editing in Avni until approval reaches their device | No instant lock — takes effect only once Approved has synced down. Leaves an editable window, detailed below. |
| Duplicate-Demand checking happens in Salesforce | Not in Avni. |
| Registration starts switched off for everyone | Turned on only for the specific groups that need it — same switch controls when the feature goes live. |
| Identify a Demand by its Avni ID if we have one, else by the Salesforce Demand ID | Avni-originated Demands always have the former; Salesforce-originated ones only have the latter. |

Coordinating the actual deployment across Salesforce, this service, and Avni is also part of what
we've agreed, but that's a rollout question rather than a sync-behaviour one — it has its own
section near the end of this document.

### A window where edits in Avni quietly go nowhere

A few of the rules above interact in a way that's easy to miss, so it's worth spelling out
explicitly — this is a deliberate trade-off, not something anyone should stumble on later and
mistake for a bug.

The window opens the moment the Demand Code comes back from Salesforce, and closes once the
Approved status reaches the device:

```
  user verifies ──► pushed to SF ──► Demand Code ──────────────► Approved status
                                     comes back                  reaches device
                                          │                            │
                                          └──── editable window ───────┘
                                          edits allowed here, but they
                                          never reach Salesforce
```

During that window, a user can absolutely edit the Demand in Avni — but the edit will never reach
Salesforce, because the push only fires the first time, and by now it's already happened. Worse,
the next sync from Salesforce will overwrite the Avni record with Salesforce's version, and the
user's edit just disappears.

That's consistent with corrections belonging in Salesforce, and the decision has been made
deliberately: the lock keys off the approval status arriving, not off the Demand Code appearing.
Because of that, the window is real, and it stays. What this means for whoever builds the Avni
form: it should visibly warn the user during this window, rather than letting people discover on
their own that a change they made simply vanished.

Before the window opens — meaning between verifying and the Demand Code actually coming back —
editing is perfectly safe. The Demand hasn't been linked to Salesforce yet, so the next sync
simply pushes the edited version, and Salesforce's upsert behaviour makes sure it updates the same
record rather than creating a second one. No warning needed there.

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
| Locked after approval — no edits, no line-item, quantity or material changes | Not instant — takes effect once Approved syncs down to the device. Before that, edits are allowed but won't reach Salesforce (the editable window above). Enforced by an Avni form rule, not by this service. |
| Status changes must show on the dashboard as a Demand moves through approval | Already handled by the existing Salesforce-to-Avni sync. No new code — just needs covering in testing. |
| Warn on duplicate Demands | Moved entirely to Salesforce. Avni performs no check of its own. |
| Resubmitting after a failure must not create a second Demand Code | Guaranteed by Salesforce's upsert behaviour, not by any retry logic on our end. |

### Two things that sound similar but aren't

It's easy to conflate "verified" and "approved" — they're different steps on opposite sides of the
sync:

| | Verified by Data POC | Demand Status |
|---|---|---|
| Set by | The Chapter Data POC / Field Supervisor, in Avni | Salesforce, synced down |
| Editable in Avni? | Yes — a checkbox | No — read-only |
| When | Before the Demand reaches Salesforce | After Salesforce has processed it |
| Controls | Whether the push fires | The edit lock |

Avni never approves anything itself — approval happens entirely in Salesforce, and Avni only
receives the result. That's what makes the approval-based lock possible at all, and it's exactly
what creates the editable window, since the Salesforce ID arrives on one sync and the approval on
a later one.

---

## What actually needs to be built

| Piece | What it does | Precedent to build from |
|---|---|---|
| A watcher | Polls Avni every cycle for new Demand records | Same pattern as the existing Distribution watcher |
| A push gate | Only pushes when the Demand has no Salesforce ID yet and is verified | Simple two-part check, nothing new architecturally |
| A field converter | Avni fields → Salesforce fields, including the account and initiative values the Demand Code needs | Distribution's converter does almost exactly this |
| A line-item converter | The form's repeating second section — material type, kit type, quantity, sub-types | Both Dispatch and Distribution already handle repeating line items |
| An address converter | Shapes the Avni address the way Salesforce expects | The reverse direction (reading an address out of Salesforce) already exists |
| The call to Salesforce | Sends the converted data to the new endpoint | Calling pattern exists; the endpoint itself doesn't — Goonj has to build it |
| ID write-back | Saves Salesforce's ID onto the Avni record, linking it and stopping further pushes | Genuinely new — no existing example in this codebase |
| Identifier precedence | Avni ID if present, else the Salesforce Demand ID | New logic |
| A separate progress marker | This direction needs its own "where did I leave off," independent of the existing pull | Small, low-risk addition |

The Salesforce endpoint is the biggest external dependency — nothing here can be proven working
until it exists, so that conversation with Goonj should start early. On our side, the ID
write-back matters most: miss it, and at best we harmlessly re-check a Demand every cycle; at
worst, the next status update from Salesforce has nothing to match against and creates a duplicate
record in Avni.

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
- Deploy their endpoint first, before we deploy anything.

**On the Avni side, the form work involves:**

- Building the Demand form itself, with all its fields.
- Building the line-items section, including the kit sub-types.
- Field validations — dates in the right order, quantities that make sense, fields that only
  appear conditionally.
- The Verified checkbox itself, visible only to the right role, and recording who verified it.
- The edit rule that keys off the approval status, plus the warning shown during the editable
  window.
- Making sure registration starts switched off, and can be turned on only for the intended roles.

**And the integration code involves:**

- The watcher and the push gate.
- The two converters — one for the Demand's own fields, one for its line items — plus the address
  conversion.
- The call to Salesforce, and writing its ID back afterward.
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

**Which user groups get access to Demand registration, and what exactly can each one do — create,
edit, void?** Registration starts off for everyone by default, so we need the specific list of
groups and what each is allowed to do before anything gets switched on. Voiding is worth a
separate thought here too: the requirement sheet allows voiding a duplicate Demand, but explicitly
bars deleting one once it's been approved, so the answer for "who can void" may not be the same
before and after approval.

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

**The edit lock keys off the approval status, not the Demand Code** — see the editable window
above.

---

## Test scenarios

These are the situations that should demonstrably work — or fail safely — before this feature is
considered done.

| # | Scenario | Setup | Action | Expected result |
|---|---|---|---|---|
| 1 | A new Avni Demand successfully reaches Salesforce | A field team fills in the Demand form in Avni | The Data POC verifies it, and the sync cycle runs | A matching Demand appears in Salesforce with every field correctly mapped |
| 2 | The Demand Code comes back to Avni | The Demand from #1 now exists in Salesforce with a code | The next sync cycle runs | The same Avni Demand — not a new one — now shows the Demand Code and its Salesforce link |
| 3 | A status change doesn't create a duplicate | The Demand from #1–2 is already linked | Salesforce moves its status to Approved | The existing Avni record updates in place; the total number of Demands in Avni doesn't increase |
| 4 | An unverified Demand isn't sent anywhere | A Demand is filled in but the Data POC hasn't verified it | A sync cycle runs | Nothing is sent to Salesforce; the Demand simply stays Pending in Avni |
| 5 | A linked Demand is never pushed again | A Demand already has a Salesforce link | Sync cycles run repeatedly | The push never fires again for that Demand — no repeat calls to Salesforce at all |
| 6 | A failed write-back recovers cleanly | The push to Salesforce succeeds, but saving the ID back onto Avni fails | The next cycle pushes the same Demand again | Salesforce recognises it and updates the existing record — no second Demand, no second code |
| 7 | Resubmitting after a dropped connection is safe | A user submits, loses connection before confirmation, and submits again | Both attempts reach Salesforce | Exactly one Demand and one Demand Code exist in Salesforce |
| 8 | Editing before verification works as expected | A Demand exists in Avni but hasn't been verified yet | A user edits a field and then verifies it | The version sent to Salesforce is the edited one, not the original |
| 9 | Editing inside the window has consequences | A Demand is already linked, but its approval hasn't reached the device yet | A user edits a field in Avni | The edit is allowed locally, never reaches Salesforce, and gets overwritten by the next sync — the form should be showing a warning throughout this |
| 9a | Editing before the window opens is safe | A Demand has been verified but its code hasn't come back yet | A user edits a field, then a sync cycle runs | The edited version is what reaches Salesforce, and it updates the same record — no duplicate |
| 10 | Editing after approval is blocked | The approved status has reached the device | A user tries to change quantity or material | The edit is blocked by the form itself |
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

---

## The order this needs to happen in

Getting this sequence wrong means either Demands pile up in Avni with nowhere to go, or Salesforce
starts receiving calls it isn't ready to handle.

**First, agree.** Settle the open questions above with Goonj — the Salesforce endpoint has the
longest lead time of anything here, so start that conversation immediately. Get the final
picklists sorted too, since building the form or the field mapping ahead of that just means
redoing the work later.

**Then build, on both sides at once.** Goonj builds their Salesforce endpoint. We build the Avni
form — with registration deliberately left switched off — and we build the integration code
alongside it. All three can happen in parallel, and our integration code can be tested against a
stand-in without waiting for Goonj's endpoint to exist.

**Then test, in stages.** Start against a stand-in, before Goonj's real endpoint is ready, for
everything that doesn't need Salesforce itself — the unverified case, the never-pushed-again case,
editing before verification, permissions, and unmapped values. Once the real endpoint exists on a
staging Salesforce org, run the rest — the happy path, the Demand Code coming back, duplicate
protection, resubmission, editing inside and outside the window, blocked edits after approval,
outage handling, identifier precedence, and the code format itself. Separately, confirm nothing
about the existing Salesforce-to-Avni path has broken. Finally, walk the full loop end to end on
staging before anything touches production.

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
| Integration code | ~5.5 |
| QA, UAT and go-live | ~8.5 |
| **Total** | **~17** |

**Form and configuration.** The form itself is the largest piece, mostly because of the longer
picklists — kit type, target community, disaster type. The line-items section, validations, the
verification checkbox, the edit rule, the warning, and the role gating are each smaller pieces on
top. Ordinary Avni admin work — creating users, roles, or locations — isn't part of this estimate;
Goonj's own tech team handles that directly.

**Integration code**, assuming it's built with AI assistance rather than typed by hand — these are
review-and-check times, not typing times. The field converter is the largest piece, mostly because
the time goes into verifying each field against what Salesforce actually expects, not into writing
the conversion itself. This doesn't compress much further: generating code is fast, but reviewing
it and discovering how an external system actually behaves both take the same time regardless of
how quickly the code appeared. The existing Goonj sync makes the point on its own — its largest
source of errors is address mismatches, which no amount of faster coding would have prevented.

**QA, UAT, and go-live** — end-to-end testing on a real Salesforce org, a regression pass on the
existing sync, a contingency buffer for bugs, supporting Goonj and partner teams through UAT,
absorbing whatever feedback comes out of it, the coordinated go-live, and a bit of support
immediately afterward. UAT feedback is the least predictable line in this whole estimate.

Development itself depends on when Salesforce's endpoint becomes available, and when the form and
other details are settled enough to actually start building.
