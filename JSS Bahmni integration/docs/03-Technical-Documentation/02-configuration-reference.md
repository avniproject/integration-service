# Avni-Bahmni Integration Configuration Reference

**Date Configured**: February 2026
**Test Environment**: JSS Ganiyari

---

## Quick Summary

The Avni-Bahmni integration is now **fully configured** for:
- ✅ Diabetes Intake sync (Bahmni → Avni)
- ✅ ANC Clinic Visit sync (Avni → Bahmni)

All UUIDs are configured and the mappings are ready to use.

---

## Configuration Values (as of Feb 2026)

### Constants (in `constants` table)

```
IntegrationBahmniLocation          58638451-1102-4846-8462-503e0ddd792f
IntegrationBahmniVisitType         2ee11869-f426-44a5-9766-1fb195a1c56f
IntegrationBahmniProvider          apiuser
IntegrationBahmniEncounterRole     apiuser
IntegrationBahmniIdentifierType    b46af68a-c79a-11e2-b284-107d46e7b2c5
BahmniIdentifierPrefix             GAN
IntegrationAvniSubjectType         Individual
```

### Core Mappings (Common Group)

| Type | Bahmni UUID | Avni Value | Purpose |
|------|---|---|---|
| AvniUUID_Concept | `3c474750-312b-4d79-b449-6e486ae7f34b` | Avni Entity UUID | Track Avni encounters in Bahmni |
| AvniEventDate_Concept | `94d9e354-8fe7-487d-9262-7807f76eb18c` | Avni Event Date | Store event dates from Avni |
| AvniUUID_VisitAttributeType | `7e9ed688-f2c1-46f2-b904-bc528dee335a` | Avni UUID | Track Avni UUIDs in visit attributes |
| AvniEventDate_VisitAttributeType | `7c24d353-718c-460f-8afc-3967461c8a01` | Avni Event Date | Store event dates in visit attributes |

### Program Enrolment Mappings

| Program | Visit Type UUID | Purpose |
|---------|---|---|
| ANC | `2ee11869-f426-44a5-9766-1fb195a1c56f` | Create Bahmni visits for ANC encounters |

**Note**: Using single visit type for all programs to simplify configuration.

---

## Deployment Checklist

### Pre-Deployment
- [ ] Bahmni concepts created (Avni Entity UUID, Avni Event Date)
- [ ] Bahmni visit attribute types created
- [ ] Bahmni forms updated with "Avni Entity UUID" concept
- [ ] Avni forms created with "Bahmni Entity UUID" field

### Deployment
- [ ] Run SQL migrations: `ANCVisitTypeMappings.sql`
- [ ] Run SQL migrations: `ANCClinicVisitMappings.sql`
- [ ] Run SQL migrations: `DiabetesIntakeMappings.sql`
- [ ] Run SQL migrations: `DiabetesIntakeAnswerMappings.sql`
- [ ] Verify constants are set correctly

### Post-Deployment
- [ ] Test Diabetes Intake sync (Bahmni → Avni)
- [ ] Test ANC Clinic Visit sync (Avni → Bahmni)
- [ ] Verify UUIDs are stored in both systems
- [ ] Monitor logs for errors

---

## Patient Identifier Rules

### Avni Storage
```
279731 (no prefix)
```

### Bahmni Storage
```
GAN279731 (with GAN prefix)
```

### Mapping Lookup
- **Avni → Bahmni**: Use Avni identifier (279731), add GAN prefix in query
- **Bahmni → Avni**: Use Bahmni identifier (GAN279731), remove GAN prefix for lookup

---

## Form Field Mapping

### ANC Clinic Visit (68 fields mapped)

**Key Fields**:
- Height, Weight, BMI
- Blood Pressure (Systolic, Diastolic)
- Gestational age, Fetal heart sound
- Edema, Oedema checks
- Referral requirements
- Date of next visit

See [ANCClinicVisitMappings.sql](../integration-data/src/main/resources/db/onetime/ANCClinicVisitMappings.sql) for complete list.

### Diabetes Intake (fields from Bahmni form)

See [DiabetesIntakeMappings.sql](../integration-data/src/main/resources/db/onetime/DiabetesIntakeMappings.sql) for complete list.

---

## Important Behaviors

### Encounter Completion
- Only encounters with `encounter_date_time` are synced
- Scheduled encounters (no date) are filtered out
- This is intentional - prevents incomplete data transfer

### UUID Deduplication
- Avni UUID is stored in Bahmni observations
- Bahmni UUID is stored in Avni form fields
- On re-sync, these UUIDs prevent duplicate creation

### Visit Structure
```
Bahmni Visit (container)
  ├─ Encounter 1 (ANC Clinic Visit observation set)
  ├─ Encounter 2 (another observation set)
  └─ Attributes:
      ├─ Avni UUID: <enrolment-uuid>
      └─ Avni Event Date: <date>
```

---

## Troubleshooting Quick Reference

| Error | Cause | Fix |
|-------|-------|-----|
| "Post failed" when creating visit | Missing visit type mapping | Verify `CommunityEnrolment_VisitType` mapping |
| Encounters not syncing | No encounter date | Set encounter date in Bahmni |
| Patient not found | Identifier mismatch | Check GAN prefix handling |
| "Answer concept not found" | Missing answer mappings | Add answer mappings for coded concepts |
| Duplicate encounters | No UUID tracking | Verify Avni/Bahmni UUID fields exist |

---

## Testing Commands

### Test Bahmni → Avni

```bash
cd /Users/nupoorkhandelwal/Avni/integration-service

# Run Diabetes Intake sync test
JAVA_HOME=/Users/nupoorkhandelwal/Library/Java/JavaVirtualMachines/ms-17.0.16/Contents/Home \
./gradlew :bahmni:test --tests "PatientEncounterEventWorkerExternalTest" \
-Dspring.profiles.active=jss_ganiyari -i
```

### Test Avni → Bahmni

```bash
# Run ANC Clinic Visit sync test
JAVA_HOME=/Users/nupoorkhandelwal/Library/Java/JavaVirtualMachines/ms-17.0.16/Contents/Home \
./gradlew :bahmni:test --tests "ProgramEncounterWorkerExternalTest.debugANCClinicVisitSync" \
-Dspring.profiles.active=jss_ganiyari -i
```

---

## Configuration Files

| File | Purpose | Status |
|------|---------|--------|
| [ANCVisitTypeMappings.sql](../integration-data/src/main/resources/db/onetime/ANCVisitTypeMappings.sql) | Constants, core mappings, visit type mapping | ✅ Configured |
| [ANCClinicVisitMappings.sql](../integration-data/src/main/resources/db/onetime/ANCClinicVisitMappings.sql) | Encounter type, form, observation mappings | ✅ Configured |
| [DiabetesIntakeMappings.sql](../integration-data/src/main/resources/db/onetime/DiabetesIntakeMappings.sql) | Encounter type, form, observation mappings | ✅ Configured |
| [DiabetesIntakeAnswerMappings.sql](../integration-data/src/main/resources/db/onetime/DiabetesIntakeAnswerMappings.sql) | Coded concept answer mappings | ✅ Configured |

---

## Adding New Programs

To add a new program (e.g., Maternal Health):

1. **Create Encounter Type Mapping**:
   ```sql
   INSERT INTO mapping_metadata (...)
   VALUES ('<BAHMNI_MATERNAL_ENCOUNTER_UUID>', 'Maternal Health', ...);
   ```

2. **Create Visit Type Mapping**:
   ```sql
   INSERT INTO mapping_metadata (...)
   VALUES ('2ee11869-f426-44a5-9766-1fb195a1c56f', 'Maternal Health', ...);
   -- Reuse the same visit type for all programs
   ```

3. **Create Observation Mappings**:
   - Map each Bahmni concept UUID to Avni field name
   - Include `data_type_hint` for coded concepts (e.g., 'Coded', 'Numeric', 'Date')

4. **Create Avni Form**:
   - Add "Bahmni Entity UUID" field first
   - Mark all fields readOnly
   - Match structure to Bahmni form

5. **Update Bahmni Form**:
   - Add "Avni Entity UUID" concept to form's concept set

---

## Current State

### Bahmni
- Location: Avni Integration Location (58638451...)
- Visit Type: Avni Bahmni Visit Type (2ee11869...)
- Provider: apiuser
- Concepts: Avni Entity UUID, Avni Event Date (created)
- Visit Attribute Types: Avni UUID, Avni Event Date (created)
- Forms: ANC Clinic Visit form configured with Avni Entity UUID concept

### Avni
- Subject Type: Individual
- Forms: Diabetes Intake (created), ANC Clinic Visit (created)
- Form Fields: All Bahmni Entity UUID fields added and marked readOnly

---

## Support

For issues or questions:
1. Check [AVNI_BAHMNI_INTEGRATION.md](AVNI_BAHMNI_INTEGRATION.md) for detailed explanation
2. Review configuration files for correct UUIDs
3. Check logs in integration service for error messages
4. Verify both systems have matching UUIDs before syncing

---

## Related Links

- [Avni-Bahmni Integration Documentation](https://avni.readme.io/docs/avni-bahmni-integration-specific#core-mappings)
- [OpenMRS REST API](https://rest.openmrs.org/)
- [Integration Guide](AVNI_BAHMNI_INTEGRATION.md)
