# Let Partners Register "Demand" in Avni (not just Salesforce)

## In one sentence

Today a Demand can only be created in Salesforce, and this service copies it into Avni. Some
partner teams are losing their Salesforce logins, so they need a way to create a Demand starting
from Avni instead — and once they do, that Demand still needs to travel to Salesforce, get
processed there, and any updates need to travel back.

---

## 1. Some background, in plain terms

- **Avni** — the mobile/web data-collection app field teams use.
- **Salesforce (SF)** — Goonj's system of record for Demand. This is where Demand Codes get
  generated and where the approval workflow (DA → DV → DPV) happens.
- **This repo (integration-service)** — a middleman. It doesn't store data itself; it just moves
  records between Avni and Salesforce on a timer (currently every few minutes).
- **"Sync"** — the act of copying a record from one system to the other. It is one-directional per
  piece of code: something either reads *from* Salesforce and writes *into* Avni, or reads *from*
  Avni and writes *into* Salesforce. There is no code today that does both directions for the same
  entity.
- **`externalId`** — every Demand stored in Avni carries the Salesforce record's ID as a hidden
  tag. This is how the sync knows "this Avni record and that Salesforce record are the same
  Demand" instead of creating duplicates every time it runs.
- **Avni source ID** — the flip side of the same idea: every Avni record has its own permanent ID
  (a UUID). We send this to Salesforce so Salesforce can recognise "I have already seen this exact
  Avni record before." This becomes important in Section 5.

---

## 2. How Demand sync works today (one-way only)

```
   Salesforce                                    Avni
  ┌───────────┐   1. "any new/changed          ┌───────────┐
  │  Demand    │      Demands since last time?" │  Demand    │
  │  created   │ ─────────────────────────────► │  subject   │
  │  here      │   2. copy each one into Avni,  │  appears   │
  │            │      tagged with Salesforce's  │  here      │
  │            │      ID as externalId          │            │
  └───────────┘                                 └───────────┘
```

- This service asks Salesforce, on a schedule: *"give me anything new or changed since I last
  asked."*
- Each Demand that comes back becomes a "Demand" record (Avni calls these **Subjects**) in Avni,
  tagged with Salesforce's ID.
- If Salesforce sends the *same* Demand again later (say, its status changed to "Approved"), it
  updates the *same* Avni record — because the `externalId` tag matches. No duplicate is created.
- Nothing currently goes the other way. If you tried to create a Demand starting in Avni today,
  it would sit in Avni forever — the code path to send it to Salesforce doesn't exist yet
  (confirmed by reading the code: it's a placeholder that throws an error if called).

**The good news:** this "other direction" already exists in this codebase for three other kinds of
records (Distribution, Activity, Dispatch Receipt). We don't have to invent the pattern — we
reuse it.

---

## 3. What needs to happen for the new flow

```
   Avni                                          Salesforce
  ┌───────────┐  3. Demand ID + Code come back  ┌───────────┐
  │  Demand    │ ◄─────────────────────────────  │  Demand    │
  │  created   │      and get saved onto the      │  created   │
  │  here by   │      same Avni record            │  (upsert), │
  │  field     │                                  │  Demand    │
  │  team      │  1. no externalId yet +          │  Code      │
  │            │     verified → send to SF  ───► │  assigned  │
  └───────────┘                                 └───────────┘
         ▲                                              │
         │        2. status changes later               │
         └───────── (Approved, DA, DV, DPV) ◄────────────┘
              (this reuses the EXISTING one-way sync from Section 2)
```

In words:

1. **A field team creates a Demand directly in Avni** (new capability — filling in the form
   described in the shared spreadsheet).
2. **This service picks it up and sends it to Salesforce** — but only when *both* are true: the
   Demand has **no `externalId` yet**, and it has been **verified by the Data POC**. *(New code —
   doesn't exist today.)*
3. **Salesforce creates the record and generates a Demand Code**, then hands back its Demand ID.
4. **This service writes Salesforce's ID back onto the Avni record** as the `externalId` tag.
   *(New — nothing in the existing code writes an ID back onto Avni after a push.)*
5. **From then on, the existing Salesforce → Avni sync (already built) takes over** — when
   Salesforce later changes the status to Approved/DA/DV/DPV, it flows back into the *same* Avni
   record, because the tag now matches.

**Once step 4 has happened, this service never pushes that Demand to Salesforce again.** The push
is a one-time "create", not an ongoing two-way mirror. Everything after creation flows in one
direction only: Salesforce → Avni. This is a deliberate decision — see Section 5.

---

## 4. Demand Code — how Salesforce numbers a Demand

From the spreadsheet's "Demand Code — Auto-Generation Logic" sheet.

Sample code: **`2026/ACC-270688/ASM/383`** — four segments joined together:

| Segment | Example | Meaning | Where it comes from |
|---|---|---|---|
| Year | `2026` | Year the Demand was created | Today's date at submission time |
| Account Code | `ACC-270688` | Code of the selected Account | Whatever the user picked in "Name of Account" |
| Initiative Code | `ASM` | Initiative / region code | Derived from "Type of Initiative" (exact mapping still TBC per the sheet) |
| Sequence Number | `383` | Running counter | Auto-increments **per account**, not globally |

**Avni cannot generate this code, and must not try.** The sequence number is a running counter
that only Salesforce can maintain — Avni has no way to know that the last Demand for this account
was number 382. So the Demand Code is always created in Salesforce and copied back to Avni
afterwards.

**What this means in practice:** a user who registers a Demand in Avni will **not see a Demand
Code straight away.** It appears only after the round-trip in Section 3 completes (push to SF →
code generated → next sync brings it back). The field teams need to be told this up front, or it
will look like a bug.

**What we have to get right on our side:** the Account Code and Initiative Code are both derived
from fields the user fills in *inside Avni*. Our Avni → Salesforce converter (Section 7, item 3)
must send those across correctly, or Salesforce cannot build the code at all.

---

## 5. The agreed rules (from the design discussion)

These are the decisions taken about *how* to make this work safely. Most of them exist to answer
one question: **what stops the same Demand being created twice, or being edited in two places at
once?**

| # | Rule | What it means in practice |
|---|---|---|
| 1 | **Demand Code comes from Salesforce only** | Avni can't generate it (needs the counter). See Section 4. |
| 2 | **Salesforce's new API must be an "upsert", keyed on the Avni source ID** | "Upsert" = *update if it already exists, otherwise create*. Salesforce treats the Avni source ID as the uniqueness key, so if we send the same Avni record twice, Salesforce updates the existing record instead of making a second one. Salesforce may also choose to **ignore** the repeat update entirely. |
| 3 | **We only push when `externalId` is absent** | Once the Avni record carries a Salesforce ID, this service never sends it to Salesforce again. The push is create-only. |
| 4 | **Corrections are made in Salesforce, not Avni** | After a Demand exists in Salesforce, that's where fixes happen. Avni-side edits do not travel back (see the warning below). |
| 5 | **Users can still edit in Avni until the approval status reaches their device** | There is no instant lock. The "no editing after Approved" rule only takes effect once the Approved status has actually synced down to the device — so there is a window where a user can still edit a Demand that Salesforce has already approved. |
| 6 | **Duplicate-Demand checking happens on the Salesforce side** | Not in Avni. Salesforce decides whether a submitted Demand duplicates an existing one. |
| 7 | **Demand registration in Avni is switched off for every role by default** | Enforced with an Avni form rule. It gets switched on only for the specific partner roles that need it — and this same switch is what gates the staged rollout in Section 9. |
| 8 | **When we call Salesforce, identify the Demand by Avni source ID if we have one, otherwise by Demand ID** | Avni-created Demands always have a source ID; Salesforce-created ones only have a Demand ID. This precedence needs implementing — it's a new change. |

### ⚠️ The consequence worth flagging: Avni edits after sync are silently lost

Rules 3, 4 and 5 interact in a way that is easy to miss, so it should be a conscious decision
rather than a surprise:

- A user **can** edit a Demand in Avni after it has been sent to Salesforce (rule 5).
- Those edits **will never reach Salesforce** — the push only fires when `externalId` is absent,
  and by then it isn't (rule 3).
- The next Salesforce → Avni sync will **overwrite** the Avni record with Salesforce's version,
  so the user's edits disappear.

This is consistent with rule 4 (corrections belong in Salesforce), but from the user's seat it
looks like "I changed it and my change vanished." Worth deciding whether the Avni form should warn
or visually lock the record once a Demand Code is present, rather than leaving users to discover
this. Flagged as an open question in Section 8.

### Why rule 2 (upsert) makes everything else much safer

Because Salesforce matches on the Avni source ID, sending the same Demand twice is **harmless**.
That single decision covers several failure modes that would otherwise each need their own
defensive code:

- The push succeeds but writing the ID back to Avni fails → next cycle pushes again → Salesforce
  recognises the source ID and does **not** create a second Demand.
- The user resubmits after a network drop → same source ID → still one Demand, one Demand Code.
- We retry after a timeout where we never learned whether Salesforce succeeded → safe to retry.

Without the upsert, each of these would produce duplicate Demands with duplicate Demand Codes.

---

## 6. Flow rules from the spreadsheet — updated for the decisions above

From the spreadsheet's "Flow rules" sheet, reconciled with Section 5:

| Rule (as written in the sheet) | How it actually works given the decisions above |
|---|---|
| **Locked after Approval** — once "Approved"/DA, no edits, no line-item changes, no quantity or material changes; admin-only correction | The lock is **not instant**. It applies from the moment the Approved status syncs down to the device (rule 5). Before that arrives, the user can still edit — and those edits won't reach Salesforce (see the warning above). Enforced by the Avni **form**, not by this service. |
| **Status changes must show on the dashboard** — DA → DV → DPV, with the Demand Code visible | Satisfied by the existing Salesforce → Avni sync. No new code needed, but it must be covered in testing. |
| **Duplicate Demand** — warn if same Account + Initiative + Material/Kit + Required Date | **Moved to Salesforce** (rule 6). Avni does not perform this check. |
| **One Demand Code per submission** — resubmitting after a network failure must not create a second code | Guaranteed by the upsert on Avni source ID (rule 2), not by retry logic on our side. |

---

## 7. What we (this repo) need to build

| # | Piece | What it does | Is there an existing example to copy? |
|---|---|---|---|
| 1 | A "watcher" that polls Avni for Demands ready to send | Checks Avni every cycle for new Demand records, the same way it already does for Distribution | Yes — `DistributionWorker` |
| 2 | The push gate: **`externalId` absent AND verified by Data POC** | Decides whether a Demand should be sent to Salesforce at all (rules 3 + the spreadsheet's verification step) | New, but simple — a two-part `if` check |
| 3 | A converter: Avni fields → Salesforce fields | Turns the Avni record into the shape Salesforce expects, including Account and Initiative (needed for the Demand Code) | Yes — `DistributionRepository` does exactly this for Distribution |
| 4 | The call to Salesforce's new upsert endpoint | Sends the converted data over | Partially — the calling pattern exists, but **Salesforce needs a brand-new upsert endpoint keyed on Avni source ID**, which Goonj's Salesforce team must build. Today Salesforce only has an endpoint to *give us* Demands, not receive them. |
| 5 | Writing Salesforce's Demand ID back onto the Avni record | Sets `externalId`, which both links the record and stops further pushes (rule 3) | No existing example — this is the genuinely new piece |
| 6 | Identifier precedence: Avni source ID if present, else Demand ID | Used when building requests to Salesforce (rule 8) | New change |
| 7 | A separate "bookmark" for this new direction | Each sync direction tracks "where did I leave off" separately. The new direction needs its own marker rather than sharing the one the existing Salesforce → Avni sync already uses | Small new setting, low risk |

**The risky parts, in plain terms:**

- **Item 4 is the biggest external dependency.** We cannot finish this without Goonj's Salesforce
  developers building the upsert endpoint. Start this conversation first — it has the longest lead
  time.
- **Item 5 is the most important code we write ourselves.** If the `externalId` never gets written
  back, two things go wrong: we keep re-pushing the same Demand every cycle (harmless thanks to
  the upsert, but noisy), and — more seriously — the Salesforce → Avni sync has nothing to match
  on, so it creates a **duplicate Avni record** when the status changes.
- **Item 7** stops two unrelated jobs from overwriting each other's progress marker.

---

## 8. Questions still open

1. Who is building the Salesforce upsert endpoint, and by when?
2. Does Salesforce return the Demand Code **in the upsert response**, or only on a later sync?
   (Decides whether Avni can show the code after one cycle or two.)
3. Several picklists in the spreadsheet are marked "confirm with Goonj team" (Disaster Type,
   Target Community, the full Kit Type list). We need the final lists before building the field
   mapping.
4. **Should the Avni form warn or lock once a Demand Code is present?** Per the warning in Section
   5, users can currently edit into a void. A warning would prevent confused support tickets.
5. Which partner accounts specifically are moving to Avni-first registration? This determines who
   gets the form rule switched on (rule 7).
6. Confirm the exact intent of rule 7 — reading it as *"Demand registration is disabled for all
   roles by default and enabled per-role deliberately"*. Worth a sentence of confirmation since
   it's also the rollout switch.
7. When Salesforce "chooses to ignore" a repeat update (rule 2), should it tell us, or stay
   silent? Affects whether we log anything on our side.

---

## 9. Test scenarios

Situations we should be able to demonstrate working — or failing safely — before calling this
done. "SF" = Salesforce.

| # | Scenario | Setup | Action | Expected result |
|---|---|---|---|---|
| 1 | Happy path — Avni Demand reaches SF | Field team fills the Demand form in Avni | Data POC verifies it; sync cycle runs | A matching Demand appears in SF with all mapped fields correct |
| 2 | Demand Code comes back | Demand from #1 is in SF with a Demand Code | Next sync cycle runs | The **same** Avni Demand (not a new one) now shows the Demand Code and its `externalId` |
| 3 | No duplicate on status update | Demand from #1–2 is linked (`externalId` set) | SF moves status to Approved/DA | The existing Avni record updates; Avni's Demand count does not increase |
| 4 | Not verified — nothing sent | Demand filled in Avni, Data POC has **not** verified it | Sync cycle runs | Nothing is sent to SF; Demand stays Pending in Avni only |
| 5 | Already linked — never pushed again | Demand already has an `externalId` | Sync cycle runs repeatedly | The push is **skipped every time** (rule 3); no calls to SF's create endpoint |
| 6 | Upsert protects against a failed write-back | Push to SF succeeds but writing `externalId` back to Avni fails | Next sync cycle pushes the same Demand again | SF recognises the Avni source ID and updates the existing record — **no second Demand, no second Demand Code** (rule 2) |
| 7 | Resubmission after network drop | User submits, connection drops before confirmation, user submits again | Both submissions reach SF | Exactly one Demand and one Demand Code exist in SF |
| 8 | Edit before verification | Demand created in Avni, not yet verified | User edits a field, then verifies | The version sent to SF is the latest edited version, not the original |
| 9 | Edit after sync, before approval reaches device | Demand already pushed and linked | User edits a field in Avni | Edit is allowed locally (rule 5), is **not** sent to SF (rule 3), and is overwritten by SF's version on the next sync. Confirm this matches what Goonj expects users to experience |
| 10 | Edit after approval reaches device | Approved status has synced down to the device | User tries to change quantity/material | Edit is blocked by the Avni form rule |
| 11 | Duplicate Demand detected in SF | An existing SF Demand matches Account + Initiative + Material/Kit + Required Date | A near-identical Demand is pushed from Avni | SF applies its own duplicate handling (rule 6); Avni performs no duplicate check of its own |
| 12 | Role without permission cannot register | A user whose role has not been enabled for Demand registration | User opens Avni | No option to register a Demand (rule 7) |
| 13 | SF endpoint down or erroring | SF's upsert endpoint returns a validation failure, 500, or times out | Push runs | Demand is not lost — recorded as a retryable error using the same error-handling the other entities already use, and retried on a later cycle |
| 14 | Identifier precedence | One Avni-created Demand (has source ID, no Demand ID) and one SF-created Demand (has Demand ID) | Integration builds requests for each | Avni-created uses the **Avni source ID**; SF-created uses the **Demand ID** (rule 8) |
| 15 | SF-originated Demand unchanged | A Demand created directly in SF, no Avni involvement | Sync cycle runs | Appears in Avni exactly as it does today — the existing SF → Avni path must not regress |
| 16 | Partner keeping their SF licence | Partner still has SF access | Partner creates a Demand in SF as before | Unaffected; flows exactly as in Section 2 |
| 17 | Unmapped coded value | A picklist value in Avni has no mapping to an SF value | User submits a Demand using it | Fails with a clear "mapping not found" error rather than silently sending a wrong value — same as the existing `AnswerMappingNotFoundForCodedConcept` handling |
| 18 | Two sync directions don't clash | Both SF → Avni and Avni → SF have pending records in the same cycle | Job runs | Each direction processes its own records; neither overwrites the other's progress marker (Section 7, item 7) |
| 19 | Demand Code format | An Avni-created Demand has completed the round trip | Inspect the Demand Code | Matches `Year/AccountCode/InitiativeCode/Sequence` (e.g. `2026/ACC-270688/ASM/383`) and the sequence is unique per account |
| 20 | Dashboard status progression | A Demand moves DA → DV → DPV in SF | Sync runs after each change | Avni's dashboard shows it under the correct status each time, with the Demand Code visible |

---

## 10. Rollout — the order matters

Deployments must be **coordinated across three systems**, in this order. Getting the order wrong
means Demands pile up in Avni with nowhere to go, or Salesforce receives calls it can't handle.

1. **Settle the open questions in Section 8** with Goonj — especially the upsert endpoint, which
   has the longest lead time.
2. **Salesforce first.** Goonj deploys the new upsert endpoint (keyed on Avni source ID, with
   duplicate handling and Demand Code generation).
3. **Integration service second.** We deploy the watcher, converter, push gate, ID write-back and
   identifier precedence (Section 7). At this point nothing happens yet — no Avni user can create
   a Demand, because the form rule is still off for everyone (rule 7).
4. **Wait 1–2 days.** A deliberate buffer to confirm both deployments are stable before any real
   data starts flowing.
5. **Open up Demand registration in Avni last** — switch the form rule on for the intended partner
   roles.

**Testing along the way:**

- Against a stubbed/mock SF endpoint, before step 2: scenarios 4, 5, 8, 12, 17.
- Once the real endpoint exists: scenarios 1, 2, 3, 6, 7, 11, 13, 14, 19.
- Regression on the untouched SF → Avni path: scenarios 15, 16, 18, 20.
- Full end-to-end loop (1 → 2 → 3) on a staging Salesforce org before production.
