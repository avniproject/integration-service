package org.avni_integration_service.lahi.service;

import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.lahi.repository.AvniStudentRepository;
import org.springframework.stereotype.Service;

@Service
public class AvniStudentService {
    private final AvniStudentRepository avniStudentRepository;

    public AvniStudentService(AvniStudentRepository avniStudentRepository) {
        this.avniStudentRepository = avniStudentRepository;
    }

    public void saveStudent(Subject subject) {
        avniStudentRepository.findMatchingStudents(subject);
        avniStudentRepository.addSubject(subject);
    }
}
