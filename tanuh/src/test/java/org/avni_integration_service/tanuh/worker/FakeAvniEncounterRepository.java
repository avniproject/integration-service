package org.avni_integration_service.tanuh.worker;

import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.domain.GeneralEncountersResponse;
import org.avni_integration_service.avni.repository.AvniEncounterRepository;
import org.avni_integration_service.util.FormatAndParseUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;

// Avni's visit list and PATCH, in memory: rows changed strictly after "from" (and strictly before "now" when one is
// given, read by 0-based offset pages), oldest first by (time, uuid), as the server orders by (time, id); a PATCH
// merges the values sent (a null removes one) and marks the visit last changed by the job user. As on avni-server 18.1,
// a PATCH that changes nothing leaves "last modified" as it was.
class FakeAvniEncounterRepository extends AvniEncounterRepository {
    static final String WORKER = "worker@tanuh_uat_local";

    final Map<String, GeneralEncounter> screenings = new LinkedHashMap<>();
    final List<String> patchedUuids = new ArrayList<>();
    final List<Map<String, Object>> patchBodies = new ArrayList<>();
    int listCalls;
    Integer failOnListCall;
    RuntimeException failReadsWith;
    RuntimeException failListsWith;
    private final String jobUser;
    private long clockMillis;

    FakeAvniEncounterRepository(String jobUser, Date start) {
        this.jobUser = jobUser;
        this.clockMillis = start.getTime();
    }

    GeneralEncounter add(String uuid, Map<String, Object> observations) {
        GeneralEncounter s = new GeneralEncounter();
        s.setUuid(uuid);
        s.setSubjectId("patient-of-" + uuid);
        s.setEncounterType("Oral Screening");
        s.setEncounterDateTime("2026-10-06T10:00:00.000Z");
        s.setVoided(false);
        observations.forEach(s::addObservation);
        touch(s, WORKER);
        screenings.put(uuid, s);
        return s;
    }

    void touch(GeneralEncounter s, String user) {
        clockMillis += 1000;
        stamp(s, user);
    }

    // One millisecond for all of them, as a single SQL statement setting last_modified_date_time = now() gives.
    void stampTogether(Collection<String> uuids) {
        clockMillis += 1000;
        uuids.forEach(uuid -> stamp(screenings.get(uuid), WORKER));
    }

    private void stamp(GeneralEncounter s, String user) {
        Map<String, Object> audit = new HashMap<>();
        audit.put("Last modified at", FormatAndParseUtil.toISODateTimeString(new Date(clockMillis)));
        audit.put("Last modified by", user);
        s.set("audit", audit);
    }

    void workerEdits(String uuid) {
        touch(screenings.get(uuid), WORKER);
    }

    @Override
    public GeneralEncountersResponse getGeneralEncounters(Date lastModifiedDateTime, String encounterType, int pageSize) {
        return list(lastModifiedDateTime, null, encounterType, pageSize, 0);
    }

    @Override
    public GeneralEncountersResponse getGeneralEncounters(Date lastModifiedDateTime, Date now, String encounterType, int pageSize, int pageNumber) {
        return list(lastModifiedDateTime, now, encounterType, pageSize, pageNumber);
    }

    private GeneralEncountersResponse list(Date lastModifiedDateTime, Date now, String encounterType, int pageSize, int pageNumber) {
        listCalls++;
        if (failListsWith != null) throw failListsWith;
        if (failOnListCall != null && listCalls == failOnListCall) {
            failOnListCall = null;
            throw new RuntimeException("connection reset");
        }
        List<GeneralEncounter> rows = screenings.values().stream()
                .filter(s -> encounterType.equals(s.getEncounterType()))
                .filter(s -> s.getLastModifiedDate().after(lastModifiedDateTime))
                .filter(s -> now == null || s.getLastModifiedDate().before(now))
                .sorted(Comparator.comparing(GeneralEncounter::getLastModifiedDate).thenComparing(GeneralEncounter::getUuid))
                .skip((long) pageNumber * pageSize)
                .limit(pageSize)
                .toList();
        GeneralEncountersResponse response = new GeneralEncountersResponse();
        response.setContent(rows.toArray(new GeneralEncounter[0]));
        return response;
    }

    @Override
    public GeneralEncounter getGeneralEncounter(String uuid) {
        if (failReadsWith != null) throw failReadsWith;
        GeneralEncounter s = screenings.get(uuid);
        if (s == null) throw HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, null, null);
        return s;
    }

    @Override
    public GeneralEncounter patch(String uuid, Map<String, Object> observations) {
        patchedUuids.add(uuid);
        patchBodies.add(new HashMap<>(observations));
        GeneralEncounter s = screenings.get(uuid);
        boolean changed = false;
        for (Map.Entry<String, Object> value : observations.entrySet()) {
            if (Objects.equals(s.getObservation(value.getKey()), value.getValue())) continue;
            changed = true;
            if (value.getValue() == null) s.getObservations().remove(value.getKey());
            else s.addObservation(value.getKey(), value.getValue());
        }
        if (changed) touch(s, jobUser);
        return s;
    }
}
