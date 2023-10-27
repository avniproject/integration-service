package org.avni_integration_service.lahi.service;

import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.integration_data.repository.MappingMetaDataRepository;
import org.avni_integration_service.lahi.config.LahiMappingDbConstants;
import org.avni_integration_service.lahi.domain.LahiStudent;
import org.avni_integration_service.lahi.domain.StudentConstants;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
public class StudentMapper extends LahiMapper {
    public StudentMapper(MappingMetaDataRepository mappingMetaDataRepository) {
        super(mappingMetaDataRepository);
    }

    public void mapToSubject(Subject subject, LahiStudent student) {
        this.populateObservations(subject, student, LahiMappingDbConstants.MAPPINGGROUP_STUDENT);
        Map<String, Object> observations = subject.getObservations();
        LahiMappingDbConstants.DEFAUL_STUDENT_OBSVALUE_MAP.forEach(observations::put);
        setOtherAddress(subject, student);
        setPhoneNumber(subject, student);
    }

    private void setOtherAddress(Subject subject, LahiStudent student) {
        Map<String, Object> subjectObservations = subject.getObservations();
        Map<String, Object> studentResponse = student.getResponse();
        StringBuilder stringBuilder = new StringBuilder();
        setAddressString(stringBuilder, (String) studentResponse.getOrDefault(StudentConstants.STATE, ""));
        setAddressString(stringBuilder, (String) studentResponse.getOrDefault(StudentConstants.OTHER_STATE, ""));
        setAddressString(stringBuilder, (String) studentResponse.getOrDefault(StudentConstants.DISTRICT, ""));
        setAddressString(stringBuilder, (String) studentResponse.getOrDefault(StudentConstants.CITY_NAME, ""));
        setAddressString(stringBuilder, (String) studentResponse.getOrDefault(StudentConstants.SCHOOL, ""));
        if (stringBuilder.length() > 0) {
            subjectObservations.put("Other School name", stringBuilder.toString());
        }
    }

    private void setAddressString(StringBuilder stringBuilder, String string) {
        if (string != null && !string.equals("")) {
            stringBuilder.append(string + " ");
        }
    }

    private void setPhoneNumber(Subject subject, LahiStudent student) {
        Map<String, Object> subjectObservations = subject.getObservations();
        String contactPhoneNumber = null;
        if (student.getValue(StudentConstants.STUDENT_CONTACT_NUMBER) != null
                && student.getValue(StudentConstants.STUDENT_CONTACT_NUMBER).toString().length() == 12) {
            contactPhoneNumber = (String) student.getValue(StudentConstants.STUDENT_CONTACT_NUMBER);
            contactPhoneNumber = contactPhoneNumber.substring(2);
            subjectObservations.put(LahiMappingDbConstants.CONTACT_PHONE_NUMBER, contactPhoneNumber);
        }
        setAlternatePhoneNumber(student, subjectObservations, contactPhoneNumber);
    }

    private void setAlternatePhoneNumber(LahiStudent student, Map<String, Object> subjectObservations, String contactPhoneNumber) {
        //todo if alternateContactNo
        // a. is a valid number and 12 or 10 digit number then set to 10 digit number
        // b. else set to CONTACT_PHONE_NUMBER
        Long alternatePhoneNumber = null;
        String alternateNumber = (String) student.getValue(StudentConstants.ALTERNATE_NUMBER);
        if (StringUtils.hasText(alternateNumber) && alternateNumber.length() == 12) {
            alternateNumber = alternateNumber.substring(2);
        }
        try {
            alternatePhoneNumber = Long.parseLong((StringUtils.hasText(alternateNumber) && alternateNumber.length() == 10) ?
                    alternateNumber : contactPhoneNumber);
            subjectObservations.put(LahiMappingDbConstants.ALTERNATE_PHONE_NUMBER, alternatePhoneNumber);
        } catch (NumberFormatException nfe) {
            // TODO: 22/09/23
        }
    }
}
