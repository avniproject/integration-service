package org.avni_integration_service.lahi.service;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.avni_integration_service.avni.domain.ObservationHolder;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.integration_data.domain.MappingMetaData;
import org.avni_integration_service.integration_data.domain.framework.MappingException;
import org.avni_integration_service.integration_data.repository.MappingMetaDataRepository;
import org.avni_integration_service.lahi.config.LahiMappingDbConstants;
import org.avni_integration_service.lahi.domain.LahiStudentConstants;
import org.avni_integration_service.lahi.domain.LahiStudent;
import org.avni_integration_service.lahi.util.DateTimeUtil;
import org.avni_integration_service.util.ObsDataType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Map;

@Service
public class StudentMapper implements LahiStudentConstants {
    private static final Logger logger = Logger.getLogger(StudentMapper.class);
    private final MappingMetaDataRepository mappingMetaDataRepository;

    public StudentMapper(MappingMetaDataRepository mappingMetaDataRepository) {
        this.mappingMetaDataRepository = mappingMetaDataRepository;
    }

    public Subject mapToSubject(LahiStudent lahiStudent) {
        Subject subject = this.subjectWithoutObservations(lahiStudent);
        this.populateObservations(subject, lahiStudent);
        Map<String, Object> observations = subject.getObservations();
        LahiMappingDbConstants.DEFAULT_STUDENT_OBS_VALUE_MAP.forEach(observations::put);
        setOtherAddress(subject, lahiStudent);
        setPhoneNumber(subject, lahiStudent);
        return subject;
    }

    private void populateObservations(ObservationHolder observationHolder, LahiStudent student) {
        Map<String, String> observationFields = student.getObservations();
        observationFields.forEach((key, value) -> {
            MappingMetaData mapping = mappingMetaDataRepository.getAvniMappingIfPresent(LahiMappingDbConstants.MAPPING_GROUP_STUDENT, LahiMappingDbConstants.MAPPING_TYPE_OBS, key);
            if (mapping == null) {
                logger.error("Mapping entry not found for observation field: " + key);
                return;
            }
            ObsDataType dataTypeHint = mapping.getDataTypeHint();
            if (dataTypeHint == null)
                observationHolder.addObservation(mapping.getAvniValue(), value);
            else if (dataTypeHint == ObsDataType.Coded && value != null) {
                MappingMetaData answerMapping = mappingMetaDataRepository.getAvniMappingIfPresent(LahiMappingDbConstants.MAPPING_GROUP_STUDENT, LahiMappingDbConstants.MAPPING_TYPE_OBS, value);
                if (answerMapping == null) {
                    String errorMessage = "Answer Mapping entry not found for coded concept answer field: " + value;
                    logger.error(errorMessage);
                    throw new MappingException(errorMessage);
                }
                observationHolder.addObservation(mapping.getAvniValue(), answerMapping.getAvniValue());
            }
        });
    }

    private void setOtherAddress(Subject subject, LahiStudent student) {
        Map<String, Object> subjectObservations = subject.getObservations();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(wrapNulls(student.getInput(LahiStudentConstants.STATE)))
                .append(wrapNulls(student.getInput(LahiStudentConstants.OTHER_STATE)))
                .append(wrapNulls(student.getInput(LahiStudentConstants.DISTRICT)))
                .append(wrapNulls(student.getInput(LahiStudentConstants.CITY_NAME)))
                .append(wrapNulls(student.getInput(LahiStudentConstants.SCHOOL)));
        if (stringBuilder.length() > 0) {
            subjectObservations.put("Other School name", stringBuilder.toString());
        }
    }

    private String wrapNulls(String input) {
        return input == null ? Strings.EMPTY : input;
    }

    private void setPhoneNumber(Subject subject, LahiStudent student) {
        Map<String, Object> subjectObservations = subject.getObservations();
        String contactPhoneNumber = null;
        String contactNumber = student.getContactPhone();
        if (contactNumber != null && contactNumber.length() == 12) {
            contactPhoneNumber = contactNumber.substring(2);
            subjectObservations.put(LahiMappingDbConstants.CONTACT_PHONE_NUMBER, contactPhoneNumber);
        }
        setAlternatePhoneNumber(student, subjectObservations, contactPhoneNumber);
    }

    private void setAlternatePhoneNumber(LahiStudent student, Map<String, Object> subjectObservations, String contactPhoneNumber) {
        Long alternatePhoneNumber;
        String alternateNumber = student.getAlternatePhoneNumber();
        if (StringUtils.hasText(alternateNumber) && alternateNumber.length() == 12) {
            alternateNumber = alternateNumber.substring(2);
        }
        alternatePhoneNumber = Long.parseLong((StringUtils.hasText(alternateNumber) && alternateNumber.length() == 10) ?
                alternateNumber : contactPhoneNumber);
        subjectObservations.put(LahiMappingDbConstants.ALTERNATE_PHONE_NUMBER, alternatePhoneNumber);
    }

    private Subject subjectWithoutObservations(LahiStudent student) {
        Subject subject = new Subject();

        String firstName = StringUtils.capitalize(student.getFirstName());
        String lastName = StringUtils.capitalize(student.getLastName());
        Date registrationDate = DateTimeUtil.registrationDate(student.getDateOfRegistration());
        Date dob = DateTimeUtil.dateOfBirth(student.getDateOfBirth());
        String gender = student.getGender();
        String externalId = student.getFlowResultId();

        subject.setExternalId(externalId);
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
