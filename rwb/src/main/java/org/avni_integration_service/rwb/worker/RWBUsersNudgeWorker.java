package org.avni_integration_service.rwb.worker;

import org.apache.log4j.Logger;
import org.avni_integration_service.common.MessageUnprocessableException;
import org.avni_integration_service.common.PlatformException;
import org.avni_integration_service.common.UnknownException;
import org.avni_integration_service.integration_data.repository.ErrorRecordRepository;
import org.avni_integration_service.rwb.service.RwbUserNudgeService;
import org.springframework.stereotype.Component;

@Component
public class RWBUsersNudgeWorker {
    private static final Logger logger = Logger.getLogger(RWBUsersNudgeWorker.class);
    private final RwbUserNudgeService rwbUserNudgeService;
    private final ErrorRecordRepository errorRecordRepository;

    public RWBUsersNudgeWorker(RwbUserNudgeService rwbUserNudgeService, ErrorRecordRepository errorRecordRepository) {
        this.rwbUserNudgeService = rwbUserNudgeService;
        this.errorRecordRepository = errorRecordRepository;
    }

    public void processUsers() {
        rwbUserNudgeService.getUsersThatHaveToReceiveNudge().forEach(userId -> {
            try {
                processUser(userId);
            } catch (PlatformException e) {
                logger.error("Platform level issue. Adding to error record.", e);
            } catch (UnknownException e) {
                logger.error("Unknown error. Adding to error record.", e);
            } catch (MessageUnprocessableException e) {
                logger.warn(String.format("Problem with message. Continue processing. %s", e.getMessage()));
            }
        });

    }

    private void processUser(String userId) throws MessageUnprocessableException, PlatformException, UnknownException {
        rwbUserNudgeService.nudgeUser(userId);
    }
}
