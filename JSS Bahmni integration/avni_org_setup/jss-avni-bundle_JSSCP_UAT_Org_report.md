# JSS Avni bundle report (JSSCP_UAT_Org)

- Forms built: 22 + Bahmni - Medication + bridge setup
- Question concepts: 552, answer-only concepts: 2325
- Encounter-type mappings: 26

## Forms

| Avni form | Questions | Multi-select |
|---|---|---|
| Bahmni - REGISTRATION_CONCEPTS | 2 | 0 |
| Bahmni - Visit Diagnoses | 4 | 0 |
| Bahmni - Blood Pressure | 5 | 0 |
| Bahmni - Discharge Summary | 20 | 0 |
| Bahmni - Discharge Summary, Surgeries and Procedures | 2 | 0 |
| Bahmni - Death Template | 5 | 0 |
| Bahmni - All_Tests_and_Panels | 389 | 2 |
| Bahmni - Obstetrical History | 5 | 0 |
| Bahmni - Obstetrics, P/V (per vaginal) | 6 | 0 |
| Bahmni - Obstetrics, P/A (per abdomen) | 5 | 0 |
| Bahmni - Diabetes Counselling Template | 7 | 0 |
| Bahmni - Diabetes Intake Template | 13 | 2 |
| Bahmni - CDST Forms | 22 | 2 |
| Bahmni - OPD Information | 1 | 0 |
| Bahmni - Referral Form, Summary | 2 | 1 |
| Bahmni - Surgeries and Procedures | 23 | 4 |
| Bahmni - Nutritional Values | 2 | 0 |
| Bahmni - General Patient Template | 14 | 0 |
| Bahmni - History and Examination | 12 | 0 |
| Bahmni - Vitals | 17 | 0 |
| Bahmni - Birth Template | 16 | 0 |
| Bahmni - Community Program Referral Form | 3 | 0 |
| Bahmni - Medication | 1 | 0 |

## Multi-select questions

- Bahmni - Albumin (Routine Urine)
- Bahmni - Anesthesia Administered
- Bahmni - CDST Form, Drugs used in his treatment
- Bahmni - Coded Operating Assistant Surgeon
- Bahmni - Coded Operating Surgeon
- Bahmni - Coded Operation Anesthetist
- Bahmni - Diabetes, Complaint
- Bahmni - Diabetes, Complications
- Bahmni - RNTCP, Testing centre
- Bahmni - Sugar (Routine Urin)
- Bahmni - Summary, Advice

## Not built (need a decision)

- Endoscopy (not-built: No questions in the Bahmni dump (only procedure names) and never recorded on prerelease)
- Advanced Procedure (not-built: No questions in the Bahmni dump (only procedure names) and never recorded on prerelease)
- Normal Procedure (not-built: No questions in the Bahmni dump (only procedure names) and never recorded on prerelease)
- Lab Samples (not-built: Never recorded on prerelease (2013-2025); lab results sync through All_Tests_and_Panels)
- Ante Natal Care Program, Outcomes (not-built: Never recorded on prerelease and the dump has no questions for it - needs a fresh Bahmni export if JSS confirm prod has data)
- Dose Quantity Units (covered: Prescription drop-down values - prescriptions sync as one readable line in Bahmni - Medication (agreed Oct 2026, route/instructions not included))
- Dosing Units (covered: Prescription drop-down values - prescriptions sync as one readable line in Bahmni - Medication (agreed Oct 2026, route/instructions not included))
- Drug Routes (covered: Prescription drop-down values - prescriptions sync as one readable line in Bahmni - Medication (agreed Oct 2026, route/instructions not included))
- Duration Units (covered: Prescription drop-down values - prescriptions sync as one readable line in Bahmni - Medication (agreed Oct 2026, route/instructions not included))
- Dosing Instructions (covered: Prescription drop-down values - prescriptions sync as one readable line in Bahmni - Medication (agreed Oct 2026, route/instructions not included))
- Other Diagnoses (answers: Not a form - list of diagnoses; they sync as answers of Visit Diagnoses > Coded Diagnosis (all Diagnosis-class concepts))

## Warnings

- [General Patient Template] 'Image' has datatype Complex - cannot be synced, skipped
- [History and Examination] 'Image' has datatype Complex - cannot be synced, skipped
