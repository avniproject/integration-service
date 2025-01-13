package org.avni_integration_service.rwb.worker;

import org.apache.log4j.Logger;
import org.avni_integration_service.common.MessageUnprocessableException;
import org.avni_integration_service.common.PlatformException;
import org.avni_integration_service.common.UnknownException;
import org.avni_integration_service.integration_data.domain.error.ErrorRecord;
import org.avni_integration_service.rwb.config.RwbSendMsgErrorType;
import org.avni_integration_service.rwb.dto.NudgeUserRequestDTO;
import org.avni_integration_service.rwb.service.RwbUserNudgeErrorService;
import org.avni_integration_service.rwb.service.RwbUserNudgeService;
import org.avni_integration_service.rwb.util.DateTimeUtil;
import org.springframework.stereotype.Component;

import java.util.Calendar;

@Component
public class RWBUsersNudgeWorker {
    private static final Logger logger = Logger.getLogger(RWBUsersNudgeWorker.class);
    private final RwbUserNudgeService rwbUserNudgeService;
    private final RwbUserNudgeErrorService rwbUserNudgeErrorService;

    public RWBUsersNudgeWorker(RwbUserNudgeService rwbUserNudgeService, RwbUserNudgeErrorService rwbUserNudgeErrorService) {
        this.rwbUserNudgeService = rwbUserNudgeService;
        this.rwbUserNudgeErrorService = rwbUserNudgeErrorService;
    }

    public void processUsers() {
        rwbUserNudgeService.getUsersThatHaveToReceiveNudge().forEach(nudgeUserRequestDTO -> {
            try {
                processUser(nudgeUserRequestDTO);
            } catch (PlatformException e) {
                logger.error("Platform level issue. Adding to error record.", e);
            } catch (UnknownException e) {
                logger.error("Unknown error. Adding to error record.", e);
            } catch (MessageUnprocessableException e) {
                logger.warn(String.format("Problem with message. Continue processing. %s", e.getMessage()));
            }
        });

    }

    private void processUser(NudgeUserRequestDTO nudgeUserRequestDTO) throws MessageUnprocessableException, PlatformException, UnknownException {
        try {
            ErrorRecord errorRecord = rwbUserNudgeErrorService.getErrorRecord(nudgeUserRequestDTO.getUserId());
            if(errorRecord != null && errorRecord.getLastErrorRecordLog().getErrorType().getName().equals(RwbSendMsgErrorType.Success.name()) &&
                    DateTimeUtil.differenceWithNowLessThanInterval(errorRecord.getLastErrorRecordLog().getLoggedAt(), 7, Calendar.DAY_OF_MONTH)) {
                logger.info(String.format("User has already been nudged successfully in the last 1 week %s", nudgeUserRequestDTO.getUserId()));
                return;
            }
            rwbUserNudgeService.nudgeUser(nudgeUserRequestDTO);
            rwbUserNudgeErrorService.saveUserNudgeSuccess(nudgeUserRequestDTO.getUserId());
        } catch (Exception exception) {
            rwbUserNudgeErrorService.saveUserNudgeError(nudgeUserRequestDTO.getUserId(), exception);
            throw exception;
        }
    }
}
