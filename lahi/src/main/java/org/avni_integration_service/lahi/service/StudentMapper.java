package org.avni_integration_service.lahi.service;

import org.apache.logging.log4j.util.Strings;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.common.PlatformException;
import org.avni_integration_service.integration_data.domain.framework.MappingException;
import org.avni_integration_service.lahi.config.LahiMappingDbConstants;
import org.avni_integration_service.lahi.domain.LahiStudentConstants;
import org.avni_integration_service.lahi.domain.Student;
import org.avni_integration_service.lahi.repository.AvniStudentRepository;
import org.avni_integration_service.lahi.util.DateTimeUtil;
import org.avni_integration_service.common.ObservationMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Map;

@Service
public class StudentMapper implements LahiStudentConstants {
    private final ObservationMapper observationMapper;

    public StudentMapper(ObservationMapper observationMapper) {
        this.observationMapper = observationMapper;
    }

    public Subject mapToSubject(Student lahiStudent) throws PlatformException {
        Subject subject = this.subjectWithoutObservations(lahiStudent);
        try {
            observationMapper.mapObservations(subject, lahiStudent.getObservations(), LahiMappingDbConstants.MAPPING_GROUP_STUDENT, LahiMappingDbConstants.MAPPING_TYPE_OBS);
        } catch (MappingException mappingException) {
            throw new PlatformException(mappingException);
        }

        Map<String, Object> observations = subject.getObservations();
        LahiMappingDbConstants.DEFAULT_STUDENT_OBS_VALUE_MAP.forEach(observations::put);
        setOtherAddress(subject, lahiStudent);
        setPhoneNumber(subject, lahiStudent);
        return subject;
    }

    private void setOtherAddress(Subject subject, Student student) {
        Map<String, Object> subjectObservations = subject.getObservations();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(wrapNulls(student.getInput(LahiStudentConstants.STATE)))
                .append(LahiStudentConstants.STRING_CONSTANT_COMMA)
                .append(wrapNulls(student.getInput(LahiStudentConstants.OTHER_STATE)))
                .append(LahiStudentConstants.STRING_CONSTANT_COMMA)
                .append(wrapNulls(student.getInput(LahiStudentConstants.DISTRICT)))
                .append(LahiStudentConstants.STRING_CONSTANT_COMMA)
                .append(wrapNulls(student.getInput(LahiStudentConstants.CITY_NAME)))
                .append(LahiStudentConstants.STRING_CONSTANT_COMMA)
                .append(wrapNulls(student.getInput(LahiStudentConstants.SCHOOL)));
        if (stringBuilder.length() > 0) {
            subjectObservations.put("Other School name", stringBuilder.toString());
        }
    }

    private String wrapNulls(String input) {
        return input == null ? Strings.EMPTY : input;
    }

    private void setPhoneNumber(Subject subject, Student student) {
        Map<String, Object> subjectObservations = subject.getObservations();
        String contactPhoneNumber = null;
        String contactNumber = student.getContactPhone();
        if (contactNumber != null && contactNumber.length() == 12) {
            contactPhoneNumber = contactNumber.substring(2);
            subjectObservations.put(AvniStudentRepository.CONTACT_PHONE_NUMBER, contactPhoneNumber);
        }
        setAlternatePhoneNumber(student, subjectObservations, contactPhoneNumber);
    }

    private void setAlternatePhoneNumber(Student student, Map<String, Object> subjectObservations, String contactPhoneNumber) {
        Long alternatePhoneNumber;
        String alternateNumber = student.getAlternatePhoneNumber();
        if (StringUtils.hasText(alternateNumber) && alternateNumber.length() == 12) {
            alternateNumber = alternateNumber.substring(2);
        }
        alternatePhoneNumber = Long.parseLong((StringUtils.hasText(alternateNumber) && alternateNumber.length() == 10) ?
                alternateNumber : contactPhoneNumber);
        subjectObservations.put(AvniStudentRepository.ALTERNATE_PHONE_NUMBER, alternatePhoneNumber);
    }

    private Subject subjectWithoutObservations(Student lahiStudent) {
        Subject subject = new Subject();

        String firstName = StringUtils.capitalize(lahiStudent.getFirstName());
        String lastName = StringUtils.capitalize(lahiStudent.getLastName());
        Date registrationDate = DateTimeUtil.toDate(lahiStudent.getDateOfRegistration(), DateTimeUtil.DATE_TIME);
        Date dob = DateTimeUtil.toDate(lahiStudent.getDateOfBirth(), DateTimeUtil.DD_MM_YYYY);
        String gender = lahiStudent.getGender();
        String externalId = lahiStudent.getFlowResultId();

        subject.setExternalId(externalId);
        subject.setAddress(STUDENT_ADDRESS);
        subject.setFirstName(firstName);
        subject.setLastName(lastName);
        subject.setRegistrationDate(registrationDate);
        subject.setDateOfBirth(dob);
        subject.setGender(gender);
        subject.setSubjectType(AvniStudentRepository.STUDENT_SUBJECT_TYPE);
        return subject;
    }
}
