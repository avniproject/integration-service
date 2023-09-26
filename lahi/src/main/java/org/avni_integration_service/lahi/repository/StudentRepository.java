package org.avni_integration_service.lahi.repository;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.avni.repository.AvniSubjectRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StudentRepository {
    //TODO rename to more appropriate name (StudentRepository)

    private static final Logger logger = Logger.getLogger(StudentRepository.class);

     private final AvniSubjectRepository avniSubjectRepository;
    public StudentRepository(AvniSubjectRepository avniSubjectRepository) {
        this.avniSubjectRepository = avniSubjectRepository;
    }




    public void insert(List<Subject> list){
            logger.info("inserting record !!!!!");
            //todo 1. handle error during create for one subject//
            //todo 2. Should we stop processing or proceed to next student, design decision.?
            list.forEach(avniSubjectRepository::create);

    }
}
