# Bahmni-Avni Test Mapping (Prerelease Environment)

> **Note:** This is for testing purposes only. The Avni individuals are random mappings, not actual matches.

## Test Environment
- **Bahmni:** https://jss-bahmni-prerelease.avniproject.org/
- **Avni:** https://prerelease.avniproject.org/

## Patient Identifier Mapping

| Bahmni GAN ID | Bahmni Patient Name | Avni Individual UUID | Avni URL |
|---------------|---------------------|----------------------|----------|
| 279731 | Laxmi Prajapati | `9de1844b-1b96-42ea-94d2-5e4210c83919` | [View](https://prerelease.avniproject.org/#/app/subject?uuid=9de1844b-1b96-42ea-94d2-5e4210c83919) |
| 279732 | son bai suryvanshi | `e654d972-c9d0-4ffc-af7a-707db02e96c4` | [View](https://prerelease.avniproject.org/#/app/subject?uuid=e654d972-c9d0-4ffc-af7a-707db02e96c4) |
| 279733 | Hemant Singh Thakur | `2a84b87f-3bd3-4224-be01-3706ea1502a5` | [View](https://prerelease.avniproject.org/#/app/subject?uuid=2a84b87f-3bd3-4224-be01-3706ea1502a5) |
| 279734 | sulochana gond | `f9be92d6-2545-4b01-ba21-ce47d641d2d9` | [View](https://prerelease.avniproject.org/#/app/subject?uuid=f9be92d6-2545-4b01-ba21-ce47d641d2d9) |
| 279735 | durgeshwari marko | `8f6d0432-8570-406b-9de8-ba9cbb167d85` | [View](https://prerelease.avniproject.org/#/app/subject?uuid=8f6d0432-8570-406b-9de8-ba9cbb167d85) |
| 279736 | Nirmla bai Vishvkrma | `a944149c-0be8-4ccc-9036-2f5f69cac17c` | [View](https://prerelease.avniproject.org/#/app/subject?uuid=a944149c-0be8-4ccc-9036-2f5f69cac17c) |

## Bahmni Patient Details (Reference)

### GAN279731
- **UUID:** 1da4aa76-97f3-4656-8bb1-fe6b3d748cb0
- **Name:** Laxmi Prajapati
- **Gender:** F
- **DOB:** 1990-10-18

### GAN279732
- **UUID:** 3192824d-ec16-4e69-8478-e38aa05ec480
- **Name:** son bai suryvanshi
- **Gender:** F
- **DOB:** 1954-09-30

### GAN279733
- **UUID:** 004131c1-8568-423c-802d-fb0e428e6c9b
- **Name:** Hemant Singh Thakur
- **Gender:** M
- **DOB:** 1988-09-30

### GAN279734
- **UUID:** 7c5280ff-2fd3-488f-b2ef-ded6f4ce99ae
- **Name:** sulochana gond
- **Gender:** F
- **DOB:** 1974-09-30

### GAN279735
- **UUID:** f75c1d44-d0f2-4c6a-803b-96edde4b67ca
- **Name:** durgeshwari marko
- **Gender:** F
- **DOB:** 1996-09-30

### GAN279736
- **UUID:** 5c440ffe-a7a9-42b9-96a4-5418b750dd96
- **Name:** Nirmla bai Vishvkrma
- **Gender:** F
- **DOB:** 1995-09-30

---

## Testing Checklist

- [ ] Created Avni individuals with Patient Identifier field
- [ ] Ran findPatient test
- [ ] Ran findSubject test
- [ ] Tested Avni → Bahmni sync (SubjectWorker)
- [ ] Tested Bahmni → Avni sync (PatientWorker)

---
*Created: 2026-02-17*
