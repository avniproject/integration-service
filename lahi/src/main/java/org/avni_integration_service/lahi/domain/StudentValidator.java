package org.avni_integration_service.lahi.domain;

import org.apache.log4j.Logger;
import org.avni_integration_service.glific.bigQuery.BigQueryClient;
import org.avni_integration_service.lahi.util.DateTimeUtil;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.Map;

@Component
public class StudentValidator {
    private static final Logger logger = Logger.getLogger(BigQueryClient.class);

    public void validateMandatoryField(Map<String, Object> map) {
        long count = LahiStudentConstants.MandatoryFields.stream().filter(field -> {
            if (map.getOrDefault(field, null) == null) {
                logger.error(String.format("%s missing for id:%s", field, map.get("id")));
                return false;
            }
            return true;
        }).count();
        if (count != 0) {
            throw new RuntimeException("Mandatory field for avni not found");
        }
    }

    public void checkAge(String dateOfBirth) {
        boolean isNotValid = false;
        if (dateOfBirth == null) {
            isNotValid = true;
        }
        if (Period.between(DateTimeUtil.dob(dateOfBirth), LocalDate.now()).getYears() < 14) {
            isNotValid = true;
        }
        if (isNotValid) {
            throw new RuntimeException("Invalid age value specified");
        }
    }
}
