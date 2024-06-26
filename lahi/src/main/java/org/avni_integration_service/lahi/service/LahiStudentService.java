package org.avni_integration_service.lahi.service;

import org.apache.log4j.Logger;
import org.avni_integration_service.glific.bigQuery.BigQueryClient;
import org.avni_integration_service.glific.bigQuery.domain.FlowResult;
import org.avni_integration_service.glific.bigQuery.mapper.FlowResultMapper;
import org.avni_integration_service.integration_data.repository.IntegratingEntityStatusRepository;
import org.avni_integration_service.lahi.domain.Student;
import org.avni_integration_service.lahi.domain.Students;
import org.springframework.stereotype.Service;

import java.util.Iterator;

@Service
public class LahiStudentService {
    public static final String STUDENT_ENTITY_TYPE = "Student";
    private final BigQueryClient bigQueryClient;
    private final IntegratingEntityStatusRepository integratingEntityStatusRepository;

    public static final String BULK_FETCH_QUERY = """
            select fr.contact_phone, fr.results, fr.id as flowresult_id, fr.inserted_at, fr.updated_at
            from `glific-lms-lahi.918956411022.flow_results` fr
            WHERE
            fr.name = 'Avni Students Registrations Flow'
            AND
            fr.updated_at >= @updated_at
            order by fr.updated_at
            limit @limit_count
            offset 0
            """;

    public static final String STUDENT_FETCH_QUERY = """
            select fr.contact_phone, fr.results, fr.id as flowresult_id, fr.inserted_at, fr.updated_at
            from `glific-lms-lahi.918956411022.flow_results` fr
            WHERE
            fr.name = 'Avni Students Registrations Flow'
            AND
            fr.id = @flowResultId
            limit 1
            """;

    public static final int LIMIT = 1000;
    private static final Logger logger = Logger.getLogger(LahiStudentService.class);

    public LahiStudentService(BigQueryClient bigQueryClient,
                              IntegratingEntityStatusRepository integratingEntityStatusRepository) {
        this.bigQueryClient = bigQueryClient;
        this.integratingEntityStatusRepository = integratingEntityStatusRepository;
    }

    public Students getStudents() {
        String fetchTime = integratingEntityStatusRepository.find(STUDENT_ENTITY_TYPE).getReadUptoDateTime().toString();
        logger.info(String.format("Getting students since %s", fetchTime));
        Iterator<FlowResult> results = bigQueryClient.getResults(BULK_FETCH_QUERY, fetchTime, LIMIT, new FlowResultMapper());
        return new Students(results);
    }

    public Student getStudent(String id) {
        FlowResult flowResult = bigQueryClient.getResult(STUDENT_FETCH_QUERY, new FlowResultMapper(), "flowResultId", Long.parseLong(id));
        return new Student(flowResult);
    }
}
