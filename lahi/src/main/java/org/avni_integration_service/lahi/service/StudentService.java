package org.avni_integration_service.lahi.service;

import org.apache.log4j.Logger;
import org.avni_integration_service.glific.bigQuery.BigQueryClient;
import org.avni_integration_service.integration_data.repository.IntegratingEntityStatusRepository;
import org.avni_integration_service.lahi.domain.LahiStudent;
import org.avni_integration_service.lahi.domain.StudentValidator;
import org.avni_integration_service.lahi.repository.AvniStudentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.avni_integration_service.lahi.domain.StudentConstants.ResultFieldList;

@Service
public class StudentService {
    public static final String ENTITYTYPE = "Student";
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

    public static final int LIMIT = 1000;
    private static final Logger logger = Logger.getLogger(StudentService.class);

    public StudentService(BigQueryClient bigQueryClient,
                          IntegratingEntityStatusRepository integratingEntityStatusRepository) {
        this.bigQueryClient = bigQueryClient;
        this.integratingEntityStatusRepository = integratingEntityStatusRepository;
    }

    public List<LahiStudent> getStudents() {
        String fetchTime = integratingEntityStatusRepository.findByEntityType(ENTITYTYPE).getReadUptoDateTime().toString();
        List<Map<String, Object>> studentsResponse = bigQueryClient.queryWithPagination(BULK_FETCH_QUERY, fetchTime, LIMIT, ResultFieldList);
        logger.info(String.format("%s Data get after fetching from glific", studentsResponse.size()));
        return studentsResponse.stream().map(LahiStudent::new).collect(Collectors.toList());
    }
}
