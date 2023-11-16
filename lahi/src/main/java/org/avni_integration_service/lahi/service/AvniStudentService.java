package org.avni_integration_service.lahi.service;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.common.MessageUnprocessableException;
import org.avni_integration_service.common.UnknownException;
import org.avni_integration_service.lahi.repository.AvniStudentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AvniStudentService {
    private final AvniStudentRepository avniStudentRepository;
    private static final Logger logger = Logger.getLogger(AvniStudentService.class);

    public AvniStudentService(AvniStudentRepository avniStudentRepository) {
        this.avniStudentRepository = avniStudentRepository;
    }

    public void saveStudent(Subject subject) throws MessageUnprocessableException, UnknownException {
        List<Subject> matchingStudents = avniStudentRepository.findMatchingStudents(subject);
        if (matchingStudents.size() > 0) {
            logger.warn(String.format("Found %d students with matching details, skipping", matchingStudents.size()));
            throw new MessageUnprocessableException(String.format("Avni has at least one such existing student: %s", subject.getExternalId()));
        }

        try {
            avniStudentRepository.addSubject(subject);
        } catch (Exception e) {
            throw new UnknownException(e);
        }
    }
}
