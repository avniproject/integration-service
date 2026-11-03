package org.avni_integration_service.tanuh.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// The names of the Oral Screening's visit type, questions and answers, and of the patient's registration questions, in
// Tanuh's Avni configuration, built from the organisation's mapping_metadata rows (docs/tanuh-mapping-bundle). Read at
// the start of every run.
public final class TanuhConcepts {
    // The flat layout of screenings captured before the repeatable groups has this many photo slots.
    public static final int LEGACY_PHOTO_SLOTS = 8;

    // One row: int_system_value, the job's key, and avni_value, the name in Avni.
    public record Mapping(String key, String name) {
    }

    public enum Concept {
        YES, NO, SUSPICIOUS, NON_SUSPICIOUS, NON_SUSPICIOUS_LEGACY,

        ORAL_SCREENING, TAKE_PHOTOS, GUIDED_PHOTOS, ORAL_IMAGE, SUSPICIOUS_LESION, SCREENING_NOT_REQUIRED_PROCEED,
        ANY_SYMPTOMS, ABLE_TO_OPEN_MOUTH, SEE_ANY_LESIONS, WORKER_OVERRIDES_AI, PRECAPTURE_ACK, SMOKELESS_TOBACCO,
        BIDI_CIGARETTE, ARECA_NUT, ALCOHOL, ALCOHOL_FREQUENCY, PLACE_OF_REFERRAL, HEALTH_WORKER_VERDICT,
        AI_VERDICT_GROUP, AI_VERDICT, AI_VERDICT_LEGACY,
        LEGACY_PHOTO, LEGACY_HEALTH_WORKER_VERDICT, LEGACY_ASHA_VERDICT, LEGACY_AI_VERDICT,

        FULL_ADDRESS, LGD_CODE,

        QC_STATUS, QC_PASS, QC_FAIL, QC_CHECK_TIME;

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private final Map<Concept, String> names;

    private TanuhConcepts(Map<Concept, String> names) {
        this.names = Collections.unmodifiableMap(names);
    }

    // Every fault in the rows is reported at once, so one fix-up covers them all.
    public static TanuhConcepts from(Collection<Mapping> mappings) {
        List<String> faults = new ArrayList<>();
        Map<Concept, String> names = new EnumMap<>(Concept.class);
        Set<Concept> given = EnumSet.noneOf(Concept.class);
        for (Mapping row : mappings) {
            String key = row.key() == null ? "" : row.key().trim();
            String name = row.name() == null ? "" : row.name().trim();
            String where = String.format("'%s'", key);
            Concept concept = conceptOf(key);
            if (concept != null) given.add(concept);
            if (name.isEmpty()) faults.add(where + " has no Avni name");
            else if (concept == null) faults.add(where + " is not a key the job uses");
            else if (names.containsKey(concept)) faults.add(where + " is given twice");
            else if (concept.key().startsWith("legacy_") != name.contains("%d"))
                faults.add(where + (concept.key().startsWith("legacy_") ? " must hold" : " must not hold") + " the photo number as %d");
            else if (concept.key().startsWith("legacy_") && !takesAPhotoNumber(name))
                faults.add(where + " must hold the photo number as one %d and no other %; write a literal % as %%");
            else names.put(concept, name);
        }
        List<String> missing = new ArrayList<>();
        for (Concept concept : Concept.values()) if (!given.contains(concept)) missing.add(concept.key());
        if (!missing.isEmpty()) faults.add("no row for " + String.join(", ", missing));
        if (!faults.isEmpty()) throw new IllegalArgumentException("Tanuh mapping: " + String.join("; ", faults));
        return new TanuhConcepts(names);
    }

    // A legacy name is formatted with each photo's number, so any other format code in it would fail every screening.
    private static boolean takesAPhotoNumber(String name) {
        try {
            String.format(name, 1);
            return name.replace("%%", "").split("%d", -1).length == 2;
        } catch (IllegalFormatException e) {
            return false;
        }
    }

    private static Concept conceptOf(String key) {
        try {
            return Concept.valueOf(key.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String name(Concept concept) {
        return names.get(concept);
    }

    // A name of the flat photo layout, for the photo's number.
    public String name(Concept concept, int slot) {
        return String.format(names.get(concept), slot);
    }
}
