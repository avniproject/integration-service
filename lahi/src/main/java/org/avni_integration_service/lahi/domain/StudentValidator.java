package org.avni_integration_service.lahi.domain;

import org.apache.log4j.Logger;
import org.avni_integration_service.lahi.service.DataExtractorService;
import org.avni_integration_service.lahi.util.DateTimeUtil;
import org.springframework.stereotype.Component;


import java.time.LocalDate;
import java.time.Period;
import java.util.Map;

@Component
public class StudentValidator {
    private static final Logger logger = Logger.getLogger(DataExtractorService.class);
    public boolean validateMandatoryField(Map<String,Object> map){
        long count =  StudentConstants.MandatoryField.stream().filter(field->{
            if(map.getOrDefault(field,null)==null){
                logger.error(String.format("%s missing for id:%s",field,map.get("id")));
                return false;
            }
            return true;
        }).count();
        return count == 0;
    }

    public boolean checkAge(String dateOfBirth){
        if(dateOfBirth == null){
            return false;
        }
        return Period.between(DateTimeUtil.dob(dateOfBirth), LocalDate.now()).getYears() > 14;
    }
}
