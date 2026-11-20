package org.avni_integration_service.tanuh.model;

import java.util.List;

// Hands a screening that passed QC to the high-risk model service, which scores it on its own time and writes its result
// to Avni itself. The answer says whether the service took the screening; an exception means the hand-over itself failed
// and is tried again.
// edgeVerdict is the phone model's verdict on the case's photos, or null when it is not known.
public interface HighRiskModelClient {
    Submission submit(String caseId, List<String> imageUris, String edgeVerdict) throws HighRiskModelException;
}
