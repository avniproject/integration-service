package org.avni_integration_service.tanuh.model;

import org.avni_integration_service.tanuh.config.TanuhConfig;

// A Google ID token for Tanuh's model service.
public interface IdTokenSource {
    // A token that is valid now, from cache when the last one has not expired.
    String idToken(TanuhConfig config) throws HighRiskModelException;

    // A fresh token, for when the service refused the last one.
    String renewedIdToken(TanuhConfig config) throws HighRiskModelException;
}
