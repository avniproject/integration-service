package org.avni_integration_service.integration_data.service.error;

import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.error.ErrorRecord;
import org.avni_integration_service.integration_data.domain.error.ErrorType;
import org.avni_integration_service.integration_data.repository.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled
@SpringBootTest(classes = {ErrorClassifier.class, ErrorTypeRepository.class, ErrorRecordRepository.class,
        ErrorRecordLogRepository.class, IntegrationSystemRepository.class})
public class ErrorRecordLogSortTest extends AbstractSpringTest implements ErrorClassifierForGoonjTestConstants {
        private final ErrorRecord errorRecord;
        private final ErrorRecordRepository errorRecordRepository;
        private final ErrorTypeRepository errorTypeRepository;
        private final ErrorRecordLogRepository errorRecordLogRepository;
        private final IntegrationSystem integrationSystem;

        @Autowired
        public ErrorRecordLogSortTest(ErrorRecordRepository errorRecordRepository, ErrorTypeRepository errorTypeRepository,
                                      ErrorRecordLogRepository errorRecordLogRepository, IntegrationSystemRepository integrationSystemRepository) {
                this.errorTypeRepository = errorTypeRepository;
                this.errorRecordLogRepository = errorRecordLogRepository;
                this.integrationSystem = integrationSystemRepository
                        .findBySystemType(IntegrationSystem.IntegrationSystemType.Goonj);
                ErrorType errorType1 = new ErrorType("test1", this.integrationSystem);
                ErrorType errorType2 = new ErrorType("test2", this.integrationSystem);
                ErrorType errorType3 = new ErrorType("test3", this.integrationSystem);
                this.errorTypeRepository.saveAll(Arrays.asList(errorType1, errorType2, errorType3));
                this.errorRecordRepository = errorRecordRepository;
                this.errorRecord = new ErrorRecord();
                this.errorRecord.setIntegratingEntityType("entity");
                this.errorRecord.setEntityId("uuid");
                this.errorRecord.addErrorLog(errorType1, "errorMsg1");
                this.errorRecord.addErrorLog(errorType2, "errorMsg2");
                this.errorRecord.addErrorLog(errorType3, "errorMsg3");
                this.errorRecord.setProcessingDisabled(false);
                this.errorRecord.setIntegrationSystem(this.integrationSystem);
                this.errorRecordRepository.save(errorRecord);
        }

        @Test
        public void sortIntegerArray() {
                List<Integer> ages = Arrays.asList(25, 30, 45, 28, 32);
                assertEquals(45, ages.stream().sorted(Comparator.comparing(Integer::intValue))
                        .reduce((first, second) -> second).orElse(null));
                assertEquals(25, ages.stream().sorted(Comparator.comparing(Integer::intValue).reversed())
                        .reduce((first, second) -> second).orElse(null));
        }


        @Test
        public void testUpdateLoggedAtForLastErrorRecordLog() {
                ErrorType errorType4 = new ErrorType("test4", this.integrationSystem);
                this.errorTypeRepository.save(errorType4);
                final String ERROR_MSG_4 = "errorMsg4";
                this.errorRecord.addErrorLog(errorType4, ERROR_MSG_4);
                ErrorRecord updatedErrorRecord = this.errorRecordRepository.save(errorRecord);
                this.errorRecordLogRepository.saveAll(updatedErrorRecord.getErrorRecordLogs());
                assertEquals(true, updatedErrorRecord.hasThisAsLastErrorTypeAndErrorMessage(errorType4, ERROR_MSG_4));
                updatedErrorRecord.getErrorRecordLogs().stream().forEach(erl -> System.out.println(erl.getId()+" erl: "+erl.getErrorType()+" "+erl.getLoggedAt().toInstant()));
                Date before = updatedErrorRecord.getErrorRecordLogs().stream().filter(erl -> erl.getErrorType().equals(errorType4)).findFirst().get().getLoggedAt();
                updatedErrorRecord.updateLoggedAtForLastErrorRecordLog();
                updatedErrorRecord = errorRecordRepository.save(updatedErrorRecord);
                this.errorRecordLogRepository.saveAll(updatedErrorRecord.getErrorRecordLogs());
                assertEquals(true, updatedErrorRecord.hasThisAsLastErrorTypeAndErrorMessage(errorType4, ERROR_MSG_4));
                Date after = updatedErrorRecord.getErrorRecordLogs().stream().filter(erl -> erl.getErrorType().equals(errorType4)).findFirst().get().getLoggedAt();
                updatedErrorRecord.getErrorRecordLogs().stream().forEach(erl -> System.out.println(erl.getId()+" erl: "+erl.getErrorType()+" "+erl.getLoggedAt().toInstant()));
                assertEquals(true, after.after(before));
        }

}
