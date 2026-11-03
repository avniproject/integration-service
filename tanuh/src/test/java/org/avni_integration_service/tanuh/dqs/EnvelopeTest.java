package org.avni_integration_service.tanuh.dqs;

import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.tanuh.domain.TanuhConceptsFixture;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class EnvelopeTest {
    private static String villageIdOf(Object properties) {
        Subject p = new Subject();
        if (properties != null) p.set("Location Properties", properties);
        return Envelope.villageId(p, TanuhConceptsFixture.defaults());
    }

    // The server may answer a Numeric attribute as a whole number, a decimal or text; the code is the same.
    @Test
    public void theLgdCodeIsSentAsTheWholeNumberItIs() {
        assertEquals("625529", villageIdOf(Map.of("LGD code", 625529)));
        assertEquals("625529", villageIdOf(Map.of("LGD code", 625529.0)));
        assertEquals("625529", villageIdOf(Map.of("LGD code", " 625529 ")));
    }

    @Test
    public void aVillageWithoutAnLgdCodeHasNoVillageId() {
        assertNull(villageIdOf(null), "the village carries no attributes");
        assertNull(villageIdOf(Map.of("Census code", 123)), "the village carries others");
        assertNull(villageIdOf(Map.of("LGD code", 6255.29)), "not a code");
        assertNull(villageIdOf(Map.of("LGD code", "unknown")));
    }
}
