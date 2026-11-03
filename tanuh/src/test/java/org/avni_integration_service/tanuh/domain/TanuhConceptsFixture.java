package org.avni_integration_service.tanuh.domain;

import com.fasterxml.jackson.databind.JsonNode;
import org.avni_integration_service.util.ObjectJsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

// The names as docs/tanuh-mapping-bundle carries them, so the tests read the very rows a new organisation imports.
public final class TanuhConceptsFixture {
    private static final String BUNDLE_ROWS = "docs/tanuh-mapping-bundle/mappingMetadata.json";
    private static List<TanuhConcepts.Mapping> rows;
    private static TanuhConcepts defaults;

    private TanuhConceptsFixture() {
    }

    public static synchronized List<TanuhConcepts.Mapping> bundleRows() {
        if (rows == null) {
            List<TanuhConcepts.Mapping> read = new ArrayList<>();
            for (JsonNode row : bundle()) read.add(new TanuhConcepts.Mapping(row.path("intSystemValue").asText(), row.path("avniValue").asText()));
            rows = List.copyOf(read);
        }
        return rows;
    }

    public static synchronized TanuhConcepts defaults() {
        if (defaults == null) defaults = TanuhConcepts.from(bundleRows());
        return defaults;
    }

    // The bundle's rows with some changed, as an organisation could edit them in the admin app.
    public static TanuhConcepts editedBy(UnaryOperator<List<TanuhConcepts.Mapping>> edit) {
        return TanuhConcepts.from(edit.apply(new ArrayList<>(bundleRows())));
    }

    private static JsonNode bundle() {
        Path dir = Paths.get("").toAbsolutePath();
        while (dir != null && !Files.exists(dir.resolve(BUNDLE_ROWS))) dir = dir.getParent();
        if (dir == null) throw new IllegalStateException(BUNDLE_ROWS + " not found above " + Paths.get("").toAbsolutePath());
        try {
            return ObjectJsonMapper.objectMapper.readTree(dir.resolve(BUNDLE_ROWS).toFile());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
