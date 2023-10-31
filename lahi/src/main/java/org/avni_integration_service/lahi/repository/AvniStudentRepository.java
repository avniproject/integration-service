package org.avni_integration_service.lahi.repository;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.avni.repository.AvniSubjectRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AvniStudentRepository {
    private static final Logger logger = Logger.getLogger(AvniStudentRepository.class);
    private final AvniSubjectRepository avniSubjectRepository;

    public AvniStudentRepository(AvniSubjectRepository avniSubjectRepository) {
        this.avniSubjectRepository = avniSubjectRepository;
    }

    public void addSubject(Subject subject) {
        avniSubjectRepository.create(subject);
    }

    public List<Subject> findMatchingStudents(Subject subject) {
        return new ArrayList<>();
    }
}
