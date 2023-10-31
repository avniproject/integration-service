package org.avni_integration_service.lahi.service;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.ObservationHolder;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.integration_data.domain.MappingMetaData;
import org.avni_integration_service.integration_data.domain.framework.MappingException;
import org.avni_integration_service.integration_data.repository.MappingMetaDataRepository;
import org.avni_integration_service.lahi.config.LahiMappingDbConstants;
import org.avni_integration_service.lahi.domain.LahiEntity;
import org.avni_integration_service.lahi.domain.LahiStudent;
import org.avni_integration_service.lahi.domain.StudentConstants;
import org.avni_integration_service.lahi.util.DateTimeUtil;
import org.avni_integration_service.util.ObsDataType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class StudentMapper implements StudentConstants {
    private static final Logger logger = Logger.getLogger(StudentMapper.class);
    private final MappingMetaDataRepository mappingMetaDataRepository;

    public StudentMapper(MappingMetaDataRepository mappingMetaDataRepository) {
        this.mappingMetaDataRepository = mappingMetaDataRepository;
    }

    public Subject mapToSubject(LahiStudent lahiStudent) {
        Subject subject = this.subjectWithoutObservations(lahiStudent.getResponse());
        this.populateObservations(subject, lahiStudent, LahiMappingDbConstants.MAPPINGGROUP_STUDENT);
        Map<String, Object> observations = subject.getObservations();
        LahiMappingDbConstants.DEFAUL_STUDENT_OBSVALUE_MAP.forEach(observations::put);
        setOtherAddress(subject, lahiStudent);
        setPhoneNumber(subject, lahiStudent);
        return subject;
    }

    private void populateObservations(ObservationHolder observationHolder, LahiEntity lahiEntity, String mappingGroup) {
        List<String> observationFields = lahiEntity.getObservationFields();
        for (String obsField : observationFields) {
            MappingMetaData mapping = mappingMetaDataRepository.getAvniMappingIfPresent(mappingGroup, LahiMappingDbConstants.MAPPINGTYPE_OBS, obsField, 5);
            if(mapping == null) {
                logger.error("Mapping entry not found for observation field: " + obsField);
                continue;
            }
            ObsDataType dataTypeHint = mapping.getDataTypeHint();
            if (dataTypeHint == null)
                observationHolder.addObservation(mapping.getAvniValue(), lahiEntity.getValue(obsField));
            else if (dataTypeHint == ObsDataType.Coded && lahiEntity.getValue(obsField) != null) {
                MappingMetaData answerMapping = mappingMetaDataRepository.getAvniMappingIfPresent(mappingGroup, LahiMappingDbConstants.MAPPINGTYPE_OBS, lahiEntity.getValue(obsField).toString(), 5);
                if(answerMapping == null) {
                    String errorMessage = "Answer Mapping entry not found for coded concept answer field: " + obsField;
                    logger.error(errorMessage);
                    throw new MappingException(errorMessage);
                }
                observationHolder.addObservation(mapping.getAvniValue(), answerMapping.getAvniValue());
            }
        }
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
            contactPhoneNumber = ((String) student.getValue(StudentConstants.STUDENT_CONTACT_NUMBER)).substring(2);
            subjectObservations.put(LahiMappingDbConstants.CONTACT_PHONE_NUMBER, contactPhoneNumber);
        }
        setAlternatePhoneNumber(student, subjectObservations, contactPhoneNumber);
    }

    private void setAlternatePhoneNumber(LahiStudent student, Map<String, Object> subjectObservations, String contactPhoneNumber) {
        Long alternatePhoneNumber;
        String alternateNumber = (String) student.getValue(StudentConstants.ALTERNATE_NUMBER);
        if (StringUtils.hasText(alternateNumber) && alternateNumber.length() == 12) {
            alternateNumber = alternateNumber.substring(2);
        }
        alternatePhoneNumber = Long.parseLong((StringUtils.hasText(alternateNumber) && alternateNumber.length() == 10) ?
                alternateNumber : contactPhoneNumber);
        subjectObservations.put(LahiMappingDbConstants.ALTERNATE_PHONE_NUMBER, alternatePhoneNumber);
    }

    private Subject subjectWithoutObservations(Map<String, Object> response) {
        Subject subject = new Subject();

        String firstName = StringUtils.capitalize(response.get(FIRST_NAME).toString());
        String lastName = StringUtils.capitalize(response.get(LAST_NAME).toString());
        Date registrationDate = DateTimeUtil.registrationDate(response.get(DATE_OF_REGISTRATION).toString());
        Date dob = DateTimeUtil.dateOfBirth(response.get(DATE_OF_BIRTH).toString());
        String gender = response.get(GENDER).toString();
        String external_id = response.get(FLOWRESULT_ID).toString();

        subject.setExternalId(external_id);
        subject.setAddress(STUDENT_ADDRESS);
        subject.setFirstName(firstName);
        subject.setLastName(lastName);
        subject.setRegistrationDate(registrationDate);
        subject.setDateOfBirth(dob);
        subject.setGender(gender);
        subject.setSubjectType("Student");
        return subject;
    }
}
