package org.avni_integration_service.amrit.worker;

import org.avni_integration_service.integration_data.domain.IntegratingEntityStatus;
import org.avni_integration_service.integration_data.domain.error.ErrorRecord;

import java.util.Date;

public interface ErrorRecordWorker {

    /**
     * 
     * @param status
     * @return EffectiveCutoffDateTime
     */
    default Date getEffectiveCutoffDateTime(IntegratingEntityStatus status) {
        return status.getReadUptoDateTime();
    }

    void processError(ErrorRecord errorRecord);
}
