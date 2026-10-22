# Creating Bahmni Patients from Avni (planned — after go-live)

**Status:** not switched on. To be confirmed with the JSS team before deployment.

Today the integration never creates anyone: it only links people who already exist in both systems (see [01-sync-design.md](01-sync-design.md)). Once ID seeding on production is complete, a small number of JSSCP individuals will still have no Bahmni ID. This document is the plan for creating those people as patients in Bahmni, so their hospital visits can sync too.

---

## When a patient would be created

| Avni "Individual ID Update" says | What happens |
|---|---|
| Registered in Bhamni? = **Yes** + Bahmni ID | Link to the existing patient (live today) — never create |
| Registered in Bhamni? = **Not yet** | **Create the patient in Bahmni** (this plan), then link |
| No ID Update form filled | Nothing — the person is not part of the integration yet |

After creation, the new Bahmni ID is written back into the person's ID Update form in Avni ("Registered in Bhamni?" = Yes), so from then on they are an ordinary linked person.

---

## What already exists in the code

The original integration can already create a Bahmni patient from an Avni person (`SubjectWorker` → `PatientService.createPatientAndSubject`). It is switched off for JSS through the jobs setting (`BAHMNI_INT_APP_TASKS`), not commented out.

| Sent to Bahmni today | Source in Avni |
|---|---|
| First name, last name | Registration |
| Date of birth | Registration |
| Gender | Registration |
| Patient ID | `GAN` + an Avni ID field — **wrong for JSS** (see below) |
| ID type, location | Integration settings |

---

## What has to change for JSS

| # | Gap | Proposal | To confirm with JSS |
|---|---|---|---|
| 1 | **Which clinic the patient belongs to** (GAN, SEM, SIV, BAM, EME) | Derive from the person's **cluster** in Avni through a small mapping table (below) | The table |
| 2 | **Who gives out the new Bahmni ID** | Ask Bahmni to generate the next ID for that clinic (Bahmni's own ID generator), instead of building it from an Avni number | Is each clinic an ID source in Bahmni? Any reserved ranges? |
| 3 | **Write the new ID back to Avni** | Update the person's latest ID Update form: Bahmni ID = new ID, Registered = Yes | OK to edit the form, or add a new ID Update visit? |
| 4 | **Never create duplicates** | Only for "Not yet"; check Bahmni first for a patient with the same name + DOB + village and flag instead of creating | What counts as "same person"? |
| 5 | **Fields Bahmni requires** | Send whatever the Bahmni registration screen requires | List of mandatory fields (address? phone? father/husband name? age if DOB unknown?) |
| 6 | **Which location/visit the registration belongs to** | Registration encounter at the clinic of the patient | Location per clinic |

### Cluster → clinic (checked against JSSCP_UAT_Org, Oct 2026)

The first three letters of the cluster name work for only two clusters, so an explicit table is safer:

| Avni cluster | First 3 letters | Bahmni prefix | Proposed clinic |
|---|---|---|---|
| Bamhani | BAM | ✅ BAM | BAM |
| Semaria | SEM | ✅ SEM | SEM |
| Shivtarai | SHI | ❌ | **SIV?** |
| Achanakmar | ACH | ❌ | **?** |
| Non programme cluster | NON | ❌ | **? (GAN?)** |
| Preventive Wellness Care Cluster | PRE | ❌ | **?** |
| testcluster | TES | ❌ | Ignore |
| — | — | GAN (Ganiyari), EME | Not clusters — hospital / other centres |

---

## Effort (with AI-assisted development)

| Work | Estimate |
|---|---|
| Cluster → clinic table + ID generation from Bahmni | 1 day |
| Required fields + duplicate check | 1 day |
| Write the new ID back to Avni, plug into the link job's "Not yet" branch | 0.5 day |
| Tests on prerelease (new patients in Bahmni, then their visits syncing back) | 1–1.5 days |
| **Total** | **3.5–4 days**, once the questions above are answered |

The link job being built now has a clear "Not yet" branch where this plugs in, behind its own on/off setting — so turning it on later changes nothing in the existing linking or visit sync.

---

## Questions for the JSS team (checklist for the deployment meeting)

- [ ] Cluster → clinic table (fill in the "?" rows)
- [ ] Does Bahmni generate IDs per clinic? Who owns the ID sequences?
- [ ] Mandatory registration fields in Bahmni, and where each comes from in Avni
- [ ] Rule for "same person already in Bahmni" (to avoid duplicates)
- [ ] Should the integration edit the existing ID Update form or add a new one when writing back the ID?
- [ ] Location to register new patients under, per clinic
- [ ] Roughly how many people are expected to need creation after seeding?
