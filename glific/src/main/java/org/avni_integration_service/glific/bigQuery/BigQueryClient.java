package org.avni_integration_service.glific.bigQuery;

import com.google.cloud.bigquery.*;
import org.apache.log4j.Logger;
import org.avni_integration_service.glific.bigQuery.config.BigQueryConnector;
import org.avni_integration_service.glific.bigQuery.mapper.BigQueryResultMapper;
import org.avni_integration_service.glific.bigQuery.mapper.BigQueryResultsMapper;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.UUID;

@Component
public class BigQueryClient {
    private final BigQueryConnector bigQueryConnector;
    private static final Logger logger = Logger.getLogger(BigQueryClient.class);

    public BigQueryClient(BigQueryConnector bigQueryConnector) {
        this.bigQueryConnector = bigQueryConnector;
    }

    public <T> Iterator<T> getResults(String query, String date, int limit, BigQueryResultMapper<T> resultMapper) {
        QueryJobConfiguration queryConfig =
                QueryJobConfiguration.newBuilder(query)
                        .addNamedParameter("updated_at", QueryParameterValue.string(date))
                        .addNamedParameter("limit_count", QueryParameterValue.int64(limit))
                        .build();
        TableResult tableResult = run(queryConfig);
        return new BigQueryResultsMapper<T>().map(tableResult, resultMapper);
    }

    private TableResult run(QueryJobConfiguration queryJobConfiguration) {
        try {
            JobId jobId = JobId.of(UUID.randomUUID().toString());
            Job queryJob = bigQueryConnector.getBigQuery().create(JobInfo.newBuilder(queryJobConfiguration).setJobId(jobId).build());
            queryJob = queryJob.waitFor();

            if (queryJob == null) {
                logger.info("query job is null");
                throw new RuntimeException("Job no longer exists");
            } else if (queryJob.getStatus().getError() != null) {
                // You can also look at queryJob.getStatus().getExecutionErrors() for all
                // errors, not just the latest one.
                logger.info(queryJob.getStatus().getError().toString());
                throw new RuntimeException(queryJob.getStatus().getError().toString());
            }

            return queryJob.getQueryResults();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
