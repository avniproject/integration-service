package org.avni_integration_service.tanuh.worker;

import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.repository.AvniEncounterRepository;
import org.avni_integration_service.integration_data.domain.IntegratingEntityStatus;
import org.avni_integration_service.integration_data.repository.IntegratingEntityStatusRepository;
import org.avni_integration_service.util.FormatAndParseUtil;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

// Walks one visit type's encounters, oldest first, from the organisation's cursor row, handing each version to the
// handler once per run and saving the cursor at each row's listed time. A handler that throws stops the walk with the
// cursor at the last row it finished, so the next run starts there.
public class EncounterCursorWalker {
    public static final int PAGE_SIZE = 1000;
    private static final long OVERLAP_MILLIS = 5_000;
    // Avni's times are read as the JVM's wall-clock, which can run up to 14 hours ahead of the true instant.
    private static final long MAX_AHEAD_MILLIS = 24L * 60 * 60 * 1000;

    public interface Handler {
        void handle(GeneralEncounter encounter);
    }

    private final AvniEncounterRepository avniEncounterRepository;
    private final IntegratingEntityStatusRepository integratingEntityStatusRepository;
    private final int pageSize;

    public EncounterCursorWalker(AvniEncounterRepository avniEncounterRepository,
                                 IntegratingEntityStatusRepository integratingEntityStatusRepository, int pageSize) {
        this.avniEncounterRepository = avniEncounterRepository;
        this.integratingEntityStatusRepository = integratingEntityStatusRepository;
        this.pageSize = pageSize;
    }

    // The server's lower bound is exclusive, so each read starts five seconds back, and the rows in those seconds
    // are listed again by the next run. A version already handled in this run is not handed over again; one from an
    // earlier run is, and the handler decides. totalElements is a page's own count, never a total.
    public void walk(String cursorEntityType, String encounterType, Handler handler) {
        IntegratingEntityStatus status = integratingEntityStatusRepository.find(cursorEntityType);
        if (status == null || status.getReadUptoDateTime() == null)
            throw new IllegalStateException(String.format("No %s cursor row for this organisation. Run its setup script.", cursorEntityType));
        if (isFarAhead(status.getReadUptoDateTime()))
            throw new IllegalStateException(String.format("The %s cursor is in the future (%s), so no %s would ever be read. Reset it to the last good time.",
                    cursorEntityType, FormatAndParseUtil.toISODateTimeString(status.getReadUptoDateTime()), encounterType));
        walkFrom(status, encounterType, new HashSet<>(), handler);
    }

    private void walkFrom(IntegratingEntityStatus status, String encounterType, Set<String> seen, Handler handler) {
        Date from = new Date(status.getReadUptoDateTime().getTime() - OVERLAP_MILLIS);
        while (true) {
            GeneralEncounter[] page = avniEncounterRepository.getGeneralEncounters(from, encounterType, pageSize).getContent();
            Date last = null;
            for (GeneralEncounter encounter : page) last = process(encounter, seen, status, handler);
            if (page.length < pageSize) return;
            Date next = new Date(last.getTime() - OVERLAP_MILLIS);
            if (next.after(from)) {
                from = next;
                continue;
            }
            // The whole full page lies within five seconds of "from", so time alone cannot move on: the rest of the
            // last row's millisecond would never be listed. One bulk statement gives thousands of rows the same one.
            processMillisecond(last, seen, status, encounterType, handler);
            from = last;
        }
    }

    // Reads one millisecond on its own, by offset pages. A write, or a newer version synced meanwhile, moves a row out
    // of the window and shifts the rest back, so a page is read again while it still holds a row not seen; passes
    // repeat until one finds nothing new.
    private void processMillisecond(Date at, Set<String> seen, IntegratingEntityStatus status, String encounterType, Handler handler) {
        Date after = new Date(at.getTime() - 1);
        Date before = new Date(at.getTime() + 1);
        boolean foundInPass;
        do {
            foundInPass = false;
            int pageNumber = 0;
            while (true) {
                GeneralEncounter[] page = avniEncounterRepository.getGeneralEncounters(after, before, encounterType, pageSize, pageNumber).getContent();
                boolean found = false;
                for (GeneralEncounter encounter : page) {
                    if (seen.contains(versionOf(encounter))) continue;
                    process(encounter, seen, status, handler);
                    found = true;
                }
                foundInPass |= found;
                if (found) continue;
                if (page.length < pageSize) break;
                pageNumber++;
            }
        } while (foundInPass);
    }

    // Returns the row's time as listed, read before a handler's own write can change it.
    private Date process(GeneralEncounter encounter, Set<String> seen, IntegratingEntityStatus status, Handler handler) {
        Date listedAt = encounter.getLastModifiedDate();
        if (isFarAhead(listedAt))
            throw new IllegalStateException(String.format("%s %s was last changed in the future (%s); the cursor stays where it is.",
                    encounter.getEncounterType(), encounter.getUuid(), FormatAndParseUtil.toISODateTimeString(listedAt)));
        // Remembered only once handled: a handler that throws leaves the version for the next run.
        String version = versionOf(encounter);
        if (!seen.contains(version)) {
            handler.handle(encounter);
            seen.add(version);
        }
        if (listedAt.getTime() > status.getReadUptoDateTime().getTime()) {
            status.setReadUptoDateTime(listedAt);
            integratingEntityStatusRepository.save(status);
        }
        return listedAt;
    }

    private static String versionOf(GeneralEncounter encounter) {
        return encounter.getUuid() + "@" + encounter.getLastModifiedDate().getTime();
    }

    private static boolean isFarAhead(Date date) {
        return date.getTime() > System.currentTimeMillis() + MAX_AHEAD_MILLIS;
    }
}
