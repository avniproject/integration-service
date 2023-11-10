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

    public static final String STUDENT_FETCH_QUERY = """
            select c.phone, fr.results, s.inserted_at
            from `glific-lms-lahi.918956411022.contacts` c, UNNEST(c.fields) AS s
            join `glific-lms-lahi.918956411022.flow_results` fr
            on fr.contact_phone = c.phone
            WHERE
            (s.label, s.value) = ('avni_reg_complete', 'Yes')
            AND
            fr.name = 'Avni Students Registrations Flow'
            AND
            fr.id = @flowResultId
            order by s.inserted_at desc
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
        String fetchTime = integratingEntityStatusRepository.findByEntityType(ENTITYTYPE).getReadUptoDateTime().toString();
        Iterator<FlowResult> results = bigQueryClient.getResults(BULK_FETCH_QUERY, fetchTime, LIMIT, new FlowResultMapper());
        return new Students(results);
    }

    public Student getStudent() {
        return null;
    }
}
