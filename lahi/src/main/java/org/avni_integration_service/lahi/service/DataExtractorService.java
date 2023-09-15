package org.avni_integration_service.lahi.service;

import com.google.cloud.bigquery.*;
import org.avni_integration_service.lahi.config.BigQueryConnector;
import org.avni_integration_service.lahi.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DataExtractorService {

    private final BigQueryConnector bigQueryConnector;

    public DataExtractorService(BigQueryConnector bigQueryConnector) {
        this.bigQueryConnector = bigQueryConnector;
    }

    public TableResult queryToBigQuery(String sqlQuery) throws InterruptedException {

        QueryJobConfiguration queryConfig =
                QueryJobConfiguration.newBuilder(
                                sqlQuery)
                        .setUseLegacySql(false)
                        .build();

        // Create a job ID so that we can safely retry.
        JobId jobId = JobId.of(UUID.randomUUID().toString());
        Job queryJob = bigQueryConnector.getBigQuery().create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());

        // Wait for the query to complete.
        queryJob = queryJob.waitFor();

        // Check for errors
        if (queryJob == null) {
            throw new RuntimeException("Job no longer exists");
        } else if (queryJob.getStatus().getError() != null) {
            // You can also look at queryJob.getStatus().getExecutionErrors() for all
            // errors, not just the latest one.
            throw new RuntimeException(queryJob.getStatus().getError().toString());
        }

        // Get the results.
        TableResult result = queryJob.getQueryResults();
        return result;



    }
}
