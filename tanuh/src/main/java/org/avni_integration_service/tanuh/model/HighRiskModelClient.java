package org.avni_integration_service.tanuh.model;

import org.avni_integration_service.avni.domain.GeneralEncounter;

import java.io.File;
import java.util.List;

// Hands a screening that passed QC to the high-risk model service. The service works asynchronously and writes its
// result to Avni itself, so nothing comes back; an exception means the hand-over itself failed.
public interface HighRiskModelClient {
    void submit(GeneralEncounter screening, List<File> photos) throws HighRiskModelException;
}
