# JSS Final Form List — Status

Source: the final form list shared by the JSS team ([Google Sheet](https://docs.google.com/spreadsheets/d/1ZRTp0mB2xPG5vlQMetj6Tp35jS9pQS2kBvV1I1tS7f0/edit?gid=2110362113#gid=2110362113)). 33 forms after removing duplicates.

**Generated** by `scripts/build_jss_avni_bundle.py` from `JSS Bahmni integration/final_forms.csv` — edit that file, not this one. Usage figures are from the prerelease Bahmni copy (JSS data up to Dec 2025), so production may hold more.

| # | Bahmni form | Status | Notes |
|---|---|---|---|
| 1 | REGISTRATION_CONCEPTS | ✅ In the Avni bundle | Recorded inside "Mobile clinic - Registration second page" (138 times, last 2016) - synced from there |
| 2 | Visit Diagnoses | ✅ In the Avni bundle |  |
| 3 | Blood Pressure | ✅ In the Avni bundle | Always recorded inside Vitals - its data appears in the Bahmni - Vitals visit; this form stays empty |
| 4 | Discharge Summary | ✅ In the Avni bundle |  |
| 5 | Discharge Summary, Surgeries and Procedures | ✅ In the Avni bundle | Always recorded inside Discharge Summary - its data appears in the Bahmni - Discharge Summary visit; this form stays empty |
| 6 | death template | ✅ In the Avni bundle |  |
| 7 | Endoscopy | ⛔ Not built | No questions in the Bahmni dump (only procedure names) and never recorded on prerelease |
| 8 | Advanced Procedure | ⛔ Not built | No questions in the Bahmni dump (only procedure names) and never recorded on prerelease |
| 9 | Normal Procedure | ⛔ Not built | No questions in the Bahmni dump (only procedure names) and never recorded on prerelease |
| 10 | Lab Samples | ⛔ Not built | Never recorded on prerelease (2013-2025); lab results sync through All_Tests_and_Panels |
| 11 | All_Tests_and_Panels | ✅ In the Avni bundle | Lab results arrive as flat obs on the LAB_RESULT encounter (LabEncounterType mapping) |
| 12 | Obstetrical History | ✅ In the Avni bundle | Recorded inside "OBS/GYN Common" (101 times, last 2014) - synced from there |
| 13 | Obstetrics, P/V (per vaginal) | ✅ In the Avni bundle | Recorded inside "Obstetrics" (4 times, last Dec 2023) - synced from there |
| 14 | Obstetrics, P/A (per abdomen) | ✅ In the Avni bundle | Recorded inside "Obstetrics" (365 times, last Dec 2023) - synced from there |
| 15 | Ante Natal Care Program, Outcomes | ⛔ Not built | Never recorded on prerelease and the dump has no questions for it - needs a fresh Bahmni export if JSS confirm prod has data |
| 16 | Diabetes Counselling Template | ✅ In the Avni bundle |  |
| 17 | Diabetes Intake Template | ✅ In the Avni bundle |  |
| 18 | CDST Forms | ✅ In the Avni bundle | Doctors fill RMRCT Form / RNTCP Form on their own (3,462 + 490 visits) Also synced from: RMRCT Form, RNTCP Form. |
| 19 | Dose Quantity Units | ✅ Covered by Bahmni - Medication | Prescription drop-down values - prescriptions sync as one readable line in Bahmni - Medication (agreed Oct 2026, route/instructions not included) |
| 20 | Dosing Units | ✅ Covered by Bahmni - Medication | Prescription drop-down values - prescriptions sync as one readable line in Bahmni - Medication (agreed Oct 2026, route/instructions not included) |
| 21 | Drug Routes | ✅ Covered by Bahmni - Medication | Prescription drop-down values - prescriptions sync as one readable line in Bahmni - Medication (agreed Oct 2026, route/instructions not included) |
| 22 | Duration Units | ✅ Covered by Bahmni - Medication | Prescription drop-down values - prescriptions sync as one readable line in Bahmni - Medication (agreed Oct 2026, route/instructions not included) |
| 23 | Dosing Instructions | ✅ Covered by Bahmni - Medication | Prescription drop-down values - prescriptions sync as one readable line in Bahmni - Medication (agreed Oct 2026, route/instructions not included) |
| 24 | Other Diagnoses | ✅ Synced as answers | Not a form - list of diagnoses; they sync as answers of Visit Diagnoses > Coded Diagnosis (all Diagnosis-class concepts) |
| 25 | OPD Information | ✅ In the Avni bundle |  |
| 26 | Referral Form, Summary | ✅ In the Avni bundle | Recorded inside "Referral Form" (1,221 times, last Dec 2025) - synced from there; other Referral Form parts not on the list |
| 27 | Surgeries and Procedures | ✅ In the Avni bundle | Doctors fill Operation Notes Template on its own (30,963 visits) Also synced from: Operation Notes Template. |
| 28 | Nutritional Values | ✅ In the Avni bundle |  |
| 29 | General Patient Template | ✅ In the Avni bundle | Doctors fill ECG Report / Spirometry Report on their own (27,294 + 86 visits) Also synced from: ECG Report, Spirometry Report. |
| 30 | History and Examination | ✅ In the Avni bundle |  |
| 31 | Vitals | ✅ In the Avni bundle |  |
| 32 | Birth Template | ✅ In the Avni bundle |  |
| 33 | Community Program Referral Form | ✅ In the Avni bundle |  |

## What the statuses mean

| Status | Meaning |
|---|---|
| ✅ In the Avni bundle | Avni form + visit type created; Bahmni visits for this form sync to Avni |
| ✅ Covered by Bahmni - Medication | Drop-down values inside a prescription; prescriptions sync as one readable line per medicine |
| ✅ Synced as answers | Not a form; its values are the answer options of another form's question |
| ⛔ Not built | Cannot be built from the current Bahmni dump, or never recorded. Revisit if JSS confirm production has data for it |

## Also in the bundle (not on the list, needed by the sync)

| Avni form | Why |
|---|---|
| Bahmni Patient Registration | Bridge record linking an Avni individual to a Bahmni patient |
| Bahmni - Medication | Receives prescriptions (drug orders) |
