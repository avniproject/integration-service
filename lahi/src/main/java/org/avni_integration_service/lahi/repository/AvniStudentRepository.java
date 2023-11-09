package org.avni_integration_service.lahi.repository;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.avni.repository.AvniSubjectRepository;
import org.avni_integration_service.lahi.util.DateTimeUtil;
import org.avni_integration_service.util.ObjectUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AvniStudentRepository {
    public static final String STUDENT_SUBJECT_TYPE = "Student";
    public static final String CONTACT_PHONE_NUMBER = "Student contact number";
    public static final String ALTERNATE_PHONE_NUMBER = "Alternate (Whatsapp number)";
    public static final String FATHERS_NAME_CONCEPT = "Father's name";

    private static final Logger logger = Logger.getLogger(AvniStudentRepository.class);
    private final AvniSubjectRepository avniSubjectRepository;

    public AvniStudentRepository(AvniSubjectRepository avniSubjectRepository) {
        this.avniSubjectRepository = avniSubjectRepository;
    }

    public void addSubject(Subject subject) {
        avniSubjectRepository.create(subject);
    }

    public List<Subject> findMatchingStudents(Subject subject) {
        LinkedHashMap<String, Object> subjectSearchCriteria = new LinkedHashMap<>();
        subjectSearchCriteria.put(CONTACT_PHONE_NUMBER, subject.getObservation(CONTACT_PHONE_NUMBER));
        Subject[] subjects = avniSubjectRepository.getSubjects(STUDENT_SUBJECT_TYPE, subjectSearchCriteria);
        return Arrays.stream(subjects).filter(x -> ObjectUtil.nullSafeEqualsIgnoreCase(x.getFirstName(), subject.getFirstName())
                && ObjectUtil.nullSafeEqualsIgnoreCase(x.getLastName(), subject.getLastName())
                && ObjectUtil.nullSafeEqualsIgnoreCase(x.getGender(), subject.getGender())
                && ObjectUtil.nullSafeEqualsIgnoreCase(x.getDateOfBirth(), subject.getDateOfBirth())
                && ObjectUtil.nullSafeEqualsIgnoreCase(x.getObservation(FATHERS_NAME_CONCEPT), subject.getObservation(FATHERS_NAME_CONCEPT))

        ).collect(Collectors.toList());
    }
}
