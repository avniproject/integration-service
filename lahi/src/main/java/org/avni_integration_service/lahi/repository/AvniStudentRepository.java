package org.avni_integration_service.lahi.repository;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.avni.repository.AvniSubjectRepository;
import org.springframework.stereotype.Component;

@Component
public class AvniStudentRepository {
    private static final Logger logger = Logger.getLogger(AvniStudentRepository.class);
    private final AvniSubjectRepository avniSubjectRepository;

    public AvniStudentRepository(AvniSubjectRepository avniSubjectRepository) {
        this.avniSubjectRepository = avniSubjectRepository;
    }

    public void addSubject(Subject subject) {
        //todo 1. handle error during create for one subject//
        //todo 2. Should we stop processing or proceed to next student, design decision.?
        avniSubjectRepository.create(subject);
        logger.info("record inserted successfully");
    }
}
