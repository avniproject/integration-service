package org.avni_integration_service.tanuh.dqs;

import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.tanuh.domain.TanuhConceptsFixture;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.avni_integration_service.tanuh.dqs.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;

public class RegistrationRecordTest {
    // A patient as /api/subject answers for one: the standard fields among the observations, the form's own beside them.
    private static Subject registered() {
        Subject p = new Subject();
        p.setUuid("patient-1");
        p.setVoided(false);
        p.set("Registration date", "2026-07-03");
        p.set("Location ID", "village-uuid-1");
        p.set("Location Properties", Map.of("LGD code", 625529));
        p.set("Registration Coordinates", Map.of("x", 13.0712, "y", 77.7981));
        p.addObservation("First name", "Lakshmi");
        p.addObservation("Last name", "Devi");
        p.addObservation("Date of birth", "1980-10-07");
        p.addObservation("Gender", "Female");
        p.addObservation("Full Address", "12, 3rd Cross, Begur");
        Map<String, Object> audit = new HashMap<>();
        audit.put("Created at", "2026-07-03T09:43:32.049Z");
        audit.put("Created by", WORKER);
        audit.put("Last modified at", "2026-07-03T09:43:32.049Z");
        audit.put("Last modified by", WORKER);
        p.set("audit", audit);
        return p;
    }

    @Test
    public void thePatientIsBothTheRecordAndTheBeneficiary() {
        Map<String, Object> record = RegistrationRecord.from(screening("s1"), registered(), TanuhConceptsFixture.defaults());

        assertEquals("patient-1", record.get("record_id"));
        assertEquals("patient-1", record.get("beneficiary_id"));
        assertEquals(WORKER, record.get("worker_id"));
        assertEquals("625529", record.get("village_id"));
        assertEquals(new BigDecimal("13.0712"), record.get("gps_lat"));
        assertEquals(new BigDecimal("77.7981"), record.get("gps_lon"));
    }

    // The registration goes with a screening, so its times are the screening's; the day it was registered is its own.
    @Test
    public void theTimesAreTheScreeningsAndTheRegistrationKeepsItsDay() {
        Map<String, Object> record = RegistrationRecord.from(screening("s1"), registered(), TanuhConceptsFixture.defaults());

        assertEquals(VISIT_AT, record.get("visit_at"));
        assertFalse(record.containsKey("visit_date"), "the service derives it from visit_at");
        assertEquals("2026-07-03", record.get("registered_on"));
        assertEquals(CREATED_AT, record.get("recorded_at"));
        assertEquals(MODIFIED_AT, record.get("received_at"));
    }

    @Test
    public void thePersonIsDescribedAsTheServiceReadsThem() {
        Map<String, Object> record = RegistrationRecord.from(screening("s1"), registered(), TanuhConceptsFixture.defaults());

        assertEquals(45, record.get("age"), "the age on the day of registration, the day before the 46th birthday");
        assertEquals("female", record.get("sex"));
        assertEquals("1980-10-07", record.get("date_of_birth"));
        assertEquals("Lakshmi Devi", record.get("full_name"));
        assertEquals("12, 3rd Cross, Begur", record.get("full_address"));
    }

    @Test
    public void whatThePatientDoesNotSayIsLeftOut() {
        Subject p = new Subject();
        p.setUuid("patient-2");
        p.addObservation("Gender", "Unknown");
        p.addObservation("Date of birth", "07/10/1980");

        Map<String, Object> record = RegistrationRecord.from(new GeneralEncounter(), p, TanuhConceptsFixture.defaults());

        assertEquals(Map.of("record_id", "patient-2", "beneficiary_id", "patient-2"), record);
    }
}
