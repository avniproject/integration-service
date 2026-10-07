package org.avni_integration_service.tanuh.model;

import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.tanuh.domain.ModelResult;

import java.io.File;
import java.util.List;

public interface HighRiskModelClient {
    ModelResult score(GeneralEncounter screening, List<File> photos) throws HighRiskModelException;
}
